package com.example.asm_final.ui

import com.example.mentor.ui.design.mentorHeader

import androidx.activity.compose.BackHandler


import android.content.Context

import android.widget.Toast

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

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

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import com.example.asm_final.AiMentorApplication

import com.example.asm_final.R

import com.example.asm_final.backend.BackendClient

import com.example.asm_final.learning.BackendLibraryRepository

import com.example.asm_final.learning.LibraryRepository

import com.example.asm_final.learning.PracticeQuestion

import com.example.asm_final.learning.PracticeSession

import com.example.asm_final.learning.QuizSummary

import com.example.asm_final.learning.LibraryPracticeController

import com.example.asm_final.learning.QuizSelectionState
import com.example.asm_final.learning.CatalogSectionPresentation
import com.example.asm_final.learning.QuizCardPresentation
import com.example.asm_final.learning.ActiveSessionCardPresentation
import com.example.asm_final.learning.ActiveSessionsSyncWarning
import com.example.asm_final.learning.LibrarySelectionPresentation
import com.example.asm_final.learning.toPresentation

import com.example.asm_final.learning.SessionPracticeState

import com.example.asm_final.learning.SessionPointerStore

import com.example.asm_final.learning.SharedPreferencesSessionPointerStore

import com.example.asm_final.learning.WrongQuestionsNotebookController

import com.example.asm_final.learning.WrongQuestionEntry

import com.example.asm_final.learning.WrongQuestionOption

import com.example.asm_final.learning.WrongQuestionRetryResult

import com.example.asm_final.utils.LocaleHelper

import com.example.mentor.data.ActiveUser

import com.example.mentor.learning.SubjectId

import com.example.mentor.ui.design.*

import kotlinx.coroutines.launch



data class LibraryQuestion(

    val id: String,

    val code: String,

    val chapter: String,

    val topic: String,

    val prompt: String,

    val optionA: String,

    val optionB: String,

    val optionC: String,

    val optionD: String,

    val correctOption: String = "",

    val explanation: String = ""

)



data class LibraryQuiz(

    val id: String,

    val title: String,

    val subjectId: String,

    val grade: Int,

    val chapter: String,

    val topic: String,

    val difficulty: String,

    val questionCount: Int,

    val questions: List<LibraryQuestion> = emptyList()

)



object LibraryUi {

    // Reference fixture for offline testing / fallback verification

    fun samplePhysicsQuiz(): LibraryQuiz = LibraryQuiz(

        id = "quiz_phy10_basic",

        title = "Bộ đề ôn tập Vật lý 10 - Động học & Động lực học",

        subjectId = "physics",

        grade = 10,

        chapter = "Chương 1 & 2",

        topic = "Động học & Động lực học",

        difficulty = "MEDIUM",

        questionCount = 5,

        questions = listOf(

            LibraryQuestion(

                id = "q_phy10_01", code = "PHY10_CH1_Q01", chapter = "Chương 1", topic = "Chuyển động thẳng đều",

                prompt = "Phương trình chuyển động của một chất điểm dọc theo trục Ox có dạng x = 5 + 6t (m; s). Vận tốc của chất điểm là:",

                optionA = "5 m/s", optionB = "6 m/s", optionC = "-5 m/s", optionD = "11 m/s",

                correctOption = "B", explanation = "So sánh với phương trình x = x₀ + vt, v = 6 m/s."

            ),

            LibraryQuestion(

                id = "q_phy10_02", code = "PHY10_CH1_Q02", chapter = "Chương 1", topic = "Chuyển động biến đổi đều",

                prompt = "Công thức liên hệ giữa vận tốc v, gia tốc a và độ dịch chuyển s trong chuyển động thẳng biến đổi đều là:",

                optionA = "v² - v₀² = 2as", optionB = "v - v₀ = 2as", optionC = "v² + v₀² = 2as", optionD = "v² - v₀² = as",

                correctOption = "A", explanation = "Công thức độc lập thời gian là v² - v₀² = 2as."

            ),

            LibraryQuestion(

                id = "q_phy10_03", code = "PHY10_CH2_Q03", chapter = "Chương 2", topic = "Định luật II Newton",

                prompt = "Một lực F = 10 N tác dụng vào vật khối lượng m = 2 kg. Gia tốc của vật là:",

                optionA = "5 m/s²", optionB = "20 m/s²", optionC = "0.2 m/s²", optionD = "8 m/s²",

                correctOption = "A", explanation = "Áp dụng định luật II Newton: a = F / m = 10 / 2 = 5 m/s²."

            ),

            LibraryQuestion(

                id = "q_phy10_04", code = "PHY10_CH1_Q04", chapter = "Chương 1", topic = "Sự rơi tự do",

                prompt = "Chuyển động rơi tự do của một vật là chuyển động:",

                optionA = "Thẳng đều", optionB = "Thẳng nhanh dần đều với gia tốc g", optionC = "Thẳng chậm dần đều", optionD = "Cong đều",

                correctOption = "B", explanation = "Rơi tự do là chuyển động thẳng nhanh dần đều dưới tác dụng duy nhất của trọng lực."

            ),

            LibraryQuestion(

                id = "q_phy10_05", code = "PHY10_CH2_Q05", chapter = "Chương 2", topic = "Tổng hợp lực",

                prompt = "Hai lực F₁ = 3 N và F₂ = 4 N vuông góc với nhau. Độ lớn hợp lực F là:",

                optionA = "1 N", optionB = "7 N", optionC = "5 N", optionD = "12 N",

                correctOption = "C", explanation = "Hai lực vuông góc có hợp lực F = √(F₁² + F₂²) = √(3² + 4²) = 5 N."

            )

        )

    )

}



