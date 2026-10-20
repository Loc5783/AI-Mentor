package com.example.asm_final.activities;

import android.content.Intent;
import android.os.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.accounts.*;
import com.example.asm_final.onboarding.*;
import com.example.asm_final.utils.*;
import com.google.android.material.chip.*;
import java.util.*;

public final class ProfileSetupActivity extends AppCompatActivity {
  private LearnerSetupModel model;
  private AppServices services;
  private String username;
  private int id;
  private boolean edit, ready;
  private EditText name, minutes;
  private com.google.android.material.textfield.MaterialAutoCompleteTextView goal;
  private TextView status;
  private Button save;
  private final int[] gradeIds = {R.id.grade_10, R.id.grade_11, R.id.grade_12};
  private final int[] interestIds = {
    R.id.interest_math,
    R.id.interest_literature,
    R.id.interest_english,
    R.id.interest_history,
    R.id.interest_geography,
    R.id.interest_chemistry,
    R.id.interest_physics,
    R.id.interest_biology,
    R.id.interest_economics_law
  };
  private final String[] subjects = {
    "MATH",
    "LITERATURE",
    "ENGLISH",
    "HISTORY",
    "GEOGRAPHY",
    "CHEMISTRY",
    "PHYSICS",
    "BIOLOGY",
    "ECONOMICS_LAW"
  };
  private int goalIndex = -1;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_profile_setup);
    AuthChrome.bind(this);
    services = ((AiMentorApplication) getApplication()).services();
    username = getIntent().getStringExtra("USERNAME");
    id = com.example.mentor.data.ActiveUser.get(this);
    edit = getIntent().getBooleanExtra("EDIT", false);
    if (id <= 0 || services.accountLookup().userId(username) != id) {
      AuthRoutes.login(this);
      return;
    }
    name = findViewById(R.id.profile_display_name);
    minutes = findViewById(R.id.profile_minutes);
    goal = findViewById(R.id.profile_goal_choice);
    status = findViewById(R.id.profile_setup_status);
    AuthFields.collapseWhenEmpty(status);
    save = findViewById(R.id.profile_setup_save);
    save.setEnabled(false);
    ChipGroup grades = findViewById(R.id.grade_choices),
        interests = findViewById(R.id.interest_choices);
    for (int i = 0; i < 3; i++) {
      Chip c = chip(gradeIds[i], getString(R.string.profile_grade_number, i + 10));
      grades.addView(c);
    }
    for (int i = 0; i < subjects.length; i++)
      interests.addView(
          chip(
              interestIds[i],
              com.example.mentor.learning.SubjectId.valueOf(subjects[i])
                  .label(com.example.asm_final.utils.LocaleHelper.getLanguage(this))));
    goal.setSimpleItems(getResources().getStringArray(R.array.learner_goals));
    goal.setOnItemClickListener((parent, view, position, row) -> goalIndex = position);
    AuthFields.live(name, OnboardingPolicy::validName, R.string.auth_invalid_name, 0);
    AuthKeyboard.bind(minutes, save);
    model = new ViewModelProvider(this).get(LearnerSetupModel.class);
    model.changes.observe(
        this,
        version -> {
          if (model.completed) {
            model.completed = false;
            AuthRoutes.open(this, username, model.draft, true);
            return;
          }
          if (model.draft != null && !ready) {
            if (model.draft.complete() && !edit) {
              AuthRoutes.open(this, username, model.draft, true);
              return;
            }
            bind(model.draft);
            ready = true;
          }
          save.setEnabled(!model.loading && !model.saving);
          status.setText(
              model.loading || model.saving
                  ? R.string.auth_processing
                  : model.draft == null || model.failed
                      ? R.string.auth_storage_error
                      : R.string.auth_empty_text);
        });
    if (model.draft != null) {
      bind(model.draft);
      ready = true;
      save.setEnabled(!model.saving);
    } else model.load(services, id);
    save.setOnClickListener(
        v -> {
          if (!ready) {
            model.load(services, id);
            return;
          }
          LearnerProfile p = collect();
          FormFeedback.clear(name, minutes);
          if (!OnboardingPolicy.validName(p.displayName)) {
            FormFeedback.error(name, R.string.auth_invalid_name);
            FormFeedback.focus(name);
            return;
          }
          if (p.minutes < 0 || p.minutes > 0 && (p.minutes < 5 || p.minutes > 240)) {
            FormFeedback.error(minutes, R.string.profile_invalid_minutes);
            FormFeedback.focus(minutes);
            return;
          }
          if (!OnboardingPolicy.valid(p)) {
            status.setText(R.string.profile_required);
            return;
          }
          save.setEnabled(false);
          status.setText(R.string.auth_processing);
          model.save(services, p);
        });
    findViewById(R.id.profile_setup_back).setOnClickListener(v -> back());
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              public void handleOnBackPressed() {
                back();
              }
            });
  }

  private Chip chip(int id, String text) {
    Chip c = new Chip(this);
    c.setId(id);
    c.setText(text);
    c.setCheckable(true);
    c.setEnsureMinTouchTargetSize(true);
    return c;
  }

  private void bind(LearnerProfile p) {
    name.setText(p.displayName);
    minutes.setText(p.minutes > 0 ? Integer.toString(p.minutes) : "");
    for (int i = 0; i < 3; i++) ((Chip) findViewById(gradeIds[i])).setChecked(p.grade == i + 10);
    Set<String> selected = new HashSet<>(Arrays.asList(p.subjects.split(",")));
    for (int i = 0; i < subjects.length; i++)
      ((Chip) findViewById(interestIds[i])).setChecked(selected.contains(subjects[i]));
    goalIndex = p.goal;
    if (goalIndex >= 0 && goalIndex < 4)
      goal.setText(getResources().getStringArray(R.array.learner_goals)[goalIndex], false);
  }

  private LearnerProfile collect() {
    int grade = 0;
    for (int i = 0; i < 3; i++) if (((Chip) findViewById(gradeIds[i])).isChecked()) grade = i + 10;
    List<String> selected = new ArrayList<>();
    for (int i = 0; i < subjects.length; i++)
      if (((Chip) findViewById(interestIds[i])).isChecked()) selected.add(subjects[i]);
    int duration = 0;
    try {
      String value = AuthFields.text(minutes).trim();
      if (!value.isEmpty()) duration = Integer.parseInt(value);
    } catch (NumberFormatException invalid) {
      duration = -1;
    }
    return new LearnerProfile(
        id,
        model.draft == null ? 4 : model.draft.step,
        AuthFields.text(name).trim(),
        grade,
        android.text.TextUtils.join(",", selected),
        goalIndex,
        duration,
        model.draft == null ? 0 : model.draft.consentedAt);
  }

  private void back() {
    if (model != null && model.saving) return;
    if (edit) {
      finish();
      return;
    }
    startActivity(
        new Intent(this, OnboardingActivity.class)
            .putExtra("USERNAME", username)
            .putExtra("PAGE", 3));
    finish();
  }

  @Override
  protected void onPause() {
    if (ready && model != null && !model.saving && !model.completed) {
      LearnerProfile draft = collect();
      model.draft = draft;
      services
          .io()
          .execute(
              () -> {
                try {
                  services.learners().saveDraft(draft);
                } catch (RuntimeException ignored) {
                }
              });
    }
    super.onPause();
  }
}
