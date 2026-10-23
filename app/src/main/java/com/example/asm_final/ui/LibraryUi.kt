package com.example.asm_final.ui

import android.content.Context
import android.view.View
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.*

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
    // Renders sample Grade 10 Physics practice questions for offline/fallback mode
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
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val subjects = SubjectId.values()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) { Text("← Quay lại", fontWeight = FontWeight.Bold) }
                Text("Thư viện câu hỏi - Lớp $grade", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(subjects, key = { it.wireId }) { subject ->
                val label = subject.label(LocaleHelper.getLanguage(context))
                val isPhysics10 = (subject == SubjectId.PHYSICS && grade == 10)
                
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
                                if (isPhysics10) "Bộ đề trắc nghiệm có sẵn" else "Nội dung lớp này đang được cập nhật",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPhysics10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val quiz = remember { LibraryUi.samplePhysicsQuiz() }
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }

    val hasContent = (subjectId == SubjectId.PHYSICS && grade == 10)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("← Bài học") }
                Text("${subjectId.label(LocaleHelper.getLanguage(context))} - Lớp $grade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onOpenNotebook) { Text("📖 Sổ lỗi sai") }
            }
        }

        if (!hasContent) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📌 Nội dung lớp này đang được cập nhật", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Kho câu hỏi trắc nghiệm của môn học này đang được ban biên soạn biên tập và sẽ sớm xuất bản.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Button(onClick = onBack) { Text("Quay lại Thư viện") }
                    }
                }
            }
        } else if (isCompleted) {
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎉 Hoàn thành bài luyện tập!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Kết quả: $correctCount / ${quiz.questions.size} câu đúng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text("Lưu ý: Bài luyện Thư viện lưu kết quả ôn tập, không thay đổi EXP hay streak.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        currentStep = 0; selectedOption = null; isSubmitted = false; correctCount = 0; isCompleted = false
                    }, modifier = Modifier.weight(1f)) {
                        Text("Luyện lại")
                    }
                    OutlinedButton(onClick = onOpenNotebook, modifier = Modifier.weight(1f)) {
                        Text("Xem Sổ lỗi sai")
                    }
                }
            }
        } else {
            val q = quiz.questions[currentStep]
            val isCorrect = selectedOption == q.correctOption

            Column(
                Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Câu ${currentStep + 1} / ${quiz.questions.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Chủ đề: ${q.topic}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / quiz.questions.size },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                )

                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(q.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        val options = listOf("A" to q.optionA, "B" to q.optionB, "C" to q.optionC, "D" to q.optionD)
                        options.forEach { (optKey, optText) ->
                            val isSelected = selectedOption == optKey
                            val btnBg = when {
                                isSubmitted && optKey == q.correctOption -> Color(0xFFD1FAE5)
                                isSubmitted && isSelected && !isCorrect -> Color(0xFFFEE2E2)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = btnBg,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !isSubmitted) { selectedOption = optKey }
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("$optKey. ", fontWeight = FontWeight.Bold)
                                    Text(optText)
                                }
                            }
                        }
                    }
                }

                if (isSubmitted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isCorrect) "✅ Chính xác!" else "❌ Chưa đúng (Đáp án đúng: ${q.correctOption})", fontWeight = FontWeight.Bold, color = if (isCorrect) Color(0xFF065F46) else Color(0xFF991B1B))
                            Text("💡 Lời giải: ${q.explanation}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (!isSubmitted) {
                        Button(
                            onClick = {
                                if (selectedOption != null) {
                                    isSubmitted = true
                                    if (selectedOption == q.correctOption) correctCount++
                                }
                            },
                            enabled = selectedOption != null
                        ) {
                            Text("Kiểm tra đáp án")
                        }
                    } else {
                        Button(onClick = {
                            if (currentStep + 1 < quiz.questions.size) {
                                currentStep++
                                selectedOption = null
                                isSubmitted = false
                            } else {
                                isCompleted = true
                            }
                        }) {
                            Text(if (currentStep + 1 < quiz.questions.size) "Câu tiếp theo →" else "Xem kết quả")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamPrepScreen(onBack: () -> Unit, onOpenAiChat: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Quay lại") }
            Text("Ôn thi tập trung", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎯 Khu vực Ôn thi & Thi thử", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Hệ thống đề thi tổng hợp và mô phỏng phòng thi đang được hoàn thiện. Trong đợt đầu, bạn có thể ôn luyện qua Thư viện bài tập hoặc thảo luận đề thi cùng Gia sư AI.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onOpenAiChat) {
                    Text("Luyện cùng Gia sư AI")
                }
            }
        }
    }
}
