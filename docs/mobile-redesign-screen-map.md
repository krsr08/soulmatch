# SoulMatch Mobile Redesign Screen Map

Source assets:
- `C:\Users\ANIRUDH\Downloads\SoulMatch Screens`
- `C:\Users\ANIRUDH\Downloads\SoulMatch_pen.pen`

Notes:
- Agent flow is deferred for this migration pass.
- Existing backend behavior must stay intact while screens are redesigned.
- Export numbering skips `04` and `09`; `10a` is an overlay state.

| # | Exported screen | Android route / owner | Status | Phase |
| --- | --- | --- | --- | --- |
| 01 | Splash screen | `system/LaunchBrandScreen` / launch path | Modify | Auth/onboarding |
| 02 | Language selection | New member pre-auth route | New | Auth/onboarding |
| 03 | Login screen | `welcome`, `phone_entry` | Modify | Auth/onboarding |
| 05 | OTP verification | `otp/{phone}` | Modify | Auth/onboarding |
| 06 | Forgot password | New auth route | New | Auth/onboarding |
| 07 | Reset password | New auth route | New | Auth/onboarding |
| 08 | Onboarding benefit | New auth/onboarding route | New | Auth/onboarding |
| 10 | Profile creation intro | `profile_wizard/1` entry | Modify | Profile creation |
| 10a | Profile creation info overlay | Profile wizard modal state | New | Profile creation |
| 11 | Basic details | `profile_wizard/1` | Modify | Profile creation |
| 12 | Religious/community details | `profile_wizard/2` | Modify | Profile creation |
| 13 | Education/career | `profile_wizard/3` | Modify | Profile creation |
| 14 | Family details | `profile_wizard/4` | Modify | Profile creation |
| 15 | Lifestyle details | `profile_wizard/5` | Modify | Profile creation |
| 16 | Partner preferences | `profile_wizard/6`, `partner_preferences` | Modify | Profile creation |
| 17 | Photo upload | Profile wizard photo section | New/Modify | Profile creation |
| 18 | Verification | `trust_details`, profile verification section | Modify | Profile creation |
| 19 | Profile preview | `my_profile`, preview mode | Modify | Profile creation |
| 20 | Profile under review | New profile review state | New | Profile creation |
| 21 | Profile rejected/correction required | New correction state | New | Profile creation |
| 22 | Home dashboard | `dashboard` | Modify | Main app |
| 23 | Matches listing | `best_matches`, `search` | Modify | Main app |
| 24 | Advanced search filter | Current search/home filter dialog | Modify | Main app |
| 25 | Match profile detail | `profile/{profileId}` | Modify | Main app |
| 26 | Express interest bottom sheet | Profile/match interest sheet | New/Modify | Main app |
| 27 | Interest sent toast state | Snackbar/toast state | New/Modify | Main app |
| 28 | Shortlisted profiles | `interests?tab=shortlisted` | Modify | Main app |
| 29 | Viewed my profile | `interests?tab=visitors` | Modify | Main app |
| 30 | My viewed profiles | `interests?tab=viewed` | Modify | Main app |
| 31 | Interests | `interests` | Modify | Main app |
| 32 | Notifications | `notifications` | Modify | Main app |
| 33 | Empty no matches | Empty state for `dashboard`, `search`, `best_matches` | Modify | Main app |
| 34 | Messages chat list | `chat_list` | Modify | Chat/profile |
| 35 | Chat detail | `chat/{participantId}/{name}` | Modify | Chat/profile |
| 36 | My profile | `my_profile` | Modify | Chat/profile |
| 37 | Edit profile | `profile_wizard/{step}?returnToProfile=true` | Modify | Chat/profile |
| 38 | Subscription plans | `subscription` | Modify | Subscription/settings |
| 39 | Payment | `subscription` payment flow | Modify | Subscription/settings |
| 40 | Payment success | Payment result state | Modify | Subscription/settings |
| 41 | Payment failure | Payment result state | Modify | Subscription/settings |
| 42 | Settings | `settings` | Modify | Subscription/settings |
| 43 | Privacy settings | Settings privacy section | Modify | Subscription/settings |
| 44 | Notification settings | Settings notification section | Modify | Subscription/settings |
| 45 | Help and support | `help_support` | Modify | Subscription/settings |
| 46 | Delete account | Settings delete account flow | Modify | Subscription/settings |
| 47 | Logout confirmation modal | Settings logout modal | Modify | Subscription/settings |

## Current Route Owners

| Area | Main files |
| --- | --- |
| Auth/onboarding | `android/app/src/main/java/com/soulmatch/app/ui/screens/auth/*` |
| Profile creation | `android/app/src/main/java/com/soulmatch/app/ui/screens/auth/ProfileWizardScreen.kt` |
| Main app | `android/app/src/main/java/com/soulmatch/app/ui/screens/home/*`, `search/*`, `interests/*` |
| Chat/profile | `android/app/src/main/java/com/soulmatch/app/ui/screens/chat/*`, `profile/*` |
| Subscription/settings | `android/app/src/main/java/com/soulmatch/app/ui/screens/subscription/*`, `settings/*` |
| Navigation | `android/app/src/main/java/com/soulmatch/app/ui/navigation/AppNavigation.kt` |

## Implementation Order

1. Auth/onboarding
2. Profile creation
3. Main app discovery/matches
4. Chat/profile/interests
5. Subscription/settings/delete
6. Agent screens later

## Profile Creation State Targets

| Export | Existing implementation target | Required behavior to preserve |
| --- | --- | --- |
| 17 Photo upload | `MyProfileScreen.kt` photo picker and `ProfileReferenceSections.kt` photo card | Multi-photo upload, primary photo, preview, delete, local pending photo state. |
| 18 Verification | `MyProfileScreen.kt` trust/verification area | Submit admin verification, show pending/verified/rejected status, preserve review notes. |
| 19 Profile preview | `MyProfileScreen.kt`, `ProfileDetailScreen.kt` preview-like read surface | Load saved profile data after login/restart, show photo/privacy/trust details. |
| 20 Under review | `ProfileVerificationCard` pending status in `MyProfileScreen.kt` | Admin review pending state and notification path must remain functional. |
| 21 Correction required | `ProfileVerificationCard` rejected status in `MyProfileScreen.kt` | Show admin note, allow profile/photo correction, allow resubmission. |

Profile wizard route stays `profile_wizard/{step}` for now so the existing save-per-step contract remains stable.
