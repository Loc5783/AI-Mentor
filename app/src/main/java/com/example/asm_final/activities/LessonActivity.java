package com.example.asm_final.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.R;
import com.example.asm_final.learning.LessonViewModel;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.learning.SubjectId;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Native learning path and practice UI. All completion/reward decisions come from the backend. */
public final class LessonActivity extends AppCompatActivity {
  private LessonViewModel model;
  private boolean returning;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
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
    setContentView(
        com.example.asm_final.ui.LearningUi.screen(this, model, this::leave, this::askAi));
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
