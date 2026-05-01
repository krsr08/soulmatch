require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const path = require('path');
const profileRoutes = require('./routes/profileRoutes');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');
const logger = require('./utils/logger');
const app = express();
app.use(helmet()); app.use(cors()); app.use(express.json({ limit: '10mb' }));
if (process.env.USE_LOCAL_STORAGE === 'true')
  app.use('/uploads', require('express').static(path.join(__dirname, '..', 'uploads')));
app.use('/api/v1/profile', profileRoutes);
app.get('/health', (req, res) => res.json({ status: 'ok', service: 'profile-service' }));
app.use(notFoundHandler);
app.use(errorHandler);
const PORT = process.env.PORT || 3002;
app.listen(PORT, () => logger.info('Profile Service on port ' + PORT));
module.exports = app;
