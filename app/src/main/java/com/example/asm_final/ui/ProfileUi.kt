package com.example.asm_final.ui

import com.example.mentor.ui.design.mentorHeader

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.json.JSONObject
import com.example.asm_final.R
import com.example.asm_final.competition.PublicLearner
import com.example.asm_final.dashboard.LearningOverviewData
import com.example.asm_final.dashboard.LearningOverviewRepository
import com.example.asm_final.competition.CompetitionViewModel
import androidx.compose.runtime.livedata.observeAsState
import java.util.function.Consumer
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.MentorButton
import com.example.mentor.ui.design.MentorCard
import com.example.mentor.ui.design.MentorSkeleton
import com.example.mentor.ui.design.MentorTokens
import com.example.mentor.ui.design.motionEnabled

private data class ProfileBadge(val title: String, val description: String, val unlocked: Boolean)
private enum class FriendsPage { PROFILE, LIST, SEARCH }

/**
 * Level 4 — Premium EdTech Profile Presentation.
 * Designed after modern Duolingo profile architecture:
 * Layered hero, large centered avatar, user identity, quick statistics,
 * primary/secondary action buttons, 2x2 overview grid, dashed friends streak slots,
 * and monthly challenge badges.
 */
class ProfileUi(
    private val context: Context,
    private val refresh: Runnable,
    private val edit: Runnable,
    private val settings: Runnable,
    private val learn: Runnable,
    private val friendsModel: CompetitionViewModel,
    private val openFriendProfile: Consumer<String>,
) {
    private var displayName by mutableStateOf("")
    private var avatar by mutableStateOf<String?>(null)
    private var bio by mutableStateOf("")
    private var grade by mutableIntStateOf(0)
    private var goal by mutableIntStateOf(-1)
    private var subjects by mutableStateOf<Set<SubjectId>>(emptySet())
    private var snapshot by mutableStateOf<LearningOverviewRepository.Snapshot?>(null)
    private var studySeconds by mutableStateOf<Long?>(null)
    private var badges by mutableStateOf<List<ProfileBadge>>(emptyList())
    private var badgesLoaded by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    private var failed by mutableStateOf(false)

    fun view(): View = composeHost(context, scenicBackground = true) {
        ProfileScreen(
            displayName, avatar, bio, grade, goal, subjects, snapshot, loading, failed,
            studySeconds, badges, badgesLoaded, friendsModel,
            { refresh.run() }, { edit.run() }, { settings.run() }, { learn.run() },
            { openFriendProfile.accept(it) }
        )
    }

    fun identity(name: String, image: String?, note: String) {
        displayName = name
        avatar = image
        bio = note.take(500)
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

    fun activity(summary: JSONObject?, badgeResponse: JSONObject?) {
        studySeconds = summary?.optLong("totalSeconds")?.takeIf { it >= 0L }
        val list = mutableListOf<ProfileBadge>()
        val rows = badgeResponse?.optJSONArray("badges")
        if (rows != null) {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index) ?: continue
                list += ProfileBadge(
                    item.optString("title").takeIf(String::isNotBlank) ?: continue,
                    item.optString("description"),
                    item.optBoolean("unlocked")
                )
            }
        }
        badges = list
        badgesLoaded = badgeResponse != null
    }

    fun failure() {
        loading = false
        failed = true
    }
}

