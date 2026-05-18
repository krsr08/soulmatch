# Admin Web Components

Reusable Admin Portal UI primitives live here. Keep page-specific data fetching and business logic in `pages/`.

## Current Components

| File | Purpose |
| --- | --- |
| `AdminPrimitives.js` | Shared `Icon`, `StatusPill`, `AdminButton`, `SectionHeader`, and `EmptyState` components used across admin pages. |

## Rules

1. Components in this folder should be presentational.
2. Do not call admin APIs directly from these components.
3. Keep permission, audit, verification, and payment decisions in backend/admin routes.
4. Move UI here only when it is shared or clearly reusable.

