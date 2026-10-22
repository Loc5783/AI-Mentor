package com.example.asm_final.ui

import android.content.Context
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.example.asm_final.R
import com.example.asm_final.dashboard.LearningOverviewData
import com.example.asm_final.dashboard.LearningOverviewRepository
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.MentorButton
import com.example.mentor.ui.design.MentorCard
import com.example.mentor.ui.design.MentorSkeleton
import com.example.mentor.ui.design.MentorTokens
import com.example.mentor.ui.design.motionEnabled

/** Profile-only presentation state; account reads and navigation remain with the Fragment. */
class ProfileUi(
    private val context: Context,
    private val refresh: Runnable,
    private val edit: Runnable,
    private val settings: Runnable,
    private val learn: Runnable,
) {
    private var displayName by mutableStateOf("")
    private var username by mutableStateOf("")
    private var avatar by mutableStateOf<String?>(null)
    private var grade by mutableIntStateOf(0)
    private var goal by mutableIntStateOf(-1)
    private var subjects by mutableStateOf<Set<SubjectId>>(emptySet())
    private var snapshot by mutableStateOf<LearningOverviewRepository.Snapshot?>(null)
    private var loading by mutableStateOf(true)
    private var failed by mutableStateOf(false)

    fun view(): View = composeHost(context) {
        ProfileScreen(displayName, username, avatar, grade, goal, subjects, snapshot, loading, failed,
            { refresh.run() }, { edit.run() }, { settings.run() }, { learn.run() })
    }

    fun identity(name: String, handle: String, image: String?) {
        displayName = name
        username = handle
        avatar = image
    }

    fun learning(year: Int, objective: Int, selected: Set<SubjectId>) {
        grade = year
        goal = objective
        subjects = selected.toSet()
    }

    fun render(value: LearningOverviewRepository.Snapshot) {
        snapshot = value
        loading = false
        failed = false
    }

    fun failure() {
        loading = false
        failed = true
    }
}

@Composable
private fun ProfileScreen(
    name: String, username: String, avatar: String?, grade: Int, goal: Int,
    subjects: Set<SubjectId>, snapshot: LearningOverviewRepository.Snapshot?,
    loading: Boolean, failed: Boolean,
    refresh: () -> Unit, edit: () -> Unit, settings: () -> Unit, learn: () -> Unit,
) {
    val data = snapshot?.data
    val motion = motionEnabled()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).testTag("profile-screen"),
    ) {
        AnimatedVisibility(visible = entered || !motion,
            enter = fadeIn() + slideInVertically { -it / 12 }) {
            ProfileHero(name, username, avatar, grade, settings, edit)
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = MentorTokens.PagePadding)
                .padding(bottom = MentorTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceLarge),
        ) {
            ProfileFocus(grade, goal, subjects, learn)
            if (loading) {
                repeat(2) { MentorSkeleton(stringResource(R.string.course_loading)) }
            } else {
                ProfileStats(data, snapshot?.mode)
                if (failed || data == null || snapshot?.mode != LearningOverviewRepository.Mode.LIVE) {
                    ProfileNotice(snapshot, failed, refresh, learn)
                }
                ProfileWeek(data)
            }
            ProfileEmptySection(
                stringResource(R.string.profile_badges_title),
                stringResource(R.string.profile_badges_empty),
                "profile-badges-empty",
            )
            ProfileEmptySection(
                stringResource(R.string.profile_friends_title),
                stringResource(R.string.profile_friends_empty),
                "profile-friends-empty",
            )
        }
    }
}

