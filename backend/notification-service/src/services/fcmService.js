const admin = require('firebase-admin');
const logger = require('../utils/logger');
let initialized = false;
const init = () => {
  if (initialized || !process.env.FIREBASE_PROJECT_ID) return;
  admin.initializeApp({ credential: admin.credential.cert({ projectId:process.env.FIREBASE_PROJECT_ID, privateKey:(process.env.FIREBASE_PRIVATE_KEY||'').replace(/\\n/g,'\n'), clientEmail:process.env.FIREBASE_CLIENT_EMAIL }) });
  initialized = true;
};
exports.sendToDevice = async (token, title, body, data) => {
  try {
    init();
    if (!initialized) throw new Error('Firebase Admin is not configured.');
    const response = await admin.messaging().send({ token, notification:{ title, body }, data:Object.fromEntries(Object.entries(data||{}).map(([k,v])=>[k,String(v)])), android:{ priority:'high', notification:{ sound:'default', channelId:'soulmatch_default' } } });
    logger.info('FCM sent: '+response);
    return response;
  } catch (err) {
    logger.error('FCM error: '+err.message);
    throw err;
  }
};
