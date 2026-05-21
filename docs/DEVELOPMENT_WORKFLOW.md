# SoulMatch Development Workflow

Use this workflow for controlled changes. It keeps the app stable while the codebase continues to become more modular.

## Branching

Use short-lived branches:

```text
codex/<purpose>
```

Examples:

- `codex/member-profile-fixes`
- `codex/agent-dashboard-polish`
- `codex/admin-cms-config`
- `codex/backend-entitlement-tests`

## Change Types

| Change Type | What It Means | Expected Validation |
| --- | --- | --- |
| Docs only | README, architecture, runbook, developer notes | `git diff --check` |
| Android UI only | Compose screen/component layout with no API change | Android unit tests when practical |
| Backend behavior | API, service logic, repository, shared rule | Owning service tests + smoke tests |
| Admin web | Admin UI/API client changes | `npm run build` and admin-service tests if backend touched |
| Database migration | SQL schema/index/data changes | Migration applies locally and services start |
| Shared rule | `backend/shared/*` behavior | All affected service tests |

## Safe Implementation Sequence

1. Read `docs/MODULE_OWNERSHIP_MAP.md`.
2. Identify the owning flow and service.
3. Make the smallest change in the owner module.
4. Add/update tests near the owner.
5. Run focused tests first.
6. Run smoke tests if backend behavior changed.
7. Update docs/checklist if ownership or runtime config changes.
8. Commit one concern at a time.

## Validation Commands

Backend smoke:

```powershell
docker compose -f docker/docker-compose.dev.yml up -d --build
node test_folder/auth-flow-smoke.js
node test_folder/api-smoke.js
```

Backend tests:

```powershell
$tests = Get-ChildItem backend -Recurse -Include *.test.js,*.spec.js |
  Where-Object { $_.FullName -notlike '*\node_modules\*' } |
  ForEach-Object { $_.FullName }
node --test $tests
```

Android tests:

```powershell
cd android
.\gradlew.bat testDebugUnitTest
```

Admin web build:

```powershell
cd admin-web
npm run build
```

## Deployment Rule

Pushing to `main` triggers the production deploy workflow. Keep feature branches separate until local validation is complete and the user explicitly approves merge/deploy.

## Cost Rule

Do not start the Azure VM or add paid cloud resources unless the user explicitly asks. When the VM is deallocated, public smoke tests will time out; this is expected for cost control.

