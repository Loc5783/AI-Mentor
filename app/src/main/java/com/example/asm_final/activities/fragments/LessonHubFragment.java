package com.example.asm_final.activities.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.example.asm_final.*;
import com.example.asm_final.R;
import com.example.asm_final.learning.LearningSetupStore;
import com.example.asm_final.utils.*;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.learning.SubjectId;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.*;

public final class LessonHubFragment extends Fragment {
  private LinearLayout content, courses;
  private TextView level;
  private int grade, owner, version;
  private Set<SubjectId> selected = EnumSet.noneOf(SubjectId.class);
  private final Set<SubjectId> toolsExpanded = EnumSet.noneOf(SubjectId.class);

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    if (saved != null)
      for (SubjectId id : SubjectId.values())
        if (saved.getBoolean("tools:" + id.name())) toolsExpanded.add(id);
  }

  @Override
  public void onSaveInstanceState(Bundle saved) {
    super.onSaveInstanceState(saved);
    for (SubjectId id : toolsExpanded) saved.putBoolean("tools:" + id.name(), true);
  }

  private AppServices services() {
    return ((AiMentorApplication) requireActivity().getApplication()).services();
  }

  private String language() {
    return LocaleHelper.getLanguage(requireContext());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    owner = ActiveUser.get(requireContext());
    content = ScreenViews.column(requireContext());
    content.addView(ScreenViews.text(requireContext(), getString(R.string.lesson_title), 28, true));
    content.addView(
        ScreenViews.text(requireContext(), getString(R.string.lesson_description), 15, false));
    level = ScreenViews.text(requireContext(), "", 17, true);
    content.addView(level);
    content.addView(ScreenViews.button(requireContext(), R.string.lesson_grade, this::chooseGrade));
    content.addView(
        ScreenViews.button(requireContext(), R.string.lesson_manage, this::chooseSubjects));
    content.addView(
        ScreenViews.text(requireContext(), getString(R.string.lesson_path_pending), 13, false));
    courses = new LinearLayout(requireContext());
    courses.setOrientation(LinearLayout.VERTICAL);
    content.addView(courses);
    content.addView(
        ScreenViews.text(requireContext(), getString(R.string.lesson_other_subjects), 13, false));
    content.addView(
        ScreenViews.button(
            requireContext(),
            R.string.lesson_tools,
            () ->
                ((com.example.asm_final.activities.HomeActivity) requireActivity())
                    .navigateTo(R.id.nav_legacy_tools, true)));
    return ScreenViews.scroll(requireContext(), content);
  }

  @Override
  public void onResume() {
    super.onResume();
    load();
  }

  private void load() {
    if (content == null) return;
    int request = ++version;
    AppServices app = services();
    android.content.Context c = requireContext().getApplicationContext();
    app.io()
        .execute(
            () -> {
              try {
                com.example.asm_final.onboarding.LearnerProfile profile =
                    app.learners().load(owner);
                int chosen = profile == null ? 0 : profile.grade;
                Set<SubjectId> subjects;
                try (LearningSetupStore store = new LearningSetupStore(c)) {
                  subjects = store.selected(owner, chosen);
                }
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(
                        () -> {
                          if (content != null && request == version) {
                            grade = chosen;
                            selected = subjects;
                            render();
                          }
                        });
              } catch (RuntimeException error) {
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(
                        () -> {
                          if (content != null && request == version)
                            level.setText(R.string.chat_storage_error);
                        });
              }
            });
  }

  private void render() {
    level.setText(
        grade >= 10 && grade <= 12
            ? getString(R.string.chat_grade, grade)
            : getString(R.string.chat_choose_grade));
    courses.removeAllViews();
    if (selected.isEmpty()) {
      courses.addView(
          ScreenViews.text(requireContext(), getString(R.string.lesson_choose_prompt), 16, false));
      return;
    }
    for (SubjectId id : new SubjectId[] {SubjectId.BIOLOGY, SubjectId.CHEMISTRY, SubjectId.PHYSICS})
      if (selected.contains(id)) {
        LinearLayout card = ScreenViews.card(requireContext());
        card.addView(ScreenViews.text(requireContext(), id.label(language()), 21, true));
        card.addView(
            ScreenViews.button(
                requireContext(),
                R.string.course_open,
                () ->
                    startActivity(
                        new android.content.Intent(
                                requireContext(),
                                com.example.asm_final.activities.LessonActivity.class)
                            .putExtra("SUBJECT", id.name().toLowerCase(java.util.Locale.ROOT))
                            .putExtra("GRADE", grade)
                            .putExtra(
                                "USERNAME",
                                getArguments() == null
                                    ? ""
                                    : getArguments().getString("USERNAME", "")))));
        LinearLayout extras = new LinearLayout(requireContext());
        extras.setOrientation(LinearLayout.VERTICAL);
        extras.addView(
            ScreenViews.text(
                requireContext(), getString(R.string.lesson_supplement_note), 13, false));
        extras.addView(
            ScreenViews.button(
                requireContext(), R.string.lesson_open_tools, () -> openSubject(id)));
        if (id == SubjectId.PHYSICS)
          extras.addView(
              ScreenViews.button(
                  requireContext(),
                  R.string.lesson_circuit,
                  () ->
                      startActivity(
                          new android.content.Intent(
                              requireContext(),
                              com.brightpath.science.presentation.CircuitDiagramActivity.class))));
        if (id == SubjectId.CHEMISTRY)
          extras.addView(
              ScreenViews.button(
                  requireContext(),
                  R.string.lesson_periodic,
                  () ->
                      startActivity(
                          new android.content.Intent(
                              requireContext(),
                              com.brightpath.science.presentation.PeriodicTableActivity.class))));
        Button toggle =
            ScreenViews.button(
                requireContext(),
                toolsExpanded.contains(id)
                    ? R.string.lesson_tools_less
                    : R.string.lesson_tools_more,
                () -> {});
        toggle.setOnClickListener(
            v -> {
              boolean open = !toolsExpanded.contains(id);
              if (open) toolsExpanded.add(id);
              else toolsExpanded.remove(id);
              toggle.setText(open ? R.string.lesson_tools_less : R.string.lesson_tools_more);
              extras.setVisibility(open ? View.VISIBLE : View.GONE);
              if (open) SoftMotion.enter(extras);
            });
        toggle.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        toggle.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                requireContext(), R.color.neumorphic_accent));
        toggle.setElevation(0f);
        card.addView(toggle);
        card.addView(extras);
        extras.setVisibility(toolsExpanded.contains(id) ? View.VISIBLE : View.GONE);
        courses.addView(card);
      }
  }

  private void openSubject(SubjectId id) {
    com.brightpath.science.util.ScienceUserSession.setUserId(requireContext(), owner);
    com.brightpath.science.util.SciencePreferenceStore preferences =
        new com.brightpath.science.util.SciencePreferenceStore(requireContext());
    com.brightpath.science.domain.model.ScienceSubject subject =
        com.brightpath.science.domain.model.ScienceSubject.valueOf(id.name());
    preferences.save(
        subject,
        com.brightpath.science.domain.model.EducationLevel.HIGH_SCHOOL.ordinal(),
        preferences.getStylePosition());
    startActivity(
        new android.content.Intent(
            requireContext(), com.brightpath.science.presentation.ScienceQuizActivity.class));
  }

  private void chooseGrade() {
    String[] grades = {
      getString(R.string.chat_grade, 10),
      getString(R.string.chat_grade, 11),
      getString(R.string.chat_grade, 12)
    };
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.chat_choose_grade)
        .setSingleChoiceItems(
            grades,
            grade >= 10 && grade <= 12 ? grade - 10 : -1,
            (d, index) -> {
              int next = index + 10;
              d.dismiss();
              AppServices app = services();
              app.io()
                  .execute(
                      () -> {
                        try {
                          app.learners().setGrade(owner, next);
                          new android.os.Handler(android.os.Looper.getMainLooper())
                              .post(
                                  () -> {
                                    if (content != null) load();
                                  });
                        } catch (RuntimeException error) {
                          new android.os.Handler(android.os.Looper.getMainLooper())
                              .post(
                                  () -> {
                                    if (content != null) level.setText(R.string.chat_storage_error);
                                  });
                        }
                      });
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void chooseSubjects() {
    if (grade < 10 || grade > 12) {
      chooseGrade();
      return;
    }
    SubjectId[] ids = {SubjectId.BIOLOGY, SubjectId.CHEMISTRY, SubjectId.PHYSICS};
    String[] labels = new String[3];
    boolean[] checks = new boolean[3];
    for (int i = 0; i < 3; i++) {
      labels[i] = ids[i].label(language());
      checks[i] = selected.contains(ids[i]);
    }
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.lesson_manage)
        .setMultiChoiceItems(labels, checks, (d, which, checked) -> checks[which] = checked)
        .setPositiveButton(
            R.string.chat_save,
            (d, w) -> {
              Set<SubjectId> chosen = EnumSet.noneOf(SubjectId.class);
              for (int i = 0; i < 3; i++) if (checks[i]) chosen.add(ids[i]);
              int savedGrade = grade;
              android.content.Context c = requireContext().getApplicationContext();
              services()
                  .io()
                  .execute(
                      () -> {
                        try (LearningSetupStore store = new LearningSetupStore(c)) {
                          store.save(owner, savedGrade, chosen);
                          new android.os.Handler(android.os.Looper.getMainLooper())
                              .post(
                                  () -> {
                                    if (content != null) load();
                                  });
                        } catch (RuntimeException error) {
                          new android.os.Handler(android.os.Looper.getMainLooper())
                              .post(
                                  () -> {
                                    if (content != null) level.setText(R.string.chat_storage_error);
                                  });
                        }
                      });
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  public void onDestroyView() {
    content = null;
    courses = null;
    level = null;
    version++;
    super.onDestroyView();
  }
}
