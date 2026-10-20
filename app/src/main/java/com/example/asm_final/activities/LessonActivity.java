package com.example.asm_final.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.R;
import com.example.asm_final.learning.LessonCompletionView;
import com.example.asm_final.learning.LessonPathData;
import com.example.asm_final.learning.LessonPathView;
import com.example.asm_final.learning.LessonViewModel;
import com.example.asm_final.utils.ScreenViews;
import com.example.asm_final.utils.SoftMotion;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.learning.SubjectId;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.*;

/** Native learning path and practice UI. All completion/reward decisions come from the backend. */
public final class LessonActivity extends AppCompatActivity {
  private LessonViewModel model;
  private LinearLayout body;
  private boolean returning;
  private String motionKey = "", feedbackKey = "";

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    if (state != null) {
      motionKey = state.getString("lesson_motion_key", "");
      feedbackKey = state.getString("lesson_feedback_key", "");
    }
    int owner = ActiveUser.get(this), grade = getIntent().getIntExtra("GRADE", 0);
    String subject = getIntent().getStringExtra("SUBJECT");
    if (owner <= 0
        || grade < 10
        || grade > 12
        || !("biology".equals(subject)
            || "chemistry".equals(subject)
            || "physics".equals(subject))) {
      finish();
      return;
    }
    model = new ViewModelProvider(this).get(LessonViewModel.class);
    model.changes().observe(this, ignored -> render());
    model.bind(owner, subject, grade);
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new androidx.activity.OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                leave();
              }
            });
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (model != null && model.owner() != ActiveUser.get(this)) {
      finish();
      return;
    }
    if (returning && model != null) {
      returning = false;
      model.reload();
    }
  }

  private void text(CharSequence value, int size, boolean bold) {
    body.addView(ScreenViews.text(this, value, size, bold));
  }

  @Override
  protected void onSaveInstanceState(Bundle saved) {
    saved.putString("lesson_motion_key", motionKey);
    saved.putString("lesson_feedback_key", feedbackKey);
    super.onSaveInstanceState(saved);
  }

  private void reveal(String key) {
    if (key.equals(motionKey)) return;
    motionKey = key;
    SoftMotion.enter(body);
  }

  private void button(int label, Runnable action) {
    body.addView(ScreenViews.button(this, label, action));
  }

  private void leave() {
    if (model.busy || model.hasPending())
      new MaterialAlertDialogBuilder(this)
          .setTitle(R.string.course_safe_exit)
          .setMessage(R.string.course_safe_exit_pending)
          .setPositiveButton(R.string.course_retry, (d, w) -> model.retry())
          .setNegativeButton(R.string.course_back, (d, w) -> finish())
          .show();
    else finish();
  }

  private void render() {
    if (model == null || isFinishing()) return;
    body = ScreenViews.column(this);
    setContentView(ScreenViews.scroll(this, body));
    button(R.string.course_back, this::leave);
    if (model.busy) {
      text(getString(R.string.course_loading), 17, false);
      body.addView(new ProgressBar(this));
      return;
    }
    if (model.error != null) {
      int message =
          "BACKEND_NOT_CONFIGURED".equals(model.error)
              ? R.string.course_no_server
              : "CURRICULUM_NOT_PUBLISHED".equals(model.error)
                  ? R.string.course_not_published
                  : model.needsLogin
                      ? ("ACCOUNT_LINK_REQUIRED".equals(model.error)
                          ? R.string.course_account_mismatch
                          : R.string.course_connect_description)
                      : model.hasPending()
                          ? R.string.course_sync_error
                          : R.string.course_network_error;
      text(getString(message), 17, false);
      if (model.needsLogin) {
        login();
        return;
      }
      if (!"BACKEND_NOT_CONFIGURED".equals(model.error)
          && !"CURRICULUM_NOT_PUBLISHED".equals(model.error))
        button(R.string.course_retry, model::retry);
      return;
    }
    try {
      if (model.hasPending()) {
        text(getString(R.string.course_sync_error), 16, false);
        button(R.string.course_retry, model::retry);
        return;
      }
      if (model.completion != null) {
        completed();
        motionKey = "complete:" + model.completion.optString("id");
        return;
      }
      if (model.attempt != null) {
        practice();
        reveal(model.attempt.optString("id") + ":" + model.attempt.optInt("step"));
        return;
      }
      if (model.path != null) {
        path();
        reveal("path");
      }
    } catch (JSONException invalid) {
      text(getString(R.string.course_network_error), 16, false);
      button(R.string.course_retry, model::reload);
    }
  }

  private void login() {
    text(getString(R.string.course_connect), 25, true);
    EditText identifier = new EditText(this);
    identifier.setHint(R.string.course_identifier);
    identifier.setSingleLine(true);
    identifier.setInputType(
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    identifier.setText(getIntent().getStringExtra("USERNAME"));
    body.addView(identifier);
    EditText password = new EditText(this);
    password.setHint(R.string.course_password);
    password.setSingleLine(true);
    password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    password.setSaveEnabled(false);
    body.addView(password);
    button(
        R.string.course_sign_in,
        () -> {
          String name = identifier.getText().toString().trim(),
              secret = password.getText().toString();
          if (name.isEmpty() || secret.isEmpty()) return;
          password.setText("");
          model.login(name, secret);
        });
  }

  private void path() throws JSONException {
    JSONObject path = model.path, enrollment = path.getJSONObject("enrollment");
    text(path.getString("title"), 27, true);
    text(getString(R.string.chat_grade, model.grade()), 16, false);
    text(path.getString("sourceNote"), 13, false);
    if ("en".equals(com.example.asm_final.utils.LocaleHelper.getLanguage(this)))
      text(getString(R.string.course_content_language), 13, false);
    JSONArray pending = path.getJSONArray("pendingAttempts");
    if (pending.length() > 0) {
      String id = pending.getJSONObject(0).getString("id");
      button(R.string.course_continue, () -> model.resume(id));
    }
    if (!"COMPLETED".equals(enrollment.getString("placementStatus"))) {
      text(getString(R.string.course_placement_note), 15, false);
      button(R.string.course_placement, () -> model.start("placement", true));
    }
    body.addView(
        new LessonPathView(
            this, new LessonPathData(path.getJSONArray("units")), id -> model.start(id, false)));
  }

  private void paragraphs(JSONArray values) throws JSONException {
    for (int i = 0; i < values.length(); i++) text(values.getString(i), 18, false);
  }

  private void practice() throws JSONException {
    JSONObject attempt = model.attempt;
    String kind = attempt.getString("kind");
    boolean lesson = "LESSON".equals(kind), placement = "PLACEMENT".equals(kind);
    int step = attempt.getInt("step"), offset = lesson ? 2 : 0;
    if (lesson && step < 2) {
      text(getString(step == 0 ? R.string.course_guidance : R.string.course_example), 26, true);
      paragraphs(attempt.getJSONArray(step == 0 ? "guidance" : "examples"));
      button(R.string.course_next, () -> model.save(step + 1, null, null));
      return;
    }
    JSONArray questions = attempt.getJSONArray("questions");
    int index = Math.max(0, step - offset);
    if (index >= questions.length()) {
      button(R.string.course_finish, model::complete);
      return;
    }
    JSONObject question = questions.getJSONObject(index);
    String qid = question.getString("id");
    text(getString(R.string.course_question, index + 1, questions.length()), 16, true);
    String difficulty = question.getString("difficulty");
    text(
        getString(
            "EASY".equals(difficulty)
                ? R.string.course_easy
                : "MEDIUM".equals(difficulty) ? R.string.course_medium : R.string.course_applied),
        24,
        true);
    text(question.getString("prompt"), 21, true);
    JSONObject previous = null;
    JSONArray answers = attempt.getJSONArray("answers");
    for (int i = 0; i < answers.length(); i++)
      if (qid.equals(answers.getJSONObject(i).getString("questionId")))
        previous = answers.getJSONObject(i);
    RadioGroup options = new RadioGroup(this);
    JSONArray choices = question.getJSONArray("options");
    for (int i = 0; i < choices.length(); i++) {
      JSONObject choice = choices.getJSONObject(i);
      RadioButton radio = new RadioButton(this);
      radio.setId(View.generateViewId());
      radio.setTag(choice.getString("id"));
      radio.setText(choice.getString("text"));
      radio.setTextSize(17);
      radio.setMinHeight(ScreenViews.dp(this, 52));
      radio.setTextColor(
          androidx.core.content.ContextCompat.getColor(this, R.color.textColorPrimary));
      options.addView(radio);
      if (previous != null && choice.getString("id").equals(previous.optString("selectedOptionId")))
        radio.setChecked(true);
    }
    body.addView(options);
    boolean answered = previous != null, correct = answered && previous.optBoolean("correct");
    if (answered && !placement) {
      TextView feedback =
          ScreenViews.text(
              this, getString(correct ? R.string.course_correct : R.string.course_wrong), 16, true);
      body.addView(feedback);
      String key =
          attempt.optString("id") + ":" + qid + ":" + previous.optInt("wrongCount") + ":" + correct;
      if (!key.equals(feedbackKey)) {
        feedbackKey = key;
        SoftMotion.enter(feedback);
      }
    }
    if (question.has("explanation") && !placement) {
      text(getString(R.string.course_help), 20, true);
      text(question.getString("explanation"), 17, false);
    }
    if (correct || (placement && answered)) {
      for (int i = 0; i < options.getChildCount(); i++) options.getChildAt(i).setEnabled(false);
      button(
          index + 1 == questions.length() ? R.string.course_finish : R.string.course_next,
          () -> {
            if (index + 1 == questions.length()) model.complete();
            else model.save(step + 1, null, null);
          });
    } else
      button(
          R.string.course_check,
          () -> {
            View selected = options.findViewById(options.getCheckedRadioButtonId());
            if (selected == null) {
              Toast.makeText(this, R.string.course_select, Toast.LENGTH_SHORT).show();
              return;
            }
            model.save(step, qid, selected.getTag().toString());
          });
    if (!placement) button(R.string.course_ask_ai, () -> askAi(question.optString("prompt")));
    text(getString(R.string.course_readonly), 13, false);
  }

  private void completed() throws JSONException {
    JSONObject value = model.completion;
    String result, streak = null;
    if ("PLACEMENT".equals(value.getString("kind"))) {
      JSONObject assessment = value.getJSONObject("assessment");
      result =
          getString(
              R.string.course_assessment, assessment.getInt("correct"), assessment.getInt("total"));
    } else {
      int xp = value.getInt("xpAwarded");
      result =
          getString(
              xp > 0 ? R.string.course_completed_reward : R.string.course_completed_retake, xp);
      streak =
          getString(
              value.getBoolean("streakLit")
                  ? R.string.course_streak_lit
                  : R.string.course_streak_already);
    }
    LessonCompletionView card = new LessonCompletionView(this, result, streak);
    body.addView(card);
    if (model.animateCompletion()) SoftMotion.complete(card);
    button(R.string.course_path, model::backToPath);
  }

  private void askAi(String question) {
    String subject = getIntent().getStringExtra("SUBJECT");
    String label =
        SubjectId.valueOf(subject.toUpperCase(java.util.Locale.ROOT))
            .label(com.example.asm_final.utils.LocaleHelper.getLanguage(this));
    String prompt = getString(R.string.course_ai_prompt, label, model.grade(), question);
    AppServices services = ((AiMentorApplication) getApplication()).services();
    services
        .io()
        .execute(
            () -> {
              try {
                if (ActiveUser.get(this) != model.owner()) return;
                com.example.asm_final.chat.ChatRecords.Thread thread =
                    services.chats().create(model.owner(), null);
                services.chats().saveDraft(model.owner(), thread.id, prompt);
                runOnUiThread(
                    () -> {
                      if (isFinishing() || ActiveUser.get(this) != model.owner()) return;
                      returning = true;
                      startActivity(
                          new Intent(this, HomeActivity.class)
                              .putExtra("USERNAME", getIntent().getStringExtra("USERNAME"))
                              .putExtra("CHAT_THREAD_ID", thread.id)
                              .putExtra("LESSON_RETURN", true));
                    });
              } catch (RuntimeException failed) {
                runOnUiThread(
                    () ->
                        Toast.makeText(this, R.string.chat_storage_error, Toast.LENGTH_LONG)
                            .show());
              }
            });
  }
}
