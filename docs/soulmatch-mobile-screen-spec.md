# SoulMatch Mobile Screen Spec

This document maps the production mobile screens that should exist in the SoulMatch app after the Figma redesign is finalized.

## Product direction

SoulMatch should behave like a top-tier matrimony app, not a casual dating app.

Core principles:

- Collect complete matrimonial profile details with clear step-by-step progress.
- Make matching profiles easy to browse, compare, filter, shortlist, and hide.
- Make safety, trust, verification, and privacy visible at every key step.
- Support family-aware decision-making without making the UI feel old-fashioned.
- Reduce repeated work with saved filters, smart bundles, and strong default sorting.

## Screen inventory

1. Entry and trust screen
2. Profile setup hub
3. Basic details screen
4. Physical, education, and career screen
5. Family and lifestyle screen
6. Horoscope and partner preferences screen
7. Photos, privacy, and verification screen
8. Match home screen
9. Search and filter screen
10. Matching profile detail screen
11. Profile action sheet or menu
12. Activity hub
13. Messages list
14. Chat thread
15. My profile and account screen
16. Privacy, contact filters, hidden profiles, and blocked profiles screen

## Full profile data coverage

The redesigned screens should cover the current `ProfileData` and related models in the Android app.

Basic identity:

- First name
- Last name
- Date of birth
- Age
- Gender
- Religion
- Caste or community
- Mother tongue
- Marital status

Physical:

- Height in ft & inch
- Weight in kg
- Complexion
- Body type
- Blood group

Education and work:

- Education level
- Occupation
- Annual income
- Working city

Family:

- Father occupation
- Mother occupation
- Number of brothers
- Number of sisters
- Family type
- Family city

Lifestyle:

- Diet
- Smoking
- Drinking
- About me

Horoscope and tradition:

- Rashi
- Nakshatra
- Manglik
- Birth city
- Gotra

Health

- Any of these deceases in Family (BP, Sugar, Thyroid) - Toggle
- Any uncureable deceases - Toggle

Profile media and privacy:

- Primary photo
- Photo gallery
- Photo privacy
- Profile visibility
- Verification state

Partner preferences:

- Preferred age min
- Preferred age max
- Preferred religion
- Manglik preference

## Match browsing requirements

Matching profile cards should support:

- Compatibility score
- Verification badge
- Recently active signal
- Occupation, city, and community snapshot
- Strong profile reasons
- Send interest
- Add to favourites or shortlist
- Open full profile
- Hide member
- Block member
- Report member

## Filtering requirements

Filtering should feel faster and richer than typical matrimony apps.

Recommended filter groups:

- Age range
- City
- Religion
- Caste or community
- Mother tongue
- Education
- Occupation type
- Income band
- Diet
- Smoking
- Drinking
- Family type
- Marital status
- Manglik
- Verified only
- Has photo
- Active recently
- High compatibility only

The UI should also support:

- Saved searches
- Smart filter bundles
- Result count preview before apply
- Sort by compatibility, activity, and freshness

## Messaging and activity requirements

Activity hub should include:

- Received interests
- Sent interests
- Accepted
- Declined
- Shortlisted or favourites
- Recent visitors
- Hidden members
- Blocked members

Messaging should include:

- Conversation list
- Mutual-interest chat thread
- Trust reminder
- Context-aware conversation starters

## Existing Android screen mapping

These current app modules should absorb the final mobile design direction:

- `WelcomeScreen`
- `PhoneEntryScreen`
- `OTPVerificationScreen`
- `ProfileWizardScreen`
- `DashboardScreen`
- `SearchScreen`
- `ProfileDetailScreen`
- `InterestsScreen`
- `ChatListScreen`
- `ChatScreen`
- `MyProfileScreen`
- `SettingsScreen`

Likely additions or expansions:

- Hidden and blocked profiles management
- Full action sheet for profile cards
- More complete partner preference editing
- Richer filter drawer or bottom sheet
- Better family-share and privacy surfaces

## Screen contracts

