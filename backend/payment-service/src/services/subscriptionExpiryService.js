const database = require('../config/database');
const logger = require('../utils/logger');
const controlPlane = require('../../../shared/controlPlane');

const DEFAULT_INTERVAL_MS = 60 * 60 * 1000;
let started = false;

async function deactivateExpiredSubscriptions() {
  const db = await database.getDB();
  const expired = await db.query(
    `UPDATE subscriptions
     SET is_active=false,
         updated_at=NOW()
     WHERE is_active=true
       AND end_date IS NOT NULL
       AND end_date <= NOW()
     RETURNING subscription_id, user_id, plan_id, end_date`
  );

  for (const row of expired.rows) {
    await controlPlane.recordServerAnalyticsEvent(db, {
      eventType: 'subscription_expired',
      serviceName: 'payment-service',
      userId: row.user_id,
      payload: {
        subscriptionId: row.subscription_id,
        planId: row.plan_id,
        expiredAt: row.end_date
      }
    });
  }

  if (expired.rowCount > 0) {
    logger.info(`Deactivated ${expired.rowCount} expired subscription(s).`);
  }
  return expired.rowCount;
}

function startSubscriptionExpiryJob() {
  if (started || process.env.DISABLE_SUBSCRIPTION_EXPIRY_JOB === 'true') return;
  started = true;
  const intervalMs = Number(process.env.SUBSCRIPTION_EXPIRY_INTERVAL_MS || DEFAULT_INTERVAL_MS);
  deactivateExpiredSubscriptions().catch((error) => logger.error(`Subscription expiry sweep failed: ${error.message}`));
  setInterval(() => {
    deactivateExpiredSubscriptions().catch((error) => logger.error(`Subscription expiry sweep failed: ${error.message}`));
  }, Number.isFinite(intervalMs) && intervalMs > 0 ? intervalMs : DEFAULT_INTERVAL_MS).unref();
}

module.exports = {
  deactivateExpiredSubscriptions,
  startSubscriptionExpiryJob
};
