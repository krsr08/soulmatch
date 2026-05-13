const { getDB } = require('../config/database');
const { randomUUID } = require('crypto');

const normalizeFilters = (raw = {}) => ({
  ageMin: raw.ageMin ? parseInt(raw.ageMin, 10) : null,
  ageMax: raw.ageMax ? parseInt(raw.ageMax, 10) : null,
  religion: raw.religion || null,
  community: raw.community || raw.caste || null,
  motherTongue: raw.motherTongue || null,
  city: raw.city || raw.location || null,
  gender: raw.gender || null,
  diet: raw.diet || null,
  education: raw.education || raw.educationLevel || null,
  occupation: raw.occupation || null,
  income: raw.income || raw.annualIncome || null,
  familyType: raw.familyType || null,
  maritalStatus: raw.maritalStatus || null,
  manglik: raw.manglik || null,
  verifiedOnly: raw.verifiedOnly === true,
  photoOnly: raw.photoOnly === true || raw.hasPhotoOnly === true,
  recentlyActiveOnly: raw.recentlyActiveOnly === true,
  page: raw.page,
  limit: raw.limit
});

const buildSearchLabel = (filters) => {
  const faith = filters.religion || 'Smart';
  const ageBand = [filters.ageMin, filters.ageMax].filter(Boolean).join('-');
  const city = filters.city || 'All cities';
  return [faith, ageBand, city].filter(Boolean).join(' ').trim();
};

const clampScore = (value) => Math.max(0, Math.min(100, Math.round(Number(value) || 0)));

const buildTrustSummary = (profile = {}) => {
  let score = 0;
  const signals = [];
  const verificationStatus = String(profile.verification_status || 'pending').toLowerCase();
  const completionScore = clampScore(profile.completion_score);
  const photoCount = Number(profile.photo_count || 0);
  const approvedVerifications = Number(profile.approved_verifications || 0);
  const approvedTypes = Array.isArray(profile.approved_verification_types) ? profile.approved_verification_types.map((item) => String(item || '').toLowerCase()) : [];
  const pendingVerifications = Number(profile.pending_verifications || 0);
  const reportCount = Number(profile.report_count || 0);

  if (profile.is_phone_verified === true) {
    score += 15;
    signals.push('Phone verified');
  }
  if (profile.firebase_verified === true) {
    score += 8;
    signals.push('Firebase identity linked');
  }
  if (completionScore >= 90) {
    score += 20;
    signals.push('Highly complete profile');
  } else if (completionScore >= 70) {
    score += 15;
    signals.push('Strong profile detail');
  } else if (completionScore >= 50) {
    score += 10;
  }
  if (verificationStatus === 'verified') {
    score += 25;
    signals.push('Admin verified');
  } else if (pendingVerifications > 0 || verificationStatus === 'pending') {
    score += 8;
  }
  if (approvedVerifications > 0) score += Math.min(15, approvedVerifications * 5);
  if (approvedTypes.includes('education')) { score += 6; signals.push('Education verified'); }
  if (approvedTypes.includes('income')) { score += 6; signals.push('Income verified'); }
  if (approvedTypes.includes('family')) { score += 6; signals.push('Family verified'); }
  if (photoCount >= 3) {
    score += 12;
    signals.push('Multiple photos');
  } else if (photoCount >= 1 || profile.primary_photo_url) {
    score += 8;
    signals.push('Photo added');
  }
  if (profile.family_city || profile.family_pincode) {
    score += 8;
    signals.push('Family location available');
  }
  if (profile.profile_status === 'active') score += 5;
  if (profile.last_login && Date.now() - new Date(profile.last_login).getTime() <= 1000 * 60 * 60 * 24 * 30) {
    score += 5;
    signals.push('Recently active');
  }
  if (reportCount === 0) {
    score += 10;
    signals.push('No open safety reports');
  } else {
    score -= Math.min(35, reportCount * 12);
  }
  const trustScore = clampScore(score);
  return {
    trust_score: trustScore,
    trust_level: trustScore >= 80 ? 'high' : trustScore >= 55 ? 'medium' : 'low',
    trust_signals: signals.slice(0, 4)
  };
};

const buildMatchReasons = (profile, filters) => {
  const reasons = [];
  if (profile.verification_status === 'verified') reasons.push('Verified profile');
  if (filters.city && profile.working_city && profile.working_city.toLowerCase().includes(String(filters.city).toLowerCase())) reasons.push('Matches your preferred city');
  if (filters.religion && profile.religion === filters.religion) reasons.push('Matches your religion preference');
  if (filters.community && `${profile.caste || ''} ${profile.religion || ''}`.toLowerCase().includes(String(filters.community).toLowerCase())) reasons.push('Community preference aligned');
  if (filters.education && profile.education_level?.toLowerCase().includes(String(filters.education).toLowerCase())) reasons.push('Education preference aligned');
  if (filters.occupation && profile.occupation?.toLowerCase().includes(String(filters.occupation).toLowerCase())) reasons.push('Profession preference aligned');
  if (Number(profile.trust_score || 0) >= 80) reasons.push('High trust score');
  return reasons.slice(0, 4);
};

