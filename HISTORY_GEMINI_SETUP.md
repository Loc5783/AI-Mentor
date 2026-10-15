# History Gemini configuration

The History module uses Google Gemini independently from Math, Science and Programming.

Configuration keys:

```properties
HISTORY_GEMINI_API_KEY=your_history_gemini_key
HISTORY_GEMINI_MODEL=gemini-flash-latest
```

For local development, place these values in the root `local.properties` file. Values in
`local.properties` override `gradle.properties`.

The History module uses Gemini for:

- multi-turn History tutoring;
- image analysis for historical documents, maps and questions;
- History Quiz generation.
