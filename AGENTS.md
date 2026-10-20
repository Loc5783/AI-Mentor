# AIMentor engineering criteria

- Apply SOLID to all new work and substantial changes. Keep UI rendering, application state, domain policy, persistence, and transport in focused components.
- Depend on narrow contracts at boundaries; compose implementations in AppServices. Extend behavior through explicit policies or adapters when useful. Avoid speculative abstractions that add no practical value.
- Preserve working behavior and data while replacing legacy flows. Remove superseded code after its callers and data paths are migrated.
- Verify changes with meaningful tests for behavior, lifecycle, identity isolation, and failures. Do not treat a successful build as proof that the entire app is complete.
- This is a general criterion for future work, requested by the user. The Git main/integration workflow image sent earlier belongs to another project and does not apply here.
- Visual direction: use short, subtle motion lighter than Duolingo. Prefer clear text and restrained transitions over decorative icons. Keep essential navigation/camera/attachment affordances. Never gate learning, rewards or interaction on an animation; respect system animation settings and stop motion when its view is detached.
