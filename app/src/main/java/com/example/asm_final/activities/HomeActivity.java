package com.example.asm_final.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.asm_final.R;
import com.example.asm_final.activities.fragments.ProfileFragment;
import com.example.asm_final.utils.ReminderHelper;

public class HomeActivity extends AppCompatActivity
    implements ProfileFragment.OnProfileUpdatedListener {
  private int authenticatedOwner;

  private final ActivityResultLauncher<String> notificationPermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            applyNotificationChoice(granted);
          });

  @Override
  protected void onCreate(Bundle state) {
    com.example.asm_final.utils.ThemeHelper.applySavedTheme(this);
    super.onCreate(state);
    String username = getIntent().getStringExtra("USERNAME");
    if (username == null || username.trim().isEmpty()) {
      logout();
      return;
    }
    com.example.asm_final.AppServices authServices =
        ((com.example.asm_final.AiMentorApplication) getApplication()).services();
    com.example.asm_final.onboarding.LearnerProfile learning;
    try {
      int accountId = authServices.accountLookup().userId(username);
      if (accountId <= 0 || accountId != com.example.mentor.data.ActiveUser.get(this)) {
        logout();
        return;
      }
      authenticatedOwner = accountId;
      learning = authServices.learners().load(accountId);
    } catch (RuntimeException storageError) {
      android.widget.Toast.makeText(
              this, R.string.auth_storage_error, android.widget.Toast.LENGTH_LONG)
          .show();
      logout();
      return;
    }
    if (learning == null) {
      logout();
      return;
    }
    if (!learning.complete()) {
      com.example.asm_final.accounts.AuthRoutes.open(this, username, learning, true);
      return;
    }
    composeShell =
        new com.example.asm_final.ui.ShellUi(
            this, username, getIntent().hasExtra("CHAT_THREAD_ID") ? R.id.nav_chat : R.id.nav_home);
    setContentView(composeShell.view());

    if (state != null) {
      String savedRoute = state.getString("CURRENT_ROUTE");
      if ("settings".equals(savedRoute)) {
        navigateTo(R.id.nav_settings, false);
      }
    } else if (getIntent() != null && getIntent().getBooleanExtra("NAVIGATE_TO_SCHEDULE", false)) {
        navigateTo(0, false);
    }

    checkNotificationPrompt();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (composeShell != null) {
      outState.putString("CURRENT_ROUTE", composeShell.currentRoute());
    }
  }

  private void checkNotificationPrompt() {
    if (ReminderHelper.shouldPromptNotification(this)) {
      showNotificationPromptDialog();
    }
  }

  private void showNotificationPromptDialog() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.notification_prompt_title)
        .setMessage(R.string.notification_prompt_message)
        .setPositiveButton(
            R.string.notification_prompt_allow,
            (dialog, which) -> {
              ReminderHelper.setHasPromptedNotification(this, true);
              if (Build.VERSION.SDK_INT >= 33
                  && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                      != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS);
              } else {
                applyNotificationChoice(true);
              }
            })
        .setNegativeButton(
            R.string.notification_prompt_dismiss,
            (dialog, which) -> {
              ReminderHelper.setHasPromptedNotification(this, true);
              dialog.dismiss();
            })
        .setCancelable(false)
        .show();
  }

  private void applyNotificationChoice(boolean requested) {
    boolean enabled =
        requested
            && androidx.core.app.NotificationManagerCompat.from(this)
                .areNotificationsEnabled();
    ReminderHelper.setReminderEnabled(this, enabled);
    int message =
        requested && !enabled
            ? R.string.reminder_permission_needed
            : enabled ? R.string.reminder_enabled_time : R.string.reminder_disabled;
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }

  private com.example.asm_final.ui.ShellUi composeShell;

  public void navigateTo(int id, boolean remember) {
    if (composeShell != null) composeShell.navigate(id, remember);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null && intent.getBooleanExtra("NAVIGATE_TO_SCHEDULE", false)) {
        navigateTo(0, false); // nav_schedule corresponds to id 0 in ShellUi
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (authenticatedOwner > 0
        && com.example.mentor.data.ActiveUser.get(this) != authenticatedOwner) finish();
  }

  public void logout() {
    int owner = com.example.mentor.data.ActiveUser.get(this);
    if (owner > 0) {
      Runnable revoke = com.example.asm_final.backend.BackendClient.detach(this, owner);
      ((com.example.asm_final.AiMentorApplication) getApplication())
          .services()
          .io()
          .execute(revoke);
    }
    com.example.asm_final.accounts.AuthSession.clear(this);
    startActivity(
        new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    finish();
  }

  @Override
  public void onProfileUpdated() {
    /* The profile reloads on resume. */
  }
}
