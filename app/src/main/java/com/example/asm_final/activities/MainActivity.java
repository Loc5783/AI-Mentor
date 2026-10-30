package com.example.asm_final.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.accounts.*;
import com.example.asm_final.utils.*;

public class MainActivity extends AppCompatActivity {
  private EditText name, password;
  private Button login;
  private TextView message, register;
  private AuthFormModel model;
  private boolean emailConfirmedCallback;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_main);
    emailConfirmedCallback = isEmailConfirmationCallback(getIntent());
    AuthChrome.bind(this);
    AppServices services = ((AiMentorApplication) getApplication()).services();
    model = new ViewModelProvider(this).get(AuthFormModel.class);
    name = findViewById(R.id.edtLoginUsername);
    password = findViewById(R.id.edtLoginPassword);
    login = findViewById(R.id.btnLogin);
    message = findViewById(R.id.tvLoginMessage);
    AuthFields.collapseWhenEmpty(message);
    register = findViewById(R.id.tvGoToRegister);
    com.google.android.material.checkbox.MaterialCheckBox remember =
        findViewById(R.id.checkRemember);
    if (com.example.asm_final.supabase.SupabaseHttp.enabled(this)) name.setHint(R.string.prof_hint_email);
    name.setText(model.identifier);
    password.setText(model.password);
    remember.setChecked(model.remember);
    AuthFields.draft(name, s -> model.identifier = s);
    AuthFields.draft(password, s -> model.password = s);
    remember.setOnCheckedChangeListener((v, checked) -> model.remember = checked);
    FormFeedback.bind(name, password);
    AuthKeyboard.bind(password, login);
    login.setOnClickListener(
        v -> {
          FormFeedback.clear(name, password);
          String identifier = AuthFields.text(name).trim(), secret = AuthFields.text(password);
          if (identifier.isEmpty() || secret.isEmpty()) {
            if (identifier.isEmpty()) FormFeedback.error(name, R.string.ux_required_username);
            if (secret.isEmpty()) FormFeedback.error(password, R.string.ux_required_password);
            FormFeedback.focus(identifier.isEmpty() ? name : password);
            message.setText("");
            return;
          }
          model.submit(
              services.io(),
              () -> services.accountAccess().signIn(identifier, secret),
              R.string.login_msg_fail);
        });
    register.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    findViewById(R.id.tvForgotPassword)
        .setOnClickListener(v -> startActivity(new Intent(this, AuthRecoveryActivity.class)));
    model.state.observe(
        this,
        s -> {
          login.setEnabled(!s.busy);
          register.setEnabled(!s.busy);
          name.setEnabled(!s.busy);
          password.setEnabled(!s.busy);
          remember.setEnabled(!s.busy);
          findViewById(R.id.tvForgotPassword).setEnabled(!s.busy);
          message.setText(
              emailConfirmedCallback && !s.busy && s.error == 0 && s.ticket == null
                  ? R.string.supabase_email_confirmed_return
                  : s.busy
                      ? R.string.auth_processing
                      : s.error == 0 ? R.string.auth_empty_text : s.error);
          if (s.ticket != null) {
            AuthTicket ticket = s.ticket;
            if (ticket.profile == null) {
              AuthSession.clear(this);
              model.consumed();
              message.setText(R.string.auth_route_error);
              return;
            }
            AuthSession.open(this, ticket.username, ticket.userId, model.remember);
            model.consumed();
            AuthRoutes.open(this, ticket.username, ticket.profile, true);
          }
        });
    if (!model.restoreAttempted) {
      model.restoreAttempted = true;
      String saved = AuthSession.remembered(this);
      if (!saved.isEmpty() && !emailConfirmedCallback) {
        model.remember = true;
        remember.setChecked(true);
        model.submit(
            services.io(),
            () -> services.accountAccess().restore(saved),
            R.string.auth_route_error);
      }
    }
  }

  private static boolean isEmailConfirmationCallback(Intent intent) {
    Uri uri = intent == null ? null : intent.getData();
    return intent != null
        && Intent.ACTION_VIEW.equals(intent.getAction())
        && uri != null
        && "aimentor".equals(uri.getScheme())
        && "auth".equals(uri.getHost())
        && "/callback".equals(uri.getPath());
  }
}
