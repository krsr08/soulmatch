const fs = require('fs');
const path = require('path');
const sharp = require('sharp');
const { randomUUID } = require('crypto');
exports.savePhotos = async (files, profileId) => {
  const urls = []; const { getDB } = require('../config/database'); const db = await getDB();
  for (const file of files) {
    const processed = await processPhoto(file);
    const url = process.env.USE_LOCAL_STORAGE === 'true' ? '/uploads/' + processed.filename : await uploadToS3(processed, 'profiles/' + profileId + '/photos/' + processed.filename);
    urls.push(url);
    const countR = await db.query('SELECT COUNT(*) FROM profile_photos WHERE profile_id=$1', [profileId]);
    const isPrimary = parseInt(countR.rows[0].count) === 0;
    await db.query('INSERT INTO profile_photos (photo_id,profile_id,photo_url,s3_key,is_primary,sequence_order) VALUES ($1,$2,$3,$4,$5,(SELECT COALESCE(MAX(sequence_order),0)+1 FROM profile_photos WHERE profile_id=$2))', [randomUUID(),profileId,url,processed.filename,isPrimary]);
    if (isPrimary) await db.query('UPDATE profiles SET primary_photo_url=$1 WHERE profile_id=$2', [url,profileId]);
  }
  return urls;
};
exports.saveVideo = async (file, profileId) => { if (process.env.USE_LOCAL_STORAGE === 'true') return '/uploads/' + file.filename; return uploadToS3(file, 'profiles/' + profileId + '/video/' + file.filename); };
const processPhoto = async (file) => {
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
  await s3.send(new PutObjectCommand({ Bucket: process.env.AWS_S3_BUCKET, Key: key, Body: fs.readFileSync(file.path), ContentType: file.mimetype }));
  fs.unlinkSync(file.path);
  return 'https://' + process.env.AWS_S3_BUCKET + '.s3.' + process.env.AWS_REGION + '.amazonaws.com/' + key;
};
