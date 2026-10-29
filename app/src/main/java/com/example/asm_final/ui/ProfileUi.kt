package com.example.asm_final.ui

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
        ProfileScreen(
            displayName, username, avatar, grade, goal, subjects, snapshot, loading, failed,
            { refresh.run() }, { edit.run() }, { settings.run() }, { learn.run() }
        )
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
    name: String,
    username: String,
    avatar: String?,
    grade: Int,
    goal: Int,
    subjects: Set<SubjectId>,
    snapshot: LearningOverviewRepository.Snapshot?,
    loading: Boolean,
    failed: Boolean,
    refresh: () -> Unit,
    edit: () -> Unit,
    settings: () -> Unit,
    learn: () -> Unit,
) {
    val data = snapshot?.data
    val motion = motionEnabled()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

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
            ProfileHero(name, avatar, settings)
        }

        // Main Profile Body
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MentorTokens.PagePadding)
                .padding(bottom = MentorTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(MentorTokens.SpaceLarge),
        ) {
            // Identity and Action Buttons
            ProfileIdentitySection(
                name = name,
                username = username,
                grade = grade,
                subjectsCount = subjects.size,
                edit = edit
            )

            if (loading) {
                repeat(2) { MentorSkeleton(stringResource(R.string.course_loading)) }
            } else {
                // Section TỔNG QUAN (2x2 Grid)
                ProfileOverviewGrid(data)

                // Error / Offline / Cache notice if applicable
                if (failed || data == null || snapshot?.mode != LearningOverviewRepository.Mode.LIVE) {
                    ProfileNotice(snapshot, failed, refresh, learn)
                }

                // Section STREAK BẠN BÈ (Dashed circular slots)
                ProfileFriendsStreak()

                // Section HUY HIỆU THỬ THÁCH THÁNG
                ProfileMonthlyChallenge(data)

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
    settings: () -> Unit,
) {
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)
    val heroBg = if (dark) Brush.linearGradient(
        listOf(Color(0xFF21365D), Color(0xFF312D59), Color(0xFF17253F))
    ) else Brush.linearGradient(listOf(MentorTokens.PrimaryContainer, MentorTokens.PrimaryContainer))

    Box(
        Modifier
            .fillMaxWidth()
            .background(heroBg)
            .testTag("profile-hero")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MentorTokens.PagePadding)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Display Name / Eyebrow on left, Settings on right
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name.ifBlank { stringResource(R.string.profile_eyebrow) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
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

            Spacer(Modifier.height(14.dp))

            // Prominent Centered Avatar (96dp)
            ProfileAvatar(name, avatar, 96.dp)
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, avatar: String?, size: androidx.compose.ui.unit.Dp) {
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
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "A",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        if (!avatar.isNullOrBlank()) {
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
 * - Subtitle: @username and selected grade, when available
 * - Quick stats: Môn học, Đang theo dõi, Người theo dõi
 * - Action buttons: Primary [CHỈNH SỬA HỒ SƠ], Secondary [QR]
 */
@Composable
private fun ProfileIdentitySection(
    name: String,
    username: String,
    grade: Int,
    subjectsCount: Int,
    edit: () -> Unit,
) {
    val context = LocalContext.current
    val handleText = if (username.isNotBlank()) "@${username.uppercase()}" else "@STUDENT"
    val gradeText = if (grade in 10..12) " · LỚP $grade" else ""

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Show only identity fields that are actually available.
        Text(
            text = "$handleText$gradeText",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Quick Stats Row: Môn học | Đang theo dõi | Người theo dõi
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
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

            Box(
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Stat 2: Following
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.profile_following_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Stat 3: Followers
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.profile_followers_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons Row: Big Primary Button + Square QR OutlinedButton
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = edit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
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

            var showQrDialog by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showQrDialog = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("profile-qr")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_qr_code),
                    contentDescription = stringResource(R.string.profile_qr_code),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (showQrDialog) {
                ProfileQrDialog(onDismiss = { showQrDialog = false })
            }
        }
    }
}

@Composable
private fun ProfileQrDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var token by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    fun loadQrToken() {
        loading = true
        error = false
        val owner = com.example.mentor.data.ActiveUser.get(context)
        Thread {
            try {
                val ep = com.example.asm_final.backend.BackendClient.endpoint(context)
                if (ep.isBlank()) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        error = true
                        loading = false
                    }
                    return@Thread
                }
                val transport = com.example.asm_final.backend.BackendClient(context, owner)
                val result = transport.request("GET", "/me/qr-code", null, null, null)
                val qrToken = result.optString("qrCodeToken", "")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (qrToken.isNotBlank()) {
                        token = qrToken
                        loading = false
                    } else {
                        error = true
                        loading = false
                    }
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    error = true
                    loading = false
                }
            }
        }.start()
    }

    fun revokeQrToken() {
        loading = true
        val owner = com.example.mentor.data.ActiveUser.get(context)
        Thread {
            try {
                val transport = com.example.asm_final.backend.BackendClient(context, owner)
                val result = transport.request("POST", "/me/qr-code/revoke", org.json.JSONObject(), null, null)
                val qrToken = result.optString("qrCodeToken", "")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (qrToken.isNotBlank()) {
                        token = qrToken
                        loading = false
                        Toast.makeText(context, "Đã tạo mã QR mới", Toast.LENGTH_SHORT).show()
                    } else {
                        error = true
                        loading = false
                    }
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    error = true
                    loading = false
                }
            }
        }.start()
    }

    LaunchedEffect(Unit) {
        loadQrToken()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ranking_close))
            }
        },
        title = {
            Text(
                stringResource(R.string.profile_qr_code),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    Text(
                        stringResource(R.string.course_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (error || token == null) {
                    Text(
                        stringResource(R.string.profile_progress_offline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { loadQrToken() }) {
                        Text(stringResource(R.string.ranking_retry))
                    }
                } else {
                    val activeToken = token!!
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(200.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            QrCodeCanvas(activeToken, Modifier.size(170.dp))
                        }
                    }

                    Text(
                        text = "Mã mời: ${activeToken.take(16)}...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("QR Token", activeToken)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Đã sao chép mã QR", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Sao chép mã")
                        }

                        OutlinedButton(
                            onClick = { revokeQrToken() }
                        ) {
                            Text("Tạo mã mới")
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun QrCodeCanvas(seedToken: String, modifier: Modifier = Modifier) {
    val bitMatrix = remember(seedToken) {
        try {
            val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java)
            hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
            hints[com.google.zxing.EncodeHintType.MARGIN] = 2
            com.google.zxing.qrcode.QRCodeWriter().encode(
                seedToken,
                com.google.zxing.BarcodeFormat.QR_CODE,
                256,
                256,
                hints
            )
        } catch (e: Exception) {
            null
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val matrix = bitMatrix
        if (matrix == null) {
            drawRect(color = Color.LightGray)
            return@Canvas
        }
        val width = matrix.width
        val height = matrix.height
        val cellWidth = size.width / width
        val cellHeight = size.height / height

        drawRect(color = Color.White)

        val darkColor = MentorTokens.Blue800
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (matrix.get(x, y)) {
                    drawRect(
                        color = darkColor,
                        topLeft = androidx.compose.ui.geometry.Offset(x * cellWidth, y * cellHeight),
                        size = androidx.compose.ui.geometry.Size(cellWidth + 0.5f, cellHeight + 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileOverviewGrid(data: LearningOverviewData?) {
    val streakDays = data?.streakDays
    val totalXp = data?.totalXp
    val rankRes = when (data?.rankTier?.uppercase()) {
        "DIAMOND" -> R.string.profile_rank_diamond
        "PLATINUM" -> R.string.profile_rank_platinum
        "GOLD" -> R.string.profile_rank_gold
        "SILVER" -> R.string.profile_rank_silver
        "BRONZE" -> R.string.profile_rank_bronze
        else -> R.string.profile_rank_pending
    }

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
            shadowElevation = 1.dp
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

                // Row 2: Rank + Lead / Pending stat
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cell 3: Rank
                    Row(
                        Modifier
                            .weight(1f)
                            .testTag("profile-rank"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_diamond),
                            contentDescription = null,
                            tint = MentorTokens.Blue600,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(rankRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.competition_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Cell 4: Lead
                    Row(
                        Modifier
                            .weight(1f)
                            .testTag("profile-lead"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trophy),
                            contentDescription = null,
                            tint = MentorTokens.Orange500,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.profile_stats_pending),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.profile_stats_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section STREAK BẠN BÈ:
 * Features 5 circular dashed slots with (+) symbol matching Image A.
 */
@Composable
private fun ProfileFriendsStreak() {
    val context = LocalContext.current
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(stringResource(R.string.profile_friends_streak_title))

        Surface(
            Modifier
                .fillMaxWidth()
                .testTag("profile-friends-empty"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, outlineColor)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = outlineColor,
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(12f, 10f),
                                                0f
                                            )
                                        )
                                    )
                                }
                                .clickable {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.profile_feature_coming_soon),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.profile_friends_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Section HUY HIỆU THỬ THÁC THÁNG:
 * Shows monthly challenge with progress and chevron.
 */
@Composable
private fun ProfileMonthlyChallenge(data: LearningOverviewData?) {
    val context = LocalContext.current
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
    val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
    val currentYear = cal.get(java.util.Calendar.YEAR)
    val totalLessons = data?.totalLessons ?: 0
    val completedLessons = data?.completedLessons ?: 0
    val progressPct = if (totalLessons > 0) ((completedLessons * 100) / totalLessons).coerceIn(0, 100) else 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    Toast.makeText(
                        context,
                        context.getString(R.string.profile_feature_coming_soon),
                        Toast.LENGTH_SHORT
                    ).show()
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileSectionTitle(stringResource(R.string.profile_challenge_title))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Surface(
            Modifier
                .fillMaxWidth()
                .testTag("profile-badges-empty"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_badge_challenge),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Thử thách tháng $currentMonth/$currentYear",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LinearProgressIndicator(
                        progress = { progressPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$progressPct% hoàn thành",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
