package com.example.asm_final.activities;

import android.os.*;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.accounts.*;
import com.example.asm_final.utils.*;

public final class AuthRecoveryActivity extends AppCompatActivity {
  private int mode;
  private long resendUntil;
  private EditText email, token, password, confirm;
  private Button submit;
  private TextView status, resend;
  private RecoveryService service;
  private AsyncActionRunner actions;
  private AuthFormModel draft;
  private CountDownTimer resendTimer;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_auth_recovery);
    AuthChrome.bind(this);
    AppServices services = ((AiMentorApplication) getApplication()).services();
    service = services.recovery();
    actions = new AsyncActionRunner(services.io(), this::runOnUiThread);
    mode = state == null ? getIntent().getIntExtra("MODE", 0) : state.getInt("mode", 0);
    resendUntil = state == null ? 0 : state.getLong("resend_until", 0);
    email = findViewById(R.id.recovery_email);
    token = findViewById(R.id.recovery_token);
    password = findViewById(R.id.recovery_password);
    confirm = findViewById(R.id.recovery_confirm);
    submit = findViewById(R.id.recovery_submit);
    status = findViewById(R.id.recovery_status);
    resend = findViewById(R.id.recovery_resend);
    draft = new ViewModelProvider(this).get(AuthFormModel.class);
    email.setText(draft.email);
    token.setText(draft.identifier);
    password.setText(draft.password);
    confirm.setText(draft.confirm);
    token.setSaveEnabled(false);
    AuthFields.draft(email, s -> draft.email = s);
    AuthFields.draft(token, s -> draft.identifier = s);
    AuthFields.draft(password, s -> draft.password = s);
    AuthFields.draft(confirm, s -> draft.confirm = s);
    AuthFields.live(
        email, AuthFields::email, R.string.auth_invalid_email, R.string.auth_email_help);
    AuthFields.live(
        password,
        PasswordPolicy::isValid,
        R.string.reg_msg_weak_pass,
        R.string.auth_password_rules);
    AuthFields.confirmation(password, confirm);
    findViewById(R.id.recovery_reset_mode)
        .setOnClickListener(
            v -> {
              mode = 1;
              render();
            });
    findViewById(R.id.recovery_verify_mode)
        .setOnClickListener(
            v -> {
              mode = 2;
              render();
            });
    findViewById(R.id.recovery_back).setOnClickListener(v -> finish());
    submit.setOnClickListener(v -> request(false));
    resend.setOnClickListener(v -> request(true));
    
    // Auto-fill email if available for email verification
    if (mode == 2) {
      String currentUser = AuthSession.remembered(this);
      if (currentUser != null && !currentUser.isEmpty()) {
        email.setText(currentUser);
        draft.email = currentUser;
      }
    }
    
    render();
  }

  private void render() {
    mode = Math.max(0, Math.min(2, mode));
    int[]
        titles =
            {R.string.auth_recovery_title, R.string.auth_reset_title, R.string.auth_verify_title},
        intro = {R.string.auth_recovery_body, R.string.auth_reset_body, R.string.auth_verify_body},
        buttons =
            {R.string.auth_recovery_send, R.string.auth_reset_button, R.string.auth_verify_button};
    ((TextView) findViewById(R.id.recovery_title)).setText(titles[mode]);
    ((TextView) findViewById(R.id.recovery_intro)).setText(intro[mode]);
    submit.setText(buttons[mode]);
    AuthFields.container(email).setVisibility(mode == 1 ? View.GONE : View.VISIBLE);
    AuthFields.container(token).setVisibility(mode == 0 ? View.GONE : View.VISIBLE);
    AuthFields.container(password).setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
    AuthFields.container(confirm).setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
    resend.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
    submit.setEnabled(service.available());
    
    updateResendButton();
    
    status.setText(
        service.available() ? R.string.auth_empty_text : R.string.auth_recovery_unavailable);
  }

  private void updateResendButton() {
    if (mode == 2 && service.available()) {
      long remaining = resendUntil - android.os.SystemClock.elapsedRealtime();
      if (remaining > 0) {
        resend.setEnabled(false);
        startResendTimer(remaining);
      } else {
        resend.setEnabled(true);
        resend.setText(R.string.auth_resend);
      }
    } else {
      resend.setEnabled(false);
    }
  }

  private void startResendTimer(long duration) {
    if (resendTimer != null) resendTimer.cancel();
    resendTimer = new CountDownTimer(duration, 1000) {
      @Override
      public void onTick(long millisUntilFinished) {
        resend.setText(getString(R.string.auth_resend_wait_timer, millisUntilFinished / 1000));
      }
      @Override
      public void onFinish() {
        resend.setEnabled(true);
        resend.setText(R.string.auth_resend);
      }
    }.start();
  }

  private void request(boolean again) {
    if (!service.available()) {
      status.setText(R.string.auth_recovery_unavailable);
      return;
    }
    String e = AuthFields.text(email).trim(),
        code = AuthFields.text(token).trim(),
        secret = AuthFields.text(password);
    FormFeedback.clear(email, token, password, confirm);
    if ((mode == 0 || again) && !AuthFields.email(e)) {
      FormFeedback.error(email, R.string.auth_invalid_email);
      FormFeedback.focus(email);
      return;
    }
    if (!again && mode > 0 && code.isEmpty()) {
      FormFeedback.error(token, R.string.auth_token_required);
      FormFeedback.focus(token);
      return;
    }
    if (mode == 1 && !PasswordPolicy.isValid(secret)) {
      FormFeedback.error(password, R.string.reg_msg_weak_pass);
      FormFeedback.focus(password);
      return;
    }
    if (mode == 1 && !secret.equals(AuthFields.text(confirm))) {
      FormFeedback.error(confirm, R.string.reg_msg_mismatch);
      FormFeedback.focus(confirm);
      return;
    }
    if ((again || mode == 0) && android.os.SystemClock.elapsedRealtime() < resendUntil) {
      status.setText(R.string.auth_resend_wait);
      return;
    }
    submit.setEnabled(false);
    resend.setEnabled(false);
    status.setText(R.string.auth_processing);
    int requestedMode = mode;
    actions.submit(
        () ->
            again
                ? service.resendVerification(e)
                : requestedMode == 0
                    ? service.requestReset(e)
                    : requestedMode == 1 ? service.reset(code, secret) : service.verify(code),
        result -> {
          submit.setEnabled(true);
          updateResendButton();
          int message =
              result == RecoveryService.Result.REQUEST_ACCEPTED
                  ? R.string.auth_request_accepted
                  : result == RecoveryService.Result.VERIFIED
                      ? R.string.auth_verified
                      : result == RecoveryService.Result.PASSWORD_RESET
                          ? R.string.auth_password_reset_done
                          : result == RecoveryService.Result.INVALID_TOKEN
                              ? R.string.auth_token_invalid
                              : result == RecoveryService.Result.UNAVAILABLE
                                  ? R.string.auth_recovery_unavailable
                                  : R.string.auth_recovery_failed;
          status.setText(message);
          if (result == RecoveryService.Result.REQUEST_ACCEPTED) {
            resendUntil = android.os.SystemClock.elapsedRealtime() + 60000;
            updateResendButton();
          }
          if (result == RecoveryService.Result.VERIFIED) {
            draft.consumed();
            android.content.SharedPreferences prefs = getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
            prefs.edit().putBoolean(LocalRecoveryService.KEY_EMAIL_VERIFIED_PREFIX + com.example.mentor.data.ActiveUser.get(this), true).apply();
            
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setMessage("Xác minh email thành công! Tài khoản của bạn đã được kích hoạt đầy đủ tính năng bảo mật.")
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .setCancelable(false)
                .show();
          } else if (result == RecoveryService.Result.PASSWORD_RESET) {
            draft.consumed();
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setPositiveButton(R.string.auth_back_login, (d, w) -> AuthRoutes.login(this))
                .setCancelable(false)
                .show();
          }
        },
        error -> {
          submit.setEnabled(true);
          updateResendButton();
          status.setText(R.string.auth_recovery_failed);
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putInt("mode", mode);
    out.putLong("resend_until", resendUntil);
    super.onSaveInstanceState(out);
  }

  @Override
  protected void onDestroy() {
    if (actions != null) actions.close();
    super.onDestroy();
  }
}
