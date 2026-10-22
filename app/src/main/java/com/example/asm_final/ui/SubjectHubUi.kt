package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
        val chosen = listOf(SubjectId.PHYSICS, SubjectId.CHEMISTRY, SubjectId.BIOLOGY)
            .filter { it in selected }
        val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(context)
        val heroBg = if (dark) MentorTokens.DarkPrimaryContainer else MentorTokens.PrimaryContainer

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Hero Header matching Profile & Competition UI
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(heroBg)
                    .padding(horizontal = MentorTokens.PagePadding, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_book_course),
                            contentDescription = null,
                            tint = MentorTokens.SuccessLightText,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.lesson_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.lesson_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).testTag("subject-list"),
                contentPadding = PaddingValues(MentorTokens.PagePadding),
                verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceMedium)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (grade in 10..12) stringResource(R.string.chat_grade, grade) else stringResource(R.string.chat_choose_grade),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.lesson_grade),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton({ chooseGrade.run() }) { Text("Đổi lớp") }
                                Button({ chooseSubjects.run() }) { Text(stringResource(R.string.lesson_manage)) }
                            }
                        }
                    }
                }

                if (loading) {
                    items(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
                }

                if (error) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.chat_storage_error), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (!loading && chosen.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.lesson_choose_prompt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MentorButton(stringResource(R.string.lesson_manage), { chooseSubjects.run() })
                            }
                        }
                    }
                }

                items(chosen, key = { it.wireId }) { subject ->
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .testTag("subject:${subject.wireId}"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 0.5.dp
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = subject.label(LocaleHelper.getLanguage(context)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val toolLabel = when (subject) {
                                    SubjectId.PHYSICS -> R.string.lesson_circuit
                                    SubjectId.CHEMISTRY -> R.string.lesson_periodic
                                    SubjectId.MATH -> R.string.lesson_open_tools
                                    else -> 0
                                }
                                if (toolLabel != 0) {
                                    OutlinedButton(onClick = { tools.accept(subject) }) {
                                        Text(stringResource(toolLabel))
                                    }
                                }
                            }

                            if (grade == 10) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.lesson_path_pending),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MentorButton(stringResource(R.string.course_open), { open.accept(subject) })
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.subject_grade10_only),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
