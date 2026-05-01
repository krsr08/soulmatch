import os
import asyncpg
_pool = None
async def get_db_pool():
    global _pool
    if _pool: return _pool
    _pool = await asyncpg.create_pool(host=os.getenv("DB_HOST","localhost"), port=int(os.getenv("DB_PORT",5432)), database=os.getenv("DB_NAME","soulmatch_db"), user=os.getenv("DB_USER","soulmatch_user"), password=os.getenv("DB_PASSWORD","soulmatch_pass"), min_size=2, max_size=10)
    return _pool
