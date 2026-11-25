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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.asm_final.R
import com.example.asm_final.dashboard.LearningOverviewData
import com.example.asm_final.dashboard.LearningOverviewRepository
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.schedule.domain.TodoTask
import com.example.mentor.ui.design.*

private data class StudyTimeSummary(val totalSeconds: Long, val week: List<Day>) {
    data class Day(val date: String, val seconds: Long)

    companion object {
        fun from(value: org.json.JSONObject?): StudyTimeSummary? {
            if (value == null) return null
            return runCatching {
                val total = value.getLong("totalSeconds")
                require(total >= 0)
                val rows = value.getJSONArray("week")
                require(rows.length() == 7)
                val days = (0 until rows.length()).map { index ->
                    val row = rows.getJSONObject(index)
                    val date = row.getString("day")
                    val seconds = row.getLong("seconds")
                    require(date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) && seconds >= 0)
                    Day(date, seconds)
                }
                StudyTimeSummary(total, days)
            }.getOrNull()
        }
    }
}

/** A view-only bridge: lifecycle, identity checks and repository work remain in the Fragment. */
class OverviewUi(private val context: Context, private val profile: Boolean,
    private val action: Runnable,
    private val edit: Runnable, private val settings: Runnable, private val learn: Runnable,
    private val schedule: Runnable = Runnable{}) {
    private var snapshot by mutableStateOf<LearningOverviewRepository.Snapshot?>(null)
    private var studyTime by mutableStateOf<StudyTimeSummary?>(null)
    private var studyTimeLoaded by mutableStateOf(false)
    private var todayTasks by mutableStateOf<List<TodoTask>?>(null)
    private var todayTasksLoaded by mutableStateOf(false)
    private var name by mutableStateOf("")
    private var avatar by mutableStateOf<String?>(null)
    private var grade by mutableIntStateOf(0)
    private var loading by mutableStateOf(true)
    private var failed by mutableStateOf(false)

    fun view(): View = composeHost(context, scenicBackground = true) {
        OverviewScreen(profile, name, avatar, grade, snapshot, studyTime, studyTimeLoaded,
            todayTasks, todayTasksLoaded, loading, failed,
            { action.run() }, { edit.run() }, { settings.run() }, { learn.run() },
            { schedule.run() })
    }
    fun loading() { loading = true; failed = false; studyTimeLoaded = false; todayTasksLoaded = false }
    fun failure() { loading = false; failed = true }
    fun identity(value: String, uri: String?) { name = value; avatar = uri }
    fun render(value: LearningOverviewRepository.Snapshot, year: Int) {
        snapshot = value; grade = year; loading = false; failed = false
    }
    fun activity(summary: org.json.JSONObject?) {
        studyTime = StudyTimeSummary.from(summary)
        studyTimeLoaded = true
    }
    fun tasks(items: List<TodoTask>?) {
        todayTasks = items?.toList()
        todayTasksLoaded = true
    }
}

