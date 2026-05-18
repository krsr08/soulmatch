import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))


class InterestTransactionSourceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with open(
            os.path.join(os.path.dirname(__file__), "..", "app", "services", "interest_service.py"),
            encoding="utf-8",
        ) as handle:
            cls.source = handle.read()

    def test_acceptance_uses_database_transaction_and_outbox(self):
        self.assertIn("async with conn.transaction():", self.source)
        self.assertIn("FOR UPDATE", self.source)
        self.assertIn("matching_outbox", self.source)
        self.assertIn("chat_conversation_create", self.source)

    def test_acceptance_creates_internal_chat_conversation(self):
        self.assertIn("/internal/conversations", self.source)
        self.assertIn("'participants': [str(sender_user_id), str(receiver_user_id)]", self.source)
        self.assertIn("'x-internal-service-secret'", self.source)


if __name__ == "__main__":
    unittest.main()
