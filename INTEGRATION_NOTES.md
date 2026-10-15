# Math and Science integration

This project contains three Android modules:

- `app`: login, home, category, profile and settings.
- `math`: the original Mathematics feature converted to an Android library.
- `science`: the Science feature included as an Android library.

## Opening the modules

Open **Danh mục** in the host application:

- **Toán học** opens `MathFeature` and `MathDashboardActivity`.
- **Khoa học** opens `ScienceFeature` and `ScienceMainActivity`.

The current host user ID is passed separately into each feature.

## AI separation

The host application does not provide a shared AI service or shared API key.

- Math keeps the original Math Gemini client, original Math API key and original Math model.
- Science keeps the original Science Gemini client, original Science API key rotation fields and original Science model selection.
- Changing one module's AI configuration does not change the other module.

## Android Studio

1. Open the folder containing `settings.gradle.kts`.
2. Run **Sync Project with Gradle Files**.
3. Run **Build > Clean Project**.
4. Run **Build > Rebuild Project**.
