# Learning AI foundations: scope and evidence

This note describes source code in the current working tree, including uncommitted changes. It does not claim that a new AI feature is available to learners, tested against the cloud project, or deployed.

| Phase | Implemented in source | Remaining before learner use |
| --- | --- | --- |
| 1 — Architecture foundation | `ExamQuestionKind` prevents unknown exam formats from silently becoming multiple choice. Exam UI validates four True/False statements before rendering them. | Review the remaining question-type branches and device UI behavior; the current change does not replace exam scoring. |
| 2 — Shared learning evidence | `LearningEvidenceReader` reads already loaded lesson, placement, AI practice, library and exam snapshots. It keeps owner, subject, grade, source and observation type, and does not infer a score from a completed lesson. | No durable evidence store, skill mapping or cloud migration was added. Reconcile snapshot versions and server identity before persistence. |
| 3 — Reasoning Arena foundation | `ReasoningReviewPolicy` accepts a versioned, reviewed skill binding, an observed answer and a learner's explicit confidence. It returns a next check, such as asking confidence, investigating a possible misconception or trying an unseen question. | No current screen collects confidence, and existing question banks do not provide a verified skill map. Its result must not be displayed as mastery, used for rewards or saved as a diagnosis. |
| 4 — Adaptive Exam Foundry foundation | `AdaptiveQuestionSelection` selects only published, reviewed, unseen practice question IDs matching subject, grade, skill-map version, format and difficulty. Missing slots remain explicit. | The selection is not wired to the app or Supabase. A curated, versioned skill map and a trusted blueprint source are needed. Timed exams continue using their existing fixed server blueprint. |
| 5 — Quality | Pure policy tests cover account isolation, missing metadata, guessing, high-confidence errors, assisted answers, duplicate observations and question reuse. | Run device and cloud integration tests before exposing these policies. Measure latency and AI cost only when an actual request path exists. |

## Decision boundaries

- An observed answer is not a skill conclusion. Confidence must come from the learner, and `independentVariant` must come from reviewed question metadata rather than the app guessing it from a question ID.
- Exam evidence uses the highest available server revision. If that revision is a summary without question details, the result stays empty until fresh details are loaded. Older detailed snapshots are not merged into newer summaries. Contradictory same-revision evidence is excluded rather than resolved by input order.
- Pending essay grading is not a wrong answer. Evidence readers return no correctness until an essay score exists. Exam and library controller readers require the active account ID; the exam reader also stops after its controller is cleared.
- A high-confidence error is a **possible** misconception requiring a diagnostic question, not a diagnosis. One correct independent answer is supporting evidence, not proof of understanding.
- Question selection returns IDs only. It never sees answer keys, grades an exam, changes XP, or writes progress. Missing vetted questions produce unfilled slots instead of substituting a different grade, subject, skill or difficulty.
- `AdaptivePracticePlanner` connects loaded library history, reasoning signals and selection. Reasoning inputs must exactly match nonconflicting supplied evidence and the blueprint's skill-map version. Library IDs are excluded only using library history for the same subject and grade: an AI-practice or exam ID with the same text is a different identifier. This is a plan from loaded history, not a guarantee that a question has never been seen in any historical session.
- The planner reports the review for each requested skill and any unfilled slots. It does not silently alter a caller's blueprint based on a reasoning signal. Choosing a diagnostic blueprint remains a future server integration using reviewed metadata.
- No new table, policy, Edge Function, Android permission or network call is required for these foundations. Rolling back this work means removing the isolated policy code and its tests; existing user records and exam attempts are untouched.

## Quality and operational limits

- The new policies are synchronous, in-memory calculations; they make no AI request and incur no AI-provider cost today. Runtime latency in a complete learner flow has not been measured.
- `ReasoningReviewPolicy` rejects observations from a different local account ID, but future callers must also verify the active authenticated session and server ownership. This policy is not an authorization boundary.
- Selection currently scans candidates for each blueprint slot. Before using a large question bank, benchmark this path and consider an indexed candidate lookup. Do not optimize against an invented workload.
- The inspected app source does not expose a reviewed skill map or confidence capture to these policies. No cloud inventory was performed, so this note does not establish what metadata exists in the production database. The new policies are not connected to learner UI, scoring or persistence.

## Next integration acceptance criteria

- Load owned history and reviewed question metadata through an authenticated server path. Record which sessions were loaded; do not label partial history as complete.
- Capture confidence directly from the learner, associate it with the exact answer attempt and question version, and retain it through server persistence before using it across sessions.
- Bind every skill label to a reviewed skill-map version. A missing or older version must not silently become a current skill conclusion.
- Treat a partial selection as incomplete. A learner-facing flow must either load more eligible questions or explain the shortage before starting a practice session.
- Keep official timed-exam blueprints and server-side grading independent from adaptive practice selection. Verify account switching, stale responses, retries and offline errors in the integrated flow.

## Verification levels

Record separately for each future integration: source present, local unit tests passed, device workflow passed, cloud workflow passed and deployed version. The presence of these files proves only the first level until the corresponding test or deployment evidence is attached.

Local checks on 20 November 2026 for this working tree: `:app:testDebugUnitTest` passed 251/251 tests after adding the loaded-history planner and skill-map version checks; `:app:assembleDebug` and `:app:lintDebug` passed with 0 lint errors, 200 warnings and 2 hints. The preceding foundation check passed 18 Node tests covering the unchanged Edge AI code; they were not repeated for this Android-only follow-up. Backend pytest, device workflows, cloud verification and deployment were not performed for these changes. The lint warnings require separate triage; a successful lint task does not mean the UI is defect-free.
