const { getDB } = require('../config/database');
const fcmService = require('../services/fcmService');
const logger = require('../utils/logger');
const { getConfigSection, renderTemplate } = require('../../shared/controlPlane');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');

async function resolveToken(db, userId) {
  const result = await db.query('SELECT fcm_token FROM users WHERE user_id=$1', [userId]);
  return result.rows[0]?.fcm_token;
}

async function deliverPush(db, { userId, title, body, data }) {
  const notification = await db.query(
    `INSERT INTO notifications (notification_id,user_id,title,body,data,status,created_at)
     VALUES (gen_random_uuid(),$1,$2,$3,$4::jsonb,'queued',NOW())
     RETURNING notification_id`,
    [userId, title, body, JSON.stringify(data || {})]
  );
  const notificationId = notification.rows[0].notification_id;
  const fcmToken = await resolveToken(db, userId);
  if (fcmToken) {
    try {
      await fcmService.sendToDevice(fcmToken, title, body, data || {});
      await db.query("UPDATE notifications SET status='sent', delivered_at=NOW() WHERE notification_id=$1", [notificationId]);
      return { notificationId, status: 'sent' };
    } catch (err) {
      await db.query("UPDATE notifications SET status='failed' WHERE notification_id=$1", [notificationId]);
      logger.error('FCM delivery failed for notification ' + notificationId + ': ' + err.message);
      return { notificationId, status: 'failed', error: err.message };
    }
  } else {
    await db.query("UPDATE notifications SET status='no_token' WHERE notification_id=$1", [notificationId]);
    logger.info('No FCM token for user ' + userId);
    return { notificationId, status: 'no_token' };
  }
}

exports.getNotifications = async (req, res, next) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT notification_id AS "notificationId", title, body, data, status, created_at AS "createdAt", read_at AS "readAt"
       FROM notifications
       WHERE user_id=$1
       ORDER BY created_at DESC
       LIMIT 50`,
      [req.user.userId]
    );
    res.json({ success:true, data:result.rows });
  } catch (err) { next(err); }
};
exports.markRead = async (req, res, next) => {
  try {
    const db = await getDB();
    await db.query(
      "UPDATE notifications SET status='read', read_at=COALESCE(read_at,NOW()) WHERE notification_id=$1 AND user_id=$2",
      [req.params.id, req.user.userId]
    );
    res.json({ success:true });
  } catch (err) { next(err); }
};
exports.markAllRead = async (req, res, next) => {
  try {
    const db = await getDB();
    await db.query(
      "UPDATE notifications SET status='read', read_at=COALESCE(read_at,NOW()) WHERE user_id=$1 AND status!='read'",
      [req.user.userId]
    );
    res.json({ success:true });
  } catch (err) { next(err); }
};
exports.registerFcmToken = async (req, res, next) => {
  try {
    const { token } = req.body || {};
    if (!token || String(token).length < 20) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'A valid FCM token is required.'));
    }
    const db = await getDB();
    await db.query('UPDATE users SET fcm_token=$1, updated_at=NOW() WHERE user_id=$2', [String(token), req.user.userId]);
    res.json({ success:true });
  } catch (err) { next(err); }
};
exports.sendPush = async (req, res, next) => {
  try {
    const { userId, title, body, data } = req.body;
    if (!userId || !title || !body) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'userId, title, and body are required.'));
    }
    const db = await getDB();
    const delivery = await deliverPush(db, { userId, title, body, data });
    res.json({ success:true, data: delivery, message:'Notification queued' });
  } catch (err) { next(err); }
};

exports.sendTemplate = async (req, res, next) => {
  try {
    const { userId, templateKey, variables, data } = req.body;
    if (!userId || !templateKey) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'userId and templateKey are required.'));
    }
    const db = await getDB();
    const templates = await getConfigSection(db, 'notification_templates');
    const template = templates[templateKey];
    if (!template) {
      return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Notification template not found.'));
    }
    const title = renderTemplate(template.title, variables || {});
    const body = renderTemplate(template.body, variables || {});
    const delivery = await deliverPush(db, { userId, title, body, data });
    res.json({ success:true, data:{ title, body, delivery }, message:'Notification queued' });
  } catch (err) {
    next(err);
  }
};
