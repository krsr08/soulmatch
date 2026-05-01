const express = require('express');
const controller = require('../controllers/searchController');
const { authenticate } = require('../middleware/authMiddleware');
const router = express.Router();
router.post('/basic', authenticate, controller.basicSearch);
router.post('/advanced', authenticate, controller.advancedSearch);
router.get('/saved', authenticate, controller.getSavedSearches);
router.post('/save', authenticate, controller.saveSearch);
module.exports = router;
