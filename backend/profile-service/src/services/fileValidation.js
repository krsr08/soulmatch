const fs = require('fs/promises');

const IMAGE_SIGNATURES = [
  { type: 'image/jpeg', ext: 'jpg', test: (bytes) => bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff },
  { type: 'image/png', ext: 'png', test: (bytes) => bytes.length >= 8 && bytes.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])) },
  { type: 'image/webp', ext: 'webp', test: (bytes) => bytes.length >= 12 && bytes.subarray(0, 4).toString('ascii') === 'RIFF' && bytes.subarray(8, 12).toString('ascii') === 'WEBP' }
];

const DOCUMENT_SIGNATURES = [
  ...IMAGE_SIGNATURES,
  { type: 'application/pdf', ext: 'pdf', test: (bytes) => bytes.length >= 5 && bytes.subarray(0, 5).toString('ascii') === '%PDF-' }
];

async function readHeader(filePath) {
  const handle = await fs.open(filePath, 'r');
  try {
    const buffer = Buffer.alloc(32);
    const { bytesRead } = await handle.read(buffer, 0, buffer.length, 0);
    return buffer.subarray(0, bytesRead);
  } finally {
    await handle.close();
  }
}

async function detectFileType(filePath, signatures) {
  const header = await readHeader(filePath);
  return signatures.find((signature) => signature.test(header)) || null;
}

async function validateImageFile(file) {
  if (!file?.path) throw new Error('Invalid uploaded image.');
  const detected = await detectFileType(file.path, IMAGE_SIGNATURES);
  if (!detected) throw new Error('Uploaded image content must be a valid JPG, PNG, or WebP file.');
  if (file.mimetype && !IMAGE_SIGNATURES.some((signature) => signature.type === file.mimetype)) {
    throw new Error('Uploaded image MIME type is not supported.');
  }
  return detected;
}

async function validateDocumentFile(file) {
  if (!file?.path) throw new Error('Invalid uploaded document.');
  const detected = await detectFileType(file.path, DOCUMENT_SIGNATURES);
  if (!detected) throw new Error('Uploaded document content must be a valid JPG, PNG, WebP, or PDF file.');
  if (file.mimetype && !DOCUMENT_SIGNATURES.some((signature) => signature.type === file.mimetype)) {
    throw new Error('Uploaded document MIME type is not supported.');
  }
  return detected;
}

module.exports = {
  validateDocumentFile,
  validateImageFile
};
