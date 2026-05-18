const DEFAULT_MEMBER_PLAN_ENTITLEMENTS = {
  bronze: {
    planId: 'bronze',
    label: 'Bronze',
    visibleMatches: 80,
    profileViews: 10,
    contactDetails: 0,
    engagePlus: false,
    shortlist: 5,
    interests: 5,
    matchAssistance: false,
    chat: false,
    spotlightBoosts: 0,
    verifiedOnly: true
  },
  silver: {
    planId: 'silver',
    label: 'Silver',
    visibleMatches: 80,
    profileViews: 30,
    contactDetails: 15,
    engagePlus: true,
    shortlist: 20,
    interests: 20,
    matchAssistance: false,
    chat: true,
    spotlightBoosts: 0,
    verifiedOnly: true
  },
  gold: {
    planId: 'gold',
    label: 'Gold',
    visibleMatches: 80,
    profileViews: 50,
    contactDetails: 30,
    engagePlus: true,
    shortlist: 40,
    interests: 40,
    matchAssistance: true,
    chat: true,
    spotlightBoosts: 2,
    verifiedOnly: true
  },
  platinum: {
    planId: 'platinum',
    label: 'Platinum',
    visibleMatches: 80,
    profileViews: 80,
    contactDetails: 80,
    engagePlus: true,
    shortlist: 80,
    interests: 80,
    matchAssistance: true,
    chat: true,
    spotlightBoosts: 4,
    verifiedOnly: true
  }
};

const USAGE_COLUMNS = {
  profile_view: 'profile_views_used',
  contact_unlock: 'contact_unlocks_used',
  shortlist: 'shortlists_used',
  interest: 'interests_used',
  spotlight: 'spotlight_boosts_used'
};

function normalizePlanId(planId) {
  const value = String(planId || '').trim().toLowerCase();
  if (!value || value === 'free') return 'bronze';
  if (value === 'fixed_access') return 'platinum';
  return Object.prototype.hasOwnProperty.call(DEFAULT_MEMBER_PLAN_ENTITLEMENTS, value) ? value : 'bronze';
}

function booleanValue(value, fallback = false) {
  if (value === undefined || value === null || value === '') return fallback;
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  const normalized = String(value).trim().toLowerCase();
  if (['false', '0', 'no', 'off', 'disabled'].includes(normalized)) return false;
  if (['true', '1', 'yes', 'on', 'enabled'].includes(normalized)) return true;
  return fallback;
}

function normalizeEntitlements(raw = {}) {
  const planId = normalizePlanId(raw.planId || raw.plan_id || raw.id);
  const base = DEFAULT_MEMBER_PLAN_ENTITLEMENTS[planId] || DEFAULT_MEMBER_PLAN_ENTITLEMENTS.bronze;
  return {
    ...base,
    ...raw,
    planId,
    label: raw.label || raw.name || base.label,
    visibleMatches: Number(raw.visibleMatches ?? raw.visible_matches ?? base.visibleMatches),
    profileViews: Number(raw.profileViews ?? raw.profile_views ?? base.profileViews),
    contactDetails: Number(raw.contactDetails ?? raw.contact_details ?? base.contactDetails),
    engagePlus: booleanValue(raw.engagePlus ?? raw.engage_plus, base.engagePlus),
    shortlist: Number(raw.shortlist ?? raw.shortlists ?? base.shortlist),
    interests: Number(raw.interests ?? base.interests),
    matchAssistance: booleanValue(raw.matchAssistance ?? raw.match_assistance, base.matchAssistance),
    chat: booleanValue(raw.chat, base.chat),
    spotlightBoosts: Number(raw.spotlightBoosts ?? raw.spotlight_boosts ?? base.spotlightBoosts),
    verifiedOnly: booleanValue(raw.verifiedOnly ?? raw.verified_only, base.verifiedOnly)
  };
}

function getEntitlements(monetization = {}, planId = 'free') {
  const normalized = normalizePlanId(planId);
  const configured = monetization.memberPlanEntitlements || monetization.member_plan_entitlements || {};
  if (Array.isArray(configured)) {
    const match = configured.find((item) => normalizePlanId(item.planId || item.plan_id || item.id) === normalized);
    return normalizeEntitlements(match || DEFAULT_MEMBER_PLAN_ENTITLEMENTS[normalized]);
  }
  const direct = configured[normalized] || configured[planId] || null;
  return normalizeEntitlements(direct || DEFAULT_MEMBER_PLAN_ENTITLEMENTS[normalized]);
}

function periodKey(date = new Date()) {
  return date.toISOString().slice(0, 7) + '-01';
}