@Composable
private fun OverviewScreen(profile: Boolean, name: String, avatar: String?, grade: Int,
    snapshot: LearningOverviewRepository.Snapshot?, studyTime: StudyTimeSummary?, studyTimeLoaded: Boolean,
    todayTasks: List<TodoTask>?, todayTasksLoaded: Boolean,
    loading: Boolean, failed: Boolean,
    action: () -> Unit, edit: () -> Unit, settings: () -> Unit, learn: () -> Unit,
    schedule: () -> Unit = {}) {
    val context = LocalContext.current
    val data = snapshot?.data
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(context)
    val palette = premiumDashboardPalette(dark)

    Column(Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        if (profile) PremiumDashboardHeader {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(palette.iconSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatar.isNullOrBlank()) {
                        AsyncImage(avatar, null, Modifier.fillMaxSize())
                    } else {
                        Text(
                            text = name.trim().firstOrNull()?.uppercase() ?: "A",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.title
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = name.ifBlank { stringResource(R.string.home_default_user) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (grade in 10..12) stringResource(R.string.chat_grade, grade) else stringResource(R.string.design_initial),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondary
                    )
                }
                IconButton(settings) {
                    Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title), tint = palette.secondary)
                }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!profile) {
                HomeGreeting(name, avatar, grade, palette, schedule)
                HomeProgressHero(data, loading, palette, learn)
                HomeTodayTasks(todayTasks, todayTasksLoaded, palette, schedule)
            }
            if (loading) {
                repeat(2) { MentorSkeleton(stringResource(R.string.course_loading)) }
            } else {
                if (failed || data == null || snapshot?.mode != LearningOverviewRepository.Mode.LIVE) {
                    PremiumDashboardCard(palette, highlighted = true) {
                            Text(stringResource(R.string.home_status_title), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = palette.title)
                            Text(stringResource(when {
                                snapshot?.needsLogin == true -> R.string.overview_expired
                                failed -> R.string.overview_error
                                snapshot?.mode == LearningOverviewRepository.Mode.CACHED -> R.string.overview_cached
                                snapshot?.mode == LearningOverviewRepository.Mode.LOCAL -> R.string.dashboard_connect_notice
                                else -> R.string.overview_error
                            }), style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
                            PremiumDashboardAction(stringResource(if (snapshot?.needsLogin == true) R.string.login_btn
                                else if (snapshot?.mode == LearningOverviewRepository.Mode.LOCAL) R.string.course_open else R.string.overview_reload),
                                palette, if (snapshot?.mode == LearningOverviewRepository.Mode.LOCAL) learn else action)
                    }
                }


                if (data != null) {
                    if (snapshot?.mode == LearningOverviewRepository.Mode.LIVE)
                        Text(stringResource(R.string.overview_current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    if (profile) PremiumDashboardCard(palette, highlighted = true) {
                        Text(stringResource(R.string.home_progress_title), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = palette.title)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            OverviewMetric(Modifier.weight(1f), R.drawable.ic_flame,
                                stringResource(R.string.overview_streak, data.streakDays),
                                stringResource(R.string.profile_streak_label), palette.streak, palette)
                            Spacer(Modifier.width(10.dp))
                            OverviewMetric(Modifier.weight(1f), R.drawable.ic_lightning,
                                stringResource(R.string.overview_daily_xp, data.dailyXp),
                                stringResource(R.string.competition_daily), palette.experience, palette)
                        }
                        HorizontalDivider(color = palette.border)
                        Row(Modifier.fillMaxWidth()) {
                            OverviewTextMetric(Modifier.weight(1f), "${data.totalXp} XP",
                                stringResource(R.string.design_total_xp), palette)
                            OverviewTextMetric(Modifier.weight(1f),
                                stringResource(R.string.overview_lessons, data.completedLessons, data.totalLessons),
                                stringResource(R.string.profile_lessons_label), palette)
                        }
                        MentorProgress(if (data.totalLessons == 0) 0f else data.completedLessons.toFloat() / data.totalLessons)
                    }
                    if (!profile) Text(stringResource(R.string.home_analysis_title),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = palette.title)
                    if (profile) {
                        HomeWeekChart(data, palette)
                        SubjectPieChart(data.subjects, palette)
                    } else {
                        HomeStudyTimeChart(studyTime, studyTimeLoaded, palette)
                        SubjectPieChart(data.subjects, palette)
                    }
                } else if (!failed) PremiumDashboardCard(palette) {
                    Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = palette.title)
                    MentorIllustration(MentorArtwork.GOALS)
                    Text(stringResource(R.string.design_week_empty), style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondary)
                }
                if (profile && data != null) Surface(
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
private fun HomeGreeting(name: String, avatar: String?, grade: Int,
    palette: PremiumDashboardPalette, openSchedule: () -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag("home-greeting"),
        shape = RoundedCornerShape(22.dp), color = palette.card,
        border = BorderStroke(1.dp, palette.border), shadowElevation = 6.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF5360DF), Color(0xFF9363DA)))),
                contentAlignment = Alignment.Center) {
                if (avatar.isNullOrBlank()) Text(name.trim().firstOrNull()?.uppercase() ?: "A",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = Color.White)
                else AsyncImage(avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.home_greeting, name.ifBlank { stringResource(R.string.home_default_user) }),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = palette.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (grade in 10..12) stringResource(R.string.chat_grade, grade)
                    else stringResource(R.string.design_initial),
                    style = MaterialTheme.typography.bodySmall, color = palette.secondary)
            }
            IconButton(openSchedule) {
                Icon(painterResource(R.drawable.ic_schedule),
                    contentDescription = stringResource(R.string.home_schedule_title),
                    tint = palette.accent)
            }
        }
    }
}

