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

const searchProfiles = async (filters, userId) => {
  const db = await getDB();
  const values = [userId];
  const conditions = [
    'p.is_published=true',
    "COALESCE(p.admin_status,'active')='active'",
    "COALESCE(p.profile_visibility,'all')!='hidden'",
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
  const page = Math.max(parseInt(filters.page, 10)||1, 1); const limit = Math.min(Math.max(parseInt(filters.limit, 10)||20, 1), 100); const offset = (page-1)*limit;
  const where = conditions.join(' AND ');
  const from = ' FROM profiles p JOIN users u ON u.user_id=p.user_id LEFT JOIN education_career ec ON p.profile_id=ec.profile_id LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id LEFT JOIN family_details fd ON p.profile_id=fd.profile_id LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id ';
  const query = 'SELECT p.profile_id,p.user_id,p.first_name,p.last_name,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,p.religion,p.caste,p.mother_tongue,p.primary_photo_url,pd.height_cm,ec.occupation,ec.working_city,ec.education_level,ec.annual_income,fd.family_type,ld.diet,hd.is_manglik FROM profiles p JOIN users u ON u.user_id=p.user_id LEFT JOIN education_career ec ON p.profile_id=ec.profile_id LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id LEFT JOIN family_details fd ON p.profile_id=fd.profile_id LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id WHERE ' + where + ' ORDER BY p.completion_score DESC, u.last_login DESC NULLS LAST LIMIT $' + idx++ + ' OFFSET $' + idx++;
  values.push(limit, offset);
  const countQ = 'SELECT COUNT(*)' + from + 'WHERE ' + where;
  const [results, count] = await Promise.all([db.query(query, values), db.query(countQ, values.slice(0,-2))]);
  return { results:results.rows, total:parseInt(count.rows[0].count), page, limit };
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
