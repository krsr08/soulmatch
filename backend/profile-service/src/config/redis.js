const { createClient } = require('redis');
const logger = require('../utils/logger');

let redisClient = null;

async function getRedis() {
  if (redisClient?.isOpen) return redisClient;
  const options = process.env.REDIS_URL
    ? { url: process.env.REDIS_URL }
    : {
        socket: {
          host: process.env.REDIS_HOST || 'localhost',
          port: parseInt(process.env.REDIS_PORT || '6379', 10)
        }
      };
  redisClient = createClient(options);
  redisClient.on('error', (err) => logger.warn(`Redis cache error: ${err.message}`));
  await redisClient.connect();
  return redisClient;
}

module.exports = { getRedis };
