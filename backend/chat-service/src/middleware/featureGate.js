const { getDB } = require('../config/database');
const { getConfigSection } = require('../../shared/controlPlane');
const { getActivePlanId, getEntitlements } = require('../../shared/memberEntitlements');

async function isChatEnabled() {
  const db = await getDB();
  const flags = await getConfigSection(db, 'feature_flags');
  return flags.chat !== false;
}

async function ensureChatEnabled(req, res, next) {
  try {
    const enabled = await isChatEnabled();
    if (!enabled) {
      return res.status(503).json({ success: false, error: { message: 'Chat is temporarily unavailable for maintenance' } });
    }
    if (!req.user || req.user.userType === 'member' || !req.user.userType) {
      const db = await getDB();
      const monetization = await getConfigSection(db, 'monetization');
      const planId = await getActivePlanId(db, req.user?.userId);
      const entitlements = getEntitlements(monetization, planId);
      if (!entitlements.chat) {
        return res.status(403).json({
          success: false,
          error: {
            code: 'UPGRADE_REQUIRED',
            message: 'Upgrade to Silver or above to chat with matches.'
          }
        });
      }
    }
    next();
  } catch (error) {
    next(error);
  }
}

module.exports = {
  ensureChatEnabled,
  isChatEnabled
};
