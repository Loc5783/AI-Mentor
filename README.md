# AI Mentor

Android app with five destinations: Dashboard, Chat AI, Lessons, Competition and Profile. Run the `app` module (`com.example.asm_final`).

## School subject modules

| Module | Subject | Current scope |
|---|---|---|
| `math` | Mathematics | Lesson framework; existing math tools, including its calculator |
| `literature` | Literature | Framework |
| `english` | English | Framework |
| `history` | History | Lesson framework; existing history tools |
| `geography` | Geography | Framework |
| `physics` | Physics | Grade-10 lessons and circuit implementation |
| `chemistry` | Chemistry | Grade-10 lessons, periodic table implementation and element data |
| `biology` | Biology | Grade-10 lessons |
| `economics-law` | Economics and Law | Framework |

Each of Physics, Chemistry and Biology has 20 short lessons, 60 practice questions and four 10-question reviews. Grades 11–12 and complete textbook coverage are not yet authored. See [content coverage](docs/content/CONTENT_PLAN_VI.md).

Dashboard renders selected learning subjects, including when the server is unavailable. It does not fall back to the removed four-category dashboard. Empty charts explain why there is no data. Interests in a profile are distinct from actual Lesson enrollments.

Programming has been removed from the build, navigation, configuration and source tree. There is no aggregate Science school subject. `science-support` is a shared compatibility library for supplementary quizzes, existing notes/history, storage and UI resources; it does not register a tenth subject. Existing Java package and SQLite identifiers in this library are retained for compatibility with stored data. Circuit and periodic-table implementations belong to their individual subject modules.

## Shared components

- `app`: navigation, account-scoped state and composition in `AppServices`.
- `learning-core`: subject catalog, grades, review and reward rules.
- `chat-core`: all-subject, grade-aware Gemini tutor and key rotation.
- `data-core`: local SQLite migrations, account identity and shared persistence.
- `ui-core`: shared responsive controls, quiz presentation, safe areas and restrained motion.
- `backend`: FastAPI/SQLite authentication, lessons, reward ledger, profiles, rankings and SSE events.

SOLID is the general development criterion. UI must not award XP or unlock lessons. A first completion earns 200 XP per lesson or 400 XP per review; a retake earns no additional XP and can light a new day's streak. Reviews appear after each five lessons. Quizzes have no countdown.

## Build

Open the repository root in Android Studio. Use the included Gradle 9.5.0 wrapper, JDK 21 and Android SDK 36. Local SDK paths go in ignored `local.properties`. Do not commit machine-specific paths.

`./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug testDebugUnitTest`

## API and backend

Root `api.properties` contains blank keys. Math and History tools use their own sections; supplementary quizzes share `QUIZ_GEMINI_API_KEY_1/2/3` and `QUIZ_GEMINI_MODEL`. General chat uses `CHAT_GEMINI_API_KEY_1/2/3` and `CHAT_GEMINI_MODEL`. No Programming configuration remains. After filling local configuration, sync and rebuild; installed apps do not read this source file dynamically. Keys embedded in an APK are extractable.

Set `MENTOR_BACKEND_URL` to a deployed HTTPS base URL to connect the native learning flow. Without it, the app explains that synchronized statistics and learning paths are unavailable; it does not invent progress or rankings. A publicly deployed backend is not included automatically.

For Android Studio's emulator on Windows, run `Start-AIMentor-Backend.cmd`. It starts a local server at port 8000 and creates ignored `backend.local.properties` if missing. Sync Gradle and run the **debug** app again; its emulator URL is `http://10.0.2.2:8000`. An older installed APK must be rebuilt to receive this setting. Sign in again to link a previously local account; existing progress is retained. Keep the backend running while testing. This setup is local to the computer, not an Internet deployment. Release APKs ignore this debug configuration and require HTTPS.

Run backend setup/tests using [backend/README.md](backend/README.md). The implemented contract is [runtime-openapi.json](docs/backend/runtime-openapi.json); `openapi.json` is an older design draft.

Cloud chat synchronization, server-connected profile editing, friend-request UI, production deployment and full offline synchronization remain unfinished. Existing local chat/project/file functionality and connected learning are separate from those pending features.

## Demo accounts

Debug local accounts: `student 01`, `student 02`, `student 03`; password `123123123`. Backend demo accounts require explicit demo mode; these are not production credentials.

Historical progress reports describe earlier snapshots. See `WORK_IN_PROGRESS.md` for the latest state and verification boundaries.
