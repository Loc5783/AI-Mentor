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
import com.example.asm_final.learning.LearningSetupStore
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

    fun view(): View = composeHost(context, scenicBackground = true) {
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
                    try {
                        navModel.navigateTo(HubSection.valueOf(section))
                    } catch (_: Exception) {}
                },
                onOpenAiChat = { openAiChat?.run() }
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
                initialGrade = navModel.grade,
                initialSubject = navModel.selectedSubject,
                onBack = { navModel.handleBack() },
                onOpenNotebook = { subj -> navModel.openNotebook(subj, fromSection = HubSection.EXAM_PREP) },
                onOpenAiChat = { openAiChat?.run() }
            )
            HubSection.ROADMAP -> RoadmapSubjectList(
                grade = navModel.grade,
                onChooseGrade = { chooseGrade.run() },
                onSelectSubject = { subj -> openSubject.accept(subj) },
                onBack = { navModel.handleBack() }
            )
            HubSection.AI_PRACTICE -> AiPracticeScreen(
                initialGrade = navModel.grade,
                initialSubject = navModel.selectedSubject,
                onBack = { navModel.handleBack() },
                onOpenNotebook = { subj -> navModel.openNotebook(subj, fromSection = HubSection.AI_PRACTICE) },
                onOpenAiChat = { openAiChat?.run() }
            )
        }
    }
}

@Composable
fun LessonHubMainView(
    grade: Int,
    onChooseGrade: () -> Unit,
    onSelectSection: (String) -> Unit,
    onOpenAiChat: () -> Unit
) {
    val palette = premiumDashboardPalette(com.example.asm_final.utils.ThemeHelper.isDarkMode(
        androidx.compose.ui.platform.LocalContext.current))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PremiumDashboardHeader(artwork = MentorArtwork.LEARNING) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.lesson_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = palette.title)
                Text(stringResource(R.string.lesson_hub_intro), style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondary)
            }
        }
        Column(Modifier.padding(horizontal = MentorTokens.PagePadding, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PremiumDashboardCard(palette) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.chat_grade, grade), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = palette.title)
                        Text(stringResource(R.string.lesson_hub_grade), style = MaterialTheme.typography.bodySmall,
                            color = palette.secondary)
                    }
                    TextButton(onClick = onChooseGrade) { Text(stringResource(R.string.lesson_hub_change_grade)) }
                }
            }
            PremiumDashboardCard(palette, highlighted = true) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.design_path), style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = palette.title)
                        Text(stringResource(R.string.lesson_hub_path_desc), style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondary)
                    }
                    MentorIllustration(MentorArtwork.LEARNING, Modifier.width(112.dp))
                }
                PremiumDashboardAction(stringResource(R.string.lesson_hub_open_path), palette,
                    { onSelectSection("ROADMAP") }, Modifier.fillMaxWidth())
            }
            Text(stringResource(R.string.lesson_hub_more), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = palette.title)
            HubSectionCard("01", stringResource(R.string.lesson_hub_library),
                stringResource(R.string.lesson_hub_library_desc), stringResource(R.string.lesson_hub_library),
                { onSelectSection("LIBRARY") })
            HubSectionCard("02", stringResource(R.string.lesson_hub_exam),
                stringResource(R.string.lesson_hub_exam_desc), stringResource(R.string.lesson_hub_exam),
                { onSelectSection("EXAM_PREP") })
            HubSectionCard("03", stringResource(R.string.lesson_hub_chat),
                stringResource(R.string.lesson_hub_chat_desc), stringResource(R.string.lesson_hub_chat),
                onOpenAiChat)
            HubSectionCard("04", stringResource(R.string.lesson_hub_ai_practice),
                stringResource(R.string.lesson_hub_ai_practice_desc), stringResource(R.string.lesson_hub_ai_practice),
                { onSelectSection("AI_PRACTICE") })
            Spacer(Modifier.height(16.dp))
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
    val palette = premiumDashboardPalette(com.example.asm_final.utils.ThemeHelper.isDarkMode(
        androidx.compose.ui.platform.LocalContext.current))
    Surface(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = actionText, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 5.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(palette.iconSurface), contentAlignment = Alignment.Center) {
                Text(icon, color = palette.accent, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = palette.title)
                Text(description, style = MaterialTheme.typography.bodySmall, color = palette.secondary)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = palette.accent)
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
    val ownerId = ActiveUser.get(context)
    val allSubjects = SubjectId.values()
    val palette = premiumDashboardPalette(com.example.asm_final.utils.ThemeHelper.isDarkMode(context))

    var selectedSubjects by remember(ownerId, grade) {
        mutableStateOf(
            try {
                val store = LearningSetupStore(context)
                val sel = store.selected(ownerId, grade)
                store.close()
                sel
            } catch (e: Exception) {
                emptySet()
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        PremiumDashboardHeader(artwork = MentorArtwork.LEARNING) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.course_back))
                }
                Text(stringResource(R.string.lesson_hub_roadmap_grade, grade),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = palette.title)
                Text(stringResource(R.string.lesson_hub_roadmap_desc),
                    style = MaterialTheme.typography.bodySmall, color = palette.secondary)
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = MentorTokens.PagePadding).testTag("subject-list"),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allSubjects, key = { it.wireId }) { subject ->
                val isEnrolled = subject in selectedSubjects

                Surface(
                    Modifier.fillMaxWidth().testTag("subject:${subject.wireId}"),
                    shape = RoundedCornerShape(22.dp),
                    color = palette.card,
                    border = BorderStroke(1.dp, if (isEnrolled) palette.accent.copy(alpha = .55f) else palette.border),
                    shadowElevation = 5.dp
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = subject.label(LocaleHelper.getLanguage(context)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.title
                            )
                            Text(
                                text = stringResource(if (isEnrolled) R.string.lesson_hub_enrolled else R.string.lesson_hub_preview),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isEnrolled) palette.accent else palette.secondary
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (isEnrolled) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val updated = selectedSubjects - subject
                                        try {
                                            val store = LearningSetupStore(context)
                                            store.save(ownerId, grade, updated)
                                            store.close()
                                        } catch (_: Exception) {}
                                        selectedSubjects = updated
                                        android.widget.Toast.makeText(context, "Đã xóa môn học khỏi lộ trình", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(stringResource(R.string.lesson_hub_remove))
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectSubject(subject) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.lesson_hub_enter))
                                }
                            } else {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val updated = selectedSubjects + subject
                                        try {
                                            val store = LearningSetupStore(context)
                                            store.save(ownerId, grade, updated)
                                            store.close()
                                        } catch (_: Exception) {}
                                        selectedSubjects = updated
                                        android.widget.Toast.makeText(context, "Đã thêm môn học vào lộ trình hiện tại", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(stringResource(R.string.lesson_hub_add))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
