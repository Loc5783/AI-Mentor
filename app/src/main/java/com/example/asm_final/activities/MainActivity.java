package com.example.asm_final.activities;

import android.content.Intent;
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

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_main);
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
              () -> {
                if (!services.authenticator().authenticate(identifier, secret)) return null;
                String canonical = services.accountLookup().canonicalUsername(identifier);
                int id = services.accountLookup().userId(canonical);
                if (id <= 0) return null;
                return new AuthTicket(canonical, id, services.learners().load(id));
              },
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
              s.busy
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
      if (!saved.isEmpty()) {
        model.remember = true;
        remember.setChecked(true);
        model.submit(
            services.io(),
            () -> {
              int id = services.accountLookup().userId(saved);
              return id > 0 ? new AuthTicket(saved, id, services.learners().load(id)) : null;
            },
            R.string.auth_route_error);
      }
    }
  }
}
