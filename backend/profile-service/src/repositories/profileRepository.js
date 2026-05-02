const { getDB } = require('../config/database');
const { randomUUID } = require('crypto');
exports.findByUserId = async (userId) => { const db = await getDB(); const r = await db.query('SELECT * FROM profiles WHERE user_id=$1 LIMIT 1', [userId]); return r.rows[0] || null; };
exports.findById = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT * FROM profiles WHERE profile_id=$1 LIMIT 1', [profileId]); return r.rows[0] || null; };
exports.getVerificationRequests = async (userId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       verification_id,
       user_id,
       type,
       status,
       document_url,
       reviewer_email,
       review_note,
       reviewed_at,
       created_at
     FROM verifications
     WHERE user_id=$1
     ORDER BY created_at DESC`,
    [userId]
  );
  return r.rows;
};
exports.createVerificationRequest = async (profile, data) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const lockedProfile = await client.query(
      'SELECT profile_id,user_id,verification_status,completion_score,primary_photo_url FROM profiles WHERE profile_id=$1 AND user_id=$2 FOR UPDATE',
      [profile.profile_id, profile.user_id]
    );
    const current = lockedProfile.rows[0];
    if (!current) {
      await client.query('ROLLBACK');
      return { status: 'not_found' };
    }
    if (current.verification_status === 'verified') {
      await client.query('COMMIT');
      return { status: 'already_verified' };
    }
    const pending = await client.query(
      `SELECT
         verification_id,
         user_id,
         type,
         status,
         document_url,
         reviewer_email,
         review_note,
         reviewed_at,
         created_at
       FROM verifications
       WHERE user_id=$1 AND status='pending'
       ORDER BY created_at DESC
       LIMIT 1`,
      [profile.user_id]
    );
    if (pending.rows[0]) {
      await client.query('COMMIT');
      return { status: 'already_pending', verification: pending.rows[0] };
    }
    const verificationId = randomUUID();
    const documentUrl = data.documentUrl || current.primary_photo_url || null;
    const inserted = await client.query(
      `INSERT INTO verifications (verification_id,user_id,type,status,document_url,created_at)
       VALUES ($1,$2,$3,'pending',$4,NOW())
       RETURNING verification_id,user_id,type,status,document_url,reviewer_email,review_note,reviewed_at,created_at`,
      [verificationId, profile.user_id, data.type, documentUrl]
    );
    await client.query(
      "UPDATE profiles SET verification_status='pending', updated_at=NOW() WHERE profile_id=$1",
      [profile.profile_id]
    );
    await client.query(
      `INSERT INTO admin_alerts (alert_id,severity,title,body,source,metadata,created_at)
       VALUES ($1,'medium','Profile verification requested',$2,'profile-service',$3::jsonb,NOW())`,
      [
        randomUUID(),
        `${profile.first_name || 'A member'} ${profile.last_name || ''}`.trim() + ' submitted a profile verification request.',
        JSON.stringify({
          type: 'profile_verification',
          verificationId,
          profileId: profile.profile_id,
          userId: profile.user_id,
          verificationType: data.type
        })
      ]
    );
    await client.query('COMMIT');
    return { status: 'created', verification: inserted.rows[0] };
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
};
exports.canViewProfile = async (profileId, viewerUserId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT p.profile_id,p.user_id,p.is_published,p.admin_status,p.profile_visibility,
            EXISTS (
              SELECT 1 FROM blocks b
              WHERE (b.blocker_id=$2 AND b.blocked_id=p.user_id)
                 OR (b.blocker_id=p.user_id AND b.blocked_id=$2)
            ) AS blocked
     FROM profiles p
     WHERE p.profile_id=$1
     LIMIT 1`,
    [profileId, viewerUserId]
  );
  const profile = r.rows[0];
  if (!profile) return { allowed: false, reason: 'not_found' };
  if (profile.user_id === viewerUserId) return { allowed: true, owner: true, profile };
  if (profile.blocked) return { allowed: false, reason: 'blocked', profile };
  if (!profile.is_published || profile.admin_status !== 'active') return { allowed: false, reason: 'not_available', profile };
  if (profile.profile_visibility === 'hidden') return { allowed: false, reason: 'hidden', profile };
  return { allowed: true, owner: false, profile };
};
exports.hasAcceptedInterestWithViewer = async (profileId, viewerUserId) => {
  const db = await getDB();
  const viewer = await exports.findByUserId(viewerUserId);
  if (!viewer) return false;
  const r = await db.query(
    `SELECT EXISTS(
       SELECT 1 FROM interests
       WHERE ((sender_id=$1 AND receiver_id=$2) OR (sender_id=$2 AND receiver_id=$1))
         AND status='accepted'
     ) AS ok`,
    [viewer.profile_id, profileId]
  );
  return Boolean(r.rows[0]?.ok);
};
exports.getPhotos = async (profileId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       photo_id,
       photo_url,
       is_primary,
       sequence_order,
       uploaded_at
     FROM profile_photos
     WHERE profile_id=$1
     ORDER BY is_primary DESC, sequence_order ASC, uploaded_at ASC`,
    [profileId]
  );
  return r.rows;
};
exports.upsertBasicInfo = async (userId, data) => {
  const db = await getDB();
  const ex = await exports.findByUserId(userId);
  if (ex) { await db.query('UPDATE profiles SET first_name=$1,last_name=$2,dob=$3,gender=$4,religion=$5,caste=$6,mother_tongue=$7,marital_status=$8,updated_at=NOW() WHERE user_id=$9', [data.firstName,data.lastName,data.dob,data.gender,data.religion,data.caste,data.motherTongue,data.maritalStatus||'never_married',userId]); return ex; }
  const r = await db.query('INSERT INTO profiles (profile_id,user_id,first_name,last_name,dob,gender,religion,caste,mother_tongue,marital_status) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) RETURNING *', [randomUUID(),userId,data.firstName,data.lastName,data.dob,data.gender,data.religion,data.caste,data.motherTongue,data.maritalStatus||'never_married']);
  return r.rows[0];
};
exports.upsertPhysical = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO physical_details (profile_id,height_cm,weight_kg,complexion,body_type,blood_group) VALUES ($1,$2,$3,$4,$5,$6) ON CONFLICT (profile_id) DO UPDATE SET height_cm=$2,weight_kg=$3,complexion=$4,body_type=$5,blood_group=$6', [p.profile_id,data.heightCm,data.weightKg,data.complexion,data.bodyType,data.bloodGroup]); return p; };
exports.upsertEducation = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO education_career (profile_id,education_level,occupation,annual_income,working_city) VALUES ($1,$2,$3,$4,$5) ON CONFLICT (profile_id) DO UPDATE SET education_level=$2,occupation=$3,annual_income=$4,working_city=$5', [p.profile_id,data.educationLevel,data.occupation,data.annualIncome,data.workingCity]); return p; };
exports.upsertFamily = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO family_details (profile_id,father_occupation,mother_occupation,num_brothers,num_sisters,family_type,family_city) VALUES ($1,$2,$3,$4,$5,$6,$7) ON CONFLICT (profile_id) DO UPDATE SET father_occupation=$2,mother_occupation=$3,num_brothers=$4,num_sisters=$5,family_type=$6,family_city=$7', [p.profile_id,data.fatherOccupation,data.motherOccupation,data.numBrothers||0,data.numSisters||0,data.familyType,data.familyCity]); return p; };
exports.upsertLifestyle = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO lifestyle_details (profile_id,diet,smoking,drinking,about_me) VALUES ($1,$2,$3,$4,$5) ON CONFLICT (profile_id) DO UPDATE SET diet=$2,smoking=$3,drinking=$4,about_me=$5', [p.profile_id,data.diet,data.smoking||'never',data.drinking||'never',data.aboutMe]); return p; };
exports.upsertHoroscope = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO horoscope_details (profile_id,rashi,nakshatra,is_manglik,birth_city,gotra) VALUES ($1,$2,$3,$4,$5,$6) ON CONFLICT (profile_id) DO UPDATE SET rashi=$2,nakshatra=$3,is_manglik=$4,birth_city=$5,gotra=$6', [p.profile_id,data.rashi,data.nakshatra,data.isManglik||false,data.birthCity,data.gotra]); return p; };
exports.upsertPreferences = async (profileId, data) => { const db = await getDB(); await db.query('INSERT INTO partner_preferences (profile_id,age_min,age_max,religion,manglik_pref) VALUES ($1,$2,$3,$4,$5) ON CONFLICT (profile_id) DO UPDATE SET age_min=$2,age_max=$3,religion=$4,manglik_pref=$5,updated_at=NOW()', [profileId,data.ageMin||18,data.ageMax||50,data.religion,data.manglikPref||'any']); };
exports.getPreferences = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT * FROM partner_preferences WHERE profile_id=$1', [profileId]); return r.rows[0] || null; };
exports.findFullByUserId = async (userId) => { const db = await getDB(); const r = await db.query('SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,pd.height_cm,pd.weight_kg,pd.complexion,pd.body_type,pd.blood_group,ec.education_level,ec.occupation,ec.annual_income,ec.working_city,fd.father_occupation,fd.mother_occupation,fd.num_brothers,fd.num_sisters,fd.family_type,fd.family_city,ld.diet,ld.smoking,ld.drinking,ld.about_me,hd.rashi,hd.nakshatra,hd.is_manglik,hd.birth_city,hd.gotra FROM profiles p LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id LEFT JOIN education_career ec ON p.profile_id=ec.profile_id LEFT JOIN family_details fd ON p.profile_id=fd.profile_id LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id WHERE p.user_id=$1 LIMIT 1', [userId]); return r.rows[0] || null; };
exports.findFullById = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT p.*,EXTRACT(YEAR FROM AGE(p.dob))::int AS age,pd.height_cm,pd.weight_kg,pd.complexion,pd.body_type,pd.blood_group,ec.education_level,ec.occupation,ec.annual_income,ec.working_city,fd.father_occupation,fd.mother_occupation,fd.num_brothers,fd.num_sisters,fd.family_type,fd.family_city,ld.diet,ld.smoking,ld.drinking,ld.about_me,hd.rashi,hd.nakshatra,hd.is_manglik,hd.birth_city,hd.gotra FROM profiles p LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id LEFT JOIN education_career ec ON p.profile_id=ec.profile_id LEFT JOIN family_details fd ON p.profile_id=fd.profile_id LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id WHERE p.profile_id=$1 LIMIT 1', [profileId]); return r.rows[0] || null; };
exports.update = async (profileId, data) => { const db = await getDB(); await db.query('UPDATE profiles SET first_name=COALESCE($1,first_name),last_name=COALESCE($2,last_name),updated_at=NOW() WHERE profile_id=$3', [data.firstName,data.lastName,profileId]); };
exports.updatePrivacy = async (profileId, data) => {
  const db = await getDB();
  const allowedPhoto = ['all', 'matches_only', 'private'];
  const allowedVisibility = ['all', 'matches_only', 'hidden'];
  const photoPrivacy = allowedPhoto.includes(data.photoPrivacy) ? data.photoPrivacy : null;
  const profileVisibility = allowedVisibility.includes(data.profileVisibility) ? data.profileVisibility : null;
  await db.query('UPDATE profiles SET photo_privacy=COALESCE($1,photo_privacy),profile_visibility=COALESCE($2,profile_visibility),updated_at=NOW() WHERE profile_id=$3', [photoPrivacy,profileVisibility,profileId]);
};
exports.setPublished = async (profileId, published) => { const db = await getDB(); await db.query('UPDATE profiles SET is_published=$1,updated_at=NOW() WHERE profile_id=$2', [published, profileId]); };
exports.updateVideoUrl = async (profileId, url) => { const db = await getDB(); await db.query('UPDATE profiles SET video_url=$1 WHERE profile_id=$2', [url,profileId]); };
exports.getPhotoCount = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT COUNT(*) FROM profile_photos WHERE profile_id=$1', [profileId]); return parseInt(r.rows[0].count); };
exports.setPrimaryPhoto = async (profileId, photoId) => {
  const db = await getDB();
  const photo = await db.query('SELECT photo_url FROM profile_photos WHERE profile_id=$1 AND photo_id=$2 LIMIT 1', [profileId, photoId]);
  if (!photo.rows[0]) return false;
  await db.query('UPDATE profile_photos SET is_primary=false WHERE profile_id=$1', [profileId]);
  await db.query('UPDATE profile_photos SET is_primary=true WHERE profile_id=$1 AND photo_id=$2', [profileId, photoId]);
  await db.query('UPDATE profiles SET primary_photo_url=$1,updated_at=NOW() WHERE profile_id=$2', [photo.rows[0].photo_url, profileId]);
  return true;
};
exports.deletePhoto = async (profileId, photoId) => {
  const db = await getDB();
  await db.query('DELETE FROM profile_photos WHERE photo_id=$1 AND profile_id=$2', [photoId,profileId]);
  const primary = await db.query('SELECT photo_url FROM profile_photos WHERE profile_id=$1 AND is_primary=true LIMIT 1', [profileId]);
  if (primary.rows[0]) {
    await db.query('UPDATE profiles SET primary_photo_url=$1,updated_at=NOW() WHERE profile_id=$2', [primary.rows[0].photo_url, profileId]);
    return;
  }
  const fallback = await db.query('SELECT photo_id,photo_url FROM profile_photos WHERE profile_id=$1 ORDER BY sequence_order ASC, uploaded_at ASC LIMIT 1', [profileId]);
  if (!fallback.rows[0]) {
    await db.query('UPDATE profiles SET primary_photo_url=NULL,updated_at=NOW() WHERE profile_id=$1', [profileId]);
    return;
  }
  await db.query(
    'UPDATE profile_photos SET is_primary = CASE WHEN photo_id=$2 THEN true ELSE false END WHERE profile_id=$1',
    [profileId, fallback.rows[0].photo_id]
  );
  await db.query('UPDATE profiles SET primary_photo_url=$1,updated_at=NOW() WHERE profile_id=$2', [fallback.rows[0].photo_url, profileId]);
};
exports.recordView = async (viewedProfileId, viewerUserId) => {
  const db = await getDB();
  const viewer = await exports.findByUserId(viewerUserId);
  if (!viewer) return;
  await db.query('INSERT INTO profile_views (viewer_id,viewed_profile_id) VALUES ($1,$2) ON CONFLICT (viewer_id, viewed_profile_id) DO UPDATE SET viewed_at=NOW()', [viewer.profile_id, viewedProfileId]);
};
exports.getViewers = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT p.profile_id,p.user_id,p.first_name,p.last_name,p.primary_photo_url,pv.viewed_at FROM profile_views pv JOIN profiles p ON p.profile_id=pv.viewer_id WHERE pv.viewed_profile_id=$1 ORDER BY pv.viewed_at DESC LIMIT 50', [profileId]); return r.rows; };
exports.blockProfile = async (blockerUserId, profileId) => {
  const db = await getDB();
  const target = await exports.findById(profileId);
  if (!target) return false;
  await db.query(
    'INSERT INTO blocks (block_id,blocker_id,blocked_id) VALUES ($1,$2,$3) ON CONFLICT (blocker_id, blocked_id) DO NOTHING',
    [randomUUID(), blockerUserId, target.user_id]
  );
  return true;
};
exports.reportProfile = async (reporterUserId, profileId, reason, description) => {
  const db = await getDB();
  const target = await exports.findById(profileId);
  if (!target) return false;
  await db.query(
    'INSERT INTO reports (report_id,reporter_id,reported_id,reason,description,status) VALUES ($1,$2,$3,$4,$5,$6)',
    [randomUUID(), reporterUserId, target.user_id, reason || 'other', description || null, 'pending']
  );
  return true;
};
exports.calcCompletion = async (profileId) => {
  const db = await getDB();
  const c = await db.query(`
    SELECT
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM profiles
          WHERE profile_id = $1
            AND NULLIF(BTRIM(first_name), '') IS NOT NULL
            AND NULLIF(BTRIM(last_name), '') IS NOT NULL
            AND dob IS NOT NULL
            AND NULLIF(BTRIM(religion), '') IS NOT NULL
            AND NULLIF(BTRIM(mother_tongue), '') IS NOT NULL
        ) THEN 1 ELSE 0
      END AS basic,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM physical_details
          WHERE profile_id = $1
            AND height_cm IS NOT NULL
            AND height_cm > 0
            AND NULLIF(BTRIM(complexion), '') IS NOT NULL
        ) THEN 1 ELSE 0
      END AS physical,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM education_career
          WHERE profile_id = $1
            AND NULLIF(BTRIM(education_level), '') IS NOT NULL
            AND NULLIF(BTRIM(occupation), '') IS NOT NULL
            AND NULLIF(BTRIM(working_city), '') IS NOT NULL
        ) THEN 1 ELSE 0
      END AS education,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM family_details
          WHERE profile_id = $1
            AND NULLIF(BTRIM(family_type), '') IS NOT NULL
            AND NULLIF(BTRIM(family_city), '') IS NOT NULL
        ) THEN 1 ELSE 0
      END AS family,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM lifestyle_details
          WHERE profile_id = $1
            AND NULLIF(BTRIM(diet), '') IS NOT NULL
            AND LENGTH(BTRIM(COALESCE(about_me, ''))) >= 30
        ) THEN 1 ELSE 0
      END AS lifestyle,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM horoscope_details
          WHERE profile_id = $1
            AND (
              NULLIF(BTRIM(COALESCE(rashi, '')), '') IS NOT NULL
              OR NULLIF(BTRIM(COALESCE(nakshatra, '')), '') IS NOT NULL
              OR NULLIF(BTRIM(COALESCE(birth_city, '')), '') IS NOT NULL
              OR NULLIF(BTRIM(COALESCE(gotra, '')), '') IS NOT NULL
              OR is_manglik IS TRUE
            )
        ) THEN 1 ELSE 0
      END AS horoscope,
      (SELECT COUNT(*) FROM profile_photos WHERE profile_id = $1) AS photos,
      (SELECT video_url FROM profiles WHERE profile_id = $1) AS video
  `, [profileId]);
  const w = { basic:20, physical:10, education:15, family:10, lifestyle:10, horoscope:10, photos:15, video:10 };
  const v = c.rows[0]; let score = 0;
  if (parseInt(v.basic)) score+=w.basic; if (parseInt(v.physical)) score+=w.physical;
  if (parseInt(v.education)) score+=w.education; if (parseInt(v.family)) score+=w.family;
  if (parseInt(v.lifestyle)) score+=w.lifestyle; if (parseInt(v.horoscope)) score+=w.horoscope;
  if (parseInt(v.photos)) score+=w.photos; if (v.video) score+=w.video;
  await db.query('UPDATE profiles SET completion_score=$1 WHERE profile_id=$2', [score,profileId]);
  return score;
};
