const express = require('express');
const controller = require('../controllers/publicController');

const router = express.Router();

router.get('/profile/:profileId', controller.renderProfileSharePage);
router.get('/landing/:slug', controller.renderLandingSharePage);

module.exports = router;
