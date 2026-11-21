package com.example.asm_final.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asm_final.learning.*
import com.example.asm_final.utils.ThemeHelper
import com.example.mentor.ui.design.MentorTokens

@Composable
fun MisconceptionBusterScreen(
    viewModel: MisconceptionViewModel,
    onClose: () -> Unit
) {
    val dark = ThemeHelper.isDarkMode(LocalContext.current)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("MisconceptionBusterScreen")
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (viewModel.currentStep == MisconceptionStep.SELECT_REASON ||
                            viewModel.currentStep == MisconceptionStep.RESULT) {
                            onClose()
                        } else {
                            viewModel.backToPreviousStep()
                        }
                    },
                    modifier = Modifier.testTag("MisconceptionBackBtn")
                ) {
                    Text(if (viewModel.currentStep == MisconceptionStep.SELECT_REASON ||
                        viewModel.currentStep == MisconceptionStep.RESULT) "✕ Đóng" else "← Quay lại")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "XƯỞNG PHÁ MỆNH ĐỀ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
            }
        }

        // Step indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MisconceptionStep.entries.forEach { step ->
                val isActive = step.ordinal <= viewModel.currentStep.ordinal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        // Content area based on step
        Box(modifier = Modifier.weight(1f)) {
            when (viewModel.currentStep) {
                MisconceptionStep.SELECT_REASON -> MisconceptionSelectReasonStep(viewModel, dark)
                MisconceptionStep.REVIEW_HINT -> MisconceptionReviewHintStep(viewModel, dark)
                MisconceptionStep.TEST_PROPOSITIONS -> MisconceptionTestStep(viewModel)
                MisconceptionStep.RESULT -> MisconceptionResultStep(viewModel, dark, onClose)
            }
        }
    }
}

