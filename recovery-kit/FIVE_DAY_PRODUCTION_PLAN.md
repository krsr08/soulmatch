# Five Day Production MVP Plan

Yes, SoulMatch can be production-ready for a controlled real-user pilot in 5 days.

No, it cannot become Shaadi-scale in 5 days. The realistic target is:

1. Secure login and account handling.
2. Real profile creation and editing.
3. Clean match/search/profile UX.
4. Working notifications and payments.
5. Admin verification controls.
6. Backups, CI/CD, monitoring, and recovery.
7. No known crashers in core flows.

## Day 1: Stability and Data Integrity

Status: In progress

Must finish:

1. Fix profile loading so logged-in users always see their own profile.
2. Keep all profile and settings changes in audit logs.
3. Add richer partner preferences and match feedback.
4. Verify no dummy data appears for authenticated production users.
5. Run backend syntax checks and Android compile.

Exit criteria:

1. Login works on release APK.
2. Existing user login does not restart new profile creation.
3. My Profile shows real cloud data.
4. Partner preferences save without errors.

## Day 2: Trust, Safety, and Verification

Status: Started

Must finish:

1. Admin verification approve/reject queue.
2. Request verification CTA only when not verified or rejected.
3. Photo privacy and access request flow.
4. Block/report/chat restrictions.
5. Trust score explanation on profile and cards.

Exit criteria:

1. A verified profile shows verified everywhere.
2. A hidden/private photo cannot be seen unless allowed.
3. Reported/blocked users cannot continue unsafe contact.

## Day 3: Payments and Subscription Confidence

Status: Started

Must finish:

1. Payment success/failure pages.
2. Subscription history line items.
3. Prevent duplicate active monthly plans.
4. Upgrade/downgrade/renew rules.
5. Razorpay webhook readiness.

Exit criteria:

1. Paid plan updates after payment verify.
2. Transaction ID, status, amount, plan, and paid date are visible.
3. Downgrade is blocked while higher plan is active.

## Day 4: Production Operations

Status: Started

Must finish:

1. GitHub Actions deploy pipeline green.
2. Azure VM backup schedule.
3. Local backup sync to laptop.
4. Health monitoring.
5. Recovery kit verified on clean steps.

Exit criteria:

1. A push to main deploys automatically.
2. VM can be restored from backup.
3. Local laptop can rebuild APK and restart cloud deployment.

## Day 5: QA Pilot Release

Status: Not started

Must finish:

1. End-to-end QA on two phones.
2. Payment test card QA.
3. Firebase OTP and Google Sign-In QA.
4. Profile, matches, search, chat, activity, subscription QA.
5. Pilot release through Firebase App Distribution.

Exit criteria:

1. No crash in core flows.
2. No confusing dead-end screens.
3. No secret exposed in GitHub.
4. Recovery documentation is usable by a non-coding person.

