package com.example.asm_final.ui

import com.example.mentor.ui.design.mentorHeader
import android.content.Context
import android.content.Intent
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
import com.example.asm_final.supabase.SupabaseHttp
import com.example.mentor.data.ActiveUser
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.learning.SubjectId
import com.example.mentor.ui.design.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.function.Consumer

class SubjectHubUi(
    private val context: Context,
    private val chooseGrade: Runnable,
    private val openSubject: Consumer<SubjectId>,
    private val openPlan: Runnable,
    private val openAiChat: Runnable? = null,
    private val retryLoad: Runnable
) {
    private var grade by mutableIntStateOf(10)
    private var loadError by mutableStateOf(false)

    fun render(year: Int) {
        grade = if (year in 10..12) year else 10
        loadError = false
    }

    fun failure() {
        loadError = true
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
                loadError = loadError,
                onRetryLoad = { retryLoad.run() },
                onChooseGrade = { chooseGrade.run() },
                onSelectSection = navModel::navigateTo,
                onSelectSubject = navModel::openWorkspace,
                onOpenPlan = { openPlan.run() }
            )
            HubSection.SUBJECT_WORKSPACE -> navModel.selectedSubject?.let { subject ->
                SubjectWorkspace(
                    subject = subject,
                    grade = navModel.grade,
                    onBack = { navModel.handleBack() },
                    onOpenLesson = { openSubject.accept(subject) },
                    onOpenLibrary = { navModel.openWorkspaceTool(HubSection.LIBRARY) },
                    onOpenAiPractice = { navModel.openWorkspaceTool(HubSection.AI_PRACTICE) },
                    onOpenExam = { navModel.openWorkspaceTool(HubSection.EXAM_PREP) },
                    onOpenNotebook = { navModel.openNotebook(subject, HubSection.SUBJECT_WORKSPACE) },
                    onOpenChat = { openAiChat?.run() },
                    onOpenExamPack = { context.startActivity(Intent(context, com.example.asm_final.activities.ExamPackActivity::class.java)) },
                    examPackAvailable = SupabaseHttp.enabled(context)
                )
            }
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
    loadError: Boolean,
    onRetryLoad: () -> Unit,
    onChooseGrade: () -> Unit,
    onSelectSection: (HubSection) -> Unit,
    onSelectSubject: (SubjectId) -> Unit,
    onOpenPlan: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val palette = premiumDashboardPalette(com.example.asm_final.utils.ThemeHelper.isDarkMode(context))
    val language = LocaleHelper.getLanguage(context)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PremiumDashboardHeader(artwork = MentorArtwork.LEARNING) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.study_hub_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = palette.title)
                Text(stringResource(R.string.study_hub_intro), style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondary)
            }
        }
        Column(Modifier.padding(horizontal = MentorTokens.PagePadding, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (loadError) {
                PremiumDashboardCard(palette) {
                    Text(stringResource(R.string.lesson_hub_load_error),
                        color = palette.title, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRetryLoad) { Text(stringResource(R.string.lesson_hub_retry)) }
                }
            }
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
            HubSectionCard("7", stringResource(R.string.study_plan_title),
                stringResource(R.string.study_plan_desc), stringResource(R.string.study_plan_open), onOpenPlan)
            Text(stringResource(R.string.study_subjects_title), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = palette.title)
            PremiumDashboardCard(palette) {
                SubjectId.values().forEachIndexed { index, subject ->
                    val label = subject.label(language)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .clickable(onClickLabel = label) { onSelectSubject(subject) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                            .background(palette.iconSurface), contentAlignment = Alignment.Center) {
                            Text(label.take(1), color = palette.accent,
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(label, Modifier.weight(1f), color = palette.title,
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("›", color = palette.accent, style = MaterialTheme.typography.titleLarge)
                    }
                    if (index < SubjectId.values().lastIndex) HorizontalDivider(color = palette.border)
                }
            }
            TextButton(onClick = { onSelectSection(HubSection.ROADMAP) }) {
                Text(stringResource(R.string.study_manage_path))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SubjectWorkspace(
    subject: SubjectId,
    grade: Int,
    onBack: () -> Unit,
    onOpenLesson: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenAiPractice: () -> Unit,
    onOpenExam: () -> Unit,
    onOpenNotebook: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenExamPack: () -> Unit,
    examPackAvailable: Boolean
) {
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val palette = premiumDashboardPalette(com.example.asm_final.utils.ThemeHelper.isDarkMode(context))
    val title = subject.label(LocaleHelper.getLanguage(context))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PremiumDashboardHeader(artwork = MentorArtwork.LEARNING) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = palette.title)
                Text(stringResource(R.string.chat_grade, grade), style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondary)
            }
        }
        Column(Modifier.padding(horizontal = MentorTokens.PagePadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.course_back)) }
            PremiumDashboardCard(palette, highlighted = true) {
                Text(stringResource(R.string.study_path_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = palette.title)
                Text(stringResource(R.string.study_path_desc), style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondary)
                PremiumDashboardAction(stringResource(R.string.study_path_open), palette,
                    onOpenLesson, Modifier.fillMaxWidth())
            }
            PremiumDashboardCard(palette) {
                Text(stringResource(R.string.study_practice_group), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = palette.title)
                Text(stringResource(R.string.lesson_hub_library_desc), color = palette.secondary,
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lesson_hub_library))
                }
                OutlinedButton(onClick = onOpenAiPractice, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lesson_hub_ai_practice))
                }
            }
            PremiumDashboardCard(palette) {
                Text(stringResource(R.string.study_exam_group), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = palette.title)
                OutlinedButton(onClick = onOpenExam, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lesson_hub_exam))
                }
                if (examPackAvailable) OutlinedButton(onClick = onOpenExamPack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.exam_pack_hub_title))
                }
            }
            PremiumDashboardCard(palette) {
                Text(stringResource(R.string.study_review_group), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = palette.title)
                TextButton(onClick = onOpenNotebook) { Text(stringResource(R.string.study_wrong_answers)) }
                TextButton(onClick = onOpenChat) { Text(stringResource(R.string.lesson_hub_chat)) }
            }
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
    val scope = rememberCoroutineScope()

    var selectedSubjects by remember(ownerId, grade) {
        mutableStateOf(
            try {
                LearningSetupStore(context).use { it.selected(ownerId, grade) }
            } catch (e: Exception) {
                emptySet()
            }
        )
    }
    var subjectPendingRemoval by remember(ownerId, grade) { mutableStateOf<SubjectId?>(null) }
    var selectionLoading by remember(ownerId, grade) { mutableStateOf(true) }
    var selectionSaving by remember(ownerId, grade) { mutableStateOf(false) }
    LaunchedEffect(ownerId, grade) {
        runCatching {
            withContext(Dispatchers.IO) {
                LearningSetupStore(context).use { it.selected(ownerId, grade) }
            }
        }.getOrNull()?.let { selectedSubjects = it }
        selectionLoading = false
    }
    fun saveSelection(updated: Set<SubjectId>, removing: Boolean) {
        if (selectionLoading || selectionSaving) return
        selectionSaving = true
        scope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    LearningSetupStore(context).use { it.save(ownerId, grade, updated) }
                }
                true
            } catch (_: Exception) { false }
            if (saved) selectedSubjects = updated
            if (removing && saved) subjectPendingRemoval = null
            selectionSaving = false
            android.widget.Toast.makeText(context, context.getString(when {
                removing && saved -> R.string.lesson_hub_remove_success
                removing -> R.string.lesson_hub_remove_failed
                saved -> R.string.lesson_hub_add_success
                else -> R.string.lesson_hub_add_failed
            }),
                android.widget.Toast.LENGTH_SHORT).show()
        }
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
                                    onClick = { subjectPendingRemoval = subject },
                                    enabled = !selectionLoading && !selectionSaving,
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
                                        saveSelection(selectedSubjects + subject, false)
                                    },
                                    enabled = !selectionLoading && !selectionSaving,
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
    subjectPendingRemoval?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectPendingRemoval = null },
            title = { Text(stringResource(R.string.lesson_hub_remove_title)) },
            text = { Text(stringResource(R.string.lesson_hub_remove_message,
                subject.label(LocaleHelper.getLanguage(context)), grade)) },
            confirmButton = {
                TextButton(onClick = { saveSelection(selectedSubjects - subject, true) },
                    enabled = !selectionLoading && !selectionSaving) { Text(stringResource(R.string.lesson_hub_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { subjectPendingRemoval = null }) {
                    Text(stringResource(R.string.lesson_hub_cancel))
                }
            }
        )
    }
}
