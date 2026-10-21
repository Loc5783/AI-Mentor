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
  private View content;
  private com.example.asm_final.ui.SubjectHubUi presenter;
  private int grade, owner, version;
  private Set<SubjectId> selected = EnumSet.noneOf(SubjectId.class);

  private AppServices services() {
    return ((AiMentorApplication) requireActivity().getApplication()).services();
  }

  private String language() {
    return LocaleHelper.getLanguage(requireContext());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    owner = ActiveUser.get(requireContext());
    presenter =
        new com.example.asm_final.ui.SubjectHubUi(
            requireContext(),
            this::chooseGrade,
            this::chooseSubjects,
            this::openSubject,
            this::openTools);
    content = presenter.view();
    return content;
  }

  private void openSubject(SubjectId id) {
    startActivity(
        new android.content.Intent(
                requireContext(), com.example.asm_final.activities.LessonActivity.class)
            .putExtra("SUBJECT", id.wireId)
            .putExtra("GRADE", grade)
            .putExtra(
                "USERNAME",
                getArguments() == null ? "" : getArguments().getString("USERNAME", "")));
  }

  private void openTools(SubjectId id) {
    Class<?> target =
        id == SubjectId.PHYSICS
            ? com.example.mentor.physics.presentation.CircuitDiagramActivity.class
            : id == SubjectId.CHEMISTRY
                ? com.example.mentor.chemistry.presentation.PeriodicTableActivity.class
                : com.example.mathmodule.MathToolsActivity.class;
    startActivity(new android.content.Intent(requireContext(), target));
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
                          if (content != null && request == version && ActiveUser.get(c) == owner) {
                            grade = chosen;
                            selected = subjects;
                            render();
                          }
                        });
              } catch (RuntimeException error) {
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(
                        () -> {
                          if (content != null && request == version && ActiveUser.get(c) == owner)
                            presenter.failure();
                        });
              }
            });
  }

  private void render() {
    presenter.render(grade, selected);
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
                                    if (content != null) presenter.failure();
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
    SubjectId[] ids = {SubjectId.PHYSICS, SubjectId.CHEMISTRY, SubjectId.BIOLOGY};
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
                                    if (content != null) presenter.failure();
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
    presenter = null;
    version++;
    super.onDestroyView();
  }
}
