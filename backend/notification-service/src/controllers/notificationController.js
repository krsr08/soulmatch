const { getDB } = require('../config/database');
const { getConfigSection, renderTemplate } = require('../../../shared/controlPlane');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const outbox = require('../services/notificationOutboxService');

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
    const delivery = await outbox.queueNotification(db, { userId, title, body, data });
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
    const storedTemplate = await db.query(
      'SELECT title_template, body_template FROM notification_templates WHERE template_key=$1 AND is_active=true LIMIT 1',
      [templateKey]
    );
    const templates = await getConfigSection(db, 'notification_templates');
    const template = storedTemplate.rows[0]
      ? { title: storedTemplate.rows[0].title_template, body: storedTemplate.rows[0].body_template }
      : templates[templateKey];
    if (!template) {
      return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Notification template not found.'));
    }
    const title = renderTemplate(template.title, variables || {});
    const body = renderTemplate(template.body, variables || {});
    const delivery = await outbox.queueNotification(db, { userId, title, body, data });
    res.json({ success:true, data:{ title, body, delivery }, message:'Notification queued' });
  } catch (err) {
    next(err);
  }
};

exports.getDeliveryStats = async (req, res, next) => {
  try {
    const db = await getDB();
    res.json({ success: true, data: await outbox.deliveryStats(db) });
  } catch (err) { next(err); }
};

exports._test = {
  isInvalidFcmTokenError: outbox.isInvalidFcmTokenError
};
