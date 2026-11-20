package com.example.asm_final.learning

/** A practice blueprint uses reviewed skill metadata, never a guessed topic-to-skill match. */
internal data class PracticeSlot(
    val skillId: String,
    val kind: ExamQuestionKind,
    val difficulty: String
)

internal data class PracticeBlueprint(
    val subjectId: String,
    val grade: Int,
    val skillMapVersion: String,
    val slots: List<PracticeSlot>
)

internal data class PracticeCandidate(
    val id: String,
    val subjectId: String,
    val grade: Int,
    val skillId: String?,
    val skillMapVersion: String?,
    val kind: ExamQuestionKind,
    val difficulty: String,
    val published: Boolean,
    val reviewed: Boolean
)

internal data class PracticeSelection(
    val selectedBySlot: Map<Int, String>,
    val unfilledSlots: List<Int>
) {
    val questionIds: List<String> get() = selectedBySlot.toSortedMap().values.toList()
}

/** Selects IDs for practice only. Existing timed exams keep their fixed server blueprint. */
internal object AdaptiveQuestionSelection {
    fun select(
        blueprint: PracticeBlueprint,
        candidates: List<PracticeCandidate>,
        seenQuestionIds: Set<String>
    ): PracticeSelection {
        require(blueprint.subjectId.isNotBlank() && blueprint.grade in 10..12)
        require(blueprint.skillMapVersion.isNotBlank())
        require(blueprint.slots.isNotEmpty()) { "A practice blueprint needs at least one slot" }
        require(blueprint.slots.all {
            it.skillId.isNotBlank() && it.kind != ExamQuestionKind.UNKNOWN && it.difficulty.isNotBlank()
        })
        require(candidates.map { it.id }.distinct().size == candidates.size) {
            "Candidate IDs must be unique"
        }

        val eligible = candidates.filter {
            it.id.isNotBlank() && it.published && it.reviewed &&
                it.subjectId == blueprint.subjectId && it.grade == blueprint.grade &&
                it.skillMapVersion == blueprint.skillMapVersion &&
                it.id !in seenQuestionIds
        }.sortedBy { it.id }
        val selected = linkedMapOf<Int, String>()
        val missing = mutableListOf<Int>()
        blueprint.slots.forEachIndexed { index, slot ->
            val match = eligible.firstOrNull {
                it.id !in selected.values && it.skillId == slot.skillId &&
                    it.kind == slot.kind && it.difficulty == slot.difficulty
            }
            if (match == null) missing += index else selected[index] = match.id
        }
        return PracticeSelection(selected, missing)
    }
}
