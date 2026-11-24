package com.example.asm_final.learning

internal data class AdaptivePracticePlan(
    val selection: PracticeSelection,
    val reviewBySkill: Map<String, ReasoningReview>,
    val excludedLoadedLibraryQuestionIds: Set<String>
)

/**
 * Joins reasoning signals and selection for the existing question library.
 * Only supplied snapshots are known: this cannot promise that a question was never seen in
 * an unloaded session. Authentication, history loading and starting a session remain server work.
 */
internal object AdaptivePracticePlanner {
    fun planFromLoadedLibraryHistory(
        ownerId: Int,
        activeOwnerId: Int,
        blueprint: PracticeBlueprint,
        libraryCandidates: List<PracticeCandidate>,
        evidence: List<LearningEvidence>,
        observations: List<ReasoningObservation>
    ): AdaptivePracticePlan {
        require(ownerId > 0 && ownerId == activeOwnerId) { "The active account changed" }
        require(evidence.all { it.ownerId == ownerId }) { "History belongs to another account" }
        require(observations.all { it.evidence.ownerId == ownerId }) {
            "Reasoning observations belong to another account"
        }
        // A review must refer to an actual supplied observation, not an invented result.
        val reconciled = LearningEvidenceReader.combine(ownerId, evidence).toSet()
        require(observations.all { it.evidence in reconciled }) {
            "Reasoning evidence is missing or conflicting in history"
        }

        // AI practice and exam IDs can collide with library IDs. They are separate namespaces.
        val seen = evidence.filter {
            it.source == LearningEvidence.Source.LIBRARY && it.subjectId == blueprint.subjectId &&
                it.grade == blueprint.grade &&
                (it.observation == LearningEvidence.Observation.ANSWERED_QUESTION ||
                    it.observation == LearningEvidence.Observation.SKIPPED_QUESTION)
        }.map { it.itemId }.toSet()
        val selection = AdaptiveQuestionSelection.select(blueprint, libraryCandidates, seen)
        val reviews = blueprint.slots.map { it.skillId }.distinct().associateWith { skillId ->
            ReasoningReviewPolicy.review(ownerId, blueprint.subjectId, blueprint.grade, skillId,
                blueprint.skillMapVersion, observations)
        }
        return AdaptivePracticePlan(selection, reviews, seen)
    }
}
