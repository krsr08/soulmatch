# SoulMatch Module Ownership Map

This is the quick navigation map for developers. Use it before changing any flow so the edit lands in the right module and does not accidentally affect Member, Agent, or Admin behavior.

## How To Read This Map

- Android files own presentation and local screen state.
- Backend services own truth, validation, privacy, entitlement, and persistence.
- Admin web owns operator workflows and configuration screens.
- Shared backend files own rules used by more than one service.

## Member Flow

| Capability | Android Screen | Android ViewModel | Backend Owner | Main Data Area | Admin Surface | Tests To Check |
| --- | --- | --- | --- | --- | --- | --- |
| Login / OTP | `ui/screens/auth/PhoneEntryScreen.kt`, `OTPVerificationScreen.kt` | `AuthViewModel.kt` | `auth-service` | `users`, token/OTP tables, Redis OTP cache | System / auth audit | `backend/auth-service/test/*`, `test_folder/auth-flow-smoke.js` |
| First-time role selection | `ui/screens/auth/RoleSelectionScreen.kt` | `AuthViewModel.kt` | `auth-service`, `profile-service` | `users.user_type`, profile bootstrap rows | Members / Agents | Android auth unit tests, smoke login |
| Member profile wizard | `ui/screens/auth/ProfileWizardScreen.kt` | `ProfileViewModel.kt` | `profile-service` | `profiles`, profile detail tables | Members 360 view | `backend/profile-service/test/*` |
| Best matches feed | `ui/screens/home/BestMatchesScreen.kt`, `DashboardScreen.kt` | `DashboardViewModel.kt` | `matching-service`, `profile-service` | `profiles`, `partner_preferences`, `blocks`, `interests` | CMS home cards, Members | `backend/matching-service/test/*`, search/filter tests |
| Refine filters / Discover | `ui/screens/search/SearchScreen.kt` | `SearchViewModel.kt` | `search-service` | profile filter columns | CMS filter options later | `backend/search-service/test/searchFilters.test.js` |
| Profile details | `ui/screens/profile/ProfileDetailScreen.kt` | `ProfileDetailViewModel.kt` | `profile-service`, `matching-service` | profile rows, photos, trust, contact unlocks | Members 360 view, Verification | profile visibility and entitlement tests |
| My profile | `ui/screens/profile/MyProfileScreen.kt` | `MyProfileViewModel.kt` | `profile-service` | profile rows, privacy, photos, preferences | Members 360 view | profile tests, Android profile tests |
| Partner preferences | `ui/screens/profile/ProfileServiceScreens.kt` | `MyProfileViewModel.kt` | `profile-service`, read by `matching-service` | `partner_preferences` | Members 360 view | matching preference tests |
| SoulMatch Assist | `ui/screens/profile/ProfileServiceScreens.kt` | `MyProfileViewModel.kt` | `profile-service`, `admin-service` | assist requests, agent sharing rows | Assist / Agents | assist allocation tests |
| Activity / interests | `ui/screens/interests/InterestsScreen.kt` | `InterestsViewModel.kt` | `matching-service` | `interests`, conversations on accept | Members activity | interest transaction tests |
| Chat | `ui/screens/chat/ChatListScreen.kt`, `ChatScreen.kt` | `ChatListViewModel.kt`, `ChatThreadViewModel.kt` | `chat-service`, `matching-service` eligibility | Mongo conversations/messages, interest gates | Chat moderation | chat IDOR and scaling tests |
| Upgrade / subscription | `ui/screens/subscription/*` | `SubscriptionViewModel.kt` | `payment-service`, shared entitlements | `subscriptions`, `payment_orders`, invoices | Subscriptions, CMS monetization | payment and entitlement tests |
| Safety / privacy | `ui/screens/settings/SettingsScreen.kt`, `ui/screens/profile/ProfileServiceScreens.kt` | `SettingsViewModel.kt`, `MyProfileViewModel.kt` | `profile-service`, `admin-service` config | privacy fields, reports, blocks | Content moderation, CMS safety content | block/report/privacy tests |

