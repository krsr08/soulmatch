const { createAdapter } = require('@socket.io/redis-adapter');
const { createClient } = require('redis');
const logger = require('../utils/logger');

function redisUrl() {
  if (process.env.REDIS_URL) return process.env.REDIS_URL;
  const host = process.env.REDIS_HOST || 'localhost';
  const port = process.env.REDIS_PORT || '6379';
  return `redis://${host}:${port}`;
}

async function attachSocketRedisAdapter(io) {
  if (process.env.SOCKET_REDIS_ADAPTER_ENABLED === 'false') {
    logger.info('Socket.IO Redis adapter disabled by SOCKET_REDIS_ADAPTER_ENABLED=false');
    return null;
  }
  const pubClient = createClient({ url: redisUrl() });
  const subClient = pubClient.duplicate();
  pubClient.on('error', (err) => logger.warn('Socket Redis pub client error: ' + err.message));
  subClient.on('error', (err) => logger.warn('Socket Redis sub client error: ' + err.message));
  try {
    await Promise.all([pubClient.connect(), subClient.connect()]);
    io.adapter(createAdapter(pubClient, subClient));
    logger.info('Socket.IO Redis adapter attached');
    return { pubClient, subClient };
  } catch (err) {
    logger.warn('Socket.IO Redis adapter unavailable, continuing single-node: ' + err.message);
    await Promise.allSettled([pubClient.quit(), subClient.quit()]);
    return null;
  }
}

module.exports = { attachSocketRedisAdapter };
