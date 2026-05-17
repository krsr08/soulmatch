function normalized(value, fallback = '') {
  const text = String(value ?? '').trim().toLowerCase();
  return text || fallback;
}

function evaluateProfileVisibility(profile = {}, viewerUserId = null) {
  if (!profile || !profile.profile_id) return { allowed: false, reason: 'not_found' };
  const owner = Boolean(viewerUserId && profile.user_id === viewerUserId);
  if (owner) return { allowed: true, owner: true, reason: 'owner' };
  if (profile.blocked === true) return { allowed: false, owner: false, reason: 'blocked' };
  if (profile.is_published === false) return { allowed: false, owner: false, reason: 'not_available' };
  if (normalized(profile.admin_status, 'active') !== 'active') {
    return { allowed: false, owner: false, reason: 'not_available' };
  }
  if (normalized(profile.profile_status, 'active') !== 'active') {
    return { allowed: false, owner: false, reason: 'inactive' };
  }
  const visibility = normalized(profile.profile_visibility, 'all');
  if (visibility === 'hidden') return { allowed: false, owner: false, reason: 'hidden' };
  if (visibility === 'matches_only') return { allowed: false, owner: false, reason: 'matches_only' };
  return { allowed: true, owner: false, reason: 'visible' };
}

function redactProfileForViewer(profile = {}, options = {}) {
  const owner = options.owner === true;
  const redacted = { ...profile };
  if (!owner && redacted.hide_last_seen) {
    redacted.last_login = null;
    redacted.last_seen = null;
    redacted.last_active_at = null;
  }
  if (!owner && options.canViewPhoto === false) {
    redacted.primary_photo_url = null;
    redacted.photo_url = null;
    redacted.photos = [];
  }
  if (!owner && options.canViewContact === false) {
    if (redacted.masked_phone) redacted.phone = redacted.masked_phone;
    if (redacted.masked_email) redacted.email = redacted.masked_email;
  }
  return redacted;
}

module.exports = {
  evaluateProfileVisibility,
  redactProfileForViewer
};
