const { Pool } = require('pg');
const logger = require('../utils/logger');
let pool = null;
const getDB = async () => {
  if (pool) return pool;
  pool = new Pool({ host: process.env.DB_HOST||'localhost', port: parseInt(process.env.DB_PORT)||5432, database: process.env.DB_NAME||'soulmatch_db', user: process.env.DB_USER||'soulmatch_user', password: process.env.DB_PASSWORD||'soulmatch_pass', max: 20 });
  pool.on('error', err => logger.error('DB pool error:', err));
  return pool;
};
module.exports = { getDB };
