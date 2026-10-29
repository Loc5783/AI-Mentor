package com.example.asm_final.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import java.util.Locale

@Composable
fun ExamPrepScreen(
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
        app?.services()?.exam(ownerId) ?: BackendExamRepository(context, ownerId)
    }
    val scope = rememberCoroutineScope()
    val viewModel = remember(repository, scope) {
        ExamPrepViewModel(repository, scope).apply {
            init(initialSubjectId = initialSubject?.wireId, initialGrade = initialGrade)
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.clear()
        }
    }

    val state by viewModel.uiState.collectAsState()

    when (state.screenState) {
        ExamScreenState.LIST -> ExamListContent(
            state = state,
            onSelectSubject = { viewModel.setSubjectFilter(it) },
            onSelectGrade = { viewModel.setGradeFilter(it) },
            onSelectExamType = { viewModel.setExamTypeFilter(it) },
            onSelectSemester = { viewModel.setSemesterFilter(it) },
            onSelectExam = { viewModel.selectExam(it) },
            onResumeSession = { viewModel.resumeSession(it) },
            onReload = { viewModel.loadExams() },
            onBack = onBack,
            onOpenNotebook = { onOpenNotebook?.invoke(null) }
        )
        ExamScreenState.DETAIL -> ExamDetailContent(
            state = state,
            onBack = { viewModel.backToList() },
            onStartExam = { viewModel.startOrResumeExam() }
        )
        ExamScreenState.IN_PROGRESS -> ExamInProgressContent(
            state = state,
            onSelectQuestion = { viewModel.selectQuestionIndex(it) },
            onSelectAnswer = { qId, opt -> viewModel.selectAnswer(qId, opt) },
            onRetryAnswer = { qId -> viewModel.retrySaveAnswer(qId) },
            onNext = { viewModel.nextQuestion() },
            onPrev = { viewModel.previousQuestion() },
            onRequestSubmit = { viewModel.requestSubmitConfirmation() },
            onDismissSubmit = { viewModel.dismissSubmitConfirmation() },
            onConfirmSubmit = { viewModel.submitSession() }
        )
        ExamScreenState.RESULT -> ExamResultContent(
            state = state,
            onSelectReviewQuestion = { viewModel.setReviewQuestionIndex(it) },
            onFinishReview = { viewModel.finishReview() },
            onOpenNotebook = {
                val subWire = state.completedSession?.subjectId
                val sub = SubjectId.values().firstOrNull { it.wireId == subWire }
                onOpenNotebook?.invoke(sub)
            }
        )
    }
}

// =========================================================================
// 1. EXAM LIST / HUB
// =========================================================================

