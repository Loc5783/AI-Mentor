package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.asm_final.R
import com.example.asm_final.dashboard.LearningOverviewData
import com.example.asm_final.dashboard.LearningOverviewRepository
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.*
import java.util.function.Consumer

/** A view-only bridge: lifecycle, identity checks and repository work remain in the Fragment. */
class OverviewUi(private val context: Context, private val profile: Boolean,
    private val open: Consumer<SubjectId>, private val action: Runnable,
    private val edit: Runnable, private val settings: Runnable, private val learn: Runnable) {
    private var snapshot by mutableStateOf<LearningOverviewRepository.Snapshot?>(null)
    private var selected by mutableStateOf<Set<SubjectId>>(emptySet())
    private var name by mutableStateOf("")
    private var avatar by mutableStateOf<String?>(null)
    private var grade by mutableIntStateOf(0)
    private var loading by mutableStateOf(true)
    private var failed by mutableStateOf(false)

    fun view(): View = composeHost(context) {
        OverviewScreen(profile, name, avatar, grade, snapshot, selected, loading, failed,
            { open.accept(it) }, { action.run() }, { edit.run() }, { settings.run() }, { learn.run() })
    }
    fun loading() { loading = true; failed = false }
    fun failure() { loading = false; failed = true }
    fun identity(value: String, uri: String?) { name = value; avatar = uri }
    fun render(value: LearningOverviewRepository.Snapshot, subjects: Set<SubjectId>, year: Int) {
        snapshot = value; selected = subjects.toSet(); grade = year; loading = false; failed = false
    }
}

@Composable
private fun OverviewScreen(profile: Boolean, name: String, avatar: String?, grade: Int,
    snapshot: LearningOverviewRepository.Snapshot?, selected: Set<SubjectId>, loading: Boolean, failed: Boolean,
    open: (SubjectId) -> Unit, action: () -> Unit, edit: () -> Unit, settings: () -> Unit, learn: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val data = snapshot?.data
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState()).padding(MentorTokens.PagePadding), verticalArrangement = Arrangement.spacedBy(MentorTokens.PagePadding)) {
        if (profile) TextButton(settings) { Text(stringResource(R.string.settings_title)) }
        Surface(shape = MaterialTheme.shapes.large, color = MentorTokens.Blue, contentColor = MentorTokens.OnBrand) {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MentorTokens.Blue, MentorTokens.Indigo))).padding(MentorTokens.SpaceLarge), verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall)) {
                if (profile) Box(Modifier.size(MentorTokens.AvatarSize).clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(name.trim().take(2).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleLarge)
                    if (!avatar.isNullOrBlank()) AsyncImage(avatar, null, Modifier.fillMaxSize())
                }
                Text(stringResource(if (profile) R.string.design_profile else R.string.design_today), style = MaterialTheme.typography.labelLarge)
                Text(name.ifBlank { stringResource(R.string.home_default_user) }, style = MaterialTheme.typography.headlineMedium)
                Text(if (grade in 10..12) stringResource(R.string.chat_grade, grade) else stringResource(R.string.design_initial))
                if (profile) MentorButton(stringResource(R.string.profile_edit), edit)
            }
        }
        if (loading) {
            repeat(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
        } else {
            if (failed || data == null || snapshot?.mode != LearningOverviewRepository.Mode.LIVE) {
                MentorCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(when {
                        snapshot?.needsLogin == true -> R.string.overview_expired
                        failed -> R.string.overview_error
                        snapshot?.mode == LearningOverviewRepository.Mode.CACHED -> R.string.overview_cached
                        snapshot?.mode == LearningOverviewRepository.Mode.LOCAL -> R.string.dashboard_connect_notice
                        else -> R.string.overview_error
                    }))
                    MentorButton(stringResource(if (snapshot?.needsLogin == true) R.string.login_btn
                        else if (snapshot?.mode == LearningOverviewRepository.Mode.LOCAL) R.string.course_open else R.string.overview_reload),
                        if (snapshot?.mode == LearningOverviewRepository.Mode.LOCAL) learn else action)
                }
            }
            if (data != null) {
                if (snapshot?.mode == LearningOverviewRepository.Mode.LIVE)
                    Text(stringResource(R.string.overview_current), style = MaterialTheme.typography.labelSmall)
                // Wrap instead of squeezing figures into unreadably small columns.
                MentorCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.overview_streak, data.streakDays), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.overview_daily_xp, data.dailyXp), style = MaterialTheme.typography.titleMedium)
                    Text("${stringResource(R.string.design_total_xp)} · ${data.totalXp} XP")
                    Text(stringResource(R.string.overview_lessons, data.completedLessons, data.totalLessons))
                    MentorProgress(if (data.totalLessons == 0) 0f else data.completedLessons.toFloat() / data.totalLessons)
                }
                WeekChart(data)
            } else if (!failed) MentorCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.design_chart_hint))
            }
            if (!profile) {
                MentorCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.design_goal), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.design_chart_hint), style = MaterialTheme.typography.bodyMedium)
                    MentorButton(stringResource(R.string.course_continue), learn)
                }
                Text(stringResource(R.string.design_subjects), style = MaterialTheme.typography.titleLarge)
                selected.forEach { subject ->
                    val progress = data?.subjects?.firstOrNull { it.id == subject.wireId }
                    MentorCard(Modifier.fillMaxWidth().testTag("dashboard-subject:${subject.wireId}")) {
                        Text(subject.label(LocaleHelper.getLanguage(context)), style = MaterialTheme.typography.titleMedium)
                        if (progress != null) {
                            Text(stringResource(R.string.overview_subject_progress, progress.completed, progress.total, progress.experience))
                            MentorProgress(if (progress.total == 0) 0f else progress.completed.toFloat() / progress.total)
                        } else Text(stringResource(R.string.dashboard_progress_pending))
                        MentorButton(stringResource(R.string.course_open), { open(subject) })
                    }
                }
                if (selected.isEmpty()) MentorButton(stringResource(R.string.lesson_choose_prompt), learn)
            } else if (data != null) MentorCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.overview_practice_heading), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(if (data.questionCount == 0) R.string.overview_no_practice else R.string.overview_practice,
                    data.firstTryCorrect, data.questionCount))
                Text(stringResource(R.string.overview_assisted, data.assisted))
            }
        }
    }
}

@Composable
internal fun WeekChart(data: LearningOverviewData) {
    MentorCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleLarge)
        if (data.week.isEmpty() || data.week.none { it.xp > 0 || it.streakLit }) {
            Text(stringResource(R.string.design_week_empty))
        } else {
            Text(stringResource(R.string.design_chart_legend), style = MaterialTheme.typography.bodySmall)
            val max = data.week.maxOf { it.xp }.coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().heightIn(min = MentorTokens.ChartHeight), horizontalArrangement = Arrangement.spacedBy(MentorTokens.SpaceSmall), verticalAlignment = Alignment.Bottom) {
                data.week.forEach { day ->
                    val label = stringResource(R.string.design_day_xp, day.day, day.xp)
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text(day.xp.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.fillMaxWidth().height((day.xp.toFloat() / max * 100).coerceAtLeast(3f).dp)
                            .clip(MaterialTheme.shapes.small).background(if (day.streakLit) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary))
                        Text(day.day.takeLast(2), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
