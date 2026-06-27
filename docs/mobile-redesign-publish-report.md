# SoulMatch Mobile Redesign Publish Report

Date: 2026-06-27

## Published Scope

- Applied the redesign work to `main`.
- Preserved Agent flow for a later phase.
- Fixed member OTP login routing so member auth requests pass `userType=member`.
- Added first-run language selection and onboarding benefit routes.
- Restyled login closer to the supplied design with language back-link, member OTP login, Google login, and trust note.
- Added forgot-password and reset-password screens matching the supplied visual flow.
- Added persisted first-run intro preferences for language and onboarding completion.
- Disabled the Kotlin compile daemon for local builds because it repeatedly hung on Windows; in-process Kotlin compilation passes.

## Functional Verification

- Local Docker auth OTP round trip verified with mock OTP `123456`.
- Android debug build verified after the intro/login changes.
- Existing GitHub Actions production workflow on `main` will:
  - run backend tests and admin build,
  - build Android debug and release APKs,
  - upload APK artifacts,
  - distribute debug APK to Firebase App Distribution when Firebase secrets are configured,
  - deploy backend/admin services to Azure VM when Azure deploy secrets are configured.

## Known Gaps To Add Later

- Password reset backend is not present in the current OTP-first auth service. The forgot/reset screens are implemented visually, but a real reset API must be added before password reset can be production-functional.
- Play Store publishing is not configured locally in this repo. Current publish path is GitHub Actions artifacts/Firebase distribution/Azure deploy.
- Azure deploy is skipped cleanly when `AZURE_VM_HOST`, `AZURE_VM_USER`, `AZURE_VM_SSH_KEY`, or SSH host reachability is not ready in GitHub Actions.
- Pencil MCP is installed but was not connected to the running Pencil app during this pass, so exported PNG/HTML assets were used as source of truth.
- Full pixel-close migration for all remaining account/payment/chat/detail screens still needs continued phase work; current publish focuses on keeping the app buildable and improving the broken member login and missing pre-auth screens.
