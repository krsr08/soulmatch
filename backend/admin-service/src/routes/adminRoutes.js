const express = require('express');
const controller = require('../controllers/adminController');
const { authenticateAdmin, authorizeAdminRoles } = require('../middleware/adminAuthMiddleware');
const { authenticateService } = require('../middleware/serviceAuthMiddleware');

const router = express.Router();

router.post('/login', controller.adminLogin);
router.post('/logout', authenticateAdmin, controller.adminLogout);

router.get('/dashboard', authenticateAdmin, controller.getDashboard);
router.get('/realtime/snapshot', authenticateAdmin, controller.getRealtimeSnapshot);
router.get('/users', authenticateAdmin, controller.getUsers);
router.put('/users/:id/ban', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.banUser);
router.put('/users/:id/unban', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.unbanUser);

router.get('/profiles', authenticateAdmin, controller.getProfiles);
router.post('/profiles', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.createProfile);
router.post('/profiles/bulk', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.bulkCreateProfiles);
router.put('/profiles/:id', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.updateProfile);
router.post('/profiles/:id/photos', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.addProfilePhoto);
router.put('/profiles/:id/photos/:photoId', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.updateProfilePhoto);
router.delete('/profiles/:id/photos/:photoId', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.deleteProfilePhoto);
router.put('/profiles/:id/photos/:photoId/approve', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.approveProfilePhoto);
router.put('/profiles/:id/photos/:photoId/reject', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.rejectProfilePhoto);
router.delete('/profiles/:id', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.deleteProfile);
router.put('/profiles/:id/status', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.updateProfileStatus);

router.get('/advisors', authenticateAdmin, controller.getAdvisors);
router.post('/advisors', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.createAdvisor);
router.put('/advisors/:id', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.updateAdvisor);
router.put('/advisors/:id/status', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.updateAdvisorStatus);
router.put('/agents/:id/approve', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.approveAgent);
router.put('/agents/:id/reject', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.rejectAgent);
router.get('/assisted-assignments', authenticateAdmin, controller.getAssistedAssignments);
router.put('/assisted-assignments/:id', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.updateAssistedAssignment);

router.get('/verifications', authenticateAdmin, controller.getPendingVerifications);
router.put('/verifications/:id/approve', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.approveVerification);
router.put('/verifications/:id/reject', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.rejectVerification);
router.get('/profile-documents', authenticateAdmin, controller.getPendingProfileDocuments);
router.put('/profile-documents/:id/approve', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.approveProfileDocument);
router.put('/profile-documents/:id/reject', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.rejectProfileDocument);

router.get('/reports', authenticateAdmin, controller.getReports);
router.put('/reports/:id/resolve', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.resolveReport);
router.get('/moderation/reports', authenticateAdmin, controller.getReports);
router.get('/moderation/inbox', authenticateAdmin, controller.getModerationInbox);
router.get('/moderation/photos', authenticateAdmin, controller.getPendingPhotoModeration);
router.get('/moderation/chat-logs', authenticateAdmin, controller.getChatLogs);

router.get('/payments', authenticateAdmin, controller.getPayments);
router.post('/payments/refunds', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.createRefund);

router.get('/alerts', authenticateAdmin, controller.getAlerts);
router.put('/alerts/:id/ack', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator', 'support_agent'), controller.ackAlert);
router.post('/system/alerts', authenticateService, controller.createSystemAlert);
router.get('/consent-events', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator', 'support_agent'), controller.getConsentEvents);
router.post('/campaigns', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'marketing_manager'), controller.createCampaign);
router.get('/audit-logs', authenticateAdmin, controller.getAuditLogs);
router.get('/roles', authenticateAdmin, controller.getRoles);
router.get('/admin-users', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.getAdminUsers);
router.post('/admin-users', authenticateAdmin, authorizeAdminRoles('super_admin'), controller.createAdminUser);
router.put('/admin-users/:id', authenticateAdmin, authorizeAdminRoles('super_admin'), controller.updateAdminUser);
router.delete('/admin-users/:id', authenticateAdmin, authorizeAdminRoles('super_admin'), controller.deleteAdminUser);
router.get('/system/inventory', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.getSystemInventory);

router.get('/stories', authenticateAdmin, controller.getPendingStories);
router.put('/stories/:id/approve', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'moderator'), controller.approveStory);

router.get('/config', authenticateAdmin, controller.getConfig);
router.put('/config/:key', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin'), controller.updateConfig);

router.get('/landing-pages', authenticateAdmin, controller.getLandingPages);
router.post('/landing-pages', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'marketing_manager'), controller.upsertLandingPage);
router.put('/landing-pages/:slug', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'marketing_manager'), controller.upsertLandingPage);

router.get('/referrals', authenticateAdmin, controller.getReferralCodes);
router.post('/referrals/codes', authenticateAdmin, authorizeAdminRoles('super_admin', 'admin', 'marketing_manager'), controller.createReferralCode);

router.get('/analytics/funnel', authenticateAdmin, controller.getAnalyticsFunnel);
router.get('/analytics/events', authenticateAdmin, controller.getAnalyticsEvents);
router.get('/service-health', authenticateAdmin, controller.getServiceHealth);

module.exports = router;
