const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const test = require('node:test');
const { validateDocumentFile, validateImageFile } = require('../src/services/fileValidation');

function tempFile(name, bytes) {
  const filePath = path.join(os.tmpdir(), `soulmatch-${Date.now()}-${Math.random().toString(36).slice(2)}-${name}`);
  fs.writeFileSync(filePath, Buffer.from(bytes));
  return filePath;
}

test('image validation accepts real image magic bytes and rejects renamed text', async () => {
  const png = tempFile('photo.png', [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00]);
  const text = tempFile('fake.jpg', Buffer.from('not an image'));
  try {
    const detected = await validateImageFile({ path: png, mimetype: 'image/png' });
    assert.equal(detected.type, 'image/png');
    await assert.rejects(
      () => validateImageFile({ path: text, mimetype: 'image/jpeg' }),
      /valid JPG, PNG, or WebP/
    );
  } finally {
    fs.rmSync(png, { force: true });
    fs.rmSync(text, { force: true });
  }
});

test('document validation accepts PDF magic bytes', async () => {
  const pdf = tempFile('document.pdf', Buffer.from('%PDF-1.7\n'));
  try {
    const detected = await validateDocumentFile({ path: pdf, mimetype: 'application/pdf' });
    assert.equal(detected.type, 'application/pdf');
  } finally {
    fs.rmSync(pdf, { force: true });
  }
});
