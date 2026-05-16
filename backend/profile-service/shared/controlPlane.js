async function getConfigSection(db, section) {
  const result = await db.query(
    'SELECT config_value FROM app_config WHERE config_key=$1 LIMIT 1',
    [section]
  );
  return result.rows[0]?.config_value || {};
}

module.exports = { getConfigSection };
