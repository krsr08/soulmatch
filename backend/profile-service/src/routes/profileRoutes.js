const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const ctrl = require('../controllers/profileController');
const { authenticate } = require('../middleware/authMiddleware');
const router = express.Router();
const storage = multer.diskStorage({
  destination: (req, file, cb) => { const d = process.env.LOCAL_UPLOAD_DIR || './uploads'; fs.mkdirSync(d, { recursive: true }); cb(null, d); },
  filename: (req, file, cb) => cb(null, Date.now() + '-' + Math.random().toString(36).slice(2) + path.extname(file.originalname))
});
const photoUpload = multer({ storage, limits: { fileSize: 5*1024*1024 }, fileFilter: (req, file, cb) => { const ok = ['image/jpeg','image/png','image/webp'].includes(file.mimetype); cb(ok?null:new Error('JPG/PNG/WebP only'), ok); } });
router.post('/create', authenticate, ctrl.createOrUpdateStep);
router.get('/me', authenticate, ctrl.getMyProfile);
router.get('/:profileId', authenticate, ctrl.getProfile);
router.put('/:profileId', authenticate, ctrl.updateProfile);
router.get('/:profileId/verifications', authenticate, ctrl.getVerifications);
router.post('/:profileId/verifications', authenticate, ctrl.submitVerification);
router.get('/:profileId/photos', authenticate, ctrl.getPhotos);
router.post('/:profileId/photos', authenticate, photoUpload.array('photos', 10), ctrl.uploadPhotos);
router.delete('/:profileId/photos/:photoId', authenticate, ctrl.deletePhoto);
router.put('/:profileId/photos/:photoId/primary', authenticate, ctrl.setPrimaryPhoto);
router.get('/:profileId/preferences', authenticate, ctrl.getPreferences);
router.put('/:profileId/preferences', authenticate, ctrl.updatePreferences);
router.get('/:profileId/completion', authenticate, ctrl.getCompletion);
router.put('/:profileId/privacy', authenticate, ctrl.updatePrivacy);
router.post('/:profileId/view', authenticate, ctrl.recordView);
router.get('/:profileId/viewers', authenticate, ctrl.getViewers);
router.post('/:profileId/block', authenticate, ctrl.blockProfile);
router.post('/:profileId/report', authenticate, ctrl.reportProfile);
module.exports = router;