@Composable

fun LibrarySubjectList(

    grade: Int,

    onSelectSubject: (SubjectId) -> Unit,

    onBack: () -> Unit,
    onChooseGrade: (() -> Unit)? = null,
    onOpenNotebook: (() -> Unit)? = null

) {

    val context = LocalContext.current

    val subjects = SubjectId.values()



    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Box(

            Modifier

                .fillMaxWidth()

                .background(MaterialTheme.colorScheme.surfaceVariant)

                .padding(16.dp)

        ) {

            Row(

                Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.SpaceBetween

            ) {

                TextButton(onClick = onBack) {

                    Text("← Bài học")

                }

                Text(

                    "Thư viện bài tập - Lớp $grade",

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold

                )

                Spacer(Modifier.width(48.dp))

            }

        }



        LazyColumn(

            Modifier.fillMaxSize().padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(10.dp)

        ) {

            items(subjects, key = { it.wireId }) { subject ->

                val label = subject.label(LocaleHelper.getLanguage(context))

                

                Surface(

                    Modifier.fillMaxWidth().clickable { onSelectSubject(subject) },

                    shape = RoundedCornerShape(16.dp),

                    color = MaterialTheme.colorScheme.surface,

                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),

                    shadowElevation = 1.dp

                ) {

                    Row(

                        Modifier.padding(16.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        Column(Modifier.weight(1f)) {

                            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Text(

                                "Bộ đề ôn tập & luyện tập trắc nghiệm",

                                style = MaterialTheme.typography.bodySmall,

                                color = MaterialTheme.colorScheme.primary

                            )

                        }

                        Button(onClick = { onSelectSubject(subject) }) {

                            Text("Vào thư viện")

                        }

                    }

                }

            }

        }

    }

}



@Composable

fun LibraryPracticeScreen(

    subjectId: SubjectId,

    grade: Int,

    onBack: () -> Unit,

    onOpenNotebook: () -> Unit

) {

    val context = LocalContext.current

    val owner = ActiveUser.get(context)

    val app = context.applicationContext as? AiMentorApplication

    val repository: LibraryRepository = remember(context, owner) {

        app?.services()?.library(owner) ?: BackendLibraryRepository(context, owner)

    }

    val scope = rememberCoroutineScope()

    val pointerStore = remember(context) { SharedPreferencesSessionPointerStore(context) }

    val controller = remember(repository, pointerStore) { LibraryPracticeController(repository, scope, pointerStore) }

    val selectionState by controller.selectionState.collectAsState()

    val sessionState by controller.sessionState.collectAsState()

    BackHandler(enabled = sessionState != null) {
        controller.backToList()
    }



    // Show toast notifications from controller if any

    LaunchedEffect(sessionState?.notificationMessage) {

        sessionState?.notificationMessage?.let { msg ->

            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

            controller.clearNotification()

        }

    }



    LaunchedEffect(selectionState.startErrorMessage) {

        selectionState.startErrorMessage?.let { msg ->

            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

        }

    }



    if (sessionState == null) {

        // LUỒNG 1: DANH SÁCH BỘ ĐỀ

        LibraryQuizSelectionView(

            subjectId = subjectId,

            grade = grade,

            controller = controller,

            selectionState = selectionState,

            onBack = onBack,

            onOpenNotebook = onOpenNotebook

        )

    } else {

        // LUỒNG 2: PHIÊN LÀM BÀI ĐANG HOẠT ĐỘNG

        LibrarySessionPracticeView(

            sessionState = sessionState!!,

            subjectId = subjectId,

            grade = grade,

            controller = controller,

            onOpenNotebook = onOpenNotebook

        )

    }

}



