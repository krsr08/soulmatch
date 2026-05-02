const admin = require('firebase-admin');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const logger = require('../utils/logger');

let initialized = false;

function init() {
  if (initialized) return;
  if (!process.env.FIREBASE_PROJECT_ID || !process.env.FIREBASE_PRIVATE_KEY || !process.env.FIREBASE_CLIENT_EMAIL) {
    return;
  }
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: process.env.FIREBASE_PROJECT_ID,
      privateKey: (process.env.FIREBASE_PRIVATE_KEY || '').replace(/\\n/g, '\n'),
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL
    })
  });
  initialized = true;
}

exports.verifyPhoneToken = async (firebaseToken) => {
  try {
    init();
    if (!initialized) {
      throw new AppError(
        503,
        ErrorCodes.SERVICE_UNAVAILABLE,
        'Firebase phone auth is not configured on the server yet.'
      );
    }
    const decoded = await admin.auth().verifyIdToken(firebaseToken, true);
    const provider = decoded.firebase && decoded.firebase.sign_in_provider;
    if (provider !== 'phone') {
      throw new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Firebase token is not a phone sign-in token.');
    }
    if (!decoded.phone_number) {
      throw new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Firebase token did not include a phone number.');
    }
    return decoded;
  } catch (error) {
    if (error instanceof AppError) throw error;
    logger.warn('Firebase phone token verification failed: ' + error.message);
    throw new AppError(401, ErrorCodes.UNAUTHORIZED, 'Firebase phone verification failed.');
  }
};
