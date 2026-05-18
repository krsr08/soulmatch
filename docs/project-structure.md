# SoulMatch Project Structure

This document is the developer map for the current codebase. It explains where future changes should go and which areas should stay separated.

## Android App

```text
android/app/src/main/java/com/soulmatch/app/
  MainActivity.kt
  data/
    api/              Retrofit APIs and request/response models
    auth/             Login routing and post-auth decisions
    local/            DataStore/user preferences
    models/           Shared API DTOs
    payments/         Payment coordinator/result handling
  di/                 Hilt modules
  services/           Crash, Firebase, push, and app services
  ui/
    components/
      cards/          Reusable cards such as profile/match cards
      dialogs/        Shared dialogs
      media/          Member photo and privacy-aware image rendering
      navigation/     Shared drawers and route constants
      premium/        Shared SoulMatch visual primitives
      status/         Shared readiness/status logic
    navigation/       AppNavigation and route wiring
    screens/
      agent/          Agent dashboard, onboarding, account, plans, profiles
      auth/           Welcome, OTP, role selection, member wizard
      chat/           Chat list and conversation screens
      home/           Member home, best matches, notifications
      interests/      Interest received/sent flow
      legal/          Terms and privacy content
      profile/        Member profile, detail, assist, family board, services
      search/         Member search
      settings/       Settings and safety
      subscription/   Plans and payment surfaces
      success/        Success stories
      system/         Launch and maintenance screens
    theme/            Compose colors/type/theme
    viewmodels/       Screen ViewModels
  util/               General utility helpers
```

## Backend

```text
backend/
  auth-service/          Login, OTP, Google/Firebase auth
  profile-service/       Member profiles, agent profiles, KYC, documents, preferences
  matching-service/      Recommendation and compatibility service
  search-service/        Search APIs
  chat-service/          Messaging APIs and socket support
  notification-service/  Push/email/notification APIs
  payment-service/       Razorpay/payment APIs
  admin-service/         Admin review and control APIs
  shared/                Cross-service helpers, config schemas, visibility, entitlements
```

Each Node service follows the same internal shape:

```text
src/
  config/          Environment and service config
  controllers/     HTTP request handling only
  middleware/      Auth, errors, validation, upload middleware
  repositories/    Database reads/writes
  routes/          Express route declarations
  services/        Domain integrations and reusable service logic
  utils/           Small pure helpers
```

## Where To Add New Work

- New member UI: `ui/screens/profile`, `ui/screens/home`, or `ui/screens/auth` depending on flow.
- New agent UI: `ui/screens/agent`. Promote reusable agent widgets into `ui/components` only after they are shared.
- New reusable UI: `ui/components/<category>`.
- New API DTO: `data/models/Models.kt`.
- New API endpoint binding: `data/api/ApiService.kt`.
- New backend profile/agent behavior: `backend/profile-service/src/controllers`, `repositories`, and `services`.
- New admin review behavior: `backend/admin-service`.
- New future infrastructure mode placeholder: `backend/shared/architectureFlags.js` and `backend/shared/configSchemas/operations.json`.

## Architecture References

Read these before making structural changes:

- `docs/ARCHITECTURE_IMPLEMENTATION_CHECKLIST.md` tracks the live status of the architecture modernization work.
- `docs/ARCHITECTURE_FOUNDATION.md` explains the current and target architecture.
- `docs/DEVELOPER_BOUNDARIES.md` defines service ownership and Android/admin mappings.
- `docs/EXTENSION_POINTS.md` explains future placeholders such as BFF, workers, blob storage, search, WAF, and observability.

## Reserved Future Folders

These folders are documented as future architecture locations. Do not create or activate the related infrastructure until the implementation phase starts.

```text
backend/
  mobile-bff/                  Optional future Android API aggregation layer
  workers/
    notification-worker/        Future async push/email/SMS delivery
    trust-score-worker/         Future async trust score recalculation
    search-index-worker/        Future OpenSearch/index sync
    payment-webhook-worker/     Future async payment reconciliation
```

## Refactor Rule

Keep behavior changes separate from structure changes. For large cleanup work, move files and update imports first, compile, then make functional changes in a separate commit.
