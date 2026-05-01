const { Pool } = require('pg');
let pool = null;
const getDB = async () => {
  if (pool) return pool;
  const configuredHost = process.env.DB_HOST || 'localhost';
  const host = configuredHost === 'postgres' && process.platform === 'win32' ? 'localhost' : configuredHost;
  pool = new Pool({ host, port:parseInt(process.env.DB_PORT)||5432, database:process.env.DB_NAME||'soulmatch_db', user:process.env.DB_USER||'soulmatch_user', password:process.env.DB_PASSWORD||'soulmatch_pass', max:10 });
  return pool;
};
module.exports = { getDB };
