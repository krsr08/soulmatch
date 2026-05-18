const { getDB } = require('../config/database');
const fcmService = require('./fcmService');
const logger = require('../utils/logger');

const MAX_ATTEMPTS = Number(process.env.NOTIFICATION_OUTBOX_MAX_ATTEMPTS || 3);
const BASE_RETRY_SECONDS = Number(process.env.NOTIFICATION_OUTBOX_BASE_RETRY_SECONDS || 60);

async function resolveToken(db, userId) {
  const result = await db.query('SELECT fcm_token FROM users WHERE user_id=$1', [userId]);
  return result.rows[0]?.fcm_token;
}

function isInvalidFcmTokenError(err) {
  const code = String(err?.code || err?.errorInfo?.code || '').toLowerCase();
  const message = String(err?.message || '').toLowerCase();
  return code.includes('registration-token-not-registered') ||
    code.includes('invalid-registration-token') ||
    message.includes('registration-token-not-registered') ||
    message.includes('invalid registration token');
}

async function queueNotification(db, { userId, title, body, data = {} }) {
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const notification = await client.query(
      `INSERT INTO notifications (notification_id,user_id,title,body,data,status,created_at)
       VALUES (gen_random_uuid(),$1,$2,$3,$4::jsonb,'queued',NOW())
       RETURNING notification_id`,
      [userId, title, body, JSON.stringify(data || {})]
    );
    const notificationId = notification.rows[0].notification_id;
    const outbox = await client.query(
      `INSERT INTO outbox_events (event_type,aggregate_type,aggregate_id,payload,status,available_at,created_at,updated_at)
       VALUES ('push_notification','notification',$1,$2::jsonb,'pending',NOW(),NOW(),NOW())
       RETURNING outbox_event_id`,
      [notificationId, JSON.stringify({ notificationId, userId, title, body, data: data || {} })]
    );
    await client.query('COMMIT');
    return { notificationId, outboxEventId: outbox.rows[0].outbox_event_id, status: 'queued' };
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
}

async function markOutboxCompleted(client, eventId) {
  await client.query(
    "UPDATE outbox_events SET status='completed', updated_at=NOW(), last_error=NULL WHERE outbox_event_id=$1",
    [eventId]
  );
}

async function moveToDlq(client, event, notificationId, error) {
  await client.query(
    `UPDATE outbox_events
     SET status='failed', attempts=$2, last_error=$3, updated_at=NOW()
     WHERE outbox_event_id=$1`,
    [event.outbox_event_id, Number(event.attempts || 0) + 1, error]
  );
  await client.query(
    `INSERT INTO notification_dlq (outbox_event_id,notification_id,payload,error,failed_at)
     VALUES ($1,$2,$3::jsonb,$4,NOW())`,
    [event.outbox_event_id, notificationId || null, JSON.stringify(event.payload || {}), error]
  );
}

async function scheduleRetry(client, event, error) {
  const attempts = Number(event.attempts || 0) + 1;
  const delaySeconds = Math.min(BASE_RETRY_SECONDS * Math.pow(2, attempts - 1), 15 * 60);
  await client.query(
    `UPDATE outbox_events
     SET status='retry',
         attempts=$2,
         last_error=$3,
         available_at=NOW() + ($4 || ' seconds')::interval,
         updated_at=NOW()
     WHERE outbox_event_id=$1`,
    [event.outbox_event_id, attempts, error, String(delaySeconds)]
  );
}

async function deliverOutboxEvent(client, event) {
  const payload = event.payload || {};
  const notificationId = payload.notificationId || event.aggregate_id;
  const userId = payload.userId;
  if (!notificationId || !userId) {
    await moveToDlq(client, event, notificationId, 'Outbox payload missing notificationId or userId.');
    return { status: 'failed' };
  }
  const fcmToken = await resolveToken(client, userId);
  if (!fcmToken) {
    await client.query("UPDATE notifications SET status='no_token' WHERE notification_id=$1", [notificationId]);
    await markOutboxCompleted(client, event.outbox_event_id);
    return { status: 'no_token' };
  }
  try {
    await fcmService.sendToDevice(fcmToken, payload.title, payload.body, payload.data || {});
    await client.query("UPDATE notifications SET status='sent', delivered_at=NOW() WHERE notification_id=$1", [notificationId]);
    await markOutboxCompleted(client, event.outbox_event_id);
    return { status: 'sent' };
  } catch (error) {
    const message = error.message || 'FCM delivery failed.';
    await client.query("UPDATE notifications SET status='failed' WHERE notification_id=$1", [notificationId]);
    if (isInvalidFcmTokenError(error)) {
      await client.query('UPDATE users SET fcm_token=NULL, updated_at=NOW() WHERE user_id=$1 AND fcm_token=$2', [userId, fcmToken]);
      await moveToDlq(client, event, notificationId, message);
      return { status: 'failed', error: message };
    }
    if (Number(event.attempts || 0) + 1 >= MAX_ATTEMPTS) {
      await moveToDlq(client, event, notificationId, message);
      return { status: 'failed', error: message };
    }
    await scheduleRetry(client, event, message);
    return { status: 'retry', error: message };
  }
}

async function processOutboxBatch(limit = 25) {
  const db = await getDB();
  const client = await db.connect();
  const outcomes = [];
  try {
    await client.query('BEGIN');
    const events = await client.query(
      `SELECT *
       FROM outbox_events
       WHERE event_type='push_notification'
         AND status IN ('pending','retry')
         AND available_at <= NOW()
       ORDER BY created_at ASC
       FOR UPDATE SKIP LOCKED
       LIMIT $1`,
      [Math.max(1, Math.min(Number(limit) || 25, 100))]
    );
    for (const event of events.rows) {
      const outcome = await deliverOutboxEvent(client, event);
      outcomes.push({ outboxEventId: event.outbox_event_id, ...outcome });
    }
    await client.query('COMMIT');
    return outcomes;
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
}

function startNotificationWorker() {
  if (process.env.NOTIFICATION_WORKER_DISABLED === 'true') return null;
  const intervalMs = Number(process.env.NOTIFICATION_WORKER_INTERVAL_MS || 10000);
  const tick = async () => {
    try {
      const outcomes = await processOutboxBatch(Number(process.env.NOTIFICATION_WORKER_BATCH_SIZE || 25));
      if (outcomes.length) logger.info(`Notification worker processed ${outcomes.length} event(s).`);
    } catch (error) {
      logger.error(`Notification worker failed: ${error.message}`);
    }
  };
  const timer = setInterval(tick, intervalMs);
  timer.unref?.();
  setTimeout(tick, 1000).unref?.();
  return timer;
}

async function deliveryStats(db) {
  const result = await db.query(
    `SELECT
       COALESCE(n.status, 'unknown') AS status,
       COUNT(*)::int AS total
     FROM notifications n
     WHERE n.created_at >= NOW() - INTERVAL '30 days'
     GROUP BY n.status
     ORDER BY n.status`
  );
  const dlq = await db.query('SELECT COUNT(*)::int AS total FROM notification_dlq WHERE failed_at >= NOW() - INTERVAL \'30 days\'');
  return { statuses: result.rows, dlq30d: dlq.rows[0]?.total || 0 };
}

module.exports = {
  deliveryStats,
  isInvalidFcmTokenError,
  processOutboxBatch,
  queueNotification,
  startNotificationWorker
};
