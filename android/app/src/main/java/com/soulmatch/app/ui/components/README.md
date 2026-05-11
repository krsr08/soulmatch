# SoulMatch UI Components

Reusable Compose UI belongs here. Screens should assemble flows and page state; shared widgets should live in one of these folders.

## Folders

- `cards`: reusable profile/match cards and card-level visual elements.
- `dialogs`: shared modal dialogs and confirmation sheets.
- `media`: shared image/photo rendering, privacy blur, and placeholders.
- `navigation`: reusable drawers, route constants, and navigation UI.
- `premium`: shared SoulMatch visual primitives such as cards, chips, headers, grids, and upgrade gates.
- `status`: shared status/readiness logic and status UI helpers.

## Rules

- Put a component here when it is used by more than one screen or is likely to be reused.
- Keep screen-specific private helpers inside the screen package until they become shared.
- Do not put network calls or ViewModel state inside these components.
- Prefer small parameters and callbacks over passing whole screen state objects.
