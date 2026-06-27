# SoulMatch Mobile Redesign - Deferred Functionality Report

Date: 2026-06-27

## Purpose

This report lists working app functionality that is present in the current codebase but is not a primary first-pass screen in the supplied member redesign set. These should stay preserved in backend/navigation code and can be promoted into the redesigned experience as later enhancements.

## Deferred Or Partially Mapped Features

| Area | Current routes / modules | Status in redesign pass | Suggested later placement |
| --- | --- | --- | --- |
| Agent flow | `agent_onboarding`, `agent_dashboard`, `agent_profiles`, `agent_activities`, `agent_plans`, `agent_account`, `agent_client_profile` | Kept aside per instruction | Separate agent redesign phase |
| SoulMatch Assist | `soulmatch_assist` | Existing service screen, not central in new member screens | Profile drawer or premium assistance entry |
| Spotlight | `spotlight` | Existing boost screen, not central in new member screens | Membership / visibility enhancement |
| Astrology services | `astrology_services` | Existing service screen, not central in new member screens | Compatibility or family details enhancement |
| Family decision board | `family_decisions` | Existing family workflow, not central in new member screens | Profile / shortlist collaboration enhancement |
| Success stories | `success_stories` | Existing trust/education content, not central in new member screens | Profile drawer or onboarding education |
| Subscription history | `subscription_history` | Existing account/payment support screen | Account settings / membership detail |
| Safety center detail articles | `safety_center/article/{id}` | Safety hub exists, detailed article hierarchy is extra | Safety center expansion |
| Help/support detail | `help_support` | Existing support path | Settings/support section |
| Notifications center | `notifications` | Existing notification list | Home header action after final icon pass |
| Chat list and chat detail | `chat_list`, `chat/{conversationId}` | Functional route exists; visibility depends on plan/features | Bottom nav or match profile CTA |
| Interests activity | `interests?tab={tab}` | Functional activity route exists | Activity tab refinement |
| Trust details | `trust_details` | Existing profile verification screen | Profile completion / verification area |
| Partner preferences | `partner_preferences` | Existing profile preference editor | Profile setup/settings refinement |
| Runtime admin/control-plane content | Backend `admin-service`, `shared/controlPlane.js` | Backend functionality, no member screen | Admin console, not mobile member UX |

## Notes

- Backend APIs, navigation routes, and view models for the deferred areas were not removed.
- The current implementation focus is member onboarding, login/OTP, home/discovery, profile, subscription, and shared theme primitives.
- Agent screens are intentionally unchanged for a later dedicated design phase.
- The new theme source of truth is orange/white: primary `#FF5C00`, secondary `#FF8533`, background `#FFFFFF`.
