package com.example.asm_final.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import com.example.mentor.learning.SubjectId

enum class HubSection {
    MAIN_HUB,
    SUBJECT_WORKSPACE,
    LIBRARY,
    EXAM_PREP,
    ROADMAP,
    NOTEBOOK,
    AI_PRACTICE
}

/**
 * Pure Kotlin presentation and state model for the Lesson Hub navigation flow.
 * Manages the study hub, subject workspaces, and their child tools with grade-scoped state.
 * Uses Compose mutableStateOf properties so UI automatically recomposes on state changes.
 */
class LessonHubNavigationModel(
    initialSection: HubSection = HubSection.MAIN_HUB,
    initialSubjectWireId: String? = null,
    initialGrade: Int = 10,
    initialNotebookSource: HubSection = HubSection.MAIN_HUB
) {
    var currentSection: HubSection by mutableStateOf(initialSection)
        private set

    var selectedSubjectWireId: String? by mutableStateOf(initialSubjectWireId)
        private set

    private val _grade = mutableIntStateOf(if (initialGrade in 10..12) initialGrade else 10)
    val grade: Int get() = _grade.intValue

    var notebookSourceSection: HubSection by mutableStateOf(initialNotebookSource)
        private set

    var toolSourceSection: HubSection by mutableStateOf(HubSection.MAIN_HUB)
        private set

    val selectedSubject: SubjectId?
        get() = selectedSubjectWireId?.let { wire ->
            SubjectId.values().firstOrNull { it.wireId == wire }
        }

    fun setGrade(newGrade: Int) {
        if (newGrade in 10..12 && newGrade != _grade.intValue) {
            _grade.intValue = newGrade
            selectedSubjectWireId = null
            currentSection = HubSection.MAIN_HUB
        }
    }

    fun navigateTo(section: HubSection) {
        toolSourceSection = HubSection.MAIN_HUB
        currentSection = section
    }

    fun openWorkspace(subjectId: SubjectId) {
        selectedSubjectWireId = subjectId.wireId
        currentSection = HubSection.SUBJECT_WORKSPACE
    }

    fun openWorkspaceTool(section: HubSection) {
        require(currentSection == HubSection.SUBJECT_WORKSPACE)
        require(section == HubSection.LIBRARY || section == HubSection.EXAM_PREP || section == HubSection.AI_PRACTICE)
        toolSourceSection = HubSection.SUBJECT_WORKSPACE
        currentSection = section
    }

    fun selectSubject(subjectId: SubjectId?) {
        selectedSubjectWireId = subjectId?.wireId
    }

    /**
     * Opens the Wrong Questions Notebook.
     * @param subjectId Specific subject filter, or null for all subjects.
     * @param fromSection The section from which Notebook was opened (defaults to currentSection).
     */
    fun openNotebook(subjectId: SubjectId? = null, fromSection: HubSection = currentSection) {
        notebookSourceSection = fromSection
        selectedSubjectWireId = subjectId?.wireId
        currentSection = HubSection.NOTEBOOK
    }

    /**
     * Handles back navigation according to the product hierarchy.
     * Returns true if back navigation was handled internally within Lesson Hub.
     * Returns false if at the root (MAIN_HUB), delegating to shell/system back.
     */
    fun handleBack(): Boolean {
        return when (currentSection) {
            HubSection.NOTEBOOK -> {
                // Return to notebookSourceSection (e.g. LIBRARY).
                // If it was opened from subject quiz list, selectedSubjectWireId was preserved so it returns to quiz list.
                // If it was opened from subject list (all subjects), selectedSubjectWireId was null so it returns to subject list.
                currentSection = notebookSourceSection
                true
            }
            HubSection.LIBRARY -> {
                if (toolSourceSection == HubSection.SUBJECT_WORKSPACE) {
                    currentSection = HubSection.SUBJECT_WORKSPACE
                } else if (selectedSubjectWireId != null) {
                    selectedSubjectWireId = null
                } else {
                    currentSection = HubSection.MAIN_HUB
                }
                true
            }
            HubSection.EXAM_PREP -> {
                currentSection = toolSourceSection
                true
            }
            HubSection.ROADMAP -> {
                currentSection = HubSection.MAIN_HUB
                true
            }
            HubSection.AI_PRACTICE -> {
                currentSection = toolSourceSection
                true
            }
            HubSection.SUBJECT_WORKSPACE -> {
                selectedSubjectWireId = null
                currentSection = HubSection.MAIN_HUB
                true
            }
            HubSection.MAIN_HUB -> {
                false
            }
        }
    }

    companion object {
        val Saver: Saver<LessonHubNavigationModel, Any> = mapSaver(
            save = { model ->
                mapOf(
                    "currentSection" to model.currentSection.name,
                    "selectedSubjectWireId" to model.selectedSubjectWireId,
                    "grade" to model.grade,
                    "toolSourceSection" to model.toolSourceSection.name,
                    "notebookSourceSection" to model.notebookSourceSection.name
                )
            },
            restore = { rawMap ->
                @Suppress("UNCHECKED_CAST")
                val map = rawMap as? Map<String, Any?> ?: emptyMap()
                val section = (map["currentSection"] as? String)?.let {
                    try { HubSection.valueOf(it) } catch (_: Exception) { null }
                } ?: HubSection.MAIN_HUB
                val subjectWireId = map["selectedSubjectWireId"] as? String
                val grade = (map["grade"] as? Int) ?: 10
                val notebookSource = (map["notebookSourceSection"] as? String)?.let {
                    try { HubSection.valueOf(it) } catch (_: Exception) { null }
                } ?: HubSection.MAIN_HUB
                LessonHubNavigationModel(
                    initialSection = section,
                    initialSubjectWireId = subjectWireId,
                    initialGrade = grade,
                    initialNotebookSource = notebookSource
                ).also { model ->
                    model.toolSourceSection = (map["toolSourceSection"] as? String)?.let {
                        runCatching { HubSection.valueOf(it) }.getOrNull()
                    } ?: HubSection.MAIN_HUB
                }
            }
        )
    }
}
