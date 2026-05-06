const express = require('express');
const { body } = require('express-validator');
const controller = require('../controllers/authController');
const { authenticate } = require('../middleware/authMiddleware');
const router = express.Router();
const userTypeVal = body('userType').optional().isIn(['member', 'agent', 'admin']).withMessage('userType must be member, agent, or admin');
const phoneVal = [body('phone').trim().matches(/^\+?[1-9]\d{9,14}$/).withMessage('Valid phone required')];
const otpVal = [
  body('phone').trim().matches(/^\+?[1-9]\d{9,14}$/),
  body('otp').trim().isLength({ min: 6, max: 6 }).isNumeric().withMessage('OTP must be 6 digits'),
  userTypeVal
];
const googleVal = [
  body('googleToken').isString().trim().notEmpty().withMessage('googleToken required'),
  userTypeVal
];
const firebasePhoneVal = [
  body('firebaseToken').isString().trim().notEmpty().withMessage('firebaseToken required'),
  body('phone').optional().trim().matches(/^\+?[1-9]\d{9,14}$/).withMessage('Valid phone required'),
  userTypeVal
];
router.post('/send-otp', [...phoneVal, userTypeVal], controller.sendOTP);
router.post('/verify-otp', otpVal, controller.verifyOTP);
router.post('/google-login', googleVal, controller.googleLogin);
router.post('/firebase-phone-login', firebasePhoneVal, controller.firebasePhoneLogin);
router.post('/select-user-type', authenticate, [userTypeVal], controller.selectUserType);
router.post('/refresh-token', controller.refreshToken);
router.post('/logout', authenticate, controller.logout);
module.exports = router;
