# V8 Functional Fixes

Project: AI Study Mentor Android multi-module application  
Modules: `app`, `math`, `science`, `programming`, `history`

## Scope requested

Gemini API keys and their current storage/configuration were intentionally left unchanged.

## Functional repairs applied

### Science

- Corrected the camera FileProvider authority from `.science.fileprovider` to the host authority `.fileprovider`.
- Restricted the host FileProvider to `cache/science_camera/`, which is the directory actually used by `ScienceActivity`.
- Added local text-answer fallback for network, timeout, quota and temporary server failures.
- Credential and permission failures are still displayed instead of being hidden behind a fallback answer.
- Local answers are visibly marked as `OFFLINE MODE`.
- Removed ambiguous library manifest provider-removal markers.

### Account and Profile

- Profile password changes are now BCrypt-hashed using the same cost as registration.
- Login no longer crashes/fails blindly when an older profile update stored plaintext.
- A successful login with a legacy plaintext value immediately migrates it back to BCrypt.
- Profile rejects a weak non-empty replacement password and clears the password field after success.
- The debug user-list helper no longer prints password hashes.

### Mathematics

- Increased `MathMentor.db` from version 7 to version 8.
- Added `user_id` to Math history and note tables.
- Added indexes for Math history/note ownership.
- History and note insert/read/update/delete/pin operations are now scoped to the active Math user.
- Added a non-destructive migration for old databases.
- Legacy shared Math rows are assigned once to the first signed-in Math user who opens the migrated database.
- Demo seeding now deletes and inserts Math data only for the demo account.

### Programming and integration

- Declared Programming `MainActivity` in the Programming module manifest.
- Removed the unused Programming camera permission.
- Removed the unused Math FileProvider declaration.
- Replaced direct `activity-ktx` dependencies with the standard `androidx.activity:activity` artifact; student source remains Java-only.

### Host app configuration

- Removed obsolete `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` permissions.
- Disabled Android backup for locally stored accounts, learning histories, notes and progress.
- Removed stale generated folders and machine-specific `local.properties` from the repaired archive.
- Added `local.properties.example` for SDK setup.

## Validation completed

- 218 Java source files found.
- 0 Kotlin source files found.
- 266 XML files parsed successfully.
- 0 malformed XML files.
- 0 duplicate fully qualified Java class names.
- 0 unresolved internal project imports.
- 0 missing Activity declarations.
- Changed `DatabaseHelper`, Math `SqliteDbHelper`, and `ResilientScienceService` passed focused Java compile checks with local stubs.
- Math v7 → v8 migration and multi-user isolation SQL passed an in-memory SQLite simulation.
- Science fallback behavior passed local Java tests for network, HTTP 401 and HTTP 429 cases.

## Build limitation

A full Gradle build was attempted, but this environment cannot resolve `services.gradle.org`, so Gradle 9.5.0 could not be downloaded. Run Gradle Sync and `:app:assembleDebug` in Android Studio on a machine with Internet access.
