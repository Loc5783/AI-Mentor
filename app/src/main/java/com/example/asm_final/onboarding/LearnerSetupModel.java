package com.example.asm_final.onboarding;

import androidx.lifecycle.*;
import com.example.asm_final.AppServices;

public final class LearnerSetupModel extends ViewModel {
  public LearnerProfile draft;
  public boolean loading, saving, completed, failed;
  public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
  private int version;

  private void changed() {
    changes.postValue(++version);
  }

  public void load(AppServices services, int id) {
    if (loading || draft != null) return;
    loading = true;
    failed = false;
    changes.setValue(++version);
    services
        .io()
        .execute(
            () -> {
              try {
                draft = services.learners().load(id);
              } catch (RuntimeException ignored) {
                failed = true;
              }
              loading = false;
              changed();
            });
  }

  public void save(AppServices services, LearnerProfile p) {
    if (saving) return;
    saving = true;
    failed = false;
    draft = p;
    services
        .io()
        .execute(
            () -> {
              try {
                services.learners().complete(p);
                draft = services.learners().load(p.userId);
                completed = true;
              } catch (RuntimeException ignored) {
                completed = false;
                failed = true;
              }
              saving = false;
              changed();
            });
  }
}
