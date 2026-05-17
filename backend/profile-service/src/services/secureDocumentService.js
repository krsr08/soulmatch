const crypto = require('crypto');
const fs = require('fs/promises');
const path = require('path');
const { validateDocumentFile } = require('./fileValidation');

const ENCRYPTION_VERSION = 'SMENC1';
const ALGORITHM = 'AES-256-GCM';

function resolveEncryptionKey() {
  const configured = process.env.DOCUMENT_ENCRYPTION_KEY || process.env.KYC_DOCUMENT_ENCRYPTION_KEY;
  const fallback = process.env.INTERNAL_SERVICE_SECRET || process.env.JWT_SECRET || 'soulmatch-local-document-key';
  const raw = configured || fallback;
  const candidate = String(raw).trim();

  if (/^[a-f0-9]{64}$/i.test(candidate)) {
    return Buffer.from(candidate, 'hex');
  }
  try {
    const decoded = Buffer.from(candidate, 'base64');
    if (decoded.length === 32) return decoded;
  } catch (_) {
    // Fall through to hash-derived key.
  }
  return crypto.createHash('sha256').update(candidate).digest();
}

function encryptionKeyRef(key) {
  return `sha256:${crypto.createHash('sha256').update(key).digest('hex').slice(0, 16)}`;
}

async function encryptUploadedFiles(files = []) {
  const key = resolveEncryptionKey();
  const keyRef = encryptionKeyRef(key);

  return Promise.all(files.map(async (file) => {
    if (!file?.path) return file;
    const sourcePath = file.path;
    const detectedType = await validateDocumentFile(file);
    const plaintext = await fs.readFile(sourcePath);
    const iv = crypto.randomBytes(12);
    const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
    const encrypted = Buffer.concat([cipher.update(plaintext), cipher.final()]);
    const tag = cipher.getAuthTag();
    const encryptedFilename = `${path.parse(file.filename || path.basename(sourcePath)).name}.enc`;
    const encryptedPath = path.join(path.dirname(sourcePath), encryptedFilename);
    const payload = Buffer.concat([Buffer.from(ENCRYPTION_VERSION), iv, tag, encrypted]);

    await fs.writeFile(encryptedPath, payload, { mode: 0o600 });
    await fs.rm(sourcePath, { force: true });

    return {
      ...file,
      path: encryptedPath,
      filename: encryptedFilename,
      isEncrypted: true,
      encryptionAlgorithm: ALGORITHM,
      encryptionKeyRef: keyRef,
      encryptionIv: iv.toString('base64'),
      contentSha256: crypto.createHash('sha256').update(plaintext).digest('hex'),
      originalFileName: file.originalname || file.filename || '',
      mimeType: detectedType.type || file.mimetype || 'application/octet-stream',
      fileSizeBytes: file.size || plaintext.length
    };
  }));
}

module.exports = {
  ALGORITHM,
  encryptUploadedFiles
};
