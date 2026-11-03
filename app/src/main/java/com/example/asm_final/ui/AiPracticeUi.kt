package com.example.asm_final.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.learning.*
import com.example.mentor.data.ActiveUser
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.*
import kotlinx.coroutines.CoroutineScope

@Composable
fun AiPracticeScreen(
    initialGrade: Int = 10,
    initialSubject: SubjectId? = null,
    onBack: () -> Unit,
    onOpenNotebook: ((SubjectId?) -> Unit)? = null,
    onOpenAiChat: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val ownerId = remember(context) { ActiveUser.get(context) }
    val app = context.applicationContext as? AiMentorApplication
    val repository = remember(context, ownerId) {
        app?.services()?.aiPractice(ownerId) ?: BackendAiPracticeRepository(context, ownerId)
    }
    val scope = rememberCoroutineScope()
    val viewModel = remember(repository, scope) {
        AiPracticeViewModel(repository, scope)
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onCleared()
        }
    }

    when (viewModel.screenState) {
        AiPracticeScreenState.SETUP -> AiPracticeSetupContent(
            viewModel = viewModel,
            initialGrade = initialGrade,
            initialSubject = initialSubject,
            onBack = onBack,
            onOpenNotebook = onOpenNotebook
        )
        AiPracticeScreenState.GENERATING -> AiPracticeGeneratingContent(
            onCancel = { viewModel.cancelGeneration() }
        )
        AiPracticeScreenState.PRACTICING -> AiPracticeActiveContent(
            viewModel = viewModel,
            onBack = { viewModel.resetToSetup() }
        )
        AiPracticeScreenState.FEEDBACK -> AiPracticeFeedbackContent(
            viewModel = viewModel,
            onOpenAiChat = onOpenAiChat
        )
        AiPracticeScreenState.SUMMARY -> AiPracticeSummaryContent(
            viewModel = viewModel,
            onBack = onBack,
            onOpenNotebook = onOpenNotebook
        )
    }
}