## Agent Flow

| Capability | Android Screen | Android ViewModel | Backend Owner | Main Data Area | Admin Surface | Tests To Check |
| --- | --- | --- | --- | --- | --- | --- |
| Agent onboarding | `ui/screens/agent/AgentOnboardingScreen.kt` | `AgentViewModel.kt` | `profile-service` | agent profile, KYC docs, bank verification, terms acceptance | Agent Verification | profile-service agent tests |
| Agent dashboard | `ui/screens/agent/AgentDashboardScreen.kt`, `AgentShell.kt` | `AgentViewModel.kt` | `profile-service`, `admin-service` summary config | agent stats, added profiles | Agents, Agent Performance | agent dashboard smoke/manual |
| Agent account | `ui/screens/agent/AgentAccountScreen.kt` | `AgentViewModel.kt` | `profile-service` | agent details, active status, commission config | Agents 360 view | profile-service agent tests |
| Add member by agent | `ui/screens/agent/AgentClientProfileScreen.kt`, `AgentProfilesScreen.kt` | `AgentViewModel.kt` | `profile-service` | member profile rows with created_by agent, visibility, verification status | Verify Members, Agent-wise profiles | profile visibility and verification tests |
| Agent plans | `ui/screens/agent/AgentPlansScreen.kt` | `AgentViewModel.kt` | `payment-service`, `admin-service` config | agent subscriptions | Agent Subscriptions | payment tests |
| Agent activities | `ui/screens/agent/AgentActivitiesScreen.kt` | `AgentViewModel.kt` | `profile-service` | profile activity/audit rows | Agents activity | profile-service tests |

## Admin Console

| Admin Area | Admin Web File | Backend Owner | Data / Config | Critical Boundary |
| --- | --- | --- | --- | --- |
| Login | `admin-web/src/pages/LoginPage.js` | `admin-service` | admin sessions, roles | Never store long-lived privileged tokens in browser storage. |
| Dashboard and management pages | `admin-web/src/pages/DashboardPage.js` | `admin-service` | aggregated SQL, service health, audit logs | Keep operator actions audited. |
| Runtime config context | `admin-web/src/context/RuntimeConfigContext.js` | `admin-service` public config | `app_config` public sections | Do not expose private `operations` config to Android. |
| Admin API client | `admin-web/src/api/adminApi.js` | `admin-service` | REST routes | Add new admin APIs here before screens call them. |
| Assist panel | `admin-web/src/pages/AssistPanel.js` | `admin-service`, `profile-service` | assist queue | Agent sharing must respect member consent. |

## Shared Rule Files

| File | Purpose | When To Change |
| --- | --- | --- |
| `backend/shared/profileVisibility.js` | Redacts profile/photo/contact data for privacy, block, status, and entitlement rules. | Any feature that changes who can see profiles, photos, or contact details. |
| `backend/shared/memberEntitlements.js` | Normalizes plan limits and feature access rules. | Any subscription/plan/upgrade/limit change. |
| `backend/shared/controlPlane.js` | Loads and normalizes runtime config from `app_config`. | Any backend/admin-configurable behavior. |
| `backend/shared/architectureFlags.js` | Documents inactive infrastructure modes for future WAF/BFF/blob/queue/search/observability adapters. | Only when adding a future adapter or environment mode. |
| `backend/shared/observability.js` | Request metrics and structured logging helpers. | Any service-level monitoring change. |

## Change Safety Checklist

Before editing a flow:

1. Identify the flow in this map.
2. Update backend truth first if the change affects privacy, payment, verification, or entitlement.
3. Update Android/Admin UI only after the API contract is clear.
4. Add or update tests in the owner service.
5. Run at least the focused owner tests plus smoke tests.
6. Keep Member and Agent changes separate unless a shared rule is intentionally changed.

