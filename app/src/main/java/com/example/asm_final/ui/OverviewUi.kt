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
import androidx.compose.ui.graphics.Color
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

    fun view(): View = composeHost(context, scenicBackground = true) {
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
    val context = LocalContext.current
    val data = snapshot?.data
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(context)
    val palette = premiumDashboardPalette(dark)

    Column(Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        PremiumDashboardHeader {
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
                if (profile) {
                    IconButton(settings) {
                        Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title), tint = palette.secondary)
                    }
                }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!profile) {
                    val scheduleLabel = stringResource(R.string.home_schedule_title)
                    PremiumDashboardCard(palette, modifier = Modifier.clickable(
                        onClickLabel = scheduleLabel, role = Role.Button, onClick = schedule)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(palette.iconSurface), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_book_course), null, Modifier.size(26.dp), tint = palette.accent)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(scheduleLabel, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold, color = palette.title)
                                Text(stringResource(R.string.home_schedule_subtitle),
                                    style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                            }
                            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = palette.muted)
                        }
                    }
                }

            if (loading) {
                repeat(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
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
                    
                    PremiumDashboardCard(palette, highlighted = true) {
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
                    HomeWeekChart(data, palette)
                    SubjectPieChart(data.subjects, palette)
                } else if (!failed) PremiumDashboardCard(palette) {
                    Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = palette.title)
                    MentorIllustration(MentorArtwork.GOALS)
                    Text(stringResource(R.string.design_week_empty), style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondary)
                }
                if (!profile) {
                    PremiumDashboardCard(palette) {
                        Text(stringResource(R.string.design_goal), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = palette.title)
                        Text(stringResource(R.string.home_suggestion_subtitle), style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondary)
                        PremiumDashboardAction(stringResource(R.string.course_continue), palette, learn)
                    }
                    Text(stringResource(R.string.design_subjects), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = palette.title)
                    selected.forEach { subject ->
                        val progress = data?.subjects?.firstOrNull { it.id == subject.wireId }
                        PremiumDashboardCard(palette, Modifier.testTag("dashboard-subject:${subject.wireId}")) {
                                Text(subject.label(LocaleHelper.getLanguage(context)),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                    color = palette.title)
                                if (progress != null) {
                                    Text(stringResource(R.string.overview_subject_progress, progress.completed, progress.total, progress.experience), style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                                    MentorProgress(if (progress.total == 0) 0f else progress.completed.toFloat() / progress.total)
                                } else Text(stringResource(R.string.dashboard_progress_pending),
                                    style = MaterialTheme.typography.bodySmall, color = palette.secondary)
                                PremiumDashboardAction(stringResource(R.string.course_open), palette, { open(subject) })
                        }
                    }
                    if (selected.isEmpty()) PremiumDashboardAction(stringResource(R.string.lesson_choose_prompt), palette, learn)
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
        Text(stringResource(R.string.design_week), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = palette.title)
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
            Row(Modifier.fillMaxWidth().heightIn(min = 116.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                data.week.forEach { day ->
                    val label = stringResource(R.string.design_day_xp, day.day, day.xp)
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.fillMaxWidth().height((day.xp.toFloat() / max * 84).coerceAtLeast(4f).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (day.streakLit) palette.streak else palette.accent))
                        Spacer(Modifier.height(6.dp))
                        Text(day.day.takeLast(2), style = MaterialTheme.typography.labelSmall,
                            color = palette.muted, maxLines = 1, overflow = TextOverflow.Clip)
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

@Composable
private fun SubjectPieChart(subjects: List<LearningOverviewData.Subject>, palette: PremiumDashboardPalette) {
    val context = LocalContext.current
    val totalXp = subjects.sumOf { it.experience }
    val activeSubjects = subjects.filter { it.experience > 0 }

    PremiumDashboardCard(palette) {
        Text(
            text = "Phân tích EXP theo môn học",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.title
        )

        if (totalXp <= 0 || activeSubjects.isEmpty()) {
            Text(
                text = "Chưa có dữ liệu kinh nghiệm (EXP) để phân tích. Hãy hoàn thành các bài học để xem biểu đồ!",
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
                            text = "Tổng EXP",
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