@Composable
private fun AiPracticeSetupContent(
    viewModel: AiPracticeViewModel,
    initialGrade: Int,
    initialSubject: SubjectId?,
    onBack: () -> Unit,
    onOpenNotebook: ((SubjectId?) -> Unit)?
) {
    val subjects = remember { SubjectId.values().toList() }
    var selectedSubject by remember { mutableStateOf(initialSubject ?: SubjectId.MATH) }
    var selectedGrade by remember { mutableIntStateOf(if (initialGrade in 10..12) initialGrade else 10) }
    var topicText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("MEDIUM") }
    var selectedCount by remember { mutableIntStateOf(5) }

    val config = viewModel.config
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)
    val heroBg = if (dark) MentorTokens.DarkPrimaryContainer else MentorTokens.PrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBg)
                .padding(horizontal = MentorTokens.PagePadding, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("AiPracticeBackBtn")) {
                    Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MentorTokens.OnBrand)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Luyện cùng Gia sư AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MentorTokens.OnBrand
                    )
                    Text(
                        text = "Soạn câu hỏi trắc nghiệm theo chủ đề và độ khó tùy chọn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MentorTokens.OnBrand.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notice card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (dark) MentorTokens.NavyCard else MentorTokens.Blue50
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MentorTokens.Blue300.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = "⚡ Ôn luyện thông minh",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MentorTokens.Blue600
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "• AI tạo câu hỏi tức thì theo chủ đề bạn yêu cầu.\n• Không ảnh hưởng chuỗi ngày hay điểm EXP lộ trình.\n• Các câu làm sai được tự động lưu vào Sổ tay câu hỏi sai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            viewModel.resumableSession?.let { saved ->
                OutlinedButton(
                    onClick = { viewModel.resumePractice() },
                    enabled = !viewModel.isSubmitting,
                    modifier = Modifier.fillMaxWidth().testTag("AiPracticeResumeBtn")
                ) { Text("Tiếp tục bài đang làm: ${saved.topic}") }
            }

            // Warning if AI provider is not configured
            if (config != null && (!config.configured || config.provider == "demo")) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) MentorTokens.WarningDarkBg else MentorTokens.WarningLightBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = if (config.provider == "demo") "⚠️ Bản minh họa, không dùng đánh giá kiến thức" else "⚠️ Dịch vụ AI chưa được cấu hình",
                            fontWeight = FontWeight.Bold,
                            color = if (dark) MentorTokens.WarningDarkText else MentorTokens.WarningLightText,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (config.provider == "demo") "Câu hỏi và đáp án là dữ liệu giả lập, không phải nội dung được AI xác minh." else "Liên hệ quản trị viên để thiết lập dịch vụ AI trước khi tạo bài luyện.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dark) MentorTokens.WarningDarkText else MentorTokens.WarningLightText
                        )
                    }
                }
            }

            // Subject selector
            Column {
                Text(
                    text = "1. Chọn môn học",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subj ->
                        val isSelected = subj == selectedSubject
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj.label("vi")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MentorTokens.Blue500,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Grade selector
            Column {
                Text(
                    text = "2. Chọn khối lớp",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 11, 12).forEach { g ->
                        val isSelected = g == selectedGrade
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGrade = g },
                            label = { Text("Lớp $g") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MentorTokens.Blue500,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Topic text input
            Column {
                Text(
                    text = "3. Chủ đề cần luyện",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = topicText,
                    onValueChange = { if (it.length <= 100) topicText = it },
                    placeholder = { Text("Ví dụ: Đạo hàm, Este, Sóng dừng, Cách mạng tháng Tám...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("AiPracticeTopicInput"),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${topicText.trim().length}/100 ký tự",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )
            }

            // Difficulty selector
            Column {
                Text(
                    text = "4. Độ khó",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "EASY" to "Dễ",
                        "MEDIUM" to "Trung bình",
                        "HARD" to "Nâng cao"
                    ).forEach { (diff, label) ->
                        val isSelected = diff == selectedDifficulty
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MentorTokens.Blue500,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Question count selector
            Column {
                Text(
                    text = "5. Số lượng câu hỏi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 10).forEach { cnt ->
                        val isSelected = cnt == selectedCount
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCount = cnt },
                            label = { Text("$cnt câu") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MentorTokens.Blue500,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Error display
            if (viewModel.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) MentorTokens.ErrorDarkBg else MentorTokens.ErrorLightBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = viewModel.errorMessage ?: "",
                        color = if (dark) MentorTokens.ErrorDarkText else MentorTokens.ErrorLightText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Start Button
            Button(
                onClick = {
                    viewModel.startPractice(
                        subjectId = selectedSubject.wireId,
                        grade = selectedGrade,
                        topic = topicText,
                        difficulty = selectedDifficulty,
                        questionCount = selectedCount
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("AiPracticeStartBtn"),
                enabled = topicText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MentorTokens.Blue600)
            ) {
                Text(
                    text = "Bắt đầu luyện tập",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            // Open Notebook Shortcut
            if (onOpenNotebook != null) {
                OutlinedButton(
                    onClick = { onOpenNotebook(selectedSubject) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📖 Mở Sổ tay câu hỏi sai môn ${selectedSubject.label("vi")}",
                        color = MentorTokens.Blue600
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPracticeGeneratingContent(
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MentorTokens.Blue500,
            strokeWidth = 3.dp,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Gia sư AI đang soạn câu hỏi...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Hệ thống đang phân tích chủ đề và chuẩn bị bộ câu hỏi trắc nghiệm kèm giải thích chi tiết.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.testTag("AiPracticeCancelGenBtn")
        ) {
            Text("Hủy tạo")
        }
    }
}

@Composable
private fun AiPracticeActiveContent(
    viewModel: AiPracticeViewModel,
    onBack: () -> Unit
) {
    val q = viewModel.currentQuestion ?: return
    val total = viewModel.session?.questions?.size ?: 1
    val index = viewModel.currentQuestionIndex + 1
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Câu $index / $total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (q.difficulty) {
                        "EASY" -> "Dễ"
                        "HARD" -> "Nâng cao"
                        else -> "Trung bình"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MentorTokens.Blue600
                )
            }
        }

        LinearProgressIndicator(
            progress = { index.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
            color = MentorTokens.Blue500
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Question Prompt Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (dark) MentorTokens.NavyCard else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = q.prompt,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Options List (A, B, C, D)
            listOf("A", "B", "C", "D").forEach { optKey ->
                val optText = q.options[optKey] ?: return@forEach
                val isSelected = viewModel.selectedOption == optKey
                val cardBorder = if (isSelected) {
                    BorderStroke(2.dp, MentorTokens.Blue500)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
                val cardBg = if (isSelected) {
                    if (dark) MentorTokens.NavyElevated else MentorTokens.Blue50
                } else {
                    if (dark) MentorTokens.NavySurface else MaterialTheme.colorScheme.surface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectOption(optKey) }
                        .testTag("AiOption_${optKey}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MentorTokens.Blue500 else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optKey,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Bottom Action Bar
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.skipCurrentQuestion() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("AiPracticeSkipBtn"),
                    enabled = !viewModel.isSubmitting
                ) {
                    Text("Bỏ qua")
                }

                Button(
                    onClick = { viewModel.submitAnswer() },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("AiPracticeSubmitBtn"),
                    enabled = viewModel.selectedOption != null && !viewModel.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MentorTokens.Blue600)
                ) {
                    Text(
                        text = if (viewModel.isSubmitting) "Đang nộp..." else "Xác nhận",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPracticeFeedbackContent(
    viewModel: AiPracticeViewModel,
    onOpenAiChat: (() -> Unit)?
) {
    val result = viewModel.lastResult ?: return
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)
    var showReportDialog by remember { mutableStateOf(false) }
    var showAskAiDialog by remember { mutableStateOf(false) }
    var askAiQuery by remember { mutableStateOf("") }

    val bannerBg = when {
        result.isCorrect -> if (dark) MentorTokens.SuccessDarkBg else MentorTokens.SuccessLightBg
        result.isSkipped -> if (dark) MentorTokens.Orange900 else MentorTokens.Orange100
        else -> if (dark) MentorTokens.ErrorDarkBg else MentorTokens.ErrorLightBg
    }
    val bannerText = when {
        result.isCorrect -> if (result.firstTryCorrect) "Chính xác! 🎉 Bạn đã trả lời đúng ngay lần đầu." else "Chính xác! 🎉 (Lần thử lại)"
        result.isSkipped -> "Bạn đã bỏ qua câu hỏi này. Câu hỏi được lưu vào Sổ tay câu hỏi sai."
        else -> "Chưa chính xác. Đáp án đúng là: ${result.correctOption}"
    }
    val bannerTextColor = when {
        result.isCorrect -> if (dark) MentorTokens.SuccessDarkText else MentorTokens.SuccessLightText
        result.isSkipped -> if (dark) MentorTokens.Orange200 else MentorTokens.Orange800
        else -> if (dark) MentorTokens.ErrorDarkText else MentorTokens.ErrorLightText
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Result Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("AiFeedbackBanner")
            ) {
                Text(
                    text = bannerText,
                    color = bannerTextColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Explanation Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (dark) MentorTokens.NavyCard else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Lời giải chi tiết từ Gia sư AI:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MentorTokens.Blue600
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = result.explanation,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Follow-up AI explanation if available
            if (viewModel.explainMoreText != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) MentorTokens.NavyElevated else MentorTokens.Blue50
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MentorTokens.Blue300)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "🤖 Gia sư giải thích thêm:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MentorTokens.Blue600
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = viewModel.explainMoreText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Report Status Confirmation
            if (viewModel.reportStatus != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) MentorTokens.NavyElevated else MentorTokens.Blue50
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✓ ${viewModel.reportStatus}",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MentorTokens.Blue600
                    )
                }
            }

            // Inquiry row: Ask AI More & Report Question
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAskAiDialog = true },
                    modifier = Modifier.weight(1f).testTag("AiAskMoreBtn")
                ) {
                    Text("Hỏi thêm AI 💬", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showReportDialog = true },
                    modifier = Modifier.weight(1f).testTag("AiReportBtn")
                ) {
                    Text("Báo câu hỏi ⚠️", fontSize = 13.sp)
                }
            }
        }

        // Bottom Navigation Bar
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If wrong, allow Retry
                if (!result.isCorrect) {
                    OutlinedButton(
                        onClick = { viewModel.retryQuestion() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("AiRetryBtn")
                    ) {
                        Text("Luyện lại câu này")
                    }
                }

                val isLast = (viewModel.currentQuestionIndex + 1) >= (viewModel.session?.questions?.size ?: 1)
                Button(
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("AiNextQuestionBtn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MentorTokens.Blue600)
                ) {
                    Text(
                        text = if (isLast) "Xem kết quả" else "Câu tiếp theo →",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Dialog: Report Question
    if (showReportDialog) {
        var reportReason by remember { mutableStateOf("Nội dung sai kiến thức") }
        var reportDetails by remember { mutableStateOf("") }
        val reasons = listOf(
            "Nội dung sai kiến thức",
            "Đáp án không chính xác",
            "Câu hỏi không rõ ràng",
            "Khác"
        )

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Báo câu hỏi có vấn đề") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chọn lý do báo cáo:", style = MaterialTheme.typography.bodySmall)
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = r }
                        ) {
                            RadioButton(
                                selected = reportReason == r,
                                onClick = { reportReason = r }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(r, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        placeholder = { Text("Mô tả chi tiết hơn (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportQuestion(reportReason, reportDetails)
                        showReportDialog = false
                    }
                ) {
                    Text("Gửi báo cáo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Dialog: Ask AI More
    if (showAskAiDialog) {
        AlertDialog(
            onDismissRequest = { showAskAiDialog = false },
            title = { Text("Hỏi thêm Gia sư AI") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Đặt câu hỏi hoặc chọn yêu cầu nhanh:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SuggestionChip(
                            onClick = { askAiQuery = "Giải thích chi tiết hơn tại sao đáp án lại như vậy?" },
                            label = { Text("Giải thích sâu", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = { askAiQuery = "Cho một ví dụ bài tập tương tự kèm lời giải" },
                            label = { Text("Ví dụ tương tự", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = askAiQuery,
                        onValueChange = { askAiQuery = it },
                        placeholder = { Text("Nhập thắc mắc của bạn...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (askAiQuery.isNotBlank()) {
                            viewModel.askAiMore(askAiQuery)
                            showAskAiDialog = false
                        }
                    },
                    enabled = askAiQuery.isNotBlank()
                ) {
                    Text("Gửi câu hỏi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskAiDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun AiPracticeSummaryContent(
    viewModel: AiPracticeViewModel,
    onBack: () -> Unit,
    onOpenNotebook: ((SubjectId?) -> Unit)?
) {
    val sess = viewModel.session ?: return
    val summary = sess.summary ?: return
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(LocalContext.current)
    val subject = remember(sess.subjectId) {
        SubjectId.values().firstOrNull { it.wireId == sess.subjectId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tổng kết bài luyện",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Score Summary Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) MentorTokens.NavyCard else MentorTokens.Blue50
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MentorTokens.Blue300.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${summary.accuracyPercent}%",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MentorTokens.Blue600
                        )
                        Text(
                            text = "Độ chính xác lần đầu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${summary.firstTryCorrectCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MentorTokens.SuccessLightText
                                )
                                Text("Đúng ngay", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${summary.assistedCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MentorTokens.Blue600
                                )
                                Text("Sau hỗ trợ", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${summary.skippedCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MentorTokens.Orange500
                                )
                                Text("Bỏ qua", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${summary.incorrectCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MentorTokens.ErrorLightText
                                )
                                Text("Chưa đúng", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                // Sổ tay note
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) MentorTokens.NavyElevated else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Các câu hỏi làm sai hoặc bỏ qua đã được tự động lưu vào Sổ tay câu hỏi sai để bạn ôn lại bất cứ lúc nào.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Chi tiết các câu hỏi:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // List of questions and user's answers
            items(sess.questions) { q ->
                val ans = sess.answers[q.id]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (dark) MentorTokens.NavySurface else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = q.prompt,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Đáp án đúng: ${q.correctOption ?: ""}",
                                fontWeight = FontWeight.Bold,
                                color = MentorTokens.SuccessLightText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (ans != null) {
                                val userStatus = when {
                                    ans.isSkipped -> "(Bạn đã bỏ qua)"
                                    ans.firstTryCorrect -> "(Đúng ngay)"
                                    else -> "(Đã làm lại / sai)"
                                }
                                Text(
                                    text = userStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val exp = q.explanation
                        if (!exp.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = exp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Bottom Actions
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.resetToSetup() },
                    modifier = Modifier.fillMaxWidth().testTag("AiPracticeNewTopicBtn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MentorTokens.Blue600)
                ) {
                    Text("Luyện chủ đề khác", fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (onOpenNotebook != null) {
                    OutlinedButton(
                        onClick = { onOpenNotebook(subject) },
                        modifier = Modifier.fillMaxWidth().testTag("AiPracticeSummaryNotebookBtn")
                    ) {
                        Text("📖 Mở Sổ tay câu hỏi sai")
                    }
                }
            }
        }
    }
}
