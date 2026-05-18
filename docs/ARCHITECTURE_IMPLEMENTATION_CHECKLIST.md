# SoulMatch Architecture Implementation Checklist

This checklist is the live tracker for the cost-safe architecture modernization work.

Status values:

- `Not Started`
- `Started`
- `Inprogress`
- `Completed`
- `Failed`

Budget rule: do not add paid Azure resources while the app is still in development unless explicitly approved. Design extension points now; activate paid infrastructure later.

## Live Status

| ID | Phase | Task | Status | Notes |
| --- | --- | --- | --- | --- |
| A0-01 | Baseline | Confirm local branch, clean git status, latest `main` synced | Completed | Work started from `main` on `codex/architecture-foundation`. |
| A0-02 | Baseline | Record current deployed architecture and service list | Completed | Captured in `docs/ARCHITECTURE_FOUNDATION.md`. |
| A0-03 | Baseline | Document production cost-safe rule: no new paid Azure resources | Completed | Captured in this file and architecture foundation docs. |
| A1-01 | Architecture Docs | Create current architecture diagram | Completed | Captured in `docs/ARCHITECTURE_FOUNDATION.md`. |
| A1-02 | Architecture Docs | Create target extensible architecture diagram | Completed | Captured with future BFF, queue, workers, blob, and search placeholders. |
| A1-03 | Architecture Docs | Document implemented now vs future placeholders | Completed | Captured in the implementation matrix. |
| A1-04 | Architecture Docs | Document request flow: Android/Admin -> Nginx -> services -> data | Completed | Captured in request-flow section. |
| A1-05 | Architecture Docs | Document async flow placeholder: service -> queue -> worker | Completed | Captured as future extension point, not activated. |
| A1-06 | Architecture Docs | Document cost-safe extension points | Completed | Captured in `docs/EXTENSION_POINTS.md`. |
| A2-01 | Developer Boundaries | Create service ownership table | Completed | Captured in `docs/DEVELOPER_BOUNDARIES.md`. |
| A2-02 | Developer Boundaries | Document which service owns which database tables | Completed | Captured in service ownership section. |
| A2-03 | Developer Boundaries | Document API responsibility per service | Completed | Captured in API responsibility section. |
| A2-04 | Developer Boundaries | Document Android screen -> API/service mapping | Completed | Captured in Android mapping section. |
| A2-05 | Developer Boundaries | Document admin page -> API/service mapping | Completed | Captured in admin mapping section. |
| A3-01 | Folder Structure | Create clean project structure guide | Completed | Existing `docs/project-structure.md` kept and linked from README. |
| A3-02 | Folder Structure | Mark shared backend utilities boundary | Completed | Captured in `docs/DEVELOPER_BOUNDARIES.md`. |
| A3-03 | Folder Structure | Define where future workers should live | Completed | Captured as `backend/workers/` future placeholder. |
| A3-04 | Folder Structure | Define where future BFF should live | Completed | Captured as `backend/mobile-bff/` future placeholder. |
| A3-05 | Folder Structure | Define where future Blob/OpenSearch integrations should live | Completed | Captured as service adapter boundaries. |
| A4-01 | Extension Placeholders | Add config placeholder for `EDGE_WAF_MODE` | Not Started | Documentation first; code config in later phase. |
| A4-02 | Extension Placeholders | Add config placeholder for `MOBILE_BFF_ENABLED` | Not Started | Documentation first; no BFF runtime yet. |
| A4-03 | Extension Placeholders | Add config placeholder for `BLOB_STORAGE_PROVIDER` | Not Started | Existing upload storage remains unchanged. |
| A4-04 | Extension Placeholders | Add config placeholder for `ASYNC_QUEUE_PROVIDER` | Not Started | Existing Redis remains unchanged. |
| A4-05 | Extension Placeholders | Add config placeholder for `SEARCH_ENGINE_PROVIDER` | Not Started | PostgreSQL search remains primary. |
| A4-06 | Extension Placeholders | Add config placeholder for `OBSERVABILITY_PROVIDER` | Not Started | Existing metrics docs remain unchanged. |
| A5-01 | Code Clarity | Add useful comments to critical shared logic | Not Started | Only after selecting specific code areas. |
| A5-02 | Code Clarity | Avoid rewriting business flows unless required | Completed | This pass is documentation-only. |
| A5-03 | Code Clarity | Add README links to architecture/developer docs | Completed | README now points to developer architecture docs. |
| A6-01 | Validation | Run backend unit tests | Not Started | Not required for docs-only pass; run before code changes. |
| A6-02 | Validation | Run Android unit tests if code changed | Not Started | No Android code changed in this pass. |
| A6-03 | Validation | Run admin-web build if frontend touched | Not Started | No frontend code changed in this pass. |
| A6-04 | Validation | Verify no secrets added | Completed | Docs contain placeholders only. |
| A7-01 | Git | Commit local architecture foundation changes | Not Started | Ready after review. |
| A7-02 | Git | Push only after confirmation | Not Started | Do not push until approved. |
| A7-03 | Deploy | Deploy to VM only when approved | Not Started | VM is not required for this phase. |

## Current Phase Summary

Current phase: developer architecture foundation.

Scope completed in this pass:

- Architecture map.
- Target architecture map with cost-safe extension points.
- Developer ownership boundaries.
- Folder structure links and future placement rules.
- Cost-safe extension point guide.

Scope intentionally not changed:

- No app runtime behavior.
- No new Azure resources.
- No database migration.
- No deployment.
- No paid infrastructure.