### 1. Entry and trust screen

Purpose:

- Present SoulMatch as a serious matchmaking app.
- Show that the app is verified, private, and family-aware.

Key UI blocks:

- Brand and trust statement
- Short explanation of how matches work
- Primary action to create profile
- Secondary action to continue with mobile

### 2. Profile setup hub

Purpose:

- Show profile progress clearly.
- Let users understand what sections remain before the profile becomes strong enough for matching.

Key UI blocks:

- Progress meter
- Section list with status
- Section edit and continue actions
- Explanation of why richer profiles perform better

### 3. Basic details screen

Purpose:

- Capture mandatory identity details early.

Required fields:

- First name
- Last name
- Date of birth
- Gender
- Religion
- Caste or community
- Mother tongue
- Marital status

### 4. Physical, education, and career screen

Purpose:

- Capture filter-heavy details in a structured way.

Required fields:

- Height
- Weight
- Complexion
- Body type
- Blood group
- Education level
- Occupation
- Annual income
- Working city

### 5. Family and lifestyle screen

Purpose:

- Capture the details most matrimony users use for trust and family fit.

Required fields:

- Father occupation
- Mother occupation
- Number of brothers
- Number of sisters
- Family type
- Family city
- Diet
- Smoking
- Drinking
- About me

### 6. Horoscope and partner preferences screen

Purpose:

- Capture optional traditional details and matching preferences.

Fields:

- Rashi
- Nakshatra
- Manglik
- Birth city
- Gotra
- Partner age min and max
- Preferred religion
- Manglik preference

### 7. Photos, privacy, and verification screen

Purpose:

- Make trust visible before matching starts.

Key UI blocks:

- Photo gallery
- Primary photo selection
- Optional profile video support
- Photo privacy setting
- Profile visibility setting
- OTP verified status
- Government ID status
- Selfie or face-check status

### 8. Match home screen

Purpose:

- Show the best matching profiles first.
- Make quick actions possible directly from the feed.

Key UI blocks:

- Search entry
- Sticky smart chips
- Compatibility-first match cards
- Send interest
- Add to favourites
- Open profile
- More actions menu

### 9. Search and filter screen

Purpose:

- Make filtering feel powerful and easy.

Key UI blocks:

- Search bar
- Saved searches
- Smart bundles
- Filter groups
- Result count preview
- Apply button

### 10. Matching profile detail screen

Purpose:

- Show complete details without feeling cluttered.

Sections:

- Overview
- Family
- Lifestyle
- Horoscope
- Preferences match

Primary actions:

- Send interest
- Add to favourites
- Share with family
- Chat if mutual

### 11. Profile action sheet or menu

Purpose:

- Surface all key profile actions without crowding the card.

Actions:

- Send interest
- Add to favourites
- Share profile
- Hide member
- Block member
- Report concern

### 12. Activity hub

Purpose:

- Replace scattered lists with a single command center.

Tabs or sections:

- Received interests
- Sent interests
- Accepted
- Declined
- Shortlisted or favourites
- Visitors
- Hidden members
- Blocked members

### 13. Messages list

Purpose:

- Show active conversations clearly.
- Prepare for later voice and video support without making chat noisy.

Key UI blocks:

- Conversation list
- Last message preview
- Unread badge
- Verification indicator

### 14. Chat thread

Purpose:

- Keep chat simple, safe, and higher quality than generic dating apps.

Key UI blocks:

- Mutual-interest label
- Safety reminder
- Message composer
- Context-aware conversation prompts

### 15. My profile and account screen

Purpose:

- Give the user a single place to manage profile quality.

Key UI blocks:

- Profile strength
- Membership snapshot
- Photos and trust
- Partner preferences
- Profile checklist
- Edit section shortcuts

### 16. Privacy, contact filters, hidden profiles, and blocked profiles screen

Purpose:

- Keep sensitive controls accessible and explicit.

Key UI blocks:

- Photo privacy toggle
- Profile visibility toggle
- Contact filter controls
- Hidden list management
- Blocked list management
