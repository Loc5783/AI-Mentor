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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.asm_final.R
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.*
import java.util.function.Consumer

class SubjectHubUi(
    private val context: Context,
    private val chooseGrade: Runnable,
    private val openSubject: Consumer<SubjectId>,
    private val openAiChat: Runnable? = null
) {
    private var grade by mutableIntStateOf(10)
    private var error by mutableStateOf(false)
    private var loading by mutableStateOf(false)

    fun render(year: Int) {
        grade = if (year in 10..12) year else 10
        loading = false
        error = false
    }

    fun failure() {
        loading = false
        error = true
    }

    fun view(): View = composeHost(context) {
        var currentSection by remember { mutableStateOf("MAIN_HUB") }
        var selectedLibrarySubject by remember { mutableStateOf<SubjectId?>(null) }

        when (currentSection) {
            "MAIN_HUB" -> LessonHubMainView(
                grade = grade,
                onChooseGrade = { chooseGrade.run() },
                onSelectSection = { section ->
                    if (section == "AI_PRACTICE" && openAiChat != null) {
                        openAiChat.run()
                    } else {
                        currentSection = section
                    }
                }
            )
            "LIBRARY" -> {
                if (selectedLibrarySubject != null) {
                    LibraryPracticeScreen(
                        subjectId = selectedLibrarySubject!!,
                        grade = grade,
                        onBack = { selectedLibrarySubject = null },
                        onOpenNotebook = {}
                    )
                } else {
                    LibrarySubjectList(
                        grade = grade,
                        onSelectSubject = { subj -> selectedLibrarySubject = subj },
                        onBack = { currentSection = "MAIN_HUB" }
                    )
                }
            }
            "EXAM_PREP" -> ExamPrepScreen(
                onBack = { currentSection = "MAIN_HUB" },
                onOpenAiChat = { openAiChat?.run() }
            )
            "ROADMAP" -> RoadmapSubjectList(
                grade = grade,
                onChooseGrade = { chooseGrade.run() },
                onSelectSubject = { subj -> openSubject.accept(subj) },
                onBack = { currentSection = "MAIN_HUB" }
            )
        }
    }
}

@Composable
fun LessonHubMainView(
    grade: Int,
    onChooseGrade: () -> Unit,
    onSelectSection: (String) -> Unit
) {
    val dark = com.example.asm_final.utils.ThemeHelper.isDarkMode(androidx.compose.ui.platform.LocalContext.current)
    val heroBg = if (dark) MentorTokens.DarkPrimaryContainer else MentorTokens.PrimaryContainer

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Hero Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(heroBg)
                .padding(horizontal = MentorTokens.PagePadding, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .size(52.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_course),
                        contentDescription = null,
                        tint = MentorTokens.SuccessLightText,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.lesson_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Chọn mục học tập phù hợp với nhu cầu của bạn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(MentorTokens.PagePadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Grade Selector Header
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (grade in 10..12) stringResource(R.string.chat_grade, grade) else stringResource(R.string.chat_choose_grade),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Khối lớp đang chọn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onChooseGrade) { Text("Đổi lớp") }
                }
            }

            // 4 Functional Cards
            HubSectionCard(
                icon = "📚",
                title = "1. Thư viện câu hỏi",
                description = "Kho bài tập trắc nghiệm có cấu trúc theo môn và lớp. Luyện tập theo chương, chủ đề và lưu sổ câu sai.",
                actionText = "Vào Thư viện",
                onClick = { onSelectSection("LIBRARY") }
            )

            HubSectionCard(
                icon = "🎯",
                title = "2. Ôn thi tập trung",
                description = "Bộ đề tổng hợp và kế hoạch luyện thi theo chương trình học.",
                actionText = "Xem Ôn thi",
                onClick = { onSelectSection("EXAM_PREP") }
            )

            HubSectionCard(
                icon = "🗺️",
                title = "3. Lộ trình học 9 môn",
                description = "Khung bài học chuẩn 9 môn học phổ thông cùng công cụ môn học.",
                actionText = "Xem Lộ trình",
                onClick = { onSelectSection("ROADMAP") }
            )

            HubSectionCard(
                icon = "🤖",
                title = "4. Luyện cùng AI",
                description = "Thảo luận, giải thích bài tập và luyện tập tương tác cùng Gia sư AI.",
                actionText = "Luyện cùng AI",
                onClick = { onSelectSection("AI_PRACTICE") }
            )
        }
    }
}

@Composable
fun HubSectionCard(
    icon: String,
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(icon, style = MaterialTheme.typography.titleLarge)
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
                    Text(actionText)
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RoadmapSubjectList(
    grade: Int,
    onChooseGrade: () -> Unit,
    onSelectSubject: (SubjectId) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allSubjects = SubjectId.values()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) { Text("← Quay lại") }
                Text("Lộ trình học - Lớp $grade", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allSubjects, key = { it.wireId }) { subject ->
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = subject.label(LocaleHelper.getLanguage(context)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Button(
                            onClick = { onSelectSubject(subject) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Xem môn học")
                        }
                    }
                }
            }
        }
    }
}
