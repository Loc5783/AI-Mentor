# v7 - History migrated to Gemini

- Replaced the previous History provider client and model configuration with Gemini.
- Added a History-only Gemini REST client using `generateContent` and `x-goog-api-key`.
- Added Gemini inline image input for historical documents, maps and photographed questions.
- Preserved multi-turn context, response continuation, Quiz JSON parsing, EXP and saved conversations.
- History now uses `HISTORY_GEMINI_API_KEY` and `HISTORY_GEMINI_MODEL` independently from all other modules.
- Updated History-specific error messages for invalid keys, rate limits, unavailable models, timeouts and Gemini server errors.
