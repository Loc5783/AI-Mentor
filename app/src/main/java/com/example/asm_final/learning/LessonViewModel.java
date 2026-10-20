package com.example.asm_final.learning;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.example.asm_final.backend.BackendClient;
import com.example.mentor.data.ActiveUser;
import java.util.UUID;
import java.util.concurrent.*;
import org.json.*;

/** Retains network work across rotation; durable lesson progress is owned by the server. */
public final class LessonViewModel extends AndroidViewModel {
  private final SavedStateHandle saved;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
  private BackendClient api;
  private int owner;
  private String subject;
  private String enrollKey;
  private int grade, version;
  private boolean closed;
  public boolean busy, needsLogin;
  public String error;
  public JSONObject path, attempt, completion;

  public LessonViewModel(Application app, SavedStateHandle saved) {
    super(app);
    this.saved = saved;
  }

  public LiveData<Integer> changes() {
    return changes;
  }

  public int owner() {
    return owner;
  }

  public int grade() {
    return grade;
  }

  public BackendClient api() {
    return api;
  }

  private void emit() {
    if (!closed) changes.setValue(++version);
  }

  public void bind(int account, String subject, int grade) {
    if (api != null) return;
    this.owner = account;
    this.subject = subject;
    this.grade = grade;
    api = new BackendClient(getApplication(), account);
    enrollKey = stableKey("enrollKey");
    String restoredAttempt = saved.get("attemptId");
    if (!api.configured()) {
      error = "BACKEND_NOT_CONFIGURED";
      emit();
      return;
    }
    task(
        () -> {
          if (!api.signedIn()) throw new BackendClient.Failure("AUTHENTICATION_REQUIRED", 401);
          String id = restoredAttempt;
          if (id != null) return api.request("GET", "/attempts/" + id, null, null, null);
          return fetchPath();
        },
        value -> {
          if (value.has("questions")) attempt = value;
          else path = value;
        });
  }

  private interface Accepted {
    void accept(JSONObject value) throws Exception;
  }

  private void task(Callable<JSONObject> work, Accepted accept) {
    if (busy || closed) return;
    busy = true;
    error = null;
    emit();
    worker.execute(
        () -> {
          try {
            JSONObject result = work.call();
            main.post(
                () -> {
                  if (closed || ActiveUser.get(getApplication()) != owner) return;
                  busy = false;
                  needsLogin = false;
                  try {
                    accept.accept(result);
                  } catch (Exception failure) {
                    error = "INVALID_SERVER_RESPONSE";
                  }
                  emit();
                });
          } catch (Exception failure) {
            main.post(
                () -> {
                  if (closed || ActiveUser.get(getApplication()) != owner) return;
                  busy = false;
                  error =
                      failure instanceof BackendClient.Failure
                          ? ((BackendClient.Failure) failure).code
                          : "NETWORK_ERROR";
                  needsLogin =
                      failure instanceof BackendClient.Failure
                          && (((BackendClient.Failure) failure).status == 401
                              || "ACCOUNT_LINK_REQUIRED"
                                  .equals(((BackendClient.Failure) failure).code));
                  emit();
                });
          }
        });
  }

  public void login(String identifier, String password) {
    task(
        () -> {
          api.signIn(identifier, password);
          return fetchPath();
        },
        result -> {
          path = result;
          needsLogin = false;
        });
  }

  private JSONObject fetchPath() throws Exception {
    JSONObject enrollment =
        api.request(
            "POST",
            "/enrollments",
            new JSONObject().put("subjectId", subject).put("grade", grade),
            enrollKey,
            null);
    return api.request(
        "GET", "/enrollments/" + enrollment.getString("id") + "/path", null, null, null);
  }

  private String stableKey(String name) {
    String key = saved.get(name);
    if (key == null) {
      key = UUID.randomUUID().toString();
      saved.set(name, key);
    }
    return key;
  }

  public void reload() {
    if (api == null || !api.configured()) return;
    if (attempt != null) {
      String id = attempt.optString("id");
      task(() -> api.request("GET", "/attempts/" + id, null, null, null), value -> attempt = value);
    } else task(this::fetchPath, value -> path = value);
  }

  public void start(String unit, boolean placement) {
    if (path == null) return;
    String requestKey = UUID.randomUUID().toString();
    task(
        () -> {
          JSONObject enrollment = path.getJSONObject("enrollment");
          JSONObject body = new JSONObject().put("enrollmentId", enrollment.getString("id"));
          if (!placement)
            body.put("lessonId", unit)
                .put("curriculumVersion", path.getString("curriculumVersion"));
          return api.request(
              "POST", placement ? "/placements" : "/attempts", body, requestKey, null);
        },
        result -> {
          attempt = result;
          completion = null;
          saved.set("attemptId", result.getString("id"));
        });
  }

  public void resume(String id) {
    task(
        () -> api.request("GET", "/attempts/" + id, null, null, null),
        result -> {
          attempt = result;
          completion = null;
          saved.set("attemptId", id);
        });
  }

  public void save(int step, String questionId, String selected) {
    if (attempt == null || busy || hasPending()) return;
    try {
      JSONArray answers = new JSONArray();
      if (questionId != null)
        answers.put(
            new JSONObject()
                .put("questionId", questionId)
                .put("selectedOptionId", selected)
                .put("submissionId", UUID.randomUUID().toString()));
      JSONObject body = new JSONObject().put("step", step).put("answers", answers);
      saved.set("pendingMethod", "PATCH");
      saved.set("pendingPath", "/attempts/" + attempt.getString("id"));
      saved.set("pendingBody", body.toString());
      saved.set("pendingRevision", attempt.getInt("revision"));
      saved.set("pendingKey", UUID.randomUUID().toString());
      retry();
    } catch (JSONException invalid) {
      error = "INVALID_SERVER_RESPONSE";
      emit();
    }
  }

  public void complete() {
    if (attempt == null || busy || hasPending()) return;
    saved.set("pendingMethod", "POST");
    saved.set("pendingPath", "/completions");
    saved.set("pendingBody", "{\"attemptId\":\"" + attempt.optString("id") + "\"}");
    saved.set("pendingKey", stableKey("complete_" + attempt.optString("id")));
    saved.set("pendingRevision", null);
    retry();
  }

  public boolean hasPending() {
    return saved.get("pendingPath") != null;
  }

  public void retry() {
    if (!hasPending()) {
      reload();
      return;
    }
    String method = saved.get("pendingMethod"),
        route = saved.get("pendingPath"),
        body = saved.get("pendingBody"),
        key = saved.get("pendingKey");
    Integer revision = saved.get("pendingRevision");
    task(
        () -> {
          try {
            return api.request(method, route, new JSONObject(body), key, revision);
          } catch (BackendClient.Failure conflict) {
            if (conflict.status != 412 || attempt == null) throw conflict;
            return api.request("GET", "/attempts/" + attempt.getString("id"), null, null, null);
          }
        },
        result -> {
          saved.set("pendingPath", null);
          if (result.has("xpAwarded")) completion = result;
          else attempt = result;
        });
  }

  public void backToPath() {
    if (busy || hasPending()) return;
    attempt = null;
    completion = null;
    saved.set("attemptId", null);
    task(this::fetchPath, result -> path = result);
  }

  public boolean animateCompletion() {
    if (completion == null) return false;
    String id = completion.optString("id");
    if (id.equals(saved.get("celebrated"))) return false;
    saved.set("celebrated", id);
    return true;
  }

  @Override
  protected void onCleared() {
    closed = true;
    worker.shutdownNow();
  }
}
