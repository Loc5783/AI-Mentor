# AIMentor engineering criteria

- Apply SOLID to all new work and substantial changes. Keep UI rendering, application state, domain policy, persistence, and transport in focused components.
- Depend on narrow contracts at boundaries; compose implementations in AppServices. Extend behavior through explicit policies or adapters when useful. Avoid speculative abstractions that add no practical value.
- The user explicitly chose a fresh nine-subject app on 21 October 2026. Remove the old four-module flows, old data imports and old local data. Preserve all subsequent new-app data; never repeat the one-time reset. Retain useful tools in the appropriate subject module.
- Verify changes with meaningful tests for behavior, lifecycle, identity isolation, and failures. Do not treat a successful build as proof that the entire app is complete.
- Git workflow (confirmed again on 21 October 2026): push module/*, feature/* or fix/* branches and open Pull Requests into integration. Leave main merges to the team lead. The user has explicitly adopted this workflow for AIMentor after the earlier mistaken attachment.
- Visual direction: use short, subtle motion lighter than Duolingo. Prefer clear text and restrained transitions over decorative icons. Keep essential navigation/camera/attachment affordances. Never gate learning, rewards or interaction on an animation; respect system animation settings and stop motion when its view is detached.