async function getActivePlanId(db, userId) {
  const result = await db.query(
    `SELECT plan_id
     FROM subscriptions
     WHERE user_id=$1
       AND is_active=true
       AND (end_date IS NULL OR end_date>NOW())
     ORDER BY created_at DESC
     LIMIT 1`,
    [userId]
  );
  return result.rows[0]?.plan_id || 'free';
}

async function ensureUsageRecord(db, userId, planId = 'free') {
  const normalizedPlanId = normalizePlanId(planId);
  const existing = await db.query(
    `SELECT *
     FROM member_subscription_usage
     WHERE user_id=$1
     LIMIT 1`,
    [userId]
  );
  const row = existing.rows[0];
  const expired = row?.period_ends_at && new Date(row.period_ends_at).getTime() <= Date.now();
  const planChanged = row && normalizePlanId(row.plan_id) !== normalizedPlanId;
  if (!row) {
    const created = await db.query(
      `INSERT INTO member_subscription_usage (
         user_id, plan_id, period_started_at, period_ends_at
       )
       VALUES ($1,$2,NOW(),NOW() + INTERVAL '30 days')
       RETURNING *`,
      [userId, normalizedPlanId]
    );
    return created.rows[0];
  }
  if (expired || planChanged) {
    const reset = await db.query(
      `UPDATE member_subscription_usage
       SET plan_id=$2,
           period_started_at=NOW(),
           period_ends_at=NOW() + INTERVAL '30 days',
           profile_views_used=0,
           contact_unlocks_used=0,
           shortlists_used=0,
           interests_used=0,
           spotlight_boosts_used=0,
           updated_at=NOW()
       WHERE user_id=$1
       RETURNING *`,
      [userId, normalizedPlanId]
    );
    return reset.rows[0];
  }
  return row;
}

async function getUsageContext(db, userId, monetization = {}) {
  const planId = await getActivePlanId(db, userId);
  const entitlements = getEntitlements(monetization, planId);
  const usage = await ensureUsageRecord(db, userId, entitlements.planId);
  return { planId: entitlements.planId, entitlements, usage };
}

async function hasMeterEvent(db, { userId, targetProfileId, eventType, period = periodKey() }) {
  const result = await db.query(
    `SELECT 1
     FROM member_meter_events
     WHERE user_id=$1 AND target_profile_id=$2 AND event_type=$3 AND period_key=$4::date
     LIMIT 1`,
    [userId, targetProfileId, eventType, period]
  );
  return Boolean(result.rows[0]);
}

async function consumeMeter(db, { userId, targetProfileId, eventType, limit, usageColumn, metadata = {} }) {
  const column = usageColumn || USAGE_COLUMNS[eventType];
  if (!column || !Object.values(USAGE_COLUMNS).includes(column)) {
    throw new Error(`Unsupported subscription usage column for ${eventType}`);
  }
  const currentPeriod = periodKey();
  const alreadyRecorded = targetProfileId
    ? await hasMeterEvent(db, { userId, targetProfileId, eventType, period: currentPeriod })
    : false;
  const usage = await ensureUsageRecord(db, userId, await getActivePlanId(db, userId));
  const used = Number(usage[column] || 0);
  if (!alreadyRecorded && Number.isFinite(Number(limit)) && Number(limit) >= 0 && used >= Number(limit)) {
    return { allowed: false, limitReached: true, used, remaining: 0, limit: Number(limit) };
  }
  if (!alreadyRecorded) {
    await db.query(
      `INSERT INTO member_meter_events (
         event_id,user_id,target_profile_id,event_type,period_key,metadata,created_at
       )
       VALUES (gen_random_uuid(),$1,$2,$3,$4::date,$5::jsonb,NOW())
       ON CONFLICT (user_id,target_profile_id,event_type,period_key) DO NOTHING`,
      [userId, targetProfileId || null, eventType, currentPeriod, JSON.stringify(metadata || {})]
    );
    await db.query(
      `UPDATE member_subscription_usage
       SET ${column}=${column}+1, updated_at=NOW()
       WHERE user_id=$1`,
      [userId]
    );
  }
  const nextUsed = alreadyRecorded ? used : used + 1;
  const numericLimit = Number.isFinite(Number(limit)) ? Number(limit) : null;
  return {
    allowed: true,
    alreadyRecorded,
    used: nextUsed,
    limit: numericLimit,
    remaining: numericLimit === null ? null : Math.max(numericLimit - nextUsed, 0)
  };
}

module.exports = {
  DEFAULT_MEMBER_PLAN_ENTITLEMENTS,
  USAGE_COLUMNS,
  consumeMeter,
  ensureUsageRecord,
  getActivePlanId,
  getEntitlements,
  getUsageContext,
  normalizePlanId,
  normalizeEntitlements,
  periodKey
};
