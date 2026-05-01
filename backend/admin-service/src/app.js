require('dotenv').config();
const express = require('express');
const http = require('http');
const helmet = require('helmet');
const cors = require('cors');
const adminRoutes = require('./routes/adminRoutes');
const publicRoutes = require('./routes/publicRoutes');
const shareRoutes = require('./routes/shareRoutes');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');
const { getRealtimeSnapshot, initAdminRealtime } = require('./realtime/adminRealtime');
const { ensureAdminSchema } = require('./services/adminSchema');
const logger = require('./utils/logger');
const app = express();
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use('/api/v1/admin', adminRoutes);
app.use('/api/v1/public', publicRoutes);
app.use('/share', shareRoutes);
app.get('/health', (req, res) => res.json({ status:'ok', service:'admin-service' }));
app.get('/metrics', async (req, res, next) => {
  try {
    const snapshot = await getRealtimeSnapshot();
    res.type('text/plain').send([
      '# HELP soulmatch_admin_live_users Users active in the last 15 minutes',
      '# TYPE soulmatch_admin_live_users gauge',
      `soulmatch_admin_live_users ${snapshot.liveUsers}`,
      '# HELP soulmatch_admin_pending_approvals Profiles waiting for approval',
      '# TYPE soulmatch_admin_pending_approvals gauge',
      `soulmatch_admin_pending_approvals ${snapshot.pendingApprovals}`,
      '# HELP soulmatch_admin_payments_today Successful payments today',
      '# TYPE soulmatch_admin_payments_today gauge',
      `soulmatch_admin_payments_today ${snapshot.paymentsToday}`,
      '# HELP soulmatch_admin_pending_reports Abuse reports waiting for review',
      '# TYPE soulmatch_admin_pending_reports gauge',
      `soulmatch_admin_pending_reports ${snapshot.pendingReports}`
    ].join('\n'));
  } catch (error) {
    next(error);
  }
});
app.use(notFoundHandler);
app.use(errorHandler);
const PORT = process.env.PORT || 3011;
const server = http.createServer(app);
initAdminRealtime(server);
ensureAdminSchema().finally(() => {
  server.listen(PORT, () => logger.info('Admin Service on port ' + PORT));
});
module.exports = app;
