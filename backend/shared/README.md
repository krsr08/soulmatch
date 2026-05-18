# SoulMatch Shared Backend Helpers

Use this folder for rules used by more than one backend service. Do not put service-specific workflows here.

## Files

| File / Folder | Purpose |
| --- | --- |
| `controlPlane.js` | Runtime config defaults, normalization, schema validation, and public/private config projection. |
| `configSchemas/` | JSON schemas for CMS/runtime config sections. |
| `memberEntitlements.js` | Member plan limits and feature access helpers. |
| `profileVisibility.js` | Central profile/photo/contact redaction rules. |
| `architectureFlags.js` | Cost-safe future infrastructure mode placeholders. |
| `observability.js` | Shared request metrics/logging helpers. |

## When To Add Shared Code

Add code here only when:

1. Two or more services need the same rule.
2. The rule must remain consistent across API surfaces.
3. The logic is not tied to one controller or database repository.

Good examples:

- Profile visibility redaction used by profile, search, and matching.
- Plan entitlement limits used by profile, matching, chat, and payment.
- Runtime config normalization used by admin and public config endpoints.

Avoid:

- One-off controller code.
- Service-specific SQL.
- Screen-specific formatting.
- Secrets or environment values.

## Safety Notes

- Changes here have a wider blast radius than normal service edits.
- Run affected backend tests plus `test_folder/api-smoke.js`.
- If a shared rule affects Android behavior, keep backend enforcement first and UI second.

