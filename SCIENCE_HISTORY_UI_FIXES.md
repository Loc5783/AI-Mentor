# Science Chat History and UI Fixes

## Scope

This update changes only the Science module behaviour and presentation. Existing Gemini/API-key configuration was intentionally left unchanged.

## 1. Science chat history now opens the saved chat

### Previous behaviour

The Science History item click opened `ScienceHistoryActivity`. That compatibility Activity redirected back to the History bottom-navigation tab, so the selected record was never passed to the chat screen.

### Updated behaviour

- A History card now creates an Intent for `ScienceActivity` containing the selected history ID.
- `ScienceActivity` retrieves the record using both `user_id` and `history_id`.
- The saved subject, education level, explanation style, question, image and answer are restored.
- The same behaviour is used by the Recent Questions section on the Science Ask/Solve tab.
- After an old chat opens, the learner can continue asking questions from its existing context.

## 2. Full Science conversations are persisted

Database version was increased from 4 to 5.

A new `conversation_id` column groups multiple question-answer rows into one chat session:

- Every new subject chat receives a unique conversation ID.
- Follow-up questions retain the current conversation ID.
- Pressing New Chat creates a new conversation ID.
- History shows one card per conversation rather than one duplicated card per message.
- Opening a card loads all messages for that conversation in chronological order.
- Conversation IDs survive screen rotation through saved-instance state.
- Cached answers are also written into the active conversation, so they remain accessible from its history.

### Existing database migration

Old rows are migrated without deletion. Because earlier versions did not store a session identifier, each legacy row receives `legacy-<history_id>`. Therefore every old question-answer pair remains openable, while full multi-turn grouping is available for conversations created by this version onward.

## 3. Quiz hero icon

The old checklist vector contained a filled rectangular path, which rendered as a solid purple block. It was replaced with a real white checklist outline inside a dark-purple rounded container.

## 4. Bottom menu visibility

`ScienceMainActivity` now applies the Android navigation-bar inset to the Science bottom-navigation height and padding. This keeps icons and labels above gesture navigation and three-button system navigation, including Android edge-to-edge layouts.

## Validation performed

- 219 Java files; 0 Kotlin files.
- 267 XML files parsed successfully.
- No missing internal Java imports.
- No unbalanced Java source blocks.
- Programming module remains Java 11 compatible.
- Science domain/use-case sources compiled with `--release 11`.
- Conversation model and cached-history tests passed.
- History repository and database helper compiled against Android-compatible test stubs.
- SQLite version 4 to version 5 migration simulation passed.
- API/Gemini configuration files were byte-for-byte unchanged.

A full Android Gradle build could not be run in the offline environment because Gradle 9.5.0 was not available locally and the wrapper could not reach `services.gradle.org`.
