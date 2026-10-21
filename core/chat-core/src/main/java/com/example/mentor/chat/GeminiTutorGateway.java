package com.example.mentor.chat;

import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Direct Gemini adapter: retains local key configuration while accepting complete chat history. */
public final class GeminiTutorGateway implements TutorGateway {
  public interface Transport {
    Response post(String key, String model, String json) throws IOException;
  }

  public static final class Response {
    public final int status;
    public final String body;

    public Response(int status, String body) {
      this.status = status;
      this.body = body;
    }
  }

  private final GeminiKeyPool pool;
  private final String model;
  private final Transport transport;

  public static TutorGateway configuredClient() {
    return fromConfiguration(
        BuildConfig.CHAT_GEMINI_MODEL,
        BuildConfig.CHAT_GEMINI_API_KEY_1,
        BuildConfig.CHAT_GEMINI_API_KEY_2,
        BuildConfig.CHAT_GEMINI_API_KEY_3);
  }

  /** Invalid optional chat configuration must not prevent the rest of the app from starting. */
  public static TutorGateway fromConfiguration(String model, String... keys) {
    try {
      return new GeminiTutorGateway(new GeminiKeyPool(keys), model, new HttpTransport());
    } catch (IllegalArgumentException invalid) {
      return new TutorGateway() {
        @Override
        public boolean configured() {
          return false;
        }

        @Override
        public TutorReply reply(TutorRequest request) throws TutorFailure {
          throw new TutorFailure(TutorFailure.Code.INVALID_CONFIGURATION);
        }
      };
    }
  }

  public GeminiTutorGateway(GeminiKeyPool pool, String model, Transport transport) {
    if (pool == null
        || transport == null
        || model == null
        || !model.trim().matches("[a-zA-Z0-9._-]+"))
      throw new IllegalArgumentException("Invalid Gemini configuration");
    this.pool = pool;
    this.model = model.trim();
    this.transport = transport;
  }

  @Override
  public boolean configured() {
    return pool.configured();
  }

  @Override
  public TutorReply reply(TutorRequest request) throws TutorFailure {
    if (!configured()) throw new TutorFailure(TutorFailure.Code.NOT_CONFIGURED);
    if (request == null) throw new TutorFailure(TutorFailure.Code.INVALID_REQUEST);
    final String payload;
    try {
      payload = payload(request).toString();
    } catch (JSONException malformed) {
      throw new TutorFailure(TutorFailure.Code.INVALID_REQUEST);
    }
    TutorFailure.Code last = TutorFailure.Code.RATE_LIMITED;
    for (int attempt = 0; attempt < 6; attempt++) {
      if (Thread.currentThread().isInterrupted())
        throw new TutorFailure(TutorFailure.Code.CANCELLED);
      int slot = pool.next();
      if (slot < 0) throw new TutorFailure(last);
      final Response response;
      try {
        response = transport.post(pool.key(slot), model, payload);
      } catch (IOException disconnected) {
        throw new TutorFailure(
            Thread.currentThread().isInterrupted()
                ? TutorFailure.Code.CANCELLED
                : TutorFailure.Code.NETWORK);
      }
      if (Thread.currentThread().isInterrupted())
        throw new TutorFailure(TutorFailure.Code.CANCELLED);
      if (response.status >= 200 && response.status < 300) return parse(response.body);
      if (response.status == 401 || response.status == 403 || response.status == 429) {
        pool.reject(slot, response.status);
        last =
            response.status == 429
                ? TutorFailure.Code.RATE_LIMITED
                : TutorFailure.Code.UNAUTHORIZED;
        continue;
      }
      // Do not automatically replay a potentially charged generation after a network/server
      // failure.
      throw new TutorFailure(
          response.status >= 500 ? TutorFailure.Code.PROVIDER : TutorFailure.Code.INVALID_REQUEST);
    }
    throw new TutorFailure(last);
  }

  private JSONObject payload(TutorRequest request) throws JSONException {
    JSONArray contents = new JSONArray();
    for (TutorTurn turn : request.turns) {
      JSONArray parts = new JSONArray();
      if (!turn.text.isEmpty()) parts.put(new JSONObject().put("text", turn.text));
      for (TutorAttachment attachment : turn.attachments)
        parts.put(
            new JSONObject()
                .put(
                    "inlineData",
                    new JSONObject()
                        .put("mimeType", attachment.mimeType)
                        .put("data", Base64.encodeToString(attachment.bytes(), Base64.NO_WRAP))));
      contents.put(
          new JSONObject()
              .put("role", turn.role == TutorTurn.Role.USER ? "user" : "model")
              .put("parts", parts));
    }
    return new JSONObject()
        .put(
            "systemInstruction",
            new JSONObject()
                .put(
                    "parts",
                    new JSONArray()
                        .put(
                            new JSONObject()
                                .put("text", TutorInstructions.forLearner(request.context)))))
        .put("contents", contents)
        .put(
            "generationConfig",
            new JSONObject().put("temperature", 0.35).put("maxOutputTokens", 4096));
  }

  private TutorReply parse(String body) throws TutorFailure {
    try {
      JSONArray candidates = new JSONObject(body).optJSONArray("candidates");
      if (candidates == null || candidates.length() == 0)
        throw new TutorFailure(TutorFailure.Code.EMPTY_RESPONSE);
      JSONObject candidate = candidates.getJSONObject(0);
      String finish = candidate.optString("finishReason", "");
      if (!finish.isEmpty() && !"STOP".equals(finish) && !"MAX_TOKENS".equals(finish))
        throw new TutorFailure(TutorFailure.Code.EMPTY_RESPONSE);
      JSONObject content = candidate.optJSONObject("content");
      JSONArray parts = content == null ? null : content.optJSONArray("parts");
      StringBuilder answer = new StringBuilder();
      if (parts != null)
        for (int i = 0; i < parts.length(); i++) {
          JSONObject part = parts.getJSONObject(i);
          // Thought/internal metadata is not an assistant answer.
          if (!part.optBoolean("thought", false)) answer.append(part.optString("text", ""));
        }
      if (answer.toString().trim().isEmpty())
        throw new TutorFailure(TutorFailure.Code.EMPTY_RESPONSE);
      return new TutorReply(answer.toString(), "MAX_TOKENS".equals(finish));
    } catch (JSONException malformed) {
      throw new TutorFailure(TutorFailure.Code.PROVIDER);
    }
  }

  private static final class HttpTransport implements Transport {
    @Override
    public Response post(String key, String model, String json) throws IOException {
      HttpURLConnection connection =
          (HttpURLConnection)
              new URL(
                      "https://generativelanguage.googleapis.com/v1beta/models/"
                          + model
                          + ":generateContent")
                  .openConnection();
      try {
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(false);
        connection.setDoOutput(true);
        connection.setRequestProperty("x-goog-api-key", key);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream output = connection.getOutputStream()) {
          output.write(bytes);
        }
        int code = connection.getResponseCode();
        InputStream input =
            code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (input == null) return new Response(code, "");
        try (InputStream source = input;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
          byte[] chunk = new byte[8192];
          int count;
          while ((count = source.read(chunk)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            if (buffer.size() + count > 2 * 1024 * 1024)
              throw new IOException("Response too large");
            buffer.write(chunk, 0, count);
          }
          return new Response(code, buffer.toString(StandardCharsets.UTF_8.name()));
        }
      } finally {
        connection.disconnect();
      }
    }
  }
}
