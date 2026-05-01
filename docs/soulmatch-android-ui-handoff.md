# SoulMatch Android UI Handoff

This file is the implementation handoff for the Android app UI refresh.

The Figma file was not updated with the expanded flow because the connected Figma team hit the Starter-plan MCP write limit. The design direction, screen coverage, and prepared Figma-generation script are already captured in:

- `docs/soulmatch-mobile-screen-spec.md`
- `docs/figma-soulmatch-full-flows.js`

This handoff explains how to turn that direction into the Android Compose app.

## Current state

These app surfaces already exist:

- `ui/screens/auth/WelcomeScreen.kt`
- `ui/screens/auth/PhoneEntryScreen.kt`
- `ui/screens/auth/OTPVerificationScreen.kt`
- `ui/screens/auth/ProfileWizardScreen.kt`
- `ui/screens/home/DashboardScreen.kt`
- `ui/screens/search/SearchScreen.kt`
- `ui/screens/profile/ProfileDetailScreen.kt`
- `ui/screens/profile/MyProfileScreen.kt`
- `ui/screens/interests/InterestsScreen.kt`
- `ui/screens/chat/ChatListScreen.kt`
- `ui/screens/chat/ChatScreen.kt`
- `ui/screens/settings/SettingsScreen.kt`
- `ui/navigation/AppNavigation.kt`

The data model already supports most of the required matrimonial detail and activity flows through:

- `ProfileData`
- `ProfileSummary`
- `PartnerPreferencesData`
- `InterestListItem`
- `ShortlistItem`
- `ViewerData`
- `ConversationItem`

## Product goal

SoulMatch should feel like a top-tier matrimony app, not a lightweight dating app.

The app should stand out in:

- complete profile detail capture
- ease of filtering
- richer matching profile cards
- trust and privacy clarity
- direct actions like interest, favourite, hide, block
- strong activity and chat UX

## What must change

### 1. Onboarding and profile completion

Primary file:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/auth/ProfileWizardScreen.kt`

Required changes:

- keep the 6-step flow but redesign it into a stronger matrimony-first experience
- make all important profile details feel explicitly required, except horoscope items that can remain optional
- improve section grouping and visual hierarchy
- add better helper copy and stronger progress visibility
- make each step feel more premium and less like a raw form

Important field coverage:

- step 1: name, DOB, gender, religion, caste/community, mother tongue, marital status
- step 2: height, weight, complexion, body type, blood group
- step 3: education, occupation, income, working city
- step 4: father occupation, mother occupation, siblings, family type, family city
- step 5: diet, smoking, drinking, about me
- step 6: rashi, nakshatra, manglik, birth city, gotra

### 2. Match browsing and home feed

Primary file:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/home/DashboardScreen.kt`

Required changes:

- redesign the match feed to feel more premium and action-oriented
- improve top summary and section hierarchy
- make match cards show more useful signal without opening profile
- support clearer quick actions for:
  `send interest`, `view profile`, `favourite/shortlist`, and a future `more` action

Suggested additions:

- stronger chips for verified, activity, compatibility, and lifestyle
- better feed header with saved filter or preference context
- more polished bottom navigation styling

### 3. Search and filtering

Primary file:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/search/SearchScreen.kt`

Required changes:

- make filtering easier and more powerful
- improve the visual structure of filters and saved searches
- add room in the UI for more matrimonial filter categories even if backend support expands later

Minimum filters the UI should visually support:

- age
- religion
- city
- diet
- verified only
- high compatibility only

Design for future extension:

- caste/community
- mother tongue
- education
- occupation
- income
- family type
- marital status
- manglik
- has photo
- recently active

### 4. Full matching profile detail

Primary file:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/profile/ProfileDetailScreen.kt`

Required changes:

- make the profile feel complete and matrimony-grade
- use stronger grouping so all details feel easy to scan
- support clearer actions for:
  `send interest`, `open chat`, `save`, and future `share with family`

Required visual sections:

- overview
- personal snapshot
- work and lifestyle
- family and traditions
- about me

Future-friendly action support:

- hide profile
- block profile
- report concern

### 5. Activity hub

Primary file:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/interests/InterestsScreen.kt`

Required changes:

- redesign this as a real command center
- make received, sent, accepted, declined, saved, and visitors feel more polished
- prepare UI space for hidden and blocked lists

Important:

- this screen should feel easier to use than typical matrimony apps, not more crowded

### 6. Messages and chat

Primary files:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/chat/ChatListScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/chat/ChatScreen.kt`

Required changes:

- improve visual polish and scanning in the conversation list
- make the chat thread feel safer and more premium
- keep context cues like mutual interest visible
- keep the composer simple
- add space for context-aware conversation assistance

Future-friendly design room:

- voice call
- video call
- safety reminder

### 7. My profile and account

Primary file:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/profile/MyProfileScreen.kt`

Required changes:

- improve profile-strength presentation
- make photos, trust, membership, and partner preferences easier to manage
- make edit-section shortcuts stronger
- improve the checklist UI

### 8. Privacy and controls

Primary file:

- `android/app/src/main/java/com/soulmatch/app/ui/screens/settings/SettingsScreen.kt`

Required changes:

- make privacy feel first-class
- better present photo privacy, profile visibility, and contact filters
- add room for hidden profiles and blocked profiles management

## Files most likely to need edits

- `android/app/src/main/java/com/soulmatch/app/ui/screens/auth/ProfileWizardScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/home/DashboardScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/search/SearchScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/profile/ProfileDetailScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/interests/InterestsScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/chat/ChatListScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/chat/ChatScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/profile/MyProfileScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/screens/settings/SettingsScreen.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/theme/Color.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/theme/Theme.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/theme/Type.kt`
- `android/app/src/main/java/com/soulmatch/app/ui/components/ProfileCard.kt`

## Recommended implementation order

1. Update theme, color system, and typography first.
2. Redesign shared profile card patterns.
3. Redesign `ProfileWizardScreen`.
4. Redesign `DashboardScreen` and `SearchScreen`.
5. Redesign `ProfileDetailScreen`.
6. Redesign `InterestsScreen`.
7. Redesign chat screens.
8. Redesign `MyProfileScreen` and `SettingsScreen`.

## Acceptance criteria

The refresh is ready when:

- all core matrimony details are clearly represented in the UI
- match browsing feels richer and more useful
- filtering feels easier and more premium
- interest, shortlist, hide, and block are clearly supported in the UX direction
- activity and chat screens feel organized and modern
- the app feels consistent across screens

## Important constraint

No expanded Figma page was successfully written yet due to the Figma MCP quota block. The Android implementation should therefore use:

- `docs/soulmatch-mobile-screen-spec.md` as the source of truth for product coverage
- `docs/figma-soulmatch-full-flows.js` as the intended visual structure and screen inventory
