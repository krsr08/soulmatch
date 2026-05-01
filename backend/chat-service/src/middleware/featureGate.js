const { getDB } = require('../config/database');
const { getConfigSection } = require('../../shared/controlPlane');

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
    next();
  } catch (error) {
    next(error);
  }
}

module.exports = {
  ensureChatEnabled,
  isChatEnabled
};
