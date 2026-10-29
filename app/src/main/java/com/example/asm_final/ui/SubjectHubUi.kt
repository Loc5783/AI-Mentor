package com.example.asm_final.ui

import com.example.mentor.ui.design.mentorHeader



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

import androidx.activity.compose.BackHandler

import androidx.compose.runtime.saveable.rememberSaveable

import com.example.asm_final.learning.HubSection
import com.example.asm_final.learning.LessonHubNavigationModel

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import com.example.asm_final.AiMentorApplication

import com.example.asm_final.R

import com.example.asm_final.learning.BackendLibraryRepository

import com.example.mentor.data.ActiveUser

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
        val ownerId = ActiveUser.get(context)
        val navModel = rememberSaveable(ownerId, saver = LessonHubNavigationModel.Saver) {
            LessonHubNavigationModel(initialGrade = grade)
        }

        LaunchedEffect(grade) {
            navModel.setGrade(grade)
        }

        BackHandler(enabled = navModel.currentSection != HubSection.MAIN_HUB) {
            navModel.handleBack()
        }

        when (navModel.currentSection) {
            HubSection.MAIN_HUB -> LessonHubMainView(
                grade = navModel.grade,
                onChooseGrade = { chooseGrade.run() },
                onSelectSection = { section ->
                    if (section == "AI_PRACTICE" && openAiChat != null) {
                        openAiChat.run()
                    } else {
                        try {
                            navModel.navigateTo(HubSection.valueOf(section))
                        } catch (_: Exception) {}
                    }
                }
            )
            HubSection.LIBRARY -> {
                val subject = navModel.selectedSubject
                if (subject != null) {
                    LibraryPracticeScreen(
                        subjectId = subject,
                        grade = navModel.grade,
                        onBack = { navModel.handleBack() },
                        onOpenNotebook = { navModel.openNotebook(subject, fromSection = HubSection.LIBRARY) }
                    )
                } else {
                    LibrarySubjectList(
                        grade = navModel.grade,
                        onSelectSubject = { subj -> navModel.selectSubject(subj) },
                        onBack = { navModel.handleBack() },
                        onChooseGrade = { chooseGrade.run() },
                        onOpenNotebook = { navModel.openNotebook(null, fromSection = HubSection.LIBRARY) }
                    )
                }
            }
            HubSection.NOTEBOOK -> {
                val app = context.applicationContext as? AiMentorApplication
                val repository = remember(context, ownerId) {
                    app?.services()?.library(ownerId) ?: BackendLibraryRepository(context, ownerId)
                }
                WrongQuestionsNotebookScreen(
                    subjectId = navModel.selectedSubject,
                    grade = navModel.grade,
                    repository = repository,
                    onBack = { navModel.handleBack() }
                )
            }
            HubSection.EXAM_PREP -> ExamPrepScreen(
                onBack = { navModel.handleBack() },
                onOpenAiChat = { openAiChat?.run() }
            )
            HubSection.ROADMAP -> RoadmapSubjectList(
                grade = navModel.grade,
                onChooseGrade = { chooseGrade.run() },
                onSelectSubject = { subj -> openSubject.accept(subj) },
                onBack = { navModel.handleBack() }
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

    Column(Modifier.fillMaxSize()) {

        // Hero Header

        Box(

            Modifier

                .fillMaxWidth()

                .mentorHeader(com.example.mentor.ui.design.MentorArtwork.LEARNING)

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

                icon = "01",

                title = "Thư viện câu hỏi",

                description = "Kho bài tập trắc nghiệm có cấu trúc theo môn và lớp. Luyện tập theo chương, chủ đề và lưu sổ câu sai.",

                actionText = "Vào Thư viện",

                onClick = { onSelectSection("LIBRARY") }

            )



            HubSectionCard(

                icon = "02",

                title = "Ôn thi tập trung",

                description = "Bộ đề tổng hợp và kế hoạch luyện thi theo chương trình học.",

                actionText = "Xem Ôn thi",

                onClick = { onSelectSection("EXAM_PREP") }

            )



            HubSectionCard(

                icon = "03",

                title = "Lộ trình học 9 môn",

                description = "Khung bài học chuẩn 9 môn học phổ thông cùng công cụ môn học.",

                actionText = "Xem Lộ trình",

                onClick = { onSelectSection("ROADMAP") }

            )



            HubSectionCard(

                icon = "04",

                title = "Luyện cùng AI",

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

        shape = RoundedCornerShape(20.dp),

        color = MaterialTheme.colorScheme.surface,

        border = BorderStroke(1.dp, Brush.linearGradient(listOf(
            MaterialTheme.colorScheme.secondary.copy(alpha = .5f),
            MaterialTheme.colorScheme.outlineVariant))),

        shadowElevation = MentorTokens.CardElevation

    ) {

        Column(

            Modifier.padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            Row(

                Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(12.dp)

            ) {

                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Text(icon, color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)

            }

            Text(

                description,

                style = MaterialTheme.typography.bodyMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

            TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
                Text(actionText)
            }

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
    BackHandler(onBack = onBack)

    val context = androidx.compose.ui.platform.LocalContext.current

    val allSubjects = SubjectId.values()



    Column(Modifier.fillMaxSize()) {

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

