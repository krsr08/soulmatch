const { createClient } = require('redis');
const logger = require('../utils/logger');
let redisClient = null;
const getRedis = async () => {
  if (redisClient && redisClient.isOpen) return redisClient;
  redisClient = createClient({ socket: { host: process.env.REDIS_HOST||'localhost', port: parseInt(process.env.REDIS_PORT)||6379 } });
  redisClient.on('error', err => logger.error('Redis error:', err));
  await redisClient.connect();
  return redisClient;
};
module.exports = { getRedis };