@Composable
private fun ProfileScreen(
    name: String,
    avatar: String?,
    bio: String,
    grade: Int,
    goal: Int,
    subjects: Set<SubjectId>,
    snapshot: LearningOverviewRepository.Snapshot?,
    loading: Boolean,
    failed: Boolean,
    studySeconds: Long?,
    badges: List<ProfileBadge>,
    badgesLoaded: Boolean,
    friendsModel: CompetitionViewModel,
    refresh: () -> Unit,
    edit: () -> Unit,
    settings: () -> Unit,
    learn: () -> Unit,
    openFriendProfile: (String) -> Unit,
) {
    val data = snapshot?.data
    val motion = motionEnabled()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val friendsState by friendsModel.friendsState().observeAsState()
    val searchState by friendsModel.searchState().observeAsState()
    val friendsPalette = premiumDashboardPalette(
        com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current))
    var friendsPage by rememberSaveable { mutableStateOf(FriendsPage.PROFILE) }
    BackHandler(enabled = friendsPage != FriendsPage.PROFILE) {
        friendsPage = if (friendsPage == FriendsPage.SEARCH) FriendsPage.LIST else FriendsPage.PROFILE
    }

    if (friendsPage != FriendsPage.PROFILE) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = MentorTokens.PagePadding, vertical = MentorTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceLarge)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    friendsPage = if (friendsPage == FriendsPage.SEARCH) FriendsPage.LIST else FriendsPage.PROFILE
                }) { Text(stringResource(R.string.friends_back)) }
                Spacer(Modifier.weight(1f))
                if (friendsPage == FriendsPage.LIST) {
                    Button(onClick = { friendsPage = FriendsPage.SEARCH }) {
                        Text(stringResource(R.string.friends_open_search))
                    }
                }
            }
            Text(stringResource(if (friendsPage == FriendsPage.SEARCH) R.string.friends_search_title
                else R.string.friends_manage_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = friendsPalette.title)
            FriendsPanel(friendsState, searchState, friendsPalette, friendsModel,
                openFriendProfile, searchMode = friendsPage == FriendsPage.SEARCH)
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("profile-screen"),
        ) {
        // The existing profile content stays interactive above the decorative canvas.
        AnimatedVisibility(
            visible = entered || !motion,
            enter = fadeIn() + slideInVertically { -it / 12 }
        ) {
            ProfileHero(name, avatar, bio, edit, settings, loading)
        }

        // Main Profile Body
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MentorTokens.PagePadding)
                .padding(bottom = MentorTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceLarge),
        ) {
            if (loading) {
                repeat(3) { MentorSkeleton(stringResource(R.string.course_loading)) }
            } else {
                // Keep account identifiers out of the public profile UI. Show only the chosen name.
                ProfileIdentitySection(
                    grade = grade,
                    subjectsCount = subjects.size,
                    edit = edit
                )
                // Section TỔNG QUAN
                ProfileOverviewGrid(data, learn)

                ProfileStudySummary(studySeconds)

                // Error / Offline / Cache notice if applicable
                if (failed || data == null || snapshot?.mode != LearningOverviewRepository.Mode.LIVE) {
                    ProfileNotice(snapshot, failed, refresh, learn)
                }

                PremiumDashboardCard(friendsPalette) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.friends_manage_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = friendsPalette.title)
                            Text(stringResource(R.string.friends_manage_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = friendsPalette.secondary)
                        }
                        TextButton(onClick = {
                            friendsModel.refreshFriends()
                            friendsPage = FriendsPage.LIST
                        }) { Text(stringResource(R.string.friends_open_list)) }
                    }
                }

                // Section HUY HIỆU THỬ THÁCH THÁNG
                ProfileBadgeCollection(badges, badgesLoaded)

                // Section HƯỚNG HỌC & TIẾN ĐỘ TUẦN
                ProfileFocus(grade, goal, subjects, learn)
                ProfileWeek(data)
            }
        }
    }
    }
}

/**
 * Top Hero Section:
 * Features top bar with Display Name on the left, Settings icon on the right,
 * and a large centered prominent avatar.
 */
