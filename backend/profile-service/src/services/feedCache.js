const { getRedis } = require('../config/redis');
const logger = require('../utils/logger');

async function invalidateMatchFeed(...userIds) {
  try {
    const redis = await getRedis();
    const keys = [];
    for (const userId of userIds.filter(Boolean)) {
      for await (const key of redis.scanIterator({ MATCH: `feed:${userId}:*`, COUNT: 100 })) {
        keys.push(key);
      }
    }
    if (keys.length) await redis.del(keys);
  } catch (error) {
    logger.debug?.(`Match feed cache invalidation skipped: ${error.message}`);
  }
}

async function invalidateAllMatchFeeds() {
  try {
    const redis = await getRedis();
    const keys = [];
    for await (const key of redis.scanIterator({ MATCH: 'feed:*', COUNT: 200 })) {
      keys.push(key);
    }
    if (keys.length) await redis.del(keys);
  } catch (error) {
    logger.debug?.(`Global match feed cache invalidation skipped: ${error.message}`);
  }
}

module.exports = { invalidateAllMatchFeeds, invalidateMatchFeed };