@Composable
private fun ProfileHero(
    name: String, username: String, avatar: String?, grade: Int,
    settings: () -> Unit, edit: () -> Unit,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val compact = LocalConfiguration.current.screenWidthDp <= 360
    val first = if (dark) MentorTokens.PrimaryDark else MentorTokens.Primary
    val second = if (dark) MentorTokens.DarkHeroEnd else MentorTokens.HeroEnd
    Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(first, second)))
        .testTag("profile-hero")) {
        // Abstract circles add depth without external artwork or distracting perpetual motion.
        Box(Modifier.align(Alignment.TopStart).offset(x = (-46).dp, y = (-68).dp).size(190.dp)
            .background(Color.White.copy(alpha = .08f), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 44.dp, y = 76.dp).size(180.dp)
            .background(MentorTokens.Peach.copy(alpha = .13f), CircleShape))
        Column(Modifier.fillMaxWidth().padding(horizontal = MentorTokens.PagePadding)
            .padding(top = 16.dp, bottom = if (compact) 20.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_eyebrow), Modifier.weight(1f), color = Color.White,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                IconButton(settings, modifier = Modifier.size(48.dp).testTag("profile-settings")) {
                    Icon(painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                        tint = Color.White)
                }
            }
            if (compact) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileAvatar(name, avatar, 72.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(name.ifBlank { stringResource(R.string.home_default_user) },
                            color = Color.White, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (username.isNotBlank()) Text("@$username", color = Color.White.copy(alpha = .9f),
                            style = MaterialTheme.typography.bodySmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(if (grade in 10..12) stringResource(R.string.profile_premium_grade, grade)
                            else stringResource(R.string.profile_grade_missing), color = Color.White,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                ProfileEditButton(edit, Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(6.dp))
                ProfileAvatar(name, avatar, 88.dp)
                Spacer(Modifier.height(12.dp))
                Text(name.ifBlank { stringResource(R.string.home_default_user) }, color = Color.White,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                if (username.isNotBlank()) Text("@$username", color = Color.White.copy(alpha = .9f),
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Text(if (grade in 10..12) stringResource(R.string.profile_premium_grade, grade)
                    else stringResource(R.string.profile_grade_missing), color = Color.White,
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                ProfileEditButton(edit)
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, avatar: String?, size: Dp) {
    Box(Modifier.size(size).shadow(8.dp, CircleShape)
        .border(3.dp, Color.White, CircleShape).clip(CircleShape)
        .background(MentorTokens.PrimaryContainer), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "A", color = MentorTokens.PrimaryDark,
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        if (!avatar.isNullOrBlank()) AsyncImage(avatar, null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop)
    }
}

@Composable
private fun ProfileEditButton(edit: () -> Unit, modifier: Modifier = Modifier) {
    Button(edit, colors = ButtonDefaults.buttonColors(
        containerColor = Color.White, contentColor = MentorTokens.PrimaryDark),
        modifier = modifier.heightIn(min = 48.dp).testTag("profile-edit")) {
        Text(stringResource(R.string.profile_edit))
    }
}

@Composable
private fun ProfileFocus(grade: Int, goal: Int, subjects: Set<SubjectId>, learn: () -> Unit) {
    MentorCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(stringResource(R.string.profile_focus_title), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        val goalId = when (goal) {
            0 -> R.string.profile_goal_0
            1 -> R.string.profile_goal_1
            2 -> R.string.profile_goal_2
            3 -> R.string.profile_goal_3
            else -> null
        }
        if (goalId != null) Text(stringResource(goalId), color = MaterialTheme.colorScheme.onSurfaceVariant)
        val language = LocaleHelper.getLanguage(LocalContext.current)
        if (subjects.isEmpty()) {
            Text(stringResource(R.string.profile_subjects_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(subjects.joinToString("  ·  ") { it.label(language) },
                color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        }
        if (grade !in 10..12 || subjects.isEmpty()) {
            MentorButton(stringResource(R.string.profile_choose_subjects), learn)
        }
    }
}

@Composable
private fun ProfileNotice(snapshot: LearningOverviewRepository.Snapshot?, failed: Boolean,
                          refresh: () -> Unit, learn: () -> Unit) {
    val message = when {
        snapshot?.needsLogin == true -> R.string.overview_expired
        failed -> R.string.overview_error
        snapshot?.mode == LearningOverviewRepository.Mode.CACHED -> R.string.overview_cached
        snapshot?.mode == LearningOverviewRepository.Mode.LOCAL -> R.string.profile_progress_offline
        snapshot?.mode == LearningOverviewRepository.Mode.NO_GRADE -> R.string.profile_grade_missing
        else -> R.string.overview_error
    }
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
            val local = snapshot?.mode == LearningOverviewRepository.Mode.LOCAL
            TextButton(if (local) learn else refresh) {
                Text(stringResource(when {
                    snapshot?.needsLogin == true -> R.string.login_btn
                    local -> R.string.course_open
                    else -> R.string.overview_reload
                }))
            }
        }
    }
}

@Composable
private fun ProfileStats(data: LearningOverviewData?, mode: LearningOverviewRepository.Mode?) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(stringResource(R.string.profile_stats_title))
        if (data != null) Text(stringResource(if (mode == LearningOverviewRepository.Mode.CACHED)
            R.string.profile_saved_data else R.string.overview_current),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Text(stringResource(R.string.profile_stats_pending),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium, shadowElevation = 2.dp) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfileStat(stringResource(R.string.profile_streak_label), data?.streakDays?.toString() ?: "—",
                        stringResource(R.string.profile_days_unit), if (dark) MentorTokens.WarmAccent else MentorTokens.Warning,
                        "profile-streak", Modifier.weight(1f))
                    ProfileStatDivider()
                    ProfileStat(stringResource(R.string.profile_xp_label), data?.totalXp?.toString() ?: "—",
                        "XP", MaterialTheme.colorScheme.primary, "profile-total-xp", Modifier.weight(1f))
                }
                HorizontalDivider(Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = .2f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfileStat(stringResource(R.string.profile_lessons_label),
                        data?.completedLessons?.toString() ?: "—", stringResource(R.string.profile_lessons_unit),
                        if (dark) MentorTokens.DarkSuccess else MentorTokens.Success,
                        "profile-lessons", Modifier.weight(1f))
                    ProfileStatDivider()
                    ProfileStat(stringResource(R.string.profile_today_label), data?.dailyXp?.toString() ?: "—",
                        "XP", MaterialTheme.colorScheme.primary, "profile-daily-xp", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, unit: String, tint: Color,
                        tag: String, modifier: Modifier = Modifier) {
    Column(modifier.testTag(tag).heightIn(min = 88.dp).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value != "—") Text(unit, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(Modifier.width(1.dp).height(64.dp)
        .background(MaterialTheme.colorScheme.outline.copy(alpha = .2f)))
}

@Composable
private fun ProfileWeek(data: LearningOverviewData?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(stringResource(R.string.design_week))
        if (data == null || data.week.isEmpty() || data.week.none { it.xp > 0 || it.streakLit }) {
            MentorCard(Modifier.fillMaxWidth().testTag("profile-week-empty")) {
                Text(stringResource(R.string.design_week_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            WeekChart(data)
        }
    }
}

@Composable
private fun ProfileEmptySection(title: String, detail: String, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(title)
        Surface(Modifier.fillMaxWidth().testTag(tag), color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .3f))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(44.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .5f),
                    CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .3f), CircleShape))
                }
                Text(detail, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
}
