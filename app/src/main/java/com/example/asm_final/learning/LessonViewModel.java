package com.example.asm_final.learning;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.example.asm_final.backend.BackendClient;
import com.example.mentor.data.ActiveUser;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.IntSupplier;
import org.json.*;

/** Retains network work across rotation; durable lesson progress is owned by the server. */
public final class LessonViewModel extends AndroidViewModel {
  public interface LessonApiClient {
    boolean configured();
    boolean signedIn() throws Exception;
    JSONObject request(String method, String path, JSONObject body, String key, Integer revision) throws Exception;
    void signIn(String identifier, String password) throws Exception;
  }

  public interface LessonApiClientFactory {
    LessonApiClient create(int account);
  }

  private static final class TaskContext {
    final long generation;
    final int owner;
    final String subject;
    final int grade;
    final LessonApiClient client;
    final String scopeKey;

    TaskContext(
        long generation,
        int owner,
        String subject,
        int grade,
        LessonApiClient client,
        String scopeKey) {
      this.generation = generation;
      this.owner = owner;
      this.subject = subject;
      this.grade = grade;
      this.client = client;
      this.scopeKey = scopeKey;
    }
  }

  private final SavedStateHandle saved;
  private final Executor worker;
  private final Executor main;
  private final IntSupplier activeUser;
  private final LessonApiClientFactory apiFactory;
  private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);

  private LessonApiClient api;
  private int owner;
  private String subject;
  private String enrollKey;
  private int grade, version;
  private long generation = 0;
  private boolean closed;
  public boolean busy, needsLogin;
  public String error;
  public JSONObject path, attempt, completion;

  public LessonViewModel(Application app, SavedStateHandle saved) {
    this(
        app,
        saved,
        (LessonApiClientFactory) null,
        Executors.newSingleThreadExecutor(),
        new Handler(Looper.getMainLooper())::post,
        app != null ? () -> ActiveUser.get(app) : null);
  }

  public LessonViewModel(
      Application app,
      SavedStateHandle saved,
      LessonApiClient api,
      Executor worker,
      Executor main) {
    this(
        app,
        saved,
        api != null ? acc -> api : null,
        worker,
        main,
        app != null ? () -> ActiveUser.get(app) : null);
  }

  public LessonViewModel(
      Application app,
      SavedStateHandle saved,
      LessonApiClient api,
      Executor worker,
      Executor main,
      IntSupplier activeUser) {
    this(app, saved, api != null ? acc -> api : null, worker, main, activeUser);
  }

  public LessonViewModel(
      Application app,
      SavedStateHandle saved,
      LessonApiClientFactory apiFactory,
      Executor worker,
      Executor main,
      IntSupplier activeUser) {
    super(app);
    this.saved = saved;
    this.apiFactory = apiFactory;
    this.worker = worker;
    this.main = main;
    this.activeUser = activeUser;
  }

  public LiveData<Integer> changes() {
    return changes;
  }

  public int owner() {
    return owner;
  }

  public String subject() {
    return subject;
  }

  public int grade() {
    return grade;
  }

  public LessonApiClient api() {
    return api;
  }

  private void emit() {
    if (!closed) {
      changes.setValue(++version);
    }
  }

  private int currentActiveUser() {
    return activeUser != null
        ? activeUser.getAsInt()
        : (getApplication() != null ? ActiveUser.get(getApplication()) : owner);
  }

  public static String normalizeSubject(String subject) {
    return subject == null ? "" : subject.trim().toLowerCase();
  }

  public static String scopeKey(int owner, String subject, int grade) {
    return owner + ":" + normalizeSubject(subject) + ":" + grade;
  }

  private String currentScopeKey() {
    return scopeKey(owner, subject, grade);
  }

  private String getScoped(String key) {
    return saved.get(currentScopeKey() + ":" + key);
  }

  private String candidateIdsKey(int owner) {
    return "legacy_candidates:" + owner;
  }

  private String candidateSubjKey(int owner, String attemptId) {
    return "legacy_candidate_subj:" + owner + ":" + attemptId;
  }

  private String candidateGradeKey(int owner, String attemptId) {
    return "legacy_candidate_grade:" + owner + ":" + attemptId;
  }

  private synchronized void preserveLegacyStateIfPresent(int owner) {
    String flatAttempt = saved.get("attemptId");
    if (flatAttempt != null && !flatAttempt.trim().isEmpty()) {
      Integer flatOwner = saved.get("savedOwner");
      int effectiveOwner = flatOwner != null ? flatOwner : owner;
      String flatSubj = saved.get("savedSubject");
      Integer flatGrade = saved.get("savedGrade");

      boolean alreadyScoped = false;
      if (flatSubj != null && flatGrade != null && flatGrade > 0) {
        String scopedKey = scopeKey(effectiveOwner, flatSubj, flatGrade) + ":attemptId";
        if (flatAttempt.equals(saved.get(scopedKey))) {
          alreadyScoped = true;
        }
      }

      if (!alreadyScoped) {
        addLegacyCandidate(effectiveOwner, flatAttempt, flatSubj, flatGrade);
      }
    }
  }

  private synchronized void addLegacyCandidate(int owner, String attemptId, String subj, Integer grade) {
    if (attemptId == null || attemptId.trim().isEmpty()) return;
    try {
      String jsonStr = saved.get(candidateIdsKey(owner));
      JSONArray arr = jsonStr != null ? new JSONArray(jsonStr) : new JSONArray();
      boolean exists = false;
      for (int i = 0; i < arr.length(); i++) {
        if (attemptId.equals(arr.optString(i))) {
          exists = true;
          break;
        }
      }
      if (!exists) {
        arr.put(attemptId);
        saved.set(candidateIdsKey(owner), arr.toString());
      }
      if (subj != null) {
        saved.set(candidateSubjKey(owner, attemptId), subj);
      }
      if (grade != null && grade > 0) {
        saved.set(candidateGradeKey(owner, attemptId), grade);
      }
    } catch (Exception ignored) {
    }
  }

  private synchronized void removeLegacyCandidate(int owner, String attemptId) {
    if (attemptId == null) return;
    try {
      String jsonStr = saved.get(candidateIdsKey(owner));
      if (jsonStr == null) return;
      JSONArray arr = new JSONArray(jsonStr);
      JSONArray newArr = new JSONArray();
      for (int i = 0; i < arr.length(); i++) {
        String id = arr.optString(i);
        if (!attemptId.equals(id)) {
          newArr.put(id);
        }
      }
      saved.set(candidateIdsKey(owner), newArr.toString());
      saved.set(candidateSubjKey(owner, attemptId), null);
      saved.set(candidateGradeKey(owner, attemptId), null);
    } catch (Exception ignored) {
    }
  }

  private synchronized String findLegacyCandidateForScope(int owner, String subject, int grade) {
    try {
      String jsonStr = saved.get(candidateIdsKey(owner));
      if (jsonStr == null) return null;
      JSONArray arr = new JSONArray(jsonStr);
      String unverifiedCandidate = null;

      for (int i = 0; i < arr.length(); i++) {
        String id = arr.optString(i);
        if (id.isEmpty()) continue;
        String subj = saved.get(candidateSubjKey(owner, id));
        Integer g = saved.get(candidateGradeKey(owner, id));

        if (subj != null && g != null && g > 0) {
          if (subj.equalsIgnoreCase(subject) && g == grade) {
            return id;
          }
        } else if (subj == null && g == null) {
          if (unverifiedCandidate == null) {
            unverifiedCandidate = id;
          }
        }
      }
      return unverifiedCandidate;
    } catch (Exception ignored) {
      return null;
    }
  }

  private void setScoped(String key, String value) {
    if ("attemptId".equals(key)) {
      preserveLegacyStateIfPresent(this.owner);
      saved.set("attemptId", value);
      saved.set("savedSubject", this.subject);
      saved.set("savedGrade", this.grade);
      saved.set("savedOwner", this.owner);
    }
    saved.set(currentScopeKey() + ":" + key, value);
  }

  private Integer getScopedInt(String key) {
    return saved.get(currentScopeKey() + ":" + key);
  }

  private void setScopedInt(String key, Integer value) {
    saved.set(currentScopeKey() + ":" + key, value);
  }

  private TaskContext snapshotContext() {
    return new TaskContext(generation, owner, subject, grade, api, currentScopeKey());
  }

  public void bind(int account, String subject, int grade) {
    String normSubject = normalizeSubject(subject);

    // 1. Cùng owner/subject/grade: idempotent, không reset tiến độ hay tạo duplicate request
    if (this.owner == account
        && Objects.equals(this.subject, normSubject)
        && this.grade == grade
        && (this.attempt != null || this.path != null || this.busy)) {
      return;
    }

    // Bảo toàn legacy state nếu có trước khi chuyển scope
    preserveLegacyStateIfPresent(account);

    // 2. Khác scope (owner, subject hoặc grade): vô hiệu hóa task cũ bằng generation mới
    this.generation++;
    this.busy = false;
    this.attempt = null;
    this.path = null;
    this.completion = null;
    this.error = null;
    this.needsLogin = false;

    this.owner = account;
    this.subject = normSubject;
    this.grade = grade;

    if (this.apiFactory != null) {
      this.api = this.apiFactory.create(account);
    } else if (getApplication() != null) {
      this.api = new BackendClient(getApplication(), account);
    }

    enrollKey = stableKey(currentScopeKey() + ":enrollKey");

    // Lấy attempt đã lưu trong đúng scope (owner:subject:grade)
    String candidateAttempt = getScoped("attemptId");
    boolean unverifiedLegacy = false;

    if (candidateAttempt == null) {
      // 1. Tìm candidate từ kho lưu legacy riêng biệt đã được bảo toàn
      String legacyCandidate = findLegacyCandidateForScope(this.owner, this.subject, this.grade);
      if (legacyCandidate != null) {
        candidateAttempt = legacyCandidate;
        String knownSubj = saved.get(candidateSubjKey(this.owner, legacyCandidate));
        Integer knownGrade = saved.get(candidateGradeKey(this.owner, legacyCandidate));
        if (knownSubj == null || knownGrade == null || knownGrade == 0) {
          unverifiedLegacy = true;
        }
      } else {
        // 2. Fallback xử lý flat attemptId ban đầu nếu có
        String legacyAttempt = saved.get("attemptId");
        if (legacyAttempt != null) {
          Integer legacyOwner = saved.get("savedOwner");
          String legacySubject = saved.get("savedSubject");
          Integer legacyGrade = saved.get("savedGrade");

          boolean ownerMatch = legacyOwner == null || legacyOwner == account;
          if (ownerMatch) {
            if (legacySubject != null && !legacySubject.equalsIgnoreCase(normSubject)) {
              // Môn legacy không khớp với môn đang bind -> không nhận vào scope này
            } else if (legacyGrade != null && legacyGrade != grade) {
              // Lớp legacy không khớp với lớp đang bind -> không nhận vào scope này
            } else if (legacySubject != null && legacyGrade != null) {
              // Đã có metadata môn/lớp và khớp hoàn toàn
              candidateAttempt = legacyAttempt;
            } else {
              // Chưa có metadata môn/lớp trong saved: cần xác minh API thực tế trước khi nhận
              candidateAttempt = legacyAttempt;
              unverifiedLegacy = true;
            }
          }
        }
      }
    }

    final String targetAttempt = candidateAttempt;
    final boolean needVerification = unverifiedLegacy;

    if (this.api == null || !this.api.configured()) {
      error = "BACKEND_NOT_CONFIGURED";
      emit();
      return;
    }

    TaskContext ctx = snapshotContext();
    task(
        ctx,
        () -> {
          if (!ctx.client.signedIn()) throw new BackendClient.Failure("AUTHENTICATION_REQUIRED", 401);
          if (targetAttempt == null) {
            return fetchPath(ctx);
          }

          JSONObject attemptResp = ctx.client.request("GET", "/attempts/" + targetAttempt, null, null, null);

          if (needVerification) {
            boolean matched = false;
            String respSubj = normalizeSubject(attemptResp.optString("subjectId", attemptResp.optString("subject", "")));
            int respG = attemptResp.optInt("grade", 0);

            String scopeEnrollId = null;
            if (!respSubj.isEmpty() && respG > 0) {
              if (respSubj.equalsIgnoreCase(ctx.subject) && respG == ctx.grade) {
                matched = true;
              }
            } else {
              String attemptEnrollId = attemptResp.optString("enrollmentId", "");
              if (!attemptEnrollId.isEmpty()) {
                JSONObject scopeEnrollment = ctx.client.request(
                    "POST",
                    "/enrollments",
                    new JSONObject().put("subjectId", ctx.subject).put("grade", ctx.grade),
                    enrollKey,
                    null);
                scopeEnrollId = scopeEnrollment.optString("id", "");
                if (!scopeEnrollId.isEmpty() && attemptEnrollId.equals(scopeEnrollId)) {
                  matched = true;
                }
              }
            }

            if (!matched) {
              if (!respSubj.isEmpty() && respG > 0) {
                saved.set(candidateSubjKey(ctx.owner, targetAttempt), respSubj);
                saved.set(candidateGradeKey(ctx.owner, targetAttempt), respG);
              }
              JSONObject scopePath;
              if (scopeEnrollId != null && !scopeEnrollId.isEmpty()) {
                scopePath = ctx.client.request("GET", "/enrollments/" + scopeEnrollId + "/path", null, null, null);
              } else {
                scopePath = fetchPath(ctx);
              }
              JSONObject envelope = new JSONObject();
              envelope.put("__isScopeMismatch", true);
              envelope.put("path", scopePath);
              return envelope;
            }
          }

          return attemptResp;
        },
        value -> {
          if (value.optBoolean("__isScopeMismatch", false)) {
            path = value.getJSONObject("path");
            attempt = null;
            return;
          }

          if (value.has("questions")) {
            String respSubject = normalizeSubject(value.optString("subjectId", value.optString("subject", "")));
            int respGrade = value.optInt("grade", 0);
            if (!respSubject.isEmpty() && !respSubject.equalsIgnoreCase(ctx.subject)) {
              task(ctx, () -> fetchPath(ctx), p -> path = p);
              return;
            }
            if (respGrade > 0 && respGrade != ctx.grade) {
              task(ctx, () -> fetchPath(ctx), p -> path = p);
              return;
            }

            attempt = value;
            setScoped("attemptId", value.optString("id", targetAttempt));
            removeLegacyCandidate(ctx.owner, targetAttempt);
          } else {
            path = value;
          }
        });
  }

  private interface Accepted {
    void accept(JSONObject value) throws Exception;
  }

  private void task(TaskContext ctx, Callable<JSONObject> work, Accepted accept) {
    if (busy || closed) return;
    busy = true;
    error = null;
    emit();
    worker.execute(
        () -> {
          try {
            JSONObject result = work.call();
            main.execute(
                () -> {
                  if (closed
                      || generation != ctx.generation
                      || currentActiveUser() != ctx.owner
                      || owner != ctx.owner
                      || !Objects.equals(subject, ctx.subject)
                      || grade != ctx.grade) {
                    return;
                  }
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
            main.execute(
                () -> {
                  if (closed
                      || generation != ctx.generation
                      || currentActiveUser() != ctx.owner
                      || owner != ctx.owner
                      || !Objects.equals(subject, ctx.subject)
                      || grade != ctx.grade) {
                    return;
                  }
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
    if (busy || closed || api == null) return;
    if (currentActiveUser() != owner) return;
    TaskContext ctx = snapshotContext();
    task(
        ctx,
        () -> {
          ctx.client.signIn(identifier, password);
          return fetchPath(ctx);
        },
        result -> {
          path = result;
          needsLogin = false;
        });
  }

  private JSONObject fetchPath(TaskContext ctx) throws Exception {
    JSONObject enrollment =
        ctx.client.request(
            "POST",
            "/enrollments",
            new JSONObject().put("subjectId", ctx.subject).put("grade", ctx.grade),
            enrollKey,
            null);
    return ctx.client.request(
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
    if (api == null || !api.configured() || busy || closed) return;
    if (currentActiveUser() != owner) return;
    TaskContext ctx = snapshotContext();
    if (attempt != null) {
      String id = attempt.optString("id");
      task(ctx, () -> ctx.client.request("GET", "/attempts/" + id, null, null, null), value -> attempt = value);
    } else {
      task(ctx, () -> fetchPath(ctx), value -> path = value);
    }
  }

  public void start(String unit, boolean placement) {
    if (path == null || busy || closed || api == null) return;
    if (currentActiveUser() != owner) return;
    TaskContext ctx = snapshotContext();
    String requestKey = UUID.randomUUID().toString();
    task(
        ctx,
        () -> {
          JSONObject enrollment = path.getJSONObject("enrollment");
          JSONObject body = new JSONObject().put("enrollmentId", enrollment.getString("id"));
          if (!placement)
            body.put("lessonId", unit)
                .put("curriculumVersion", path.getString("curriculumVersion"));
          return ctx.client.request(
              "POST", placement ? "/placements" : "/attempts", body, requestKey, null);
        },
        result -> {
          attempt = result;
          completion = null;
          setScoped("attemptId", result.getString("id"));
        });
  }

  public void resume(String id) {
    if (busy || closed || api == null) return;
    if (currentActiveUser() != owner) return;
    TaskContext ctx = snapshotContext();
    task(
        ctx,
        () -> ctx.client.request("GET", "/attempts/" + id, null, null, null),
        result -> {
          attempt = result;
          completion = null;
          setScoped("attemptId", id);
        });
  }

  public void save(int step, String questionId, String selected) {
    if (attempt == null || busy || hasPending() || closed) return;
    if (currentActiveUser() != owner) return;
    try {
      JSONArray answers = new JSONArray();
      if (questionId != null)
        answers.put(
            new JSONObject()
                .put("questionId", questionId)
                .put("selectedOptionId", selected)
                .put("submissionId", UUID.randomUUID().toString()));
      JSONObject body = new JSONObject().put("step", step).put("answers", answers);
      setScoped("pendingMethod", "PATCH");
      setScoped("pendingPath", "/attempts/" + attempt.getString("id"));
      setScoped("pendingBody", body.toString());
      setScopedInt("pendingRevision", attempt.getInt("revision"));
      setScoped("pendingKey", UUID.randomUUID().toString());
      retry();
    } catch (JSONException invalid) {
      error = "INVALID_SERVER_RESPONSE";
      emit();
    }
  }

  public void complete() {
    if (attempt == null || busy || hasPending() || closed) return;
    if (currentActiveUser() != owner) return;
    setScoped("pendingMethod", "POST");
    setScoped("pendingPath", "/completions");
    setScoped("pendingBody", "{\"attemptId\":\"" + attempt.optString("id") + "\"}");
    setScoped("pendingKey", stableKey(currentScopeKey() + ":complete_" + attempt.optString("id")));
    setScopedInt("pendingRevision", null);
    retry();
  }

  public boolean hasPending() {
    return getScoped("pendingPath") != null;
  }

  public void retry() {
    if (!hasPending()) {
      reload();
      return;
    }
    if (currentActiveUser() != owner || busy || closed || api == null) return;
    String method = getScoped("pendingMethod"),
        route = getScoped("pendingPath"),
        body = getScoped("pendingBody"),
        key = getScoped("pendingKey");
    Integer revision = getScopedInt("pendingRevision");
    TaskContext ctx = snapshotContext();
    task(
        ctx,
        () -> {
          try {
            return ctx.client.request(method, route, new JSONObject(body), key, revision);
          } catch (BackendClient.Failure conflict) {
            if (conflict.status != 412 || attempt == null) throw conflict;
            return ctx.client.request("GET", "/attempts/" + attempt.getString("id"), null, null, null);
          }
        },
        result -> {
          setScoped("pendingPath", null);
          if (result.has("xpAwarded")) completion = result;
          else attempt = result;
        });
  }

  public void backToPath() {
    if (busy || hasPending() || closed || api == null) return;
    if (currentActiveUser() != owner) return;
    TaskContext ctx = snapshotContext();
    attempt = null;
    completion = null;
    setScoped("attemptId", null);
    task(ctx, () -> fetchPath(ctx), result -> path = result);
  }

  public boolean animateCompletion() {
    if (completion == null) return false;
    String id = completion.optString("id");
    if (id.equals(getScoped("celebrated"))) return false;
    setScoped("celebrated", id);
    return true;
  }

  @Override
  protected void onCleared() {
    closed = true;
    generation++;
    if (worker instanceof ExecutorService) {
      ((ExecutorService) worker).shutdownNow();
    }
  }
}
