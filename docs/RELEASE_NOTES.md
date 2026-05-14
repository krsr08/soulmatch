# SoulMatch Release Notes

## 1.0.3 - 2026-05-14

- Added configurable Best Matches insert cards for membership upgrades, paid services, trust nudges, and safety content.
- Added configurable scam-awareness carousel cards that can be managed from Admin CMS.
- Added membership tier controls for Bronze, Silver, Gold, and Platinum with a backend-configurable feature matrix.
- Replaced the member bottom Account/Profile tab with an Upgrade tab linked to subscription flows.
- Added Admin CMS management for feed cards, scam cards, and membership feature permissions.

## 1.0.2 - 2026-05-14

- Fixed My Profile crash when backend partner preference list fields such as deal breakers, education, locations, or occupations are returned as null.
- Normalized partner preference data in the My Profile view model before saving or rendering it.
- Hardened My Profile readiness checks against legacy records with null text fields.

## 1.0.1 - 2026-05-14

- Hardened member dashboard profile and match data handling against null values returned by older or incomplete records.
- Improved Home spacing when the profile-strength card is shown below the filter pills.
- Added photo preview support in My Profile while keeping add, delete, and make-primary controls available as icons.
- Updated Admin member verification actions so already verified profiles show an unverify action instead of another approve tick.
- Added backend support for the Admin unverify action so it removes the verified badge without treating the profile as rejected.
- Refined Admin Verification Command Center layout and added auto-dismissing toast feedback for approve, reject, and status actions.

## 1.0.0 - Initial development build

- Baseline SoulMatch Android app, backend services, agent flow, member flow, and admin console.