@Composable
private fun HomeProgressHero(data: LearningOverviewData?, loading: Boolean,
    palette: PremiumDashboardPalette, learn: () -> Unit) {
    val progress = if (data == null || data.totalLessons == 0) 0f
        else (data.completedLessons.toFloat() / data.totalLessons).coerceIn(0f, 1f)
    PremiumDashboardCard(palette, Modifier.testTag("home-progress-hero"), highlighted = true) {
        Text(stringResource(R.string.home_next_step_title),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = palette.title)
        Text(stringResource(R.string.home_next_step_desc),
            style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
        if (data != null && data.totalLessons > 0) {
            Text(stringResource(R.string.overview_lessons, data.completedLessons, data.totalLessons),
                style = MaterialTheme.typography.bodySmall, color = palette.title)
            MentorProgress(progress)
        } else if (loading) {
            MentorSkeleton(stringResource(R.string.course_loading))
        }
        PremiumDashboardAction(stringResource(R.string.home_next_step_action), palette,
            learn, Modifier.fillMaxWidth())
    }
}

@Composable
private fun HomeTodayTasks(tasks: List<TodoTask>?, loaded: Boolean,
    palette: PremiumDashboardPalette, openSchedule: () -> Unit) {
    PremiumDashboardCard(palette, Modifier.testTag("home-today-tasks")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.home_today_tasks), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = palette.title)
            if (!tasks.isNullOrEmpty()) Text(stringResource(R.string.home_tasks_count,
                tasks.count { it.isCompleted }, tasks.size),
                style = MaterialTheme.typography.labelMedium, color = palette.accent,
                fontWeight = FontWeight.Bold)
        }
        when {
            !loaded -> MentorSkeleton(stringResource(R.string.course_loading))
            tasks == null -> Text(stringResource(R.string.home_tasks_unavailable),
                style = MaterialTheme.typography.bodySmall, color = palette.secondary)
            tasks.isEmpty() -> Text(stringResource(R.string.home_tasks_empty),
                style = MaterialTheme.typography.bodySmall, color = palette.secondary)
            else -> tasks.sortedWith(compareBy<TodoTask> { it.isCompleted }
                .thenByDescending { it.priority }).take(3).forEach { task ->
                Surface(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = openSchedule),
                    shape = RoundedCornerShape(14.dp), color = palette.iconSurface,
                    border = BorderStroke(1.dp, palette.border)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (task.isCompleted) "✓" else "○",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (task.isCompleted) Color(0xFF0B9C86) else palette.accent)
                        Column(Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold, color = palette.title,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            task.linkedSubject?.takeIf { it.isNotBlank() }?.let { subject ->
                                Text(subject, style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondary)
                            }
                        }
                    }
                }
            }
        }
        TextButton(openSchedule) { Text(stringResource(R.string.home_tasks_open)) }
    }
}

@Composable
private fun OverviewMetric(modifier: Modifier, icon: Int, value: String, label: String,
    accent: Color, palette: PremiumDashboardPalette) {
    Row(modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(icon), null, Modifier.size(23.dp), tint = accent)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = palette.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.secondary)
        }
    }
}

@Composable
private fun OverviewTextMetric(modifier: Modifier, value: String, label: String,
    palette: PremiumDashboardPalette) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = palette.title, maxLines = 2)
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.secondary)
    }
}

