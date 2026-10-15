package com.example.modulelaptrinh;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modulelaptrinh.adapters.PendingImageAdapter;
import com.example.modulelaptrinh.adapters.ProgrammingChatAdapter;
import com.example.modulelaptrinh.database.ProgrammingDbHelper;
import com.example.modulelaptrinh.utils.ChatGeminiApiClient;
import com.example.modulelaptrinh.utils.GeminiApiException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgrammingChatActivity extends AppCompatActivity {
    private static final String TAG = "ProgrammingChatActivity";
    private static final int MAX_CONTEXT_MESSAGES = 12;
    private static final int MAX_CONTEXT_CHARS = 12_000;
    private static final int MAX_CONTINUATIONS = 2;

    private RecyclerView recyclerView;
    private RecyclerView rvPendingImages;
    private EditText edtQuestion;
    private ImageView btnSend;
    private ImageView btnUploadImage;
    private ImageView btnBack;
    private ProgressBar pbChatLoading;
    private ProgrammingChatAdapter chatAdapter;
    private PendingImageAdapter pendingImageAdapter;
    private List<ProgrammingMessage> messageList;
    private ProgrammingDbHelper dbHelper;

    private String language = "Lập trình";
    private long sessionId = -1;
    private int userId;
    private boolean isFirstMessageSent;
    private Dialog previewDialog;
    private boolean isSending;
    private ChatGeminiApiClient geminiClient;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            return;
                        }

                        List<Uri> selectedUris = new ArrayList<>();
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                if (uri != null) {
                                    selectedUris.add(uri);
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            selectedUris.add(result.getData().getData());
                        }

                        if (selectedUris.isEmpty()) {
                            return;
                        }

                        int remainingSlots = pendingImageAdapter.getRemainingSlots();
                        if (remainingSlots <= 0) {
                            Toast.makeText(this, "Đã đạt giới hạn 5 ảnh", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<Uri> urisToAdd = selectedUris.size() > remainingSlots
                                ? selectedUris.subList(0, remainingSlots)
                                : selectedUris;
                        pendingImageAdapter.addImages(urisToAdd);
                        updatePendingImagesVisibility();

                        if (selectedUris.size() > remainingSlots) {
                            Toast.makeText(
                                    this,
                                    "Chỉ thêm " + remainingSlots + " ảnh đầu tiên",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_chat_detail);

        userId = Math.max(0, getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, 0));

        try {
            dbHelper = new ProgrammingDbHelper(this);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi tạo database", e);
            Toast.makeText(this, R.string.prog_chat_error_open_db, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        geminiClient = new ChatGeminiApiClient();
        language = getIntent().getStringExtra("LANGUAGE");
        if (language == null || language.trim().isEmpty()) {
            language = getString(R.string.programming_app_name);
        }

        String initialQuestion = getIntent().getStringExtra("QUESTION");
        sessionId = getIntent().getLongExtra("SESSION_ID", -1);

        initViews();
        initAdapters();
        initChatHistory(initialQuestion);
        setupListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_chat_detail);
        rvPendingImages = findViewById(R.id.rv_pending_images);
        edtQuestion = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.iv_send);
        btnUploadImage = findViewById(R.id.iv_attach);
        btnBack = findViewById(R.id.iv_back);
        pbChatLoading = findViewById(R.id.pb_chat_loading);
    }

    private void initAdapters() {
        messageList = new ArrayList<>();
        chatAdapter = new ProgrammingChatAdapter(messageList, this::showImagePreviewDialog);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        pendingImageAdapter = new PendingImageAdapter(
                new PendingImageAdapter.OnImageActionListener() {
                    @Override
                    public void onImageClick(int position, Uri uri) {
                        showImagePreviewDialog(uri);
                    }

                    @Override
                    public void onRemoveClick(int position) {
                        pendingImageAdapter.removeImage(position);
                        updatePendingImagesVisibility();
                    }
                }
        );
        rvPendingImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rvPendingImages.setAdapter(pendingImageAdapter);
        rvPendingImages.setVisibility(View.GONE);
    }

    private void initChatHistory(String initialQuestion) {
        if (sessionId != -1) {
            if (!dbHelper.isSessionOwnedByUser(sessionId, userId)) {
                Toast.makeText(this, R.string.prog_chat_error_session_owner, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            loadChatHistory(sessionId);
        } else {
            sessionId = dbHelper.createNewSession(userId);
            if (sessionId == -1) {
                Toast.makeText(this, R.string.prog_chat_error_create_session, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String welcomeMessage = getString(R.string.prog_chat_welcome_format, language);
            dbHelper.insertMessage(sessionId, welcomeMessage, false, null);
            addMessage(welcomeMessage, false, null);
        }

        if (initialQuestion != null
                && !initialQuestion.trim().isEmpty()
                && !isFirstMessageSent) {
            isFirstMessageSent = true;
            edtQuestion.setText(initialQuestion.trim());
            edtQuestion.postDelayed(this::sendMessage, 300);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnUploadImage.setOnClickListener(v -> {
            if (isSending) {
                return;
            }
            if (pendingImageAdapter.isFull()) {
                Toast.makeText(this, R.string.prog_chat_limit_images, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            imagePickerLauncher.launch(intent);
        });

        btnSend.setOnClickListener(v -> sendMessage());
        edtQuestion.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void updatePendingImagesVisibility() {
        if (pendingImageAdapter.getImageCount() > 0) {
            rvPendingImages.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            rvPendingImages.startAnimation(fadeIn);
        } else {
            rvPendingImages.setVisibility(View.GONE);
        }
    }

    private void loadChatHistory(long targetSessionId) {
        try {
            List<ProgrammingMessage> history = dbHelper.getMessagesBySession(targetSessionId);
            messageList.clear();
            messageList.addAll(history);
            chatAdapter.notifyDataSetChanged();
            if (!messageList.isEmpty()) {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi tải lịch sử chat", e);
            Toast.makeText(this, R.string.prog_chat_error_load_history, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        if (isSending || sessionId == -1) {
            return;
        }

        String question = edtQuestion.getText().toString().trim();
        List<Uri> pendingUris = pendingImageAdapter.getImageUris();

        if (question.isEmpty() && pendingUris.isEmpty()) {
            Toast.makeText(this, R.string.prog_chat_input_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        List<byte[]> imageBytesList = new ArrayList<>();
        List<String> imageMimeTypes = new ArrayList<>();
        List<String> imageBase64List = new ArrayList<>();

        for (Uri uri : pendingUris) {
            byte[] imageBytes = convertUriToBytes(uri);
            if (imageBytes != null && imageBytes.length > 0) {
                imageBytesList.add(imageBytes);
                imageMimeTypes.add("image/jpeg");
                imageBase64List.add(Base64.encodeToString(imageBytes, Base64.NO_WRAP));
            }
        }

        if (!pendingUris.isEmpty() && imageBytesList.isEmpty()) {
            Toast.makeText(this, R.string.prog_chat_error_read_image, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasImages = !imageBytesList.isEmpty();
        String displayMessage = question;
        if (displayMessage.isEmpty() && hasImages) {
            displayMessage = getString(R.string.prog_chat_sent_images, imageBytesList.size());
        }

        String promptToSend = buildContextualPrompt(question, hasImages);
        List<byte[]> immutableImages = new ArrayList<>(imageBytesList);
        List<String> immutableMimeTypes = new ArrayList<>(imageMimeTypes);
        String savedDisplayMessage = displayMessage;

        long inserted = dbHelper.insertMessage(
                sessionId,
                savedDisplayMessage,
                true,
                imageBase64List
        );
        if (inserted == -1) {
            Toast.makeText(this, R.string.prog_chat_error_save_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        isSending = true;
        setSendingState(true);
        addMessage(savedDisplayMessage, true, imageBase64List);
        edtQuestion.setText("");
        pendingImageAdapter.clearAll();
        updatePendingImagesVisibility();

        new Thread(() -> {
            try {
                ChatGeminiApiClient.Result result = geminiClient.generateText(
                        promptToSend,
                        immutableImages,
                        immutableMimeTypes,
                        0.55,
                        4096
                );
                ChatGeminiApiClient.Result completed = continueIfNeeded(
                        result,
                        promptToSend
                );

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    isSending = false;
                    setSendingState(false);
                    handleApiResponse(completed, savedDisplayMessage);
                });
            } catch (Exception error) {
                Log.e(TAG, "Lỗi gọi AI Lập trình", error);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    isSending = false;
                    setSendingState(false);
                    addMessage(buildErrorMessage(error), false, null);
                });
            }
        }).start();
    }

    private String buildContextualPrompt(String question, boolean hasImages) {
        StringBuilder context = new StringBuilder();
        int start = Math.max(0, messageList.size() - MAX_CONTEXT_MESSAGES);

        for (int i = start; i < messageList.size(); i++) {
            ProgrammingMessage message = messageList.get(i);
            String content = message.getContent();
            if (content == null || content.trim().isEmpty()) {
                content = message.hasImages() ? "[IMAGE]" : "";
            }
            if (content.isEmpty()) {
                continue;
            }

            String line = (message.isUser() ? "USER: " : "AI: ")
                    + content.trim() + "\n";
            if (context.length() + line.length() > MAX_CONTEXT_CHARS) {
                int remaining = MAX_CONTEXT_CHARS - context.length();
                if (remaining > 0) {
                    context.append(line, 0, Math.min(remaining, line.length()));
                }
                break;
            }
            context.append(line);
        }

        String currentQuestion = question == null ? "" : question.trim();
        if (currentQuestion.isEmpty() && hasImages) {
            currentQuestion = "Analyze attached code/error images.";
        }

        StringBuilder prompt = new StringBuilder(getString(R.string.prog_chat_ai_prefix))
                .append("\nSelected Subject: ")
                .append(language)
                .append("\n\nRECENT CONTEXT:\n")
                .append(context.length() == 0 ? "(No previous context)\n" : context)
                .append("\nCURRENT REQUEST:\n")
                .append(currentQuestion);

        if (hasImages) {
            prompt.append("\n\n").append(getString(R.string.prog_chat_image_prompt));
        }
        return prompt.toString();
    }

    private ChatGeminiApiClient.Result continueIfNeeded(
            ChatGeminiApiClient.Result initial,
            String originalPrompt
    ) throws Exception {
        if (initial == null || !initial.wasTruncated()) {
            return initial;
        }

        String answer = initial.text;
        int promptTokens = initial.promptTokens;
        int candidateTokens = initial.candidateTokens;
        int totalTokens = initial.totalTokens;
        String finishReason = initial.finishReason;

        for (int i = 0; i < MAX_CONTINUATIONS
                && "MAX_TOKENS".equalsIgnoreCase(finishReason); i++) {
            String answerTail = answer.length() > 5_000
                    ? answer.substring(answer.length() - 5_000)
                    : answer;

            String continuationPrompt = getString(R.string.prog_chat_ai_prefix)
                    + "\n\nThis is the original request:\n"
                    + trimToLength(originalPrompt, 7_000)
                    + "\n\nThis is the end of the partial answer:\n"
                    + answerTail
                    + "\n\nPlease continue exactly from where it left off. "
                    + "Do not repeat the previous parts and do not restart the introduction.";

            ChatGeminiApiClient.Result continuation = geminiClient.generateText(
                    continuationPrompt,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.35,
                    4096
            );

            answer = mergeContinuation(answer, continuation.text);
            promptTokens += continuation.promptTokens;
            candidateTokens += continuation.candidateTokens;
            totalTokens += continuation.totalTokens;
            finishReason = continuation.finishReason;
        }

        return new ChatGeminiApiClient.Result(
                answer,
                promptTokens,
                candidateTokens,
                totalTokens,
                initial.status,
                finishReason
        );
    }

    private String mergeContinuation(String original, String continuation) {
        String left = original == null ? "" : original.trim();
        String right = continuation == null ? "" : continuation.trim();
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty() || left.endsWith(right)) {
            return left;
        }

        int maxOverlap = Math.min(600, Math.min(left.length(), right.length()));
        for (int overlap = maxOverlap; overlap >= 20; overlap--) {
            String leftEnd = left.substring(left.length() - overlap);
            String rightStart = right.substring(0, overlap);
            if (leftEnd.equals(rightStart)) {
                return left + right.substring(overlap);
            }
        }
        return left + "\n" + right;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(value.length() - maxLength);
    }

    private String buildErrorMessage(Exception error) {
        if (error instanceof GeminiApiException) {
            GeminiApiException geminiError = (GeminiApiException) error;
            int status = geminiError.getHttpStatus();
            if (status == 429) {
                return getString(R.string.prog_chat_error_generic);
            }
            return getString(R.string.prog_chat_error_api, status, geminiError.getMessage());
        }

        String message = error.getMessage();
        if (message != null && message.toLowerCase().contains("blocked")) {
            return getString(R.string.prog_chat_error_blocked);
        }
        if (error instanceof IllegalStateException && error.getCause() != null) {
            return getString(R.string.prog_chat_error_connect);
        }
        return getString(R.string.prog_chat_error_generic);
    }

    private void setSendingState(boolean sending) {
        pbChatLoading.setVisibility(sending ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!sending);
        btnUploadImage.setEnabled(!sending);
        edtQuestion.setEnabled(!sending);
        btnSend.setAlpha(sending ? 0.45f : 1f);
        btnUploadImage.setAlpha(sending ? 0.45f : 1f);
    }

    private void handleApiResponse(ChatGeminiApiClient.Result result, String question) {
        if (result == null || result.text == null || result.text.trim().isEmpty()) {
            addMessage(getString(R.string.prog_chat_ai_no_text), false, null);
            return;
        }

        String aiAnswer = result.text.trim();
        addMessage(aiAnswer, false, null);

        try {
            dbHelper.insertMessage(sessionId, aiAnswer, false, null);
            if (messageList.size() <= 4) {
                String sessionName = question == null ? getString(R.string.prog_chat_new_conv) : question.trim();
                if (sessionName.length() > 40) {
                    sessionName = sessionName.substring(0, 40) + "...";
                }
                if (!sessionName.isEmpty()) {
                    dbHelper.updateSessionName(sessionId, sessionName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi lưu câu trả lời", e);
        }

        if (result.wasTruncated()) {
            Toast.makeText(
                    this,
                    R.string.prog_chat_truncated,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private byte[] convertUriToBytes(Uri uri) {
        if (uri == null) {
            return null;
        }

        try (InputStream boundsStream = getContentResolver().openInputStream(uri)) {
            if (boundsStream == null) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(boundsStream, null, options);

            int targetSize = 1280;
            int sampleSize = 1;
            int maxDimension = Math.max(options.outWidth, options.outHeight);
            while (maxDimension / sampleSize > targetSize) {
                sampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = Math.max(1, sampleSize);
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            try (InputStream imageStream = getContentResolver().openInputStream(uri)) {
                if (imageStream == null) {
                    return null;
                }
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream, null, options);
                if (bitmap == null) {
                    return null;
                }

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);
                    bitmap.recycle();
                    return outputStream.toByteArray();
                }
            }
        } catch (OutOfMemoryError error) {
            Log.e(TAG, "Ảnh quá lớn", error);
            runOnUiThread(() -> Toast.makeText(
                    this,
                    R.string.prog_chat_error_large_image,
                    Toast.LENGTH_SHORT
            ).show());
            return null;
        } catch (Exception error) {
            Log.e(TAG, "Lỗi chuyển đổi ảnh", error);
            return null;
        }
    }

    private void addMessage(String text, boolean isUser, List<String> imageBase64List) {
        Runnable action = () -> {
            ProgrammingMessage message = new ProgrammingMessage(
                    text == null ? "" : text,
                    isUser,
                    imageBase64List
            );
            messageList.add(message);
            int position = messageList.size() - 1;
            chatAdapter.notifyItemInserted(position);
            recyclerView.scrollToPosition(position);
        };

        if (Thread.currentThread() == getMainLooper().getThread()) {
            action.run();
        } else {
            runOnUiThread(action);
        }
    }

    private void showImagePreviewDialog(String imageBase64) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            return;
        }
        try {
            byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    decodedBytes,
                    0,
                    decodedBytes.length
            );
            showImagePreviewDialog(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi decode ảnh", e);
        }
    }

    private void showImagePreviewDialog(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            showImagePreviewDialog(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi load ảnh", e);
        }
    }

    private void showImagePreviewDialog(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        if (previewDialog != null && previewDialog.isShowing()) {
            previewDialog.dismiss();
        }

        try {
            previewDialog = new Dialog(
                    this,
                    android.R.style.Theme_Black_NoTitleBar_Fullscreen
            );
            previewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            previewDialog.setContentView(R.layout.dialog_image_preview);

            Window window = previewDialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                window.setAttributes(layoutParams);
            }

            ImageView ivPreview = previewDialog.findViewById(R.id.iv_preview);
            ImageView ivClosePreview = previewDialog.findViewById(R.id.iv_close_preview);
            ivPreview.setImageBitmap(bitmap);
            ivClosePreview.setOnClickListener(v -> previewDialog.dismiss());
            previewDialog.setOnDismissListener(dialog -> previewDialog = null);
            previewDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi hiển thị preview", e);
        }
    }

    @Override
    protected void onDestroy() {
        if (previewDialog != null && previewDialog.isShowing()) {
            previewDialog.dismiss();
            previewDialog = null;
        }
        super.onDestroy();
    }
}