@Composable
private fun ProfileHero(
    name: String,
    avatar: String?,
    bio: String,
    edit: () -> Unit,
    settings: () -> Unit,
    loading: Boolean,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .mentorHeader(com.example.mentor.ui.design.MentorArtwork.IDEAS)
            .testTag("profile-hero")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MentorTokens.PagePadding)
                .padding(top = 18.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Display Name / Eyebrow on left, Settings on right
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loading) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(26.dp)
                            .padding(end = 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .testTag("profile-name-loading")
                    )
                } else {
                    Text(
                        text = name.ifBlank { stringResource(R.string.profile_eyebrow) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).testTag("profile-display-name")
                    )
                }
                IconButton(
                    onClick = settings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("profile-settings")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!loading) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.profile_note_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        bio.ifBlank { stringResource(R.string.profile_note_empty) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = edit, modifier = Modifier.testTag("profile-add-note")) {
                        Text(stringResource(if (bio.isBlank()) R.string.profile_add_note else R.string.profile_edit_note))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Prominent Centered Avatar (96dp)
            ProfileAvatar(name, avatar, 88.dp, loading)
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, avatar: String?, size: androidx.compose.ui.unit.Dp, loading: Boolean) {
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)
    val borderBrush = if (dark) Brush.linearGradient(listOf(Color(0xFFFFD166), Color(0xFF8B5CF6)))
        else Brush.linearGradient(listOf(Color.White, Color.White))

    Box(
        Modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .border(3.5.dp, borderBrush, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!loading) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (!loading && !avatar.isNullOrBlank()) {
            AsyncImage(
                model = avatar,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * User Identity & Quick Stats:
 * - Subtitle: selected grade, when available; internal account names are not shown
 * - Quick stats: Môn học, Đang theo dõi, Người theo dõi
 * - Action button for editing the learner profile
 */
@Composable
private fun ProfileIdentitySection(
    grade: Int,
    subjectsCount: Int,
    edit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (grade in 10..12) {
            Text(
                text = stringResource(R.string.profile_grade_label, grade),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Show only verified profile data; social counts are not available from the current API.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stat 1: Subjects
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_course),
                    contentDescription = null,
                    tint = MentorTokens.SuccessLightText,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.profile_quick_subjects, subjectsCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

        }

        Button(
            onClick = edit,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("profile-edit")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_profile),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.profile_edit),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ProfileOverviewGrid(data: LearningOverviewData?, onCompletedLessonsClick: () -> Unit) {
    val streakDays = data?.streakDays
    val totalXp = data?.totalXp

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(stringResource(R.string.profile_overview_title))

        val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)
        val borderAlpha = if (motionEnabled() && dark) {
            val transition = rememberInfiniteTransition(label = "profile glass border")
            val animated by transition.animateFloat(0.45f, 0.8f,
                infiniteRepeatable(tween(2800), RepeatMode.Reverse), label = "border glow")
            animated
        } else 0.55f
        val glassBorder = if (dark) Brush.linearGradient(listOf(
            Color(0xFFFFD166).copy(alpha = borderAlpha),
            Color(0xFF8B5CF6).copy(alpha = 0.4f),
            Color(0xFFFFD166).copy(alpha = borderAlpha),
        )) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (dark) Color(0xFF1E293B).copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, glassBorder),
            shadowElevation = 6.dp
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Streak + Total EXP
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cell 1: Streak
                    Row(
                        Modifier
                            .weight(1f)
                            .testTag("profile-streak"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flame),
                            contentDescription = null,
                            tint = MentorTokens.RewardStreakLight,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (streakDays == null) "—" else stringResource(R.string.profile_streak_stat, streakDays),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.profile_streak_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Cell 2: Total EXP
                    Row(
                        Modifier
                            .weight(1f)
                            .testTag("profile-total-xp"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lightning),
                            contentDescription = null,
                            tint = MentorTokens.RewardXpBar,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (totalXp == null) "—" else stringResource(R.string.profile_xp_stat, totalXp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.profile_xp_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Row 2: Completed lessons (Clickable Roadmap Navigation Card)
                val lessonsText = if (data == null) "—"
                    else if (data.totalLessons > 0) stringResource(R.string.profile_lessons_ratio, data.completedLessons, data.totalLessons)
                    else stringResource(R.string.profile_lessons_unit, data.completedLessons)

                Surface(
                    onClick = onCompletedLessonsClick,
                    modifier = Modifier.fillMaxWidth().testTag("profile-lessons"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (dark) Color(0xFF334155).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_book_course),
                                contentDescription = null,
                                tint = MentorTokens.Orange500,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = lessonsText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.profile_lessons_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_view_roadmap),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Renders only badge facts returned by the account-scoped backend. */
@Composable
private fun ProfileBadgeCollection(badges: List<ProfileBadge>, loaded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(stringResource(R.string.profile_challenge_title))

        Surface(
            Modifier
                .fillMaxWidth()
                .testTag(if (badges.isEmpty()) "profile-badges-empty" else "profile-badges-loaded"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    !loaded -> Text(
                        stringResource(R.string.profile_badge_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    badges.isEmpty() -> Text(
                        stringResource(R.string.profile_badge_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> badges.forEach { badge ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(42.dp).clip(CircleShape)
                                    .background(if (badge.unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_badge_challenge),
                                    contentDescription = null,
                                    tint = if (badge.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(badge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                stringResource(if (badge.unlocked) R.string.profile_badge_unlocked else R.string.profile_badge_locked),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (badge.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStudySummary(totalSeconds: Long?) {
    MentorCard(Modifier.fillMaxWidth().testTag("profile-study-time")) {
        Text(stringResource(R.string.profile_study_time_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            totalSeconds?.let { seconds ->
                val minutes = seconds / 60
                when {
                    minutes >= 60 -> stringResource(R.string.profile_study_time_hours, minutes / 60, minutes % 60)
                    seconds < 60 -> stringResource(R.string.profile_study_time_seconds, seconds.toInt())
                    else -> stringResource(R.string.profile_study_time_minutes, minutes.toInt())
                }
            } ?: stringResource(R.string.profile_study_time_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Section HƯỚNG HỌC (Grade, Target, Enrolled subjects)
 */
@Composable
private fun ProfileFocus(grade: Int, goal: Int, subjects: Set<SubjectId>, learn: () -> Unit) {
    MentorCard(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.profile_focus_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        val goalId = when (goal) {
            0 -> R.string.profile_goal_0
            1 -> R.string.profile_goal_1
            2 -> R.string.profile_goal_2
            3 -> R.string.profile_goal_3
            else -> null
        }
        if (goalId != null) {
            Text(
                stringResource(goalId),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        val language = LocaleHelper.getLanguage(LocalContext.current)
        if (subjects.isEmpty()) {
            Text(
                stringResource(R.string.profile_subjects_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                subjects.joinToString("  ·  ") { it.label(language) },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (grade !in 10..12 || subjects.isEmpty()) {
            MentorButton(stringResource(R.string.profile_choose_subjects), learn)
        }
    }
}

@Composable
private fun ProfileNotice(
    snapshot: LearningOverviewRepository.Snapshot?,
    failed: Boolean,
    refresh: () -> Unit,
    learn: () -> Unit,
) {
    val message = when {
        snapshot?.needsLogin == true -> R.string.overview_expired
        failed -> R.string.overview_error
        snapshot?.mode == LearningOverviewRepository.Mode.CACHED -> R.string.overview_cached
        snapshot?.mode == LearningOverviewRepository.Mode.LOCAL -> R.string.profile_progress_offline
        snapshot?.mode == LearningOverviewRepository.Mode.NO_GRADE -> R.string.profile_grade_missing
        else -> R.string.overview_error
    }
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            val local = snapshot?.mode == LearningOverviewRepository.Mode.LOCAL
            TextButton(if (local) learn else refresh) {
                Text(
                    stringResource(
                        when {
                            snapshot?.needsLogin == true -> R.string.login_btn
                            local -> R.string.course_open
                            else -> R.string.overview_reload
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun ProfileWeek(data: LearningOverviewData?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(stringResource(R.string.design_week))
        if (data == null || data.week.isEmpty() || data.week.none { it.xp > 0 || it.streakLit }) {
            MentorCard(Modifier.fillMaxWidth().testTag("profile-week-empty")) {
                Text(
                    stringResource(R.string.design_week_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            WeekChart(data)
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun PublicProfileContent(
    learner: PublicLearner?,
    avatar: android.graphics.Bitmap?,
    loading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit
) {
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)
    val heroBg = if (dark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(heroBg)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(80.dp)
                        .shadow(4.dp, CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar != null) {
                        Image(
                            bitmap = avatar.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = learner?.nickname?.trim()?.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = learner?.nickname ?: stringResource(R.string.ranking_loading),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (learner != null) {
                    Text(
                        text = "@LEARNER_${learner.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(MentorTokens.PagePadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (loading && learner == null) {
                Box(
                    Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (failed && learner == null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MentorErrorNotice(stringResource(R.string.ranking_profile_error))
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.ranking_retry))
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.public_profile_about),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = learner?.bio?.takeIf { it.isNotBlank() } ?: stringResource(R.string.public_profile_bio_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val streakVal = stringResource(R.string.public_profile_streak_value, learner?.streakDays ?: 0)
                val streakLbl = stringResource(R.string.public_profile_streak_label)
                val xpVal = stringResource(R.string.public_profile_xp_value, learner?.dailyXp ?: 0)
                val xpLbl = stringResource(R.string.public_profile_xp_label)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "$streakLbl: $streakVal"
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flame),
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = streakVal,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = streakLbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "$xpLbl: $xpVal"
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lightning),
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = xpVal,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = xpLbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.public_profile_privacy_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
