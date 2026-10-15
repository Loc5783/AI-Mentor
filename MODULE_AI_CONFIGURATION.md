# Module AI configuration

The integrated application keeps each feature module's original AI implementation, API key and model configuration.

- Mathematics uses its original Gemini client, key and model.
- Science uses its original Gemini client, key rotation fields and model selection.
- Programming uses its original Gemini chat/quiz clients and key.
- History uses its own Google Gemini client, History-specific key and model under `HISTORY_GEMINI_*` BuildConfig fields.
- The host `app` module provides no AI client and does not override any module key.

Only navigation and user-session handoff are shared by the host application.
