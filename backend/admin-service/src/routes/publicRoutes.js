const express = require('express');
const controller = require('../controllers/publicController');

const router = express.Router();

router.get('/config', controller.getRuntimeConfig);
router.post('/analytics', controller.trackAnalyticsEvent);
router.get('/profiles/:profileId', controller.getPublicProfile);
router.get('/landing-pages/:slug', controller.getLandingPage);

module.exports = router;