@Composable
private fun ExamListContent(
    state: ExamUiState,
    onSelectSubject: (String?) -> Unit,
    onSelectGrade: (Int) -> Unit,
    onSelectExamType: (String?) -> Unit,
    onSelectSemester: (Int?) -> Unit,
    onSelectExam: (ExamSummary) -> Unit,
    onResumeSession: (ExamSession) -> Unit,
    onReload: () -> Unit,
    onBack: () -> Unit,
    onOpenNotebook: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Về Hub", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ôn thi Giữa kỳ & Cuối kỳ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Đề thi chuẩn hóa THPT 9 môn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenNotebook) {
                    Text("📖 Sổ lỗi sai", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Filters scroll
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Grade Selector
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Khối lớp:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                listOf(10, 11, 12).forEach { gr ->
                    val isSelected = state.selectedGrade == gr
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectGrade(gr) },
                        label = { Text("Lớp $gr") }
                    )
                }
            }

            // Subject Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedSubjectId == null,
                        onClick = { onSelectSubject(null) },
                        label = { Text("Tất cả môn") }
                    )
                }
                items(SubjectId.values()) { subj ->
                    val isSelected = state.selectedSubjectId == subj.wireId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSubject(subj.wireId) },
                        label = { Text(subj.label("vi")) }
                    )
                }
            }

            // Exam Type & Semester Filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedExamType == null,
                        onClick = { onSelectExamType(null) },
                        label = { Text("Tất cả kỳ") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedExamType == "MIDTERM",
                        onClick = { onSelectExamType(if (state.selectedExamType == "MIDTERM") null else "MIDTERM") },
                        label = { Text("Giữa kỳ") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedExamType == "FINAL",
                        onClick = { onSelectExamType(if (state.selectedExamType == "FINAL") null else "FINAL") },
                        label = { Text("Cuối kỳ") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedSemester == 1,
                        onClick = { onSelectSemester(if (state.selectedSemester == 1) null else 1) },
                        label = { Text("HK 1") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedSemester == 2,
                        onClick = { onSelectSemester(if (state.selectedSemester == 2) null else 2) },
                        label = { Text("HK 2") }
                    )
                }
            }
        }

        HorizontalDivider()

        // List Content
        if (state.isLoadingExams) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Text("Đang tải danh sách đề thi...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (state.examListError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        state.examListError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onReload) {
                        Text("Thử lại")
                    }
                }
            }
        } else if (state.exams.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Chưa có đề thi nào phù hợp với bộ lọc.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.exams) { exam ->
                    val activeSession = state.activeSessionsByExamId[exam.id]
                    ExamCard(
                        exam = exam,
                        activeSession = activeSession,
                        onSelect = {
                            if (activeSession != null) {
                                onResumeSession(activeSession)
                            } else {
                                onSelectExam(exam)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamCard(
    exam: ExamSummary,
    activeSession: ExamSession?,
    onSelect: () -> Unit
) {
    val sub = SubjectId.values().firstOrNull { it.wireId == exam.subjectId }
    val subLabel = sub?.label("vi") ?: exam.subjectId.uppercase(Locale.ROOT)
    val examTypeLabel = if (exam.examType == "MIDTERM") "Giữa kỳ" else "Cuối kỳ"
    val hasActive = activeSession != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (hasActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (hasActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Badges row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        subLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "Lớp ${exam.grade}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        "$examTypeLabel (HK${exam.semester})",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "v${exam.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Title
            Text(
                exam.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Info row: duration, questions, points
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⏱️ ${exam.durationMinutes} phút",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "📝 ${exam.questionCount} câu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "🎯 ${exam.totalPoints} điểm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Active in-progress alert or action button
            if (hasActive) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "⚡ Đang có bài làm dở dang",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Tiếp tục làm →",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. EXAM DETAIL / RULES
// =========================================================================

@Composable
private fun ExamDetailContent(
    state: ExamUiState,
    onBack: () -> Unit,
    onStartExam: () -> Unit
) {
    val exam = state.selectedExam ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Danh sách đề")
            }
            Text(
                "Chi tiết đề thi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Exam summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    exam.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Môn học:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(exam.subjectId.uppercase(Locale.ROOT), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Khối lớp:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Lớp ${exam.grade}", fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kỳ thi:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${if (exam.examType == "MIDTERM") "Giữa kỳ" else "Cuối kỳ"} (HK${exam.semester})",
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Thời gian làm bài:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${exam.durationMinutes} phút", fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Số lượng câu hỏi:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${exam.questionCount} câu", fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng điểm quy chuẩn:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${exam.totalPoints} điểm (thang 10)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Rules & Instruction Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "📌 Quy chế phòng thi trực tuyến",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "1. Đồng hồ đếm ngược được tính chuẩn xác từ máy chủ ngay khi bạn bấm nút 'Bắt đầu làm bài'.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "2. Việc đóng ứng dụng, tắt máy hoặc chuyển tab KHÔNG dừng đồng hồ. Thời gian vẫn tiếp tục trôi.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "3. Mỗi lựa chọn đáp án được tự động lưu về máy chủ. Bạn có thể thay đổi hoặc bấm lại đáp án đã chọn để bỏ chọn.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "4. Khi hết giờ đếm ngược (00:00), hệ thống máy chủ sẽ tự động đóng đề và chấm bài.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (state.startSessionError != null) {
            Text(
                state.startSessionError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onStartExam,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !state.isStartingSession
        ) {
            if (state.isStartingSession) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Đang vào phòng thi...")
            } else {
                Text("🚀 Bắt đầu làm bài", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// 3. IN-PROGRESS EXAM ROOM
// =========================================================================

@Composable
private fun ExamInProgressContent(
    state: ExamUiState,
    onSelectQuestion: (Int) -> Unit,
    onSelectAnswer: (String, String) -> Unit,
    onRetryAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onRequestSubmit: () -> Unit,
    onDismissSubmit: () -> Unit,
    onConfirmSubmit: () -> Unit
) {
    val session = state.currentSession ?: return
    val questions = session.questions
    val currentIndex = state.currentQuestionIndex
    val currentQuestion = questions.getOrNull(currentIndex)

    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val timeFormatted = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header / Timer Bar
        Surface(
            tonalElevation = 4.dp,
            color = if (state.isTimerCritical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        session.examTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        "Thời gian còn lại:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isTimerCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Countdown Timer
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.isTimerCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        "⏱️ $timeFormatted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.isTimerCritical) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = onRequestSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !state.isSubmitting
                ) {
                    Text("Nộp bài")
                }
            }
        }

        if (state.submitError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.submitError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Question Palette (Horizontal Strip)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(questions) { index, q ->
                    val isCurrent = index == currentIndex
                    val isAnswered = !state.localAnswers[q.id].isNullOrBlank()

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isAnswered -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable { onSelectQuestion(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent || isAnswered) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.onPrimary
                                isAnswered -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        // Main Question Scrollable Body
        if (currentQuestion != null) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Câu ${currentIndex + 1} / ${questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${currentQuestion.points} điểm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Prompt
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        currentQuestion.prompt,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Options A, B, C, D
                val selectedOpt = state.localAnswers[currentQuestion.id]
                val isSavingThis = state.savingAnswers[currentQuestion.id] == true

                val options = listOf(
                    "A" to currentQuestion.optionA,
                    "B" to currentQuestion.optionB,
                    "C" to currentQuestion.optionC,
                    "D" to currentQuestion.optionD
                )

                options.forEach { (key, text) ->
                    val isChosen = selectedOpt == key
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAnswer(currentQuestion.id, key) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (isChosen) 2.dp else 1.dp,
                            if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                val saveStatus = state.answerSaveStatuses[currentQuestion.id]
                when (saveStatus) {
                    AnswerSaveStatus.SAVING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Text(
                                "Đang lưu đáp án...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    AnswerSaveStatus.CONFIRMED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "✓ Đã lưu trên máy chủ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    AnswerSaveStatus.FAILED_UNSAVED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "⚠️ Chưa lưu được đáp án",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { onRetryAnswer(currentQuestion.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Thử lưu lại", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    null -> {}
                }
            }
        }

        // Bottom Navigation Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    enabled = currentIndex > 0
                ) {
                    Text("← Câu trước")
                }

                Text(
                    "${currentIndex + 1} / ${questions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onNext,
                    enabled = currentIndex < questions.size - 1
                ) {
                    Text("Câu tiếp →")
                }
            }
        }
    }

    // Submit confirmation dialog
    if (state.showSubmitConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissSubmit,
            title = { Text("Xác nhận nộp bài") },
            text = {
                val unanswered = state.unansweredCount
                val unsaved = state.unsavedAnswersCount
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (unsaved > 0) {
                        Text(
                            "⚠️ Có $unsaved câu trả lời chưa được lưu thành công trên máy chủ. Bạn nên kiểm tra lại các câu này trước khi nộp.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (unanswered > 0) {
                        Text("Bạn còn $unanswered câu chưa trả lời. Bạn có chắc chắn muốn nộp bài thi ngay bây giờ không?")
                    } else {
                        Text("Bạn đã trả lời đầy đủ các câu hỏi. Bạn có chắc chắn muốn nộp bài thi để chấm điểm không?")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSubmit,
                    enabled = !state.isSubmitting
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Nộp bài ngay")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissSubmit,
                    enabled = !state.isSubmitting
                ) {
                    Text("Làm tiếp")
                }
            }
        )
    }
}

// =========================================================================
// 4. EXAM RESULT & REVIEW
// =========================================================================

@Composable
private fun ExamResultContent(
    state: ExamUiState,
    onSelectReviewQuestion: (Int) -> Unit,
    onFinishReview: () -> Unit,
    onOpenNotebook: () -> Unit
) {
    val completed = state.completedSession ?: return
    val questions = completed.questions
    val currentRevIndex = state.reviewQuestionIndex
    val currentRevQuestion = questions.getOrNull(currentRevIndex)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Result summary banner
        Surface(
            color = if (completed.state == "TIMED_OUT") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (completed.state == "TIMED_OUT") "⏱️ ĐÃ HẾT GIỜ LÀM BÀI" else "🎉 HOÀN THÀNH BÀI THI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    String.format(Locale.ROOT, "%.2f / 10.0", completed.score10),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Điểm thô: ${completed.rawScore} / ${completed.totalPoints} đ", style = MaterialTheme.typography.bodyMedium)
                    Text("Số câu đúng: ${completed.correctCount} / ${completed.totalCount}", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenNotebook,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📖 Sổ lỗi sai")
                    }
                    Button(
                        onClick = onFinishReview,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Về danh sách đề")
                    }
                }
            }
        }

        // Review Question Strip
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(questions) { index, q ->
                    val isCurrent = index == currentRevIndex
                    val isCorrect = q.isCorrect == true
                    val isAnswered = !q.selectedOption.isNullOrBlank()

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isCorrect -> Color(0xFF2E7D32) // Forest Green
                                    isAnswered -> MaterialTheme.colorScheme.error // Red
                                    else -> Color.Gray
                                }
                            )
                            .clickable { onSelectReviewQuestion(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Review Question Detail Scroll
        if (currentRevQuestion != null) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chi tiết Câu ${currentRevIndex + 1} / ${questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            currentRevQuestion.isCorrect == true -> "✓ Đúng (+${currentRevQuestion.points} đ)"
                            !currentRevQuestion.selectedOption.isNullOrBlank() -> "✗ Sai (0 đ)"
                            else -> "○ Chưa trả lời (0 đ)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            currentRevQuestion.isCorrect == true -> Color(0xFF2E7D32)
                            !currentRevQuestion.selectedOption.isNullOrBlank() -> MaterialTheme.colorScheme.error
                            else -> Color.Gray
                        }
                    )
                }

                // Prompt
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        currentRevQuestion.prompt,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Options review
                val userOpt = currentRevQuestion.selectedOption
                val correctOpt = currentRevQuestion.correctOption

                val options = listOf(
                    "A" to currentRevQuestion.optionA,
                    "B" to currentRevQuestion.optionB,
                    "C" to currentRevQuestion.optionC,
                    "D" to currentRevQuestion.optionD
                )

                options.forEach { (key, text) ->
                    val isCorrectChoice = key == correctOpt
                    val isUserChoice = key == userOpt

                    val borderColor = when {
                        isCorrectChoice -> Color(0xFF2E7D32)
                        isUserChoice -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    val bgColor = when {
                        isCorrectChoice -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                        isUserChoice -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (isCorrectChoice || isUserChoice) 2.dp else 1.dp, borderColor),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCorrectChoice -> Color(0xFF2E7D32)
                                            isUserChoice -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrectChoice || isUserChoice) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCorrectChoice) {
                                Text("✓ Đáp án đúng", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelSmall)
                            } else if (isUserChoice) {
                                Text("✗ Bạn đã chọn", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Explanation
                if (!currentRevQuestion.explanation.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💡 Lời giải chi tiết:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(currentRevQuestion.explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
