# SoulMatch Android Screen Map

This folder contains screen-level Compose UI. Screens should assemble page layout, collect ViewModel state, and call callbacks. Reusable visual parts should move to `ui/components/` only after they are shared by more than one screen.

## Screen Folders

| Folder | Owns |
| --- | --- |
| `auth` | Welcome, phone login, OTP, role selection, member profile wizard. |
| `home` | Member home, best matches, notifications. |
| `profile` | My Profile, profile detail, SoulMatch Assist, services, family board. |
| `agent` | Agent onboarding, dashboard, account, plans, member creation, agent activity. |
| `interests` | Received/sent/accepted/declined interest activity. |
| `chat` | Messenger list and chat thread. |
| `search` | Discover/refine matches and filter UI. |
| `subscription` | Upgrade page, package cards, subscription history. |
| `settings` | Account settings, safety, privacy, support surfaces. |
| `success` | Success stories. |
| `legal` | Terms and privacy content. |
| `system` | Launch, maintenance, and system-level screens. |

## Screen Rules

1. Keep network calls in ViewModels/repositories, not in composables.
2. Keep reusable UI in `ui/components/`.
3. Keep backend-enforced behavior backend-owned; UI should not unlock paid/private behavior alone.
4. For Member-only changes, avoid touching `agent/`.
5. For Agent-only changes, avoid touching `home/`, `profile/`, and member wizard files unless shared DTOs require it.
6. Use `UiFormatters.kt` for common display formatting instead of duplicating text transformations.

## ViewModel Pairing

Most screens pair with `ui/viewmodels/*ViewModel.kt`. When adding a screen state field:

1. Add the field to the relevant state model.
2. Keep validation close to the action that sends data to the backend.
3. Let backend errors surface as user-friendly UI state.
4. Add unit tests for non-trivial state decisions.

## References

- `docs/MODULE_OWNERSHIP_MAP.md`
- `docs/project-structure.md`
- `android/app/src/main/java/com/soulmatch/app/ui/components/README.md`

