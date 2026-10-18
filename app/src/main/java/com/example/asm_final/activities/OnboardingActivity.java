package com.example.asm_final.activities;

import android.content.Intent;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.example.asm_final.*;
import com.example.asm_final.accounts.*;
import com.example.asm_final.onboarding.*;
import com.example.asm_final.utils.*;

public final class OnboardingActivity extends AppCompatActivity {
  private String username;
  private int id, page;
  private boolean replay, ready;
  private AppServices services;
  private AsyncActionRunner actions;
  private final int[] titles = {
    R.string.onboard_1_title,
    R.string.onboard_2_title,
    R.string.onboard_3_title,
    R.string.onboard_4_title
  };
  private final int[] bodies = {
    R.string.onboard_1_body,
    R.string.onboard_2_body,
    R.string.onboard_3_body,
    R.string.onboard_4_body
  };
  private final int[] art = {
    R.drawable.auth_art_welcome,
    R.drawable.auth_art_chat,
    R.drawable.auth_art_path,
    R.drawable.auth_art_shield
  };

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_onboarding);
    AuthChrome.bind(this);
    services = ((AiMentorApplication) getApplication()).services();
    actions = new AsyncActionRunner(services.io(), this::runOnUiThread);
    username = getIntent().getStringExtra("USERNAME");
    id = com.example.mentor.data.ActiveUser.get(this);
    replay = getIntent().getBooleanExtra("REPLAY", false);
    page = state == null ? -1 : state.getInt("page", -1);
    findViewById(R.id.onboarding_next).setEnabled(false);
    findViewById(R.id.onboarding_skip).setEnabled(false);
    findViewById(R.id.onboarding_back).setOnClickListener(v -> back());
    findViewById(R.id.onboarding_next).setOnClickListener(v -> next(false));
    findViewById(R.id.onboarding_skip).setOnClickListener(v -> next(true));
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              public void handleOnBackPressed() {
                back();
              }
            });
    actions.submit(
        () -> {
          if (id <= 0 || services.accountLookup().userId(username) != id) return null;
          return services.learners().load(id);
        },
        profile -> {
          if (profile == null) {
            AuthRoutes.login(this);
            return;
          }
          replay = replay && profile.complete();
          if (profile.complete() && !replay) {
            AuthRoutes.open(this, username, profile, true);
            return;
          }
          int review = getIntent().getIntExtra("PAGE", -1);
          if (!replay && profile.step >= OnboardingPolicy.PROFILE && review < 0) {
            AuthRoutes.open(this, username, profile, true);
            return;
          }
          if (page < 0)
            page = replay ? 0 : review >= 0 ? Math.min(3, review) : Math.min(3, profile.step);
          ready = true;
          render();
        },
        error -> {
          ((TextView) findViewById(R.id.onboarding_body)).setText(R.string.auth_storage_error);
          ((Button) findViewById(R.id.onboarding_next)).setText(R.string.auth_retry);
          findViewById(R.id.onboarding_next).setEnabled(true);
          findViewById(R.id.onboarding_next).setOnClickListener(v -> recreate());
          findViewById(R.id.onboarding_back).setEnabled(true);
          findViewById(R.id.onboarding_back).setOnClickListener(v -> AuthRoutes.login(this));
        });
  }

  private void render() {
    ((TextView) findViewById(R.id.onboarding_step))
        .setText(getString(R.string.onboard_step, page + 1));
    ((com.google.android.material.progressindicator.LinearProgressIndicator)
            findViewById(R.id.onboarding_progress))
        .setProgressCompat(page + 1, false);
    ((ImageView) findViewById(R.id.onboarding_art)).setImageResource(art[page]);
    ((TextView) findViewById(R.id.onboarding_title)).setText(titles[page]);
    ((TextView) findViewById(R.id.onboarding_body)).setText(bodies[page]);
    findViewById(R.id.onboarding_back).setEnabled(page > 0);
    findViewById(R.id.onboarding_next).setEnabled(true);
    findViewById(R.id.onboarding_skip).setEnabled(true);
    ((Button) findViewById(R.id.onboarding_next))
        .setText(
            page == 3
                ? (replay ? R.string.auth_close_guide : R.string.auth_profile_setup)
                : page == 0 ? R.string.auth_start : R.string.auth_next);
    ((TextView) findViewById(R.id.onboarding_skip))
        .setText(replay ? R.string.auth_close_guide : R.string.auth_skip);
    View body = findViewById(R.id.onboarding_body);
    if (android.os.Build.VERSION.SDK_INT < 26
        || android.animation.ValueAnimator.areAnimatorsEnabled()) {
      body.setAlpha(.85f);
      body.animate().alpha(1).setDuration(160).start();
    }
  }

  private void back() {
    if (!ready) return;
    if (page > 0) {
      page--;
      render();
    } else finish();
  }

  private void next(boolean skip) {
    if (!ready) return;
    if (replay) {
      if (skip || page == 3) finish();
      else {
        page++;
        render();
      }
      return;
    }
    int target = skip ? 4 : page + 1;
    ready = false;
    findViewById(R.id.onboarding_next).setEnabled(false);
    findViewById(R.id.onboarding_skip).setEnabled(false);
    findViewById(R.id.onboarding_back).setEnabled(false);
    actions.submit(
        () -> {
          services.learners().advance(id, target);
          return true;
        },
        saved -> {
          ready = true;
          if (target == 4) {
            startActivity(
                new Intent(this, ProfileSetupActivity.class).putExtra("USERNAME", username));
            finish();
          } else {
            page = target;
            render();
          }
        },
        error -> {
          ready = true;
          ((TextView) findViewById(R.id.onboarding_body)).setText(R.string.auth_storage_error);
          findViewById(R.id.onboarding_next).setEnabled(true);
          findViewById(R.id.onboarding_back).setEnabled(page > 0);
          findViewById(R.id.onboarding_skip).setEnabled(true);
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putInt("page", page);
    super.onSaveInstanceState(out);
  }

  @Override
  protected void onDestroy() {
    if (actions != null) actions.close();
    super.onDestroy();
  }
}