@Composable

private fun LibraryQuizSelectionView(
    subjectId: SubjectId,
    grade: Int,
    controller: LibraryPracticeController,
    selectionState: QuizSelectionState,
    onBack: () -> Unit,
    onOpenNotebook: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(subjectId, grade) {
        controller.loadQuizzes(subjectId.wireId, grade)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) { Text("← Danh sách môn") }
                Text(
                    "${subjectId.label(LocaleHelper.getLanguage(context))} - Lớp $grade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onOpenNotebook) { Text("📖 Sổ lỗi sai") }
            }
        }

        if (selectionState.confirmingRestartQuiz != null) {
            val quizToRestart = selectionState.confirmingRestartQuiz!!
            AlertDialog(
                onDismissRequest = { controller.cancelRestartConfirmation() },
                title = { Text("Bắt đầu lại bài luyện?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Bài luyện \"${quizToRestart.title}\" đang có một phiên làm dở. Bắt đầu lại sẽ bỏ qua tiến độ cũ và tạo bài làm mới hoàn toàn.")
                },
                confirmButton = {
                    Button(onClick = { controller.startQuiz(quizToRestart, forceRestart = true) }) {
                        Text("Bắt đầu lại")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { controller.cancelRestartConfirmation() }) {
                        Text("Hủy")
                    }
                }
            )
        }

        val presentation = selectionState.toPresentation()

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Error banner on start/resume
            if (presentation.startError != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "⚠️ Không thể bắt đầu/mở lại bài luyện",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    presentation.startError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(onClick = { controller.clearStartError() }) {
                                Text("Đóng", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // 2. Warning banner for active sessions sync
            if (presentation.activeSessionsSyncWarning != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "⚠️ Tiến độ bài làm dở chưa đồng bộ",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    presentation.activeSessionsSyncWarning.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            if (presentation.activeSessionsSyncWarning.canRetry) {
                                Button(onClick = { controller.retryLoadActiveSessions(subjectId.wireId, grade) }) {
                                    Text("Thử lại")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Standalone Active Sessions (in-progress sessions not bound to a visible public quiz card)
            if (presentation.standaloneActiveSessions.isNotEmpty()) {
                item {
                    Text(
                        if (selectionState.quizzes.isNullOrEmpty())
                            "Bài luyện đang làm dở (Đề thi hiện không mở lượt làm mới):"
                        else
                            "Bài luyện đang làm dở từ đề trước đó:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(presentation.standaloneActiveSessions, key = { it.sessionId }) { activeCard ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(activeCard.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                activeCard.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "⏳ Đang làm dở (Đã trả lời ${activeCard.answeredCount}/${activeCard.totalCount} câu)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    if (!activeCard.isSynced) {
                                        Text(
                                            "Chưa đồng bộ mới nhất",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { controller.resumeSessionDirect(activeCard.session) },
                                    enabled = selectionState.startingQuizId == null && selectionState.resumingQuizId == null
                                ) {
                                    Text(if (activeCard.isResuming) "Đang mở lại..." else "Tiếp tục làm bài")
                                }
                            }
                        }
                    }
                }
            }

            // 4. Catalog Section
            when (val catalog = presentation.catalogSection) {
                is CatalogSectionPresentation.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator()
                                Text("Đang tải danh sách bộ đề từ máy chủ...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is CatalogSectionPresentation.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("⚠️ Lỗi tải dữ liệu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(catalog.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { controller.loadQuizzes(subjectId.wireId, grade) }) {
                                        Text("Thử lại")
                                    }
                                    OutlinedButton(onClick = onBack) {
                                        Text("Quay lại")
                                    }
                                }
                            }
                        }
                    }
                }
                is CatalogSectionPresentation.Empty -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(catalog.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text(
                                    catalog.detailMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (!catalog.hasActiveSessionsAbove) {
                                    Button(onClick = onBack) { Text("Quay lại Thư viện") }
                                }
                            }
                        }
                    }
                }
                is CatalogSectionPresentation.Available -> {
                    item {
                        Text(
                            "Chọn bộ đề để bắt đầu luyện tập:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(catalog.quizzes, key = { it.quiz.id }) { itemPresentation ->
                        val quiz = itemPresentation.quiz
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text(
                                            "${quiz.questionCount} câu",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (quiz.difficulty.isNotBlank()) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Text(
                                                quiz.difficulty,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    if (quiz.chapter.isNotBlank()) {
                                        Text(
                                            quiz.chapter,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (quiz.topic.isNotBlank()) {
                                    Text(
                                        "Chủ đề: ${quiz.topic}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (itemPresentation.hasActiveSession) {
                                    val activeSession = itemPresentation.activeSession!!
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "⏳ Đang làm dở (Đã trả lời ${activeSession.answeredCount}/${activeSession.totalCount} câu)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            if (!itemPresentation.isSynced) {
                                                Text(
                                                    "Chưa đồng bộ mới nhất",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { controller.requestRestartConfirmation(quiz) },
                                            enabled = selectionState.startingQuizId == null && selectionState.resumingQuizId == null
                                        ) {
                                            Text("Bắt đầu lại")
                                        }
                                        Button(
                                            onClick = { controller.resumeSession(quiz, activeSession.id) },
                                            enabled = selectionState.startingQuizId == null && selectionState.resumingQuizId == null
                                        ) {
                                            Text(if (itemPresentation.isResuming) "Đang mở lại..." else "Tiếp tục làm bài")
                                        }
                                    }
                                } else {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = { controller.startQuiz(quiz) },
                                            enabled = selectionState.startingQuizId == null && selectionState.resumingQuizId == null
                                        ) {
                                            Text(if (itemPresentation.isStarting) "Đang chuẩn bị..." else "Bắt đầu làm bài")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable

private fun LibrarySessionPracticeView(

    sessionState: SessionPracticeState,

    subjectId: SubjectId,

    grade: Int,

    controller: LibraryPracticeController,

    onOpenNotebook: () -> Unit

) {

    val context = LocalContext.current

    val session = sessionState.session

    val quizTitle = sessionState.quizTitle

    val currentStep = sessionState.currentStep

    val selectedOption = sessionState.selectedOption

    val isSubmittingAnswer = sessionState.isSubmittingAnswer

    val isCompleting = sessionState.isCompleting



    val isCompleted = session.state == "COMPLETED"

    val questions = session.questions



    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Box(

            Modifier

                .fillMaxWidth()

                .background(MaterialTheme.colorScheme.surfaceVariant)

                .padding(16.dp)

        ) {

            Row(

                Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.SpaceBetween

            ) {

                TextButton(onClick = { controller.backToList() }) { Text("← Danh sách đề") }

                Text(

                    if (quizTitle.isNotBlank()) quizTitle else "${subjectId.label(LocaleHelper.getLanguage(context))} - Lớp $grade",

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold,

                    maxLines = 1

                )

                OutlinedButton(onClick = onOpenNotebook) { Text("📖 Sổ lỗi sai") }

            }

        }



        if (isCompleted) {

            // MÀN HÌNH HOÀN THÀNH BÀI THI

            Column(

                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.spacedBy(16.dp)

            ) {

                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {

                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        Text("🎉 Hoàn thành bài luyện tập!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                        Text(

                            "Kết quả: ${session.correctCount} / ${session.totalCount} câu đúng (Điểm: ${session.score} / ${session.maxScore})",

                            style = MaterialTheme.typography.titleLarge,

                            fontWeight = FontWeight.SemiBold,

                            color = MaterialTheme.colorScheme.primary

                        )

                        Text(

                            "Lưu ý: Bài luyện Thư viện lưu kết quả ôn tập, không thay đổi EXP hay streak.",

                            style = MaterialTheme.typography.bodySmall,

                            color = MaterialTheme.colorScheme.onSurfaceVariant,

                            textAlign = TextAlign.Center

                        )

                    }

                }



                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                    Button(

                        onClick = { controller.retryPractice(session.quizId) },

                        modifier = Modifier.weight(1f)

                    ) {

                        Text("Luyện lại đề này")

                    }

                    OutlinedButton(onClick = { controller.backToList() }, modifier = Modifier.weight(1f)) {

                        Text("Chọn đề khác")

                    }

                }

            }

        } else if (questions.isNotEmpty() && currentStep < questions.size) {

            val q = questions[currentStep]

            val isAnswered = q.userAnswer != null

            val currentSelected = selectedOption ?: q.userAnswer



            Column(

                Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),

                verticalArrangement = Arrangement.spacedBy(12.dp)

            ) {

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                    Text("Câu ${currentStep + 1} / ${questions.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (q.topic.isNotBlank()) {

                        Text("Chủ đề: ${q.topic}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                    }

                }



                LinearProgressIndicator(

                    progress = { (currentStep + 1).toFloat() / questions.size },

                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))

                )



                Surface(

                    shape = RoundedCornerShape(16.dp),

                    color = MaterialTheme.colorScheme.surface,

                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),

                    modifier = Modifier.fillMaxWidth()

                ) {

                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        Text(q.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)



                        val options = listOf("A" to q.optionA, "B" to q.optionB, "C" to q.optionC, "D" to q.optionD)

                        options.forEach { (optKey, optText) ->

                            val isChosen = currentSelected == optKey

                            val btnBg = when {

                                isAnswered && optKey == q.correctOption -> Color(0xFFD1FAE5)

                                isAnswered && isChosen && q.isCorrect == false -> Color(0xFFFEE2E2)

                                isChosen -> MaterialTheme.colorScheme.primaryContainer

                                else -> MaterialTheme.colorScheme.surface

                            }



                            Surface(

                                shape = RoundedCornerShape(12.dp),

                                color = btnBg,

                                border = BorderStroke(1.dp, if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .clickable(enabled = !isAnswered && !isSubmittingAnswer) {

                                        controller.selectOption(optKey)

                                    }

                            ) {

                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {

                                    Text("$optKey. ", fontWeight = FontWeight.Bold)

                                    Text(optText)

                                }

                            }

                        }

                    }

                }



                if (isAnswered) {

                    val correct = q.isCorrect == true

                    Surface(

                        shape = RoundedCornerShape(12.dp),

                        color = if (correct) Color(0xFFECFDF5) else Color(0xFFFEF2F2),

                        border = BorderStroke(1.dp, if (correct) Color(0xFF10B981) else Color(0xFFEF4444)),

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

                            Text(

                                if (correct) "✅ Chính xác!" else "❌ Chưa đúng (Đáp án đúng: ${q.correctOption ?: ""})",

                                fontWeight = FontWeight.Bold,

                                color = if (correct) Color(0xFF065F46) else Color(0xFF991B1B)

                            )

                            if (!q.explanation.isNullOrBlank()) {

                                Text("💡 Lời giải: ${q.explanation}", style = MaterialTheme.typography.bodyMedium)

                            }

                        }

                    }

                }



                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

                    if (!isAnswered) {

                        Button(

                            onClick = { controller.submitAnswer() },

                            enabled = selectedOption != null && !isSubmittingAnswer

                        ) {

                            Text(if (isSubmittingAnswer) "Đang kiểm tra..." else "Kiểm tra đáp án")

                        }

                    } else {

                        // Đã trả lời câu này

                        val allAnswered = questions.all { it.userAnswer != null }

                        if (currentStep + 1 < questions.size) {

                            Button(

                                onClick = { controller.nextQuestion() }

                            ) {

                                Text("Câu tiếp theo →")

                            }

                        } else {

                            // Câu cuối cùng: Nút hoàn thành bài luyện

                            Button(

                                onClick = { controller.completeSession() },

                                enabled = allAnswered && !isCompleting

                            ) {

                                Text(if (isCompleting) "Đang nộp bài..." else "Xem kết quả bài luyện")

                            }

                        }

                    }

                }

            }

        }

    }

}









