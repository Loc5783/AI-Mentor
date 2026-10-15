# History module integration

The `history` feature is an Android library module opened by the host through `HistoryFeature.open(context, userId)`.

## AI isolation

The History module now uses Google Gemini with History-specific Gradle fields (`HISTORY_GEMINI_API_KEY`, `HISTORY_GEMINI_MODEL`). Its AI client and configuration remain separate from Math, Science and Programming.

## Functional changes

- All user-facing provider branding was standardized to AI; the History backend now uses Gemini.
- A shared top-left Back button is available on every History bottom-navigation page; Chat and Quiz retain their own Back buttons.
- Conversation messages are stored by `user_id` and `conversation_id`.
- The History page lists whole conversations; tapping an item reopens the full chat and long-pressing deletes it.
- The former event/category page is now Notes only, with add, edit, delete and pin/unpin.
- Quiz scopes are Vietnam, Foreign history, and Mixed.
- Each scope supports Middle School, High School and University question generation.
- Correct answers earn 5/10/20 EXP for Easy/Medium/Hard questions. Level is derived from total History EXP.
