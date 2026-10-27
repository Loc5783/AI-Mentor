package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    private val edit: Runnable, private val settings: Runnable, private val learn: Runnable, private val schedule: Runnable = Runnable{}) {
    private var snapshot by mutableStateOf<LearningOverviewRepository.Snapshot?>(null)
    private var selected by mutableStateOf<Set<SubjectId>>(emptySet())
    private var name by mutableStateOf("")
    private var avatar by mutableStateOf<String?>(null)
    private var grade by mutableIntStateOf(0)
    private var loading by mutableStateOf(true)
    private var failed by mutableStateOf(false)

    fun view(): View = composeHost(context) {
        OverviewScreen(profile, name, avatar, grade, snapshot, selected, loading, failed,
            { open.accept(it) }, { action.run() }, { edit.run() }, { settings.run() }, { learn.run() }, { schedule.run() })
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
    open: (SubjectId) -> Unit, action: () -> Unit, edit: () -> Unit, settings: () -> Unit, learn: () -> Unit, schedule: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val data = snapshot?.data
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(context)
    val heroBg = if (dark) MentorTokens.DarkPrimaryContainer else MentorTokens.PrimaryContainer

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())) {
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
                        .size(60.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatar.isNullOrBlank()) {
                        AsyncImage(avatar, null, Modifier.fillMaxSize())
                    } else {
                        Text(
                            text = name.trim().take(2).uppercase().ifBlank { "HI" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = name.ifBlank { stringResource(R.string.home_default_user) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (grade in 10..12) stringResource(R.string.chat_grade, grade) else stringResource(R.string.design_initial),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (profile) {
                    IconButton(settings) {
                        Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(MentorTokens.PagePadding), verticalArrangement = Arrangement.spacedBy(MentorTokens.PagePadding)) {
            if (loading) {
                repeat(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
            } else {
                if (failed || data == null || snapshot?.mode != LearningOverviewRepository.Mode.LIVE) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                }

                if (!profile) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().clickable { schedule() }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(R.drawable.ic_book_course), contentDescription = null, tint = MentorTokens.Info, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Thời khóa biểu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Xem lịch học cá nhân", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (data != null) {
                    if (snapshot?.mode == LearningOverviewRepository.Mode.LIVE)
                        Text(stringResource(R.string.overview_current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    // Gamified Stats Grid Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(painter = painterResource(R.drawable.ic_flame), contentDescription = null, tint = MentorTokens.RewardStreakLight, modifier = Modifier.size(26.dp))
                                    Column {
                                        Text(stringResource(R.string.overview_streak, data.streakDays), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.profile_streak_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(painter = painterResource(R.drawable.ic_lightning), contentDescription = null, tint = MentorTokens.RewardXpBar, modifier = Modifier.size(26.dp))
                                    Column {
                                        Text(stringResource(R.string.overview_daily_xp, data.dailyXp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.profile_xp_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${data.totalXp} XP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.design_total_xp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${data.completedLessons} / ${data.totalLessons} bài", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.profile_lessons_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            MentorProgress(if (data.totalLessons == 0) 0f else data.completedLessons.toFloat() / data.totalLessons)
                        }
                    }
                    WeekChart(data)
                } else if (!failed) Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.design_chart_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!profile) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.design_goal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.design_chart_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MentorButton(stringResource(R.string.course_continue), learn)
                        }
                    }
                    Text(stringResource(R.string.design_subjects), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    selected.forEach { subject ->
                        val progress = data?.subjects?.firstOrNull { it.id == subject.wireId }
                        Surface(
                            Modifier.fillMaxWidth().testTag("dashboard-subject:${subject.wireId}"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(subject.label(LocaleHelper.getLanguage(context)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (progress != null) {
                                    Text(stringResource(R.string.overview_subject_progress, progress.completed, progress.total, progress.experience), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MentorProgress(if (progress.total == 0) 0f else progress.completed.toFloat() / progress.total)
                                } else Text(stringResource(R.string.dashboard_progress_pending), style = MaterialTheme.typography.bodySmall)
                                MentorButton(stringResource(R.string.course_open), { open(subject) })
                            }
                        }
                    }
                    if (selected.isEmpty()) MentorButton(stringResource(R.string.lesson_choose_prompt), learn)
                } else if (data != null) Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.overview_practice_heading), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(if (data.questionCount == 0) R.string.overview_no_practice else R.string.overview_practice,
                            data.firstTryCorrect, data.questionCount), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.overview_assisted, data.assisted), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun WeekChart(data: LearningOverviewData) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (data.week.isEmpty() || data.week.none { it.xp > 0 || it.streakLit }) {
                Text(stringResource(R.string.design_week_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.design_chart_legend), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}