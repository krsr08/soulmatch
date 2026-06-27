require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const authRoutes = require('./routes/authRoutes');
const { errorHandler } = require('./middleware/errorHandler');
const logger = require('./utils/logger');
const { buildCorsOptions } = require('./utils/corsOptions');
const { installExpressObservability } = require('../../shared/observability');
const app = express();
const trustProxyHops = parseInt(process.env.TRUST_PROXY_HOPS || '1', 10);
app.set('trust proxy', Number.isFinite(trustProxyHops) && trustProxyHops > 0 ? trustProxyHops : 1);
if (
  process.env.NODE_ENV === 'production' &&
  process.env.MOCK_OTP === 'true' &&
  process.env.ALLOW_MOCK_OTP_IN_PRODUCTION !== 'true'
) {
  throw new Error('MOCK_OTP=true in production requires ALLOW_MOCK_OTP_IN_PRODUCTION=true.');
}
app.use(helmet());
app.use(cors(buildCorsOptions()));
app.use(rateLimit({ windowMs: 15 * 60 * 1000, max: 100 }));
app.use(express.json({ limit: '10kb' }));
app.use(morgan('combined', { stream: { write: m => logger.info(m.trim()) } }));
installExpressObservability(app, { serviceName: 'auth-service' });
app.use('/api/v1/auth', authRoutes);
app.get('/health', (req, res) => res.json({ status: 'ok', service: 'auth-service' }));
app.use((req, res) => res.status(404).json({ success: false, error: { message: 'Not found' } }));
app.use(errorHandler);
const PORT = process.env.PORT || 3001;
app.listen(PORT, () => logger.info('Auth Service on port ' + PORT));
module.exports = app;