@Composable

fun WrongQuestionsNotebookScreen(

    subjectId: SubjectId?,

    grade: Int,

    repository: LibraryRepository,

    onBack: () -> Unit

) {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val controller = remember(repository) { WrongQuestionsNotebookController(repository, scope) }

    val state by controller.state.collectAsState()



    LaunchedEffect(subjectId, grade, state.filterState) {

        controller.loadNotebook(subjectId?.wireId, grade, state.filterState)

    }



    Column(

        Modifier

            .fillMaxSize()

            .background(MaterialTheme.colorScheme.background)

    ) {

        // Header

        Surface(

            tonalElevation = 2.dp,

            modifier = Modifier.fillMaxWidth().mentorHeader(),
            color = androidx.compose.ui.graphics.Color.Transparent

        ) {

            Row(

                Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp, vertical = 12.dp),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.SpaceBetween

            ) {

                TextButton(onClick = onBack) {

                    Text("← Quay lại")

                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Text(

                        "📖 Sổ lỗi sai",

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold

                    )

                    Text(

                        if (subjectId != null) "${subjectId.label(LocaleHelper.getLanguage(context))} - Lớp $grade" else "Lớp $grade",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

                Spacer(Modifier.width(64.dp))

            }

        }



        // Filter tabs

        Row(

            Modifier

                .fillMaxWidth()

                .padding(horizontal = 16.dp, vertical = 8.dp),

            horizontalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            val filters = listOf(

                "ALL" to "Tất cả",

                "NEEDS_REVIEW" to "⚠️ Cần ôn tập",

                "RESOLVED" to "✓ Đã làm đúng"

            )

            filters.forEach { (code, label) ->

                val isSelected = state.filterState == code

                FilterChip(

                    selected = isSelected,

                    onClick = { controller.loadNotebook(subjectId?.wireId, grade, code) },

                    label = { Text(label) }

                )

            }

        }



        if (state.isLoading) {

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    CircularProgressIndicator()

                    Text("Đang tải sổ lỗi sai...", style = MaterialTheme.typography.bodyMedium)

                }

            }

        } else if (state.errorMessage != null) {

            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(state.errorMessage ?: "Lỗi tải dữ liệu", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)

                    Button(onClick = { controller.loadNotebook(subjectId?.wireId, grade, state.filterState) }) {

                        Text("Thử lại")

                    }

                }

            }

        } else if (state.items.isNullOrEmpty()) {

            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(

                        "🎉 Tuyệt vời! Bạn không có câu sai nào trong mục này.",

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.SemiBold,

                        textAlign = TextAlign.Center

                    )

                    Text(

                        "Hãy tiếp tục làm bài luyện để củng cố kiến thức.",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant,

                        textAlign = TextAlign.Center

                    )

                }

            }

        } else {

            LazyColumn(

                Modifier.fillMaxSize().padding(16.dp),

                verticalArrangement = Arrangement.spacedBy(14.dp)

            ) {

                if (state.retryErrorMessage != null) {

                    item {

                        Surface(

                            shape = RoundedCornerShape(12.dp),

                            color = MaterialTheme.colorScheme.errorContainer,

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Text(

                                state.retryErrorMessage ?: "",

                                modifier = Modifier.padding(14.dp),

                                style = MaterialTheme.typography.bodySmall,

                                color = MaterialTheme.colorScheme.onErrorContainer

                            )

                        }

                    }

                }



                items(state.items!!, key = { it.id }) { item ->

                    val isResolved = item.state == "RESOLVED"

                    val isRetrying = state.retryingEntryId == item.id

                    val selectedRetryOpt = state.selectedRetryOptions[item.id]

                    val retryResult = state.retryResults[item.id]



                    Surface(

                        shape = RoundedCornerShape(16.dp),

                        color = MaterialTheme.colorScheme.surface,

                        border = BorderStroke(

                            1.dp,

                            if (isResolved) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant

                        ),

                        shadowElevation = 1.dp,

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            // Badge row

                            Row(

                                Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {

                                        Text(

                                            item.chapter.ifBlank { "Chương chung" },

                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),

                                            style = MaterialTheme.typography.labelSmall

                                        )

                                    }

                                    if (item.difficulty.isNotBlank()) {

                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {

                                            Text(

                                                item.difficulty,

                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),

                                                style = MaterialTheme.typography.labelSmall

                                            )

                                        }

                                    }

                                }



                                Surface(

                                    shape = RoundedCornerShape(8.dp),

                                    color = if (isResolved) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer

                                ) {

                                    Text(

                                        if (isResolved) "✓ Đã làm đúng lại" else "⚠️ Cần ôn tập",

                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),

                                        style = MaterialTheme.typography.labelSmall,

                                        fontWeight = FontWeight.Bold,

                                        color = if (isResolved) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

                                    )

                                }

                            }



                            // Prompt

                            Text(

                                item.prompt,

                                style = MaterialTheme.typography.bodyLarge,

                                fontWeight = FontWeight.SemiBold

                            )



                            // Options display

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                                item.options.forEach { opt ->

                                    val isSelectedForRetry = selectedRetryOpt == opt.id

                                    val isCorrectAnswer = opt.id == item.correctOption



                                    val bg = when {

                                        isSelectedForRetry -> MaterialTheme.colorScheme.primaryContainer

                                        isResolved && isCorrectAnswer -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)

                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

                                    }



                                    Surface(

                                        shape = RoundedCornerShape(10.dp),

                                        color = bg,

                                        border = if (isSelectedForRetry) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .clickable(enabled = item.canRetry && !isResolved && !isRetrying) {

                                                controller.selectRetryOption(item.id, opt.id)

                                            }

                                    ) {

                                        Row(

                                            Modifier.padding(10.dp),

                                            verticalAlignment = Alignment.CenterVertically,

                                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                                        ) {

                                            Surface(

                                                shape = CircleShape,

                                                color = if (isSelectedForRetry) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,

                                                modifier = Modifier.size(24.dp)

                                            ) {

                                                Box(contentAlignment = Alignment.Center) {

                                                    Text(

                                                        opt.id,

                                                        style = MaterialTheme.typography.labelSmall,

                                                        fontWeight = FontWeight.Bold,

                                                        color = if (isSelectedForRetry) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                                                    )

                                                }

                                            }

                                            Text(

                                                "${opt.id}. ${opt.text}",

                                                style = MaterialTheme.typography.bodyMedium,

                                                modifier = Modifier.weight(1f)

                                            )

                                        }

                                    }

                                }

                            }



                            // Previous mistake & Explanation

                            Surface(

                                shape = RoundedCornerShape(10.dp),

                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

                                    Text(

                                        "Lịch sử làm bài: Bạn đã chọn đáp án ${item.selectedOption} (Đáp án chuẩn: ${item.correctOption})",

                                        style = MaterialTheme.typography.bodySmall,

                                        fontWeight = FontWeight.SemiBold,

                                        color = MaterialTheme.colorScheme.onSurfaceVariant

                                    )

                                    if (item.explanation.isNotBlank()) {

                                        Text(

                                            "💡 Lời giải: ${item.explanation}",

                                            style = MaterialTheme.typography.bodySmall,

                                            color = MaterialTheme.colorScheme.onSurfaceVariant

                                        )

                                    }

                                }

                            }



                            // Feedback / Retry Section

                            if (retryResult != null) {

                                Surface(

                                    shape = RoundedCornerShape(10.dp),

                                    color = if (retryResult.isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,

                                    modifier = Modifier.fillMaxWidth()

                                ) {

                                    Row(

                                        Modifier.padding(12.dp),

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.SpaceBetween

                                    ) {

                                        Text(

                                            if (retryResult.isCorrect) "🎉 Chính xác! Bạn đã hiểu và sửa đúng câu hỏi này." else "❌ Chưa chính xác, hãy xem lại lời giải chi tiết.",

                                            style = MaterialTheme.typography.bodySmall,

                                            fontWeight = FontWeight.Bold,

                                            color = if (retryResult.isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,

                                            modifier = Modifier.weight(1f)

                                        )

                                        TextButton(onClick = { controller.clearRetryFeedback(item.id) }) {

                                            Text("Đóng")

                                        }

                                    }

                                }

                            }



                            if (item.canRetry && !isResolved) {

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

                                    Button(

                                        onClick = { controller.submitRetry(item) },

                                        enabled = selectedRetryOpt != null && !isRetrying

                                    ) {

                                        Text(if (isRetrying) "Đang kiểm tra..." else "Luyện lại câu này")

                                    }

                                }

                            } else if (!item.hasSnapshot) {

                                Text(

                                    "ℹ️ Câu hỏi từ phiên bản cũ (không hỗ trợ luyện lại trực tiếp).",

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

}

