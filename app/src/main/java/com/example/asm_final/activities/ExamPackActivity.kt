package com.example.asm_final.activities

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.asm_final.learning.ExamPackStore
import com.example.asm_final.AiMentorApplication
import com.example.asm_final.learning.ExamPackFoundry
import com.example.asm_final.ui.composeHost
import com.example.asm_final.utils.LocaleHelper
import com.example.mentor.data.ActiveUser
import com.example.mentor.learning.SubjectId
import kotlinx.coroutines.launch

/** File import → private draft → human review → practice. All cloud work stays off main. */
class ExamPackActivity : AppCompatActivity() {
    private var pickedImage by mutableStateOf<Uri?>(null)
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickedImage = uri
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickedImage = savedInstanceState?.getString("picked_exam_uri")?.let(Uri::parse)
        val owner = ActiveUser.get(this)
        if (owner <= 0) { finish(); return }
        val store = (application as AiMentorApplication).services().examPacks(owner)
        setContentView(composeHost(this, scenicBackground = true) {
            ExamPackPage(store, pickedImage, onPick = {
                picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp"))
            }, onBack = ::finish)
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("picked_exam_uri", pickedImage?.toString())
        super.onSaveInstanceState(outState)
    }
}

@Composable
private fun ExamPackPage(store: ExamPackStore, pickedImage: Uri?, onPick: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var packs by remember { mutableStateOf<List<ExamPackFoundry.ExamPack>>(emptyList()) }
    var current by remember { mutableStateOf<ExamPackFoundry.ExamPack?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var grade by remember { mutableIntStateOf(10) }
    var subject by remember { mutableStateOf(SubjectId.values().first()) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var practice by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var revealed by remember { mutableStateOf(false) }
    val language = LocaleHelper.getLanguage(LocalContext.current)
    val context = LocalContext.current
    BackHandler { if (current != null) { current = null; practice = false } else onBack() }

    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = ""
        scope.launch {
            try { block() }
            catch (failure: Exception) { error = failure.message ?: "Không thể kết nối. Hãy thử lại." }
            finally { busy = false }
        }
    }
    LaunchedEffect(store) {
        run { packs = store.list() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = { if (current != null) { current = null; practice = false } else onBack() }) {
            Text("← Quay lại")
        }
        Text("Kho đề của bạn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tách câu từ PDF hoặc ảnh đề, rà đáp án rồi mới luyện tập. Đề nháp chỉ mình bạn thấy.",
            style = MaterialTheme.typography.bodyMedium)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) Surface(color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp)) {
            Text(error, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
        }

        val pack = current
        if (pack == null) {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tạo đề từ PDF hoặc ảnh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth(),
                        label = { Text("Tên đề") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (10..12).forEach { value -> FilterChip(grade == value, onClick = { grade = value },
                            label = { Text("Lớp $value") }) }
                    }
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) { Text("Môn: ${subject.label(language)} ▾") }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            SubjectId.values().forEach { item -> DropdownMenuItem(text = { Text(item.label(language)) },
                                onClick = { subject = item; expanded = false }) }
                        }
                    }
                    OutlinedButton(onClick = onPick) { Text(if (pickedImage == null) "Chọn tệp đề" else "Đã chọn tệp · Đổi tệp") }
                    Button(enabled = !busy && title.isNotBlank() && pickedImage != null, onClick = {
                        val image = pickedImage ?: return@Button
                        run {
                            val created = store.importFile(title, subject.wireId, grade, image)
                            packs = store.list(); current = created; questionIndex = 0
                        }
                    }) { Text("Tách câu hỏi bằng AI") }
                }
            }
            Text("Đề đã lưu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            packs.forEach { item ->
                Surface(Modifier.fillMaxWidth().clickable(enabled = !busy) {
                    run { current = store.load(item.id); questionIndex = 0; practice = false }
                }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val label = runCatching { SubjectId.fromWireId(item.subjectId).label(language) }
                            .getOrDefault(item.subjectId)
                        Text("Lớp ${item.grade} · $label · Chạm để mở")
                    }
                }
            }
        } else if (pack.questions.isEmpty()) {
            Text("Đề này chưa lưu được câu hỏi. Hãy thử nhập lại tệp khi có mạng.")
        } else {
            val question = pack.questions[questionIndex.coerceIn(pack.questions.indices)]
            Text(pack.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Câu ${questionIndex + 1}/${pack.questions.size} · ${question.sourceFileName}, trang ${question.sourcePageIndex}")
            if (pack.sourceFiles.firstOrNull()?.storagePath != null) {
                TextButton(enabled = !busy, onClick = {
                    run {
                        val source = store.sourceFile(pack)
                        val uri = FileProvider.getUriForFile(context,
                            context.packageName + ".fileprovider", source)
                        context.startActivity(Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, pack.sourceFiles.first().mimeType)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    }
                }) { Text("Mở tệp nguồn để đối chiếu") }
            }
            if (!practice) {
                Text("${pack.questions.count { it.isApproved }}/${pack.questions.size} câu đã rà")
                if (pack.isApproved) Button(onClick = {
                    practice = true; questionIndex = 0; selectedAnswer = null; revealed = false
                }) { Text("Luyện đề") }
                ExamPackReview(question, busy) { key, prompt, options, explanation, topic ->
                    run { current = store.review(pack, question.id, key, prompt, options, explanation, topic) }
                }
            } else {
                Text(question.prompt, style = MaterialTheme.typography.titleMedium)
                listOf("A", "B", "C", "D").forEach { key ->
                    val answer = question.options[key].orEmpty()
                    FilterChip(selectedAnswer == key, onClick = { if (!revealed) selectedAnswer = key },
                        label = { Text("$key. $answer") })
                }
                if (revealed) {
                    Text(if (selectedAnswer == question.correctOption) "Đúng" else "Cần ôn lại · Đáp án ${question.correctOption}")
                    Text(question.explanation.orEmpty())
                } else Button(enabled = selectedAnswer != null, onClick = { revealed = true }) { Text("Kiểm tra") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = questionIndex > 0, onClick = {
                    questionIndex--; selectedAnswer = null; revealed = false
                }) { Text("Câu trước") }
                Button(enabled = questionIndex < pack.questions.lastIndex, onClick = {
                    questionIndex++; selectedAnswer = null; revealed = false
                }) { Text("Câu sau") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExamPackReview(q: ExamPackFoundry.ExtractedQuestion, busy: Boolean,
                           onSave: (String, String, Map<String, String>, String, String) -> Unit) {
    var prompt by remember(q.id) { mutableStateOf(q.prompt) }
    var answer by remember(q.id) { mutableStateOf(q.correctOption.orEmpty()) }
    var explanation by remember(q.id) { mutableStateOf(q.explanation.orEmpty()) }
    var topic by remember(q.id) { mutableStateOf(q.topic.orEmpty()) }
    val options = remember(q.id) { mutableStateMapOf<String, String>().apply { putAll(q.options) } }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (q.isApproved) "Đã rà đáp án" else "Rà câu hỏi và chọn đáp án đúng",
                fontWeight = FontWeight.Bold)
            OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("Câu hỏi") })
            listOf("A", "B", "C", "D").forEach { key ->
                OutlinedTextField(options[key].orEmpty(), { options[key] = it }, Modifier.fillMaxWidth(),
                    label = { Text("Đáp án $key") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("A", "B", "C", "D").forEach { key ->
                    FilterChip(answer == key, onClick = { answer = key }, label = { Text("Đúng: $key") })
                }
            }
            OutlinedTextField(explanation, { explanation = it }, Modifier.fillMaxWidth(),
                label = { Text("Lời giải đã kiểm tra") })
            OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), label = { Text("Chủ đề") })
            Button(enabled = !busy && answer.isNotBlank(),
                onClick = { onSave(answer, prompt, options.toMap(), explanation, topic) }) { Text("Lưu câu đã rà") }
        }
    }
}