const searchProfiles = async (filters, userId) => {
  const db = await getDB();
  const values = [userId];
  const conditions = [
    'p.is_published=true',
    "COALESCE(p.admin_status,'active')='active'",
    "COALESCE(p.profile_status,'active')='active'",
    "COALESCE(p.profile_visibility,'all')!='hidden'",
    "u.is_active=true",
    "COALESCE(u.is_banned,false)=false",
    `p.user_id != $1`,
    `NOT EXISTS (
      SELECT 1 FROM blocks b
      WHERE (b.blocker_id=$1 AND b.blocked_id=p.user_id)
         OR (b.blocker_id=p.user_id AND b.blocked_id=$1)
    )`
  ];
  let idx = values.length + 1;
  if (filters.ageMin) { conditions.push('EXTRACT(YEAR FROM AGE(p.dob))>=$' + idx++); values.push(filters.ageMin); }
  if (filters.ageMax) { conditions.push('EXTRACT(YEAR FROM AGE(p.dob))<=$' + idx++); values.push(filters.ageMax); }
  if (filters.gender) { conditions.push('p.gender=$' + idx++); values.push(filters.gender); }
  if (filters.religion) { conditions.push('p.religion=$' + idx++); values.push(filters.religion); }
  if (filters.community) { conditions.push('(p.caste ILIKE $' + idx + ' OR p.religion ILIKE $' + idx + ')'); values.push('%' + filters.community + '%'); idx++; }
  if (filters.motherTongue) { conditions.push('p.mother_tongue ILIKE $' + idx++); values.push('%' + filters.motherTongue + '%'); }
  if (filters.city) { conditions.push('ec.working_city ILIKE $' + idx++); values.push('%' + filters.city + '%'); }
  if (filters.diet) { conditions.push('ld.diet=$' + idx++); values.push(filters.diet); }
  if (filters.education) { conditions.push('ec.education_level ILIKE $' + idx++); values.push('%' + filters.education + '%'); }
  if (filters.occupation) { conditions.push('ec.occupation ILIKE $' + idx++); values.push('%' + filters.occupation + '%'); }
  if (filters.income) { conditions.push('ec.annual_income ILIKE $' + idx++); values.push('%' + filters.income + '%'); }
  if (filters.familyType) { conditions.push('fd.family_type ILIKE $' + idx++); values.push('%' + filters.familyType + '%'); }
  if (filters.maritalStatus) { conditions.push('p.marital_status ILIKE $' + idx++); values.push('%' + filters.maritalStatus + '%'); }
  if (filters.manglik && ['yes','true','manglik'].includes(String(filters.manglik).toLowerCase())) conditions.push('hd.is_manglik=true');
  if (filters.manglik && ['no','false','non-manglik'].includes(String(filters.manglik).toLowerCase())) conditions.push('COALESCE(hd.is_manglik,false)=false');
  if (filters.verifiedOnly) conditions.push("COALESCE(p.verification_status,'pending')='verified'");
  if (filters.photoOnly) conditions.push("NULLIF(p.primary_photo_url,'') IS NOT NULL");
  if (filters.recentlyActiveOnly) conditions.push("u.last_login >= NOW() - INTERVAL '30 days'");
  const page = Math.max(parseInt(filters.page, 10)||1, 1); const limit = Math.min(Math.max(parseInt(filters.limit, 10)||20, 15), 100); const offset = (page-1)*limit;
  const where = conditions.join(' AND ');
  const from = ' FROM profiles p JOIN users u ON u.user_id=p.user_id LEFT JOIN education_career ec ON p.profile_id=ec.profile_id LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id LEFT JOIN family_details fd ON p.profile_id=fd.profile_id LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id ';
  const visiblePhotoSql = `
    CASE
      WHEN COALESCE(p.photo_privacy,'all')='all'
        OR (
          COALESCE(p.photo_privacy,'all')='matches_only'
          AND EXISTS (
            SELECT 1
            FROM interests i
            JOIN profiles viewer ON viewer.user_id=$1
            WHERE ((i.sender_id=viewer.profile_id AND i.receiver_id=p.profile_id) OR (i.sender_id=p.profile_id AND i.receiver_id=viewer.profile_id))
              AND i.status='accepted'
          )
        )
        OR EXISTS (
          SELECT 1
          FROM profile_photo_access_requests par
          WHERE par.target_profile_id=p.profile_id
            AND par.requester_user_id=$1
            AND par.status='approved'
            AND (par.expires_at IS NULL OR par.expires_at > NOW())
        )
      THEN p.primary_photo_url
      ELSE NULL
    END AS primary_photo_url,
    CASE
      WHEN COALESCE(p.photo_privacy,'all')='all'
        OR (
          COALESCE(p.photo_privacy,'all')='matches_only'
          AND EXISTS (
            SELECT 1
            FROM interests i
            JOIN profiles viewer ON viewer.user_id=$1
            WHERE ((i.sender_id=viewer.profile_id AND i.receiver_id=p.profile_id) OR (i.sender_id=p.profile_id AND i.receiver_id=viewer.profile_id))
              AND i.status='accepted'
          )
        )
        OR EXISTS (
          SELECT 1
          FROM profile_photo_access_requests par
          WHERE par.target_profile_id=p.profile_id
            AND par.requester_user_id=$1
            AND par.status='approved'
            AND (par.expires_at IS NULL OR par.expires_at > NOW())
        )
      THEN FALSE
      ELSE TRUE
    END AS is_photo_private`;
  const query = `SELECT
       p.profile_id,
       p.user_id,
       p.first_name,
       p.last_name,
       EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
       p.gender,
       p.religion,
       p.caste,
       p.mother_tongue,
       p.marital_status,
       p.created_at,
       ${visiblePhotoSql},
       p.profile_created_by,
       p.hide_last_seen,
       p.verification_status,
       p.completion_score,
       COALESCE(p.profile_status,'active') AS profile_status,
       u.is_verified AS is_phone_verified,
       (u.google_id IS NOT NULL) AS firebase_verified,
       u.last_login,
       CASE
         WHEN u.last_login >= NOW() - INTERVAL '15 minutes' THEN 'Active'
         WHEN u.last_login >= NOW() - INTERVAL '3 days' THEN 'Recently Active'
         ELSE 'Active Recently'
       END AS last_active_label,
       pd.height_cm,
       ec.occupation,
       ec.working_city,
       ec.education_level,
       ec.annual_income,
       fd.family_type,
       fd.family_city,
       fd.family_state,
       fd.family_pincode,
       ld.diet,
       hd.is_manglik,
       COALESCE((SELECT COUNT(*)::int FROM profile_photos pp WHERE pp.profile_id=p.profile_id), 0) AS photo_count,
       COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), 0) AS approved_verifications,
       COALESCE((SELECT array_agg(DISTINCT v.type) FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), ARRAY[]::text[]) AS approved_verification_types,
       COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status='pending'), 0) AS pending_verifications,
       COALESCE((SELECT COUNT(*)::int FROM reports rp WHERE rp.reported_id=p.user_id AND rp.status IN ('pending','open','reviewing')), 0) AS report_count
     FROM profiles p
     JOIN users u ON u.user_id=p.user_id
     LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
     LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
     LEFT JOIN family_details fd ON p.profile_id=fd.profile_id
     LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
     LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
     WHERE ${where}
     ORDER BY p.completion_score DESC, u.last_login DESC NULLS LAST
     LIMIT $${idx++} OFFSET $${idx++}`;
  values.push(limit, offset);
  const countQ = 'SELECT COUNT(*)' + from + 'WHERE ' + where;
  const [results, count] = await Promise.all([db.query(query, values), db.query(countQ, values.slice(0,-2))]);
  const enriched = results.rows.map((row) => {
    const trust = buildTrustSummary(row);
    const withTrust = { ...row, ...trust };
    return {
      ...withTrust,
      is_verified: row.verification_status === 'verified',
      match_reasons: buildMatchReasons(withTrust, filters)
    };
  });
  return { results:enriched, total:parseInt(count.rows[0].count), page, limit };
};
exports.basicSearch = async (req, res, next) => {
  try {
    const filters = normalizeFilters(req.body);
    const result = await searchProfiles(filters, req.user.userId);
    res.json({ success:true, data:result });
  } catch (err) {
    next(err);
  }
};
exports.advancedSearch = async (req, res, next) => {
  try {
    const filters = normalizeFilters(req.body);
    const result = await searchProfiles(filters, req.user.userId);
    res.json({ success:true, data:result });
  } catch (err) {
    next(err);
  }
};
exports.getSavedSearches = async (req, res, next) => {
  try {
    const db = await getDB();
    const r = await db.query(
      `SELECT
         search_id AS "searchId",
         label,
         age_min AS "ageMin",
         age_max AS "ageMax",
         religion,
         city
       FROM saved_searches
       WHERE user_id=$1
       ORDER BY created_at DESC
       LIMIT 20`,
      [req.user.userId]
    );
    res.json({ success:true, data:r.rows });
  } catch (err) {
    next(err);
  }
};
exports.saveSearch = async (req, res, next) => {
  try {
    const db = await getDB();
    const filters = normalizeFilters(req.body);
    const saved = {
      searchId: randomUUID(),
      label: String(req.body?.label || '').trim() || buildSearchLabel(filters),
      ageMin: filters.ageMin,
      ageMax: filters.ageMax,
      religion: filters.religion,
      city: filters.city
    };
    await db.query(
      `INSERT INTO saved_searches
         (search_id,user_id,label,age_min,age_max,religion,city,gender,diet)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)`,
      [
        saved.searchId,
        req.user.userId,
        saved.label,
        saved.ageMin,
        saved.ageMax,
        saved.religion,
        saved.city,
        filters.gender,
        filters.diet
      ]
    );
    res.json({ success:true, data:saved });
  } catch (err) {
    next(err);
  }
};
