const fs = require('fs');
const path = require('path');
const sharp = require('sharp');
const { randomUUID } = require('crypto');
const { validateImageFile } = require('./fileValidation');

function encodeMediaKey(key) {
  return Buffer.from(String(key || ''), 'utf8').toString('base64url');
}

function decodeMediaKey(token) {
  try {
    const key = Buffer.from(String(token || ''), 'base64url').toString('utf8');
    return key && !key.includes('..') ? key : null;
  } catch (_) {
    return null;
  }
}

function signedMediaRoute(key) {
  return `/api/v1/profile/media/${encodeMediaKey(key)}`;
}

exports.savePhotos = async (files, profileId) => {
  const urls = []; const { getDB } = require('../config/database'); const db = await getDB();
  const autoApprove = process.env.AUTO_APPROVE_PROFILE_PHOTOS === 'true';
  for (const file of files) {
    const processed = await processPhoto(file);
    const key = 'profiles/' + profileId + '/photos/' + processed.filename;
    const url = process.env.USE_LOCAL_STORAGE === 'true' ? '/uploads/' + processed.filename : await uploadToS3(processed, key);
    urls.push(url);
    const countR = await db.query('SELECT COUNT(*) FROM profile_photos WHERE profile_id=$1', [profileId]);
    const isPrimary = parseInt(countR.rows[0].count) === 0;
    await db.query(
      `INSERT INTO profile_photos (photo_id,profile_id,photo_url,s3_key,is_primary,is_approved,review_status,sequence_order)
       VALUES ($1,$2,$3,$4,$5,$6,$7,(SELECT COALESCE(MAX(sequence_order),0)+1 FROM profile_photos WHERE profile_id=$2))`,
      [randomUUID(),profileId,url,key,isPrimary,autoApprove,autoApprove ? 'approved' : 'pending']
    );
    if (isPrimary && autoApprove) await db.query('UPDATE profiles SET primary_photo_url=$1 WHERE profile_id=$2', [url,profileId]);
  }
  return urls;
};
exports.saveVideo = async (file, profileId) => { if (process.env.USE_LOCAL_STORAGE === 'true') return '/uploads/' + file.filename; return uploadToS3(file, 'profiles/' + profileId + '/video/' + file.filename); };
const processPhoto = async (file) => {
  await validateImageFile(file);
  const outputName = randomUUID() + '.jpg';
  const outputPath = path.join(path.dirname(file.path), outputName);
  await sharp(file.path)
    .rotate()
    .resize({ width: 1600, height: 1600, fit: 'inside', withoutEnlargement: true })
    .jpeg({ quality: 82, mozjpeg: true })
    .toFile(outputPath);
  fs.unlinkSync(file.path);
  return { ...file, path: outputPath, filename: outputName, mimetype: 'image/jpeg' };
};
const uploadToS3 = async (file, key) => {
  const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3');
  const s3 = new S3Client({ region: process.env.AWS_REGION });
  await s3.send(new PutObjectCommand({
    Bucket: process.env.AWS_S3_BUCKET,
    Key: key,
    Body: fs.readFileSync(file.path),
    ContentType: file.mimetype,
    ServerSideEncryption: 'AES256'
  }));
  fs.unlinkSync(file.path);
  return signedMediaRoute(key);
};

exports.redirectToSignedMedia = async (token, res) => {
  const key = decodeMediaKey(token);
  if (!key) {
    res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Media file not found.' } });
    return;
  }
  if (process.env.USE_LOCAL_STORAGE === 'true') {
    res.redirect(302, '/uploads/' + path.basename(key));
    return;
  }
  const { S3Client, GetObjectCommand } = require('@aws-sdk/client-s3');
  const { getSignedUrl } = require('@aws-sdk/s3-request-presigner');
  const s3 = new S3Client({ region: process.env.AWS_REGION });
  const signedUrl = await getSignedUrl(
    s3,
    new GetObjectCommand({ Bucket: process.env.AWS_S3_BUCKET, Key: key }),
    { expiresIn: Number(process.env.SIGNED_MEDIA_URL_TTL_SECONDS || 300) }
  );
  res.set('Cache-Control', 'private, max-age=60');
  res.redirect(302, signedUrl);
};

exports._test = {
  decodeMediaKey,
  encodeMediaKey,
  signedMediaRoute
};