@Composable
private fun MisconceptionSelectReasonStep(
    viewModel: MisconceptionViewModel,
    dark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "1. Nhận diện nguyên nhân hiểu sai",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hãy chọn nguyên nhân bạn nghĩ đã dẫn đến câu trả lời chưa chính xác:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Question Context Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (dark) MentorTokens.NavyCard else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Câu hỏi đã làm:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                ChatAnswerView(viewModel.questionPrompt, MaterialTheme.colorScheme.onSurface)
                viewModel.selectedOption?.let {
                    Text("Đáp án bạn đã chọn: $it", color = MentorTokens.ErrorLightText, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text(
            text = "Lý do đã được kiểm duyệt (Môn Toán):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        MisconceptionBuster.MATH_CATALOG.forEach { item ->
            val isSelected = viewModel.selectedReasonId == item.id
            Card(
                onClick = {
                    viewModel.selectedReasonId = item.id
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else if (dark) MentorTokens.NavyCard else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("MisconceptionReason_${item.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectedReasonId = item.id
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        viewModel.errorMessage?.let {
            MentorErrorNotice(it)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.proceedToHint() },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("MisconceptionProceedHintBtn"),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Xem gợi ý & Phản ví dụ →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MisconceptionReviewHintStep(
    viewModel: MisconceptionViewModel,
    dark: Boolean
) {
    val session = viewModel.activeSession ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "2. Phân tích & Phản ví dụ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (dark) MentorTokens.NavyCard else MentorTokens.Blue50
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💡 Nguyên nhân:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(session.misconceptionReason, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Verified Hint
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔍 Gợi ý chuẩn hoá:", fontWeight = FontWeight.Bold, color = MentorTokens.Blue600)
                ChatAnswerView(session.aiHint, MaterialTheme.colorScheme.onSurface)
            }
        }

        // Counterexample
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (dark) MentorTokens.NavyElevated else MentorTokens.Orange100.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MentorTokens.Orange600.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚡ Phản ví dụ phá vỡ mệnh đề:", fontWeight = FontWeight.Bold, color = MentorTokens.Orange600)
                ChatAnswerView(session.aiCounterexample, MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { viewModel.proceedToTest() },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("MisconceptionProceedTestBtn")
        ) {
            Text("Làm 2 câu kiểm chứng →", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MisconceptionTestStep(
    viewModel: MisconceptionViewModel
) {
    val session = viewModel.activeSession ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "3. Kiểm chứng mệnh đề",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Xác định tính Đúng / Sai của hai mệnh đề sau:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Proposition 1
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("MisconceptionPropCard1")
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mệnh đề 1:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ChatAnswerView(session.prop1Prompt, MaterialTheme.colorScheme.onSurface)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = viewModel.prop1Answer == true,
                        onClick = { viewModel.prop1Answer = true },
                        label = { Text("Đúng", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("MisconceptionProp1_True")
                    )
                    FilterChip(
                        selected = viewModel.prop1Answer == false,
                        onClick = { viewModel.prop1Answer = false },
                        label = { Text("Sai", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("MisconceptionProp1_False")
                    )
                }
            }
        }

        // Proposition 2
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("MisconceptionPropCard2")
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mệnh đề 2:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ChatAnswerView(session.prop2Prompt, MaterialTheme.colorScheme.onSurface)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = viewModel.prop2Answer == true,
                        onClick = { viewModel.prop2Answer = true },
                        label = { Text("Đúng", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("MisconceptionProp2_True")
                    )
                    FilterChip(
                        selected = viewModel.prop2Answer == false,
                        onClick = { viewModel.prop2Answer = false },
                        label = { Text("Sai", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("MisconceptionProp2_False")
                    )
                }
            }
        }

        viewModel.errorMessage?.let {
            MentorErrorNotice(it)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { viewModel.submitVerifications() },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("MisconceptionSubmitVerificationsBtn"),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Nộp bài kiểm chứng", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MisconceptionResultStep(
    viewModel: MisconceptionViewModel,
    dark: Boolean,
    onClose: () -> Unit
) {
    val session = viewModel.activeSession ?: return
    val isResolved = session.verificationStatus == MisconceptionVerificationStatus.RESOLVED

    val bannerBg = if (isResolved) {
        if (dark) MentorTokens.SuccessDarkBg else MentorTokens.SuccessLightBg
    } else {
        if (dark) MentorTokens.Orange900 else MentorTokens.Orange100
    }

    val bannerTextColor = if (isResolved) {
        if (dark) MentorTokens.SuccessDarkText else MentorTokens.SuccessLightText
    } else {
        if (dark) MentorTokens.Orange200 else MentorTokens.Orange800
    }

    val bannerText = if (isResolved) {
        "ĐÃ PHÁ VỠ HIỂU SAI 🎉\nBạn đã trả lời chính xác cả 2 mệnh đề kiểm chứng!"
    } else {
        "CẦN ÔN TẬP THÊM ⚠️\nBạn chưa trả lời đúng cả 2 mệnh đề kiểm chứng. Câu hỏi vẫn được giữ trong danh sách cần ôn."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "4. Kết quả kiểm chứng",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = bannerBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().testTag("MisconceptionResultBanner")
        ) {
            Text(
                text = bannerText,
                color = bannerTextColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(20.dp)
            )
        }

        if (session.isDraft) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Lưu ý: Kết quả đang được lưu dạng bản nháp ngoại tuyến và sẽ tự đồng bộ khi có kết nối mạng.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary details
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chi tiết lựa chọn của bạn:", fontWeight = FontWeight.Bold)
                Text("• Mệnh đề 1: ${if (session.prop1UserAns == true) "Đúng" else if (session.prop1UserAns == false) "Sai" else "Chưa chọn"}")
                Text("• Mệnh đề 2: ${if (session.prop2UserAns == true) "Đúng" else if (session.prop2UserAns == false) "Sai" else "Chưa chọn"}")
                Text("• Trạng thái: ${if (isResolved) "Đã kiểm chứng (RESOLVED)" else "Cần ôn tập (NEEDS_REVIEW)"}",
                    fontWeight = FontWeight.Bold,
                    color = if (isResolved) MentorTokens.SuccessLightText else MentorTokens.Orange600
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isResolved) {
                OutlinedButton(
                    onClick = { viewModel.retryVerification() },
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("MisconceptionRetryVerificationBtn")
                ) {
                    Text("Thử lại mệnh đề")
                }
            }

            Button(
                onClick = { onClose() },
                modifier = Modifier.weight(1.2f).heightIn(min = 50.dp).testTag("MisconceptionDoneBtn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Hoàn tất", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