@Composable
private fun HomeWeekChart(data: LearningOverviewData, palette: PremiumDashboardPalette) {
    PremiumDashboardCard(palette) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = palette.title)
            Text(stringResource(R.string.home_week_chart_legend), style = MaterialTheme.typography.labelSmall,
                color = palette.secondary)
        }
        Spacer(Modifier.height(8.dp))
        if (data.week.isEmpty() || data.week.none { it.xp > 0 || it.streakLit }) {
            Text(stringResource(R.string.design_week_empty), style = MaterialTheme.typography.bodyMedium,
                color = palette.secondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(7) {
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(palette.border))
                }
            }
        } else {
            val max = data.week.maxOf { it.xp }.coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().heightIn(min = 126.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                data.week.forEach { day ->
                    val label = stringResource(R.string.design_day_xp, day.day, day.xp) +
                        if (day.streakLit) stringResource(R.string.design_streak_day) else ""
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        if (day.streakLit) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flame),
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(14.dp).padding(bottom = 2.dp)
                            )
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }
                        Text(
                            text = if (day.xp > 0) "${day.xp}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.fillMaxWidth().height((day.xp.toFloat() / max * 72).coerceAtLeast(4f).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(palette.accent))
                        Spacer(Modifier.height(6.dp))
                        Text(day.day.takeLast(2), style = MaterialTheme.typography.labelSmall,
                            color = palette.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Clip)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStudyTimeChart(summary: StudyTimeSummary?, loaded: Boolean, palette: PremiumDashboardPalette) {
    PremiumDashboardCard(palette, Modifier.testTag("home-study-time")) {
        Text(stringResource(R.string.home_study_time_title), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = palette.title)
        if (!loaded) {
            MentorSkeleton(stringResource(R.string.course_loading))
            return@PremiumDashboardCard
        }
        if (summary == null) {
            Text(stringResource(R.string.home_study_time_unavailable),
                style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
            return@PremiumDashboardCard
        }
        val minutes = summary.totalSeconds / 60
        val total = when {
            minutes >= 60 -> stringResource(R.string.home_study_time_hours, minutes / 60, minutes % 60)
            summary.totalSeconds < 60 -> stringResource(R.string.home_study_time_seconds, summary.totalSeconds)
            else -> stringResource(R.string.home_study_time_minutes, minutes)
        }
        Text(total, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = palette.title)
        Text(stringResource(R.string.home_study_time_week), style = MaterialTheme.typography.bodySmall,
            color = palette.secondary)
        if (summary.week.all { it.seconds == 0L }) {
            Text(stringResource(R.string.home_study_time_empty), style = MaterialTheme.typography.bodyMedium,
                color = palette.secondary)
        } else {
            val maximum = summary.week.maxOf { it.seconds }.coerceAtLeast(1L)
            Row(Modifier.fillMaxWidth().heightIn(min = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                summary.week.forEach { day ->
                    val label = stringResource(R.string.home_study_time_day, day.date, day.seconds / 60)
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.fillMaxWidth().height((day.seconds.toFloat() / maximum * 72)
                            .coerceAtLeast(4f).dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(palette.accent))
                        Spacer(Modifier.height(6.dp))
                        Text(day.date.takeLast(2), style = MaterialTheme.typography.labelSmall,
                            color = palette.title, fontWeight = FontWeight.Bold)
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
                        val label = stringResource(R.string.design_day_xp, day.day, day.xp) +
                            if (day.streakLit) stringResource(R.string.design_streak_day) else ""
                        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = label },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            if (day.streakLit) Icon(painter = painterResource(R.drawable.ic_flame),
                                contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(14.dp))
                            Text(day.xp.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.fillMaxWidth().height((day.xp.toFloat() / max * 100).coerceAtLeast(3f).dp)
                                .clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary))
                            Text(day.day.takeLast(2), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectPieChart(subjects: List<LearningOverviewData.Subject>, palette: PremiumDashboardPalette) {
    val context = LocalContext.current
    val totalXp = subjects.sumOf { it.experience }
    val activeSubjects = subjects.filter { it.experience > 0 }

    PremiumDashboardCard(palette) {
        Text(
            text = stringResource(R.string.home_subject_chart_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.title
        )

        if (totalXp <= 0 || activeSubjects.isEmpty()) {
            Text(
                text = stringResource(R.string.home_subject_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondary
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 32f
                        var startAngle = -90f

                        activeSubjects.forEach { sub ->
                            val sweepAngle = (sub.experience.toFloat() / totalXp.toFloat()) * 360f
                            val color = subjectColor(sub.id)
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle.coerceAtLeast(2f),
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalXp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.title
                        )
                        Text(
                            text = stringResource(R.string.home_subject_chart_total),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary
                        )
                    }
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeSubjects.forEach { sub ->
                        val percent = if (totalXp > 0) (sub.experience * 100) / totalXp else 0
                        val label = try {
                            SubjectId.fromWireId(sub.id).label(LocaleHelper.getLanguage(context))
                        } catch (e: Exception) {
                            sub.id.replaceFirstChar { it.uppercase() }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(subjectColor(sub.id))
                            )
                            Text(
                                text = "$label: ${sub.experience} XP ($percent%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun subjectColor(wireId: String): Color = when (wireId) {
    "math" -> Color(0xFF3B82F6)
    "physics" -> Color(0xFF8B5CF6)
    "chemistry" -> Color(0xFF06B6D4)
    "biology" -> Color(0xFF10B981)
    "literature" -> Color(0xFFF59E0B)
    "english" -> Color(0xFF6366F1)
    "history" -> Color(0xFFEF4444)
    "geography" -> Color(0xFF14B8A6)
    "economics_law", "economics-law" -> Color(0xFFEC4899)
    else -> Color(0xFF64748B)
}
