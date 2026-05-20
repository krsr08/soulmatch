# SoulMatch Admin Web Map

The admin web app is the operator console for verification, CMS/config, subscriptions, moderation, and system controls.

## Files

| File / Folder | Purpose |
| --- | --- |
| `App.js` | Admin route shell and protected routes. |
| `api/adminApi.js` | Central admin API client. Add new admin endpoints here before using them in screens. |
| `components/` | Shared presentational admin UI primitives. |
| `context/RuntimeConfigContext.js` | Loads runtime/CMS config for admin editing. |
| `pages/LoginPage.js` | Admin login. |
| `pages/DashboardPage.js` | Current main admin console with sections for members, agents, payments, moderation, CMS, system, and RBAC. |
| `pages/admin/` | Extracted admin section panels that are split out of the main dashboard page. |
| `pages/AssistPanel.js` | SoulMatch Assist admin queue/support panel. |
| `pages/UsersPage.js` | Admin user management surface. |

## Admin Rules

1. Every operator action that changes users, agents, verification, payments, config, or moderation should be audited by the backend.
2. Admin UI must not display raw secrets. Show configured/not configured status only.
3. Runtime config changes must match schemas in `backend/shared/configSchemas/`.
4. Add backend permission checks before adding buttons in the UI.
5. Keep mock fallback data out of production admin decisions.

## Common Change Flow

1. Add/adjust backend route in `backend/admin-service`.
2. Add API wrapper in `api/adminApi.js`.
3. Add UI in the relevant Dashboard section.
4. Add admin-service tests for auth/RBAC/audit behavior.
5. Run `npm run build` in `admin-web`.

## Future Refactor Boundary

`DashboardPage.js` currently owns many admin sections. Future cleanup should split it by domain:

```text
pages/admin/
  MembersPage.js
  AgentsPage.js
  VerificationPage.js
  PaymentsPage.js
  ModerationPage.js
  CmsPage.js
  SystemPage.js
  RoleMasterPage.js
```

The members, agents, and verification panels have started this split under `pages/admin/`. Continue moving one section at a time and keep behavior unchanged during each move.
