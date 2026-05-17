import os
import sys
import types
import unittest

from fastapi import HTTPException

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

matching_engine_stub = types.ModuleType("app.services.matching_engine")


async def _unused(*_args, **_kwargs):
    raise AssertionError("matching engine should not be called in route guard tests")


matching_engine_stub.get_recommended_matches = _unused
matching_engine_stub.get_compatibility = _unused
sys.modules["app.services.matching_engine"] = matching_engine_stub

auth_stub = types.ModuleType("app.middleware.auth")
auth_stub.get_current_user = lambda: {"userId": "user-1"}
sys.modules["app.middleware.auth"] = auth_stub

from app.routes.matches import _RATE_BUCKETS, rate_limited_user


class MatchingSecurityTests(unittest.TestCase):
    def setUp(self):
        _RATE_BUCKETS.clear()
        os.environ["MATCH_RATE_WINDOW_SECONDS"] = "60"
        os.environ["MATCH_RATE_LIMIT"] = "2"

    def tearDown(self):
        _RATE_BUCKETS.clear()
        os.environ.pop("MATCH_RATE_WINDOW_SECONDS", None)
        os.environ.pop("MATCH_RATE_LIMIT", None)

    def test_rate_limiter_blocks_after_user_limit(self):
        current_user = {"userId": "user-1"}

        self.assertEqual(rate_limited_user(current_user), current_user)
        self.assertEqual(rate_limited_user(current_user), current_user)

        with self.assertRaises(HTTPException) as ctx:
            rate_limited_user(current_user)

        self.assertEqual(ctx.exception.status_code, 429)

    def test_rate_limiter_requires_user_identity(self):
        with self.assertRaises(HTTPException) as ctx:
            rate_limited_user({})

        self.assertEqual(ctx.exception.status_code, 401)


if __name__ == "__main__":
    unittest.main()
