import json
import os

import redis.asyncio as redis

_client = None
FEED_TTL_SECONDS = int(os.getenv("MATCH_FEED_CACHE_TTL_SECONDS", "300"))


async def get_client():
    global _client
    if _client is not None:
        return _client
    url = os.getenv("REDIS_URL")
    if url:
        _client = redis.from_url(url, decode_responses=True)
    else:
        _client = redis.Redis(
            host=os.getenv("REDIS_HOST", "localhost"),
            port=int(os.getenv("REDIS_PORT", "6379")),
            decode_responses=True,
        )
    return _client


def cache_key(user_id: str, page: int, limit: int, verified_only: bool) -> str:
    return f"feed:{user_id}:{int(page)}:{int(limit)}:{1 if verified_only else 0}"


async def get_feed(user_id: str, page: int, limit: int, verified_only: bool):
    try:
        client = await get_client()
        raw = await client.get(cache_key(user_id, page, limit, verified_only))
        return json.loads(raw) if raw else None
    except Exception:
        return None


async def set_feed(user_id: str, page: int, limit: int, verified_only: bool, payload: dict):
    try:
        client = await get_client()
        await client.setex(cache_key(user_id, page, limit, verified_only), FEED_TTL_SECONDS, json.dumps(payload))
    except Exception:
        return None


async def invalidate_feed(*user_ids):
    try:
        client = await get_client()
        keys = []
        for user_id in [item for item in user_ids if item]:
            async for key in client.scan_iter(match=f"feed:{user_id}:*"):
                keys.append(key)
        if keys:
            await client.delete(*keys)
    except Exception:
        return None
