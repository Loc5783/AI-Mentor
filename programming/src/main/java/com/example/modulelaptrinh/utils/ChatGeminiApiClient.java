package com.example.modulelaptrinh.utils;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatGeminiApiClient {
    // API keys are loaded from local Gradle properties.
    private static final String API_KEY_1 = com.example.modulelaptrinh.BuildConfig.PROGRAMMING_CHAT_GEMINI_API_KEY_1;
    private static final String API_KEY_2 = com.example.modulelaptrinh.BuildConfig.PROGRAMMING_CHAT_GEMINI_API_KEY_2;
    private static final String API_KEY_3 = com.example.modulelaptrinh.BuildConfig.PROGRAMMING_CHAT_GEMINI_API_KEY_3;

    private static final String[] CONFIGURED_KEYS = buildKeyList();
    private static final long[] DISABLED_UNTIL = new long[CONFIGURED_KEYS.length];
    private static int currentKeyIndex;
    private static String currentModel = "gemini-flash-latest";
    private static String currentStatus = "GOOD";

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 120_000;
    private static final int MAX_ATTEMPTS = 6;

    public static final class Result {
        public final String text;
        public final int promptTokens;
        public final int candidateTokens;
        public final int totalTokens;
        public final String status;
        public final String finishReason;

        public Result(String text, int promptTokens, int candidateTokens, int totalTokens,
                      String status, String finishReason) {
            this.text = text == null ? "" : text.trim();
            this.promptTokens = Math.max(0, promptTokens);
            this.candidateTokens = Math.max(0, candidateTokens);
            this.totalTokens = Math.max(0, totalTokens);
            this.status = status == null ? "" : status.trim();
            this.finishReason = finishReason == null ? "" : finishReason.trim();
        }

        public boolean wasTruncated() {
            return "MAX_TOKENS".equalsIgnoreCase(finishReason);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public String getModelName() {
        return currentModel;
    }

    public void setModelName(String modelName) {
        if (modelName != null && !modelName.trim().isEmpty()) {
            currentModel = modelName.trim();
        }
    }

    public String getStatus() {
        return currentStatus;
    }

    public synchronized String getActiveKeyLabel() {
        return CONFIGURED_KEYS.length == 0
                ? "no key"
                : "key " + (currentKeyIndex + 1) + "/" + CONFIGURED_KEYS.length;
    }

    public boolean isConfigured() {
        return CONFIGURED_KEYS.length > 0;
    }

    public Result testConnection() throws Exception {
        // FIX: Sử dụng empty lists thay vì null để tránh ambiguous
        return generateText("Reply with exactly: OK", Collections.emptyList(), Collections.emptyList(), 0.0, 32);
    }

    public Result generateText(String prompt, byte[] imageBytes, String imageMimeType,
                               double temperature, int maxOutputTokens) throws Exception {
        List<byte[]> images = imageBytes == null
                ? Collections.emptyList()
                : Collections.singletonList(imageBytes);
        List<String> mimeTypes = imageBytes == null
                ? Collections.emptyList()
                : Collections.singletonList(imageMimeType);
        return generateText(prompt, images, mimeTypes, temperature, maxOutputTokens);
    }

    public Result generateText(String prompt, List<byte[]> imageBytesList,
                               List<String> imageMimeTypes, double temperature,
                               int maxOutputTokens) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("No Gemini API key is configured.");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty.");
        }

        List<byte[]> safeImages = imageBytesList == null
                ? Collections.emptyList()
                : imageBytesList;
        List<String> safeMimeTypes = imageMimeTypes == null
                ? Collections.emptyList()
                : imageMimeTypes;

        Exception lastError = null;
        int maximumAttempts = Math.max(MAX_ATTEMPTS, CONFIGURED_KEYS.length * 2);

        for (int attempt = 0; attempt < maximumAttempts; attempt++) {
            int keyIndex = nextAvailableKeyIndex();
            if (keyIndex < 0) {
                long wait = shortestCooldown();
                if (wait > 0 && wait <= 5_000L) {
                    Thread.sleep(wait);
                    continue;
                }
                break;
            }

            HttpURLConnection connection = null;
            try {
                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + currentModel + ":generateContent";
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("x-goog-api-key", CONFIGURED_KEYS[keyIndex]);

                JSONObject request = buildRequest(
                        prompt.trim(),
                        safeImages,
                        safeMimeTypes,
                        temperature,
                        maxOutputTokens
                );
                byte[] requestBytes = request.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(requestBytes.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBytes);
                }

                int statusCode = connection.getResponseCode();
                String responseBody = readResponse(connection, statusCode);

                if (statusCode >= 200 && statusCode < 300) {
                    synchronized (ChatGeminiApiClient.class) {
                        currentKeyIndex = keyIndex;
                        DISABLED_UNTIL[keyIndex] = 0L;
                    }
                    currentStatus = attempt == 0 ? "GOOD" : "MEDIUM";
                    return parseFullResponse(responseBody);
                }

                GeminiApiException apiError = createApiException(statusCode, responseBody);
                lastError = apiError;
                currentStatus = "BUSY";

                if (statusCode == 401 || statusCode == 403) {
                    disableKey(keyIndex, Long.MAX_VALUE);
                    continue;
                }
                if (statusCode == 429) {
                    disableKey(keyIndex, System.currentTimeMillis() + 60_000L);
                    continue;
                }
                if (statusCode >= 500 && statusCode <= 599) {
                    Thread.sleep(Math.min(8_000L, 1_000L * (attempt + 1L)));
                    continue;
                }
                throw apiError;
            } catch (IOException networkError) {
                lastError = networkError;
                currentStatus = "BUSY";
                if (attempt + 1 < maximumAttempts) {
                    Thread.sleep(Math.min(4_000L, 750L * (attempt + 1L)));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        if (lastError instanceof GeminiApiException) {
            throw lastError;
        }
        throw new IllegalStateException(
                "Gemini request failed after multiple attempts.",
                lastError
        );
    }

    private static String[] buildKeyList() {
        List<String> values = new ArrayList<>();
        addUniqueKey(values, API_KEY_1);
        addUniqueKey(values, API_KEY_2);
        addUniqueKey(values, API_KEY_3);
        return values.toArray(new String[0]);
    }

    private static void addUniqueKey(List<String> keys, String value) {
        if (value == null) {
            return;
        }
        String key = value.trim();
        if (!key.isEmpty() && !keys.contains(key)) {
            keys.add(key);
        }
    }

    private static synchronized int nextAvailableKeyIndex() {
        if (CONFIGURED_KEYS.length == 0) {
            return -1;
        }
        long now = System.currentTimeMillis();
        for (int offset = 0; offset < CONFIGURED_KEYS.length; offset++) {
            int index = (currentKeyIndex + offset) % CONFIGURED_KEYS.length;
            if (DISABLED_UNTIL[index] <= now) {
                currentKeyIndex = index;
                return index;
            }
        }
        return -1;
    }

    private static synchronized void disableKey(int index, long until) {
        if (index < 0 || index >= DISABLED_UNTIL.length) {
            return;
        }
        DISABLED_UNTIL[index] = until;
        if (CONFIGURED_KEYS.length > 0) {
            currentKeyIndex = (index + 1) % CONFIGURED_KEYS.length;
        }
    }

    private static synchronized long shortestCooldown() {
        long now = System.currentTimeMillis();
        long shortest = Long.MAX_VALUE;
        for (long value : DISABLED_UNTIL) {
            if (value > now && value != Long.MAX_VALUE) {
                shortest = Math.min(shortest, value - now);
            }
        }
        return shortest == Long.MAX_VALUE ? -1L : shortest;
    }

    private Result parseFullResponse(String responseBody) throws Exception {
        JSONObject root = new JSONObject(responseBody);
        StringBuilder answer = new StringBuilder();
        String finishReason = "";

        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates != null && candidates.length() > 0) {
            JSONObject firstCandidate = candidates.optJSONObject(0);
            if (firstCandidate != null) {
                finishReason = firstCandidate.optString("finishReason", "");
                JSONObject content = firstCandidate.optJSONObject("content");
                JSONArray parts = content == null ? null : content.optJSONArray("parts");
                if (parts != null) {
                    for (int i = 0; i < parts.length(); i++) {
                        JSONObject part = parts.optJSONObject(i);
                        String text = part == null ? "" : part.optString("text", "");
                        if (!text.isEmpty()) {
                            if (answer.length() > 0) {
                                answer.append('\n');
                            }
                            answer.append(text);
                        }
                    }
                }
            }
        }

        String text = answer.toString().trim();
        if (text.isEmpty()) {
            JSONObject feedback = root.optJSONObject("promptFeedback");
            String reason = feedback == null ? "" : feedback.optString("blockReason", "");
            if (!reason.isEmpty()) {
                throw new IllegalStateException("Gemini blocked the request: " + reason);
            }
            throw new IllegalStateException("Gemini returned an empty answer.");
        }

        JSONObject usage = root.optJSONObject("usageMetadata");
        int prompt = usage == null ? 0 : usage.optInt("promptTokenCount", 0);
        int candidate = usage == null ? 0 : usage.optInt("candidatesTokenCount", 0);
        int total = usage == null ? 0 : usage.optInt("totalTokenCount", 0);
        return new Result(text, prompt, candidate, total, currentStatus, finishReason);
    }

    private JSONObject buildRequest(String prompt, List<byte[]> imageBytesList,
                                    List<String> imageMimeTypes, double temperature,
                                    int maxOutputTokens) throws Exception {
        JSONArray parts = new JSONArray();

        for (int i = 0; i < imageBytesList.size(); i++) {
            byte[] image = imageBytesList.get(i);
            if (image == null || image.length == 0) {
                continue;
            }
            String mimeType = i < imageMimeTypes.size()
                    ? imageMimeTypes.get(i)
                    : null;
            if (mimeType == null || mimeType.trim().isEmpty()) {
                mimeType = "image/jpeg";
            }

            JSONObject inlineData = new JSONObject()
                    .put("mime_type", mimeType.trim())
                    .put("data", Base64.encodeToString(image, Base64.NO_WRAP));
            parts.put(new JSONObject().put("inline_data", inlineData));
        }

        parts.put(new JSONObject().put("text", prompt));

        JSONObject userContent = new JSONObject()
                .put("role", "user")
                .put("parts", parts);
        JSONObject generationConfig = new JSONObject()
                .put("temperature", Math.max(0.0, Math.min(1.0, temperature)))
                .put("maxOutputTokens", Math.max(32, Math.min(8192, maxOutputTokens)));

        return new JSONObject()
                .put("contents", new JSONArray().put(userContent))
                .put("generationConfig", generationConfig);
    }

    private String readResponse(HttpURLConnection connection, int statusCode) throws Exception {
        InputStream stream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }

    private GeminiApiException createApiException(int statusCode, String responseBody) {
        String message = responseBody == null ? "" : responseBody.trim();
        String apiStatus = "";
        try {
            JSONObject error = new JSONObject(message).optJSONObject("error");
            if (error != null) {
                message = error.optString("message", message);
                apiStatus = error.optString("status", "");
            }
        } catch (Exception ignored) {
        }
        return new GeminiApiException(statusCode, apiStatus, message);
    }
}