require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const notifRoutes = require('./routes/notificationRoutes');
const { startNotificationWorker } = require('./services/notificationOutboxService');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');
const logger = require('./utils/logger');
const { buildCorsOptions } = require('./utils/corsOptions');
const { installExpressObservability } = require('../../shared/observability');
const { getDB } = require('./config/database');
const app = express();
app.use(helmet()); app.use(cors(buildCorsOptions())); app.use(express.json());
installExpressObservability(app, {
  serviceName: 'notification-service',
  extraMetrics: async () => {
    const db = await getDB();
    const [pending, dlq] = await Promise.all([
      db.query("SELECT COUNT(*)::int AS count FROM outbox_events WHERE status IN ('pending','retry')"),
      db.query('SELECT COUNT(*)::int AS count FROM notification_dlq')
    ]);
    return [
      '# HELP soulmatch_notification_outbox_depth Pending notification outbox events.',
      '# TYPE soulmatch_notification_outbox_depth gauge',
      `soulmatch_notification_outbox_depth ${pending.rows[0]?.count || 0}`,
      '# HELP soulmatch_notification_dlq_depth Dead-lettered notification events.',
      '# TYPE soulmatch_notification_dlq_depth gauge',
      `soulmatch_notification_dlq_depth ${dlq.rows[0]?.count || 0}`
    ].join('\n');
  }
});
app.use('/api/v1/notifications', notifRoutes);
app.get('/health', (req, res) => res.json({ status:'ok', service:'notification-service' }));
app.use(notFoundHandler);
app.use(errorHandler);
const PORT = process.env.PORT || 3006;
app.listen(PORT, () => logger.info('Notification Service on port ' + PORT));
startNotificationWorker();
module.exports = app;
