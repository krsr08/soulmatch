# Play Store Data Safety Alignment

SoulMatch collects matrimony profile data, contact details, photos, verification documents, chat metadata, payment events, analytics events, and device notification tokens.

Before Play Store submission:

- Match disclosed data types to `docs/ANALYTICS_EVENTS.md`.
- Disclose account creation, profile matching, messaging, payments, safety reporting, and fraud prevention as collection purposes.
- Mark KYC and verification documents as sensitive personal information.
- Confirm data deletion flow uses `POST /api/v1/profile/delete-account`.
- Confirm data export flow uses `GET /api/v1/profile/export`.
- Confirm third-party processors: Firebase/FCM, Google Sign-In, Razorpay, Twilio if live, cloud hosting/storage.
- Confirm encryption in transit through HTTPS gateway and encryption at rest for databases/storage.
