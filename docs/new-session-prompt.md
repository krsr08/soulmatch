# New Session Prompt

Use this prompt in a new Codex session:

```text
Open the SoulMatch workspace at C:\Users\ANIRUDH\Documents\soulmatch and continue the Android UI refresh.

Important context:
- The expanded Figma redesign could not be written because the connected Figma team hit the Starter-plan MCP write limit.
- Use these local files as the source of truth:
  - C:\Users\ANIRUDH\Documents\soulmatch\docs\soulmatch-mobile-screen-spec.md
  - C:\Users\ANIRUDH\Documents\soulmatch\docs\soulmatch-android-ui-handoff.md
  - C:\Users\ANIRUDH\Documents\soulmatch\docs\figma-soulmatch-full-flows.js
- Do not assume the Figma file contains the full design. It does not.

What to build:
- Redesign the Android Compose app so SoulMatch feels like a premium matrimony app, not a lightweight dating app.
- Focus on:
  - complete profile-detail collection
  - better match cards
  - easier search and filtering
  - send interest and shortlist actions
  - room for hide/block/report actions
  - better activity hub
  - better chat experience
  - better my-profile and privacy settings UX

Start by reading:
- C:\Users\ANIRUDH\Documents\soulmatch\docs\soulmatch-android-ui-handoff.md
- C:\Users\ANIRUDH\Documents\soulmatch\android\app\src\main\java\com\soulmatch\app\ui\navigation\AppNavigation.kt

Then implement the UI refresh directly in the Android app.

Files most likely to change:
- ProfileWizardScreen.kt
- DashboardScreen.kt
- SearchScreen.kt
- ProfileDetailScreen.kt
- InterestsScreen.kt
- ChatListScreen.kt
- ChatScreen.kt
- MyProfileScreen.kt
- SettingsScreen.kt
- ProfileCard.kt
- theme files

Please make real code changes, not just a plan, and verify the build as far as practical.
```
