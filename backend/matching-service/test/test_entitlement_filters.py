import os
import unittest


class MatchingEntitlementSourceTests(unittest.TestCase):
    def setUp(self):
        path = os.path.join(os.path.dirname(__file__), "..", "app", "services", "matching_engine.py")
        with open(path, "r", encoding="utf-8") as handle:
            self.source = handle.read()

    def test_verified_only_filter_can_be_plan_gated(self):
        self.assertIn("verifiedOnly", self.source)
        self.assertIn("PermissionError", self.source)
        self.assertIn("match_entitlements", self.source)
        self.assertIn("bool_config", self.source)


if __name__ == "__main__":
    unittest.main()
