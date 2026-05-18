# SoulMatch Analytics Events

This taxonomy defines production analytics events used by the Android app, backend services, and Admin Console funnel. Server milestone events are written by trusted backend services only. Client events are accepted through the public analytics endpoint, rate limited, and never trusted for user identity.

## Event Trust Levels

| Trust level | Source | Identity handling | Usage |
| --- | --- | --- | --- |
| Server-signed | Backend services | `user_id` comes from service-side auth/database context | Admin funnel, payment/auth milestones, operational KPIs |
| Client telemetry | Android app | Caller `userId` is ignored by the public endpoint | Page views, taps, UX diagnostics |

Server-signed events include these payload keys:

| Key | Purpose |
| --- | --- |
| `serverSigned` | Always `true` for backend milestones |
| `recordedBy` | Service name that wrote the event |
| `schemaVersion` | Event schema date/version |

## Server-Signed Product Funnel

| Event | Service | Trigger | Required payload |
| --- | --- | --- | --- |
| `sign_up` | `auth-service` | New OTP, Google, or Firebase phone account is created | `method`, `inviteCode`, `acquisitionSource` |
| `account_type_selected` | `auth-service` | First-time account chooses member or agent | `selectedUserType` |
| `profile_published` | `profile-service` | Member profile reaches publish criteria | `profileId`, `completionScore`, `step`, `profileCreatedBy` |
| `interest_sent` | `matching-service` | Member sends or resends interest | `interestId`, `senderProfileId`, `receiverProfileId`, `receiverUserId`, `resent` |
| `interest_accepted` | `matching-service` | Receiver accepts an interest | `interestId`, `senderUserId`, `receiverUserId` |
| `chat_created` | `matching-service` / `chat-service` | Accepted interest opens a chat conversation | `chatId`, `interestId`, participant user IDs |
| `payment_order_created` | `payment-service` | Razorpay order is created | `planId`, `requestedPlanId`, `gateway`, `amount`, `providerOrderId` |
| `payment_click` | `payment-service` | Payment order creation starts the purchase flow | `planId`, `requestedPlanId`, `gateway`, `amount`, `purchaseAction` |
| `payment_success` | `payment-service` | Client verify or webhook activates a paid order | `planId`, `gateway`, `amount`, `subscriptionId`, `source` |
| `payment_failed` | `payment-service` | Razorpay webhook reports failure | `planId`, `gateway`, `amount`, `providerOrderId`, `providerPaymentId` |
| `subscription_activated` | `payment-service` | Subscription row and usage period are activated | `planId`, `gateway`, `amount`, `subscriptionId`, `source` |

## Client Telemetry

Android batches client telemetry to `POST /public/analytics` using `events: [...]`.

| Event | Trigger | Payload |
| --- | --- | --- |
| `page_view` | Navigation route changes | `route`, `clientEventId`, `appVersion` |
| `click` | Explicit tracked tap target | `route`, `target`, `clientEventId`, `appVersion` |

Public analytics controls:

- Unsupported event types return `400`.
- Payloads above the configured byte limit return `413`.
- Batches are capped by `PUBLIC_ANALYTICS_MAX_BATCH`.
- Rate limits are applied per client IP/window.
- Public analytics ignores caller-supplied `userId`.

## Admin Funnel

`GET /admin/analytics/funnel` is backed by real SQL tables, not mock events:

| Funnel step | SQL source |
| --- | --- |
| Signup completed | `users.created_at` |
| Profile published | `profiles.is_published` and `profiles.updated_at` |
| Interest sent | `interests.sent_at` |
| Chat created | `chat_conversation_metadata.created_at` |
| Payment completed | `transactions`, `payment_orders`, `subscriptions` |

The endpoint returns a 500 error if the SQL path is unavailable. It must not show empty mock funnel data on production errors.
