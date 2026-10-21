package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.*
import java.util.function.Consumer

class SubjectHubUi(private val context: Context, private val chooseGrade: Runnable,
    private val chooseSubjects: Runnable, private val open: Consumer<SubjectId>, private val tools: Consumer<SubjectId>) {
    private var grade by mutableIntStateOf(0)
    private var selected by mutableStateOf<Set<SubjectId>>(emptySet())
    private var error by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    fun render(year: Int, subjects: Set<SubjectId>) { grade = year; selected = subjects.toSet(); loading = false; error = false }
    fun failure() { loading = false; error = true }
    fun view(): View = composeHost(context) {
        val active = setOf(SubjectId.BIOLOGY, SubjectId.CHEMISTRY, SubjectId.PHYSICS)
        LazyColumn(Modifier.fillMaxSize().testTag("subject-list").background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(MentorTokens.PagePadding), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)) {
            item {
                Text(stringResource(R.string.lesson_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.lesson_description), Modifier.padding(top = MentorTokens.SpaceSmall))
            }
            item {
                MentorCard(Modifier.fillMaxWidth()) {
                    Text(if (grade in 10..12) stringResource(R.string.chat_grade, grade) else stringResource(R.string.chat_choose_grade),
                        style = MaterialTheme.typography.titleLarge)
                    TextButton({ chooseGrade.run() }) { Text(stringResource(R.string.lesson_grade)) }
                    MentorButton(stringResource(R.string.lesson_manage), { chooseSubjects.run() })
                }
            }
            if (loading) items(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
            if (error) item { Text(stringResource(R.string.chat_storage_error)) }
            if (!loading && selected.isEmpty()) item { Text(stringResource(R.string.lesson_choose_prompt)) }
            items(SubjectId.values().filter { it in selected || it !in active }, key = { it.wireId }) { subject ->
                MentorCard(Modifier.fillMaxWidth().testTag("subject:${subject.wireId}")) {
                    Text(subject.label(LocaleHelper.getLanguage(context)), style = MaterialTheme.typography.titleLarge)
                    if (subject in active) {
                        Text(stringResource(R.string.lesson_path_pending), style = MaterialTheme.typography.bodySmall)
                        MentorButton(stringResource(R.string.course_open), { open.accept(subject) })
                    } else Text(stringResource(R.string.subject_lessons_soon))
                    val toolLabel = when (subject) {
                        SubjectId.PHYSICS -> R.string.lesson_circuit
                        SubjectId.CHEMISTRY -> R.string.lesson_periodic
                        SubjectId.MATH -> R.string.lesson_open_tools
                        else -> 0
                    }
                    if (toolLabel != 0) OutlinedButton({ tools.accept(subject) }) { Text(stringResource(toolLabel)) }
                }
            }
        }
    }
}
