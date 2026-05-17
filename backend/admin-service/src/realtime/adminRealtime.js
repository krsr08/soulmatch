const jwt = require('jsonwebtoken');
const { getDB } = require('../config/database');
const logger = require('../utils/logger');

let io = null;

function getAdminSecret() {
  return process.env.ADMIN_JWT_SECRET || process.env.JWT_SECRET || 'dev-admin-secret';
}

function adminVerifyOptions() {
  return {
    issuer: process.env.ADMIN_JWT_ISSUER || 'soulmatch-admin',
    audience: process.env.ADMIN_JWT_AUDIENCE || 'soulmatch-admin-api'
  };
}

function isAdminRole(role) {
  return ['admin', 'super_admin', 'moderator', 'support_agent', 'marketing_manager'].includes(role);
}

function realtimeCorsOrigins() {
  const configured = (process.env.CORS_ORIGINS || '').split(',').map((item) => item.trim()).filter(Boolean);
  if (configured.length) return configured;
  if (process.env.NODE_ENV !== 'production') return ['http://localhost:3000', 'http://127.0.0.1:3000'];
  return [];
}

async function count(db, sql, params = []) {
  try {
    const result = await db.query(sql, params);
    return Number(result.rows[0]?.count || result.rows[0]?.total || 0);
  } catch (_) {
    return 0;
  }
}

async function getRealtimeSnapshot() {
  const db = await getDB();
  const [
    totalUsers,
    totalProfiles,
    liveUsers,
    activeProfiles,
    pendingApprovals,
    premiumUsers,
    totalRevenue,
    revenue30d,
    newUsersToday,
    paymentsToday,
    revenueToday,
    matchesToday,
    pendingReports,
    fraudAlerts,
    signups,
    paymentClicks,
    paymentSuccesses,
    matchesMade,
    dau,
    mau
  ] = await Promise.all([
    count(db, 'SELECT COUNT(*) FROM users'),
    count(db, 'SELECT COUNT(*) FROM profiles'),
    count(db, "SELECT COUNT(*) FROM users WHERE last_login >= NOW() - INTERVAL '15 minutes'"),
    count(db, 'SELECT COUNT(*) FROM profiles WHERE is_published=true'),
    count(db, "SELECT COUNT(*) FROM profiles WHERE is_published=false OR COALESCE(verification_status,'pending')='pending'"),
    count(db, "SELECT COUNT(*) FROM subscriptions WHERE is_active=true AND plan_id!='free'"),
    count(db, 'SELECT COALESCE(SUM(amount_paid),0) AS total FROM subscriptions WHERE is_active=true'),
    count(db, "SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE created_at >= NOW() - INTERVAL '30 days' AND status IN ('paid','success','captured')"),
    count(db, "SELECT COUNT(*) FROM users WHERE created_at >= CURRENT_DATE"),
    count(db, "SELECT COUNT(*) FROM transactions WHERE created_at >= CURRENT_DATE AND status IN ('paid','success','captured')"),
    count(db, "SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE created_at >= CURRENT_DATE AND status IN ('paid','success','captured')"),
    count(db, "SELECT COUNT(*) FROM interests WHERE status='accepted' AND responded_at >= CURRENT_DATE"),
    count(db, "SELECT COUNT(*) FROM reports WHERE status='pending'"),
    count(db, "SELECT COUNT(*) FROM admin_alerts WHERE status='open' AND severity IN ('high','critical')"),
    count(db, "SELECT COUNT(*) FROM analytics_events WHERE event_type='sign_up' AND created_at >= NOW() - INTERVAL '30 days'"),
    count(db, "SELECT COUNT(*) FROM analytics_events WHERE event_type='payment_click' AND created_at >= NOW() - INTERVAL '30 days'"),
    count(db, "SELECT COUNT(*) FROM analytics_events WHERE event_type='payment_success' AND created_at >= NOW() - INTERVAL '30 days'"),
    count(db, "SELECT COUNT(*) FROM analytics_events WHERE event_type='match_made' AND created_at >= NOW() - INTERVAL '30 days'"),
    count(db, "SELECT COUNT(DISTINCT user_id) AS total FROM analytics_events WHERE created_at >= CURRENT_DATE AND user_id IS NOT NULL"),
    count(db, "SELECT COUNT(DISTINCT user_id) AS total FROM analytics_events WHERE created_at >= NOW() - INTERVAL '30 days' AND user_id IS NOT NULL")
  ]);

  const conversionRate = signups ? Number(((paymentSuccesses / signups) * 100).toFixed(2)) : 0;
  const matchSuccessRate = signups ? Number(((matchesMade / signups) * 100).toFixed(2)) : 0;
  return {
    generatedAt: new Date().toISOString(),
    totalUsers,
    totalProfiles,
    liveUsers,
    activeUsers: liveUsers,
    activeProfiles,
    pendingApprovals,
    premiumUsers,
    totalRevenue,
    revenue30d,
    newUsersToday,
    paymentsToday,
    revenueToday,
    matchesToday,
    pendingReports,
    fraudAlerts,
    dau,
    mau,
    conversionRate,
    matchSuccessRate,
    analytics: {
      signups,
      paymentClicks,
      paymentSuccesses,
      matchesMade
    }
  };
}

function initAdminRealtime(server) {
  const { Server } = require('socket.io');
  io = new Server(server, {
    path: '/admin-socket',
    cors: { origin: realtimeCorsOrigins(), methods: ['GET', 'POST'] }
  });

  const namespace = io.of('/admin');
  namespace.use((socket, next) => {
    try {
      const token = socket.handshake.auth?.token || socket.handshake.query?.token;
      if (!token) return next(new Error('ADMIN_AUTH_REQUIRED'));
      const decoded = jwt.verify(token, getAdminSecret(), adminVerifyOptions());
      if (!isAdminRole(decoded.role)) return next(new Error('FORBIDDEN'));
      socket.admin = decoded;
      return next();
    } catch (error) {
      return next(error);
    }
  });

  namespace.on('connection', async (socket) => {
    socket.join('admins');
    socket.emit('admin:connected', {
      role: socket.admin.role,
      connectedAt: new Date().toISOString()
    });
    try {
      socket.emit('admin:snapshot', await getRealtimeSnapshot());
    } catch (error) {
      logger.warn(`Realtime snapshot failed: ${error.message}`);
    }
  });

  const intervalMs = Number(process.env.ADMIN_REALTIME_INTERVAL_MS || 10000);
  setInterval(async () => {
    if (!namespace.sockets.size) return;
    try {
      namespace.to('admins').emit('admin:snapshot', await getRealtimeSnapshot());
    } catch (error) {
      logger.warn(`Realtime broadcast failed: ${error.message}`);
    }
  }, intervalMs);

  return io;
}

function broadcastAdminEvent(type, payload = {}) {
  if (!io) return;
  io.of('/admin').to('admins').emit(type, {
    ...payload,
    emittedAt: new Date().toISOString()
  });
}

module.exports = {
  broadcastAdminEvent,
  getRealtimeSnapshot,
  initAdminRealtime
};
