package com.example.asm_final.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.asm_final.R;
import com.example.asm_final.activities.fragments.ProfileFragment;

public class HomeActivity extends AppCompatActivity
    implements ProfileFragment.OnProfileUpdatedListener {
  private int authenticatedOwner;

  @Override
  protected void onCreate(Bundle state) {
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
  }

  private com.example.asm_final.ui.ShellUi composeShell;

  public void navigateTo(int id, boolean remember) {
    if (composeShell != null) composeShell.navigate(id, remember);
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
