# Theme and Language Fixes

## Root causes fixed

1. Every Activity was forced to `Theme.MaterialComponents.Light.NoActionBar`, so `AppCompatDelegate.MODE_NIGHT_YES` could not activate the night resources.
2. Dark-mode state existed only in the current process and was lost after the app was closed.
3. Locale changes were applied through a temporary wrapped Context and only some Activities used that wrapper. Login and feature-module Activities therefore did not consistently receive the selected language.
4. The Settings screen manually recreated the Activity after changing locale, while AppCompat also handles recreation. This could cause duplicate recreation and inconsistent state.
5. Several app-shell colors and visible texts were hard-coded, so the UI could show light surfaces or Vietnamese text after switching modes/languages.

## Main changes

- Added `AiMentorApplication` to apply saved theme and locale before any Activity starts.
- Added `ThemeHelper` with persistent dark/light preference.
- Reworked `LocaleHelper` to use `AppCompatDelegate.setApplicationLocales`.
- Updated all Activity declarations to DayNight-compatible themes.
- Assigned each subject module its own DayNight theme.
- Added a complete dark palette for the app shell in `values-night/colors.xml`.
- Converted app-shell backgrounds, cards, dividers, chart separators, and recent-activity colors to theme-aware resources.
- Removed legacy `attachBaseContext` locale overrides.
- Removed hard-coded login/settings/navigation text and added matching Vietnamese/English resources.

## Manual verification checklist

1. Open the app in Light Mode and navigate through Login, Register, Home, Category, Profile, and Settings.
2. Enable Dark Mode in Settings. Confirm that the Activity recreates once and the switch remains enabled.
3. Close the app completely, reopen it, and confirm Dark Mode is still active.
4. Change the language to English. Confirm Settings, drawer, bottom navigation, Login/Register, Home, Category, and Profile use English resources.
5. Log out and confirm the Login screen remains in English.
6. Close and reopen the app; confirm the selected language persists.
7. Change back to Vietnamese and repeat the persistence check.
8. Open Science, Math, History, and Programming to confirm they no longer receive a forced Light-only Activity theme.

## Validation performed in this workspace

- Parsed all 267 Android XML files successfully.
- Checked Vietnamese and English app-shell string resources: both contain 105 matching keys with no duplicates.
- Checked all modified Java files for balanced braces and parentheses.
- Full Gradle build could not run in this offline workspace because Gradle 9.5.0 was not cached and the wrapper could not download it.
