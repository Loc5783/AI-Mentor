package com.example.asm_final.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.accounts.*;
import com.example.asm_final.onboarding.*;
import com.example.asm_final.utils.*;

public class RegisterActivity extends AppCompatActivity {
  private EditText name, email, password, confirm;
  private TextView message;
  private AuthFormModel model;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_register);
    AuthChrome.bind(this);
    AppServices services = ((AiMentorApplication) getApplication()).services();
    model = new ViewModelProvider(this).get(AuthFormModel.class);
    name = findViewById(R.id.edtRegisterUsername);
    email = findViewById(R.id.edtRegisterEmail);
    password = findViewById(R.id.edtRegisterPassword);
    confirm = findViewById(R.id.edtRegisterConfirmPassword);
    message = findViewById(R.id.tvRegisterMessage);
    AuthFields.collapseWhenEmpty(message);
    Button submit = findViewById(R.id.btnRegister);
    com.google.android.material.checkbox.MaterialCheckBox terms = findViewById(R.id.checkTerms);
    TextView back = findViewById(R.id.tvGoToLogin);
    name.setText(model.name);
    email.setText(model.email);
    password.setText(model.password);
    confirm.setText(model.confirm);
    terms.setChecked(model.accepted);
    AuthFields.draft(name, s -> model.name = s);
    AuthFields.draft(email, s -> model.email = s);
    AuthFields.draft(password, s -> model.password = s);
    AuthFields.draft(confirm, s -> model.confirm = s);
    terms.setOnCheckedChangeListener((v, checked) -> model.accepted = checked);
    AuthFields.live(name, OnboardingPolicy::validName, R.string.auth_invalid_name, 0);
    AuthFields.live(
        email, AuthFields::email, R.string.auth_invalid_email, R.string.auth_email_help);
    AuthFields.live(
        password,
        PasswordPolicy::isValid,
        R.string.reg_msg_weak_pass,
        R.string.auth_password_rules);
    AuthFields.confirmation(password, confirm);
    AuthKeyboard.bind(confirm, submit);
    findViewById(R.id.auth_terms).setOnClickListener(v -> AuthChrome.terms(this));
    back.setOnClickListener(v -> finish());
    submit.setOnClickListener(
        v -> {
          String n = AuthFields.text(name).trim(),
              e = AuthFields.text(email).trim(),
              p = AuthFields.text(password),
              c = AuthFields.text(confirm);
          FormFeedback.clear(name, email, password, confirm);
          message.setText("");
          EditText invalid = null;
          if (!OnboardingPolicy.validName(n)) {
            FormFeedback.error(name, R.string.auth_invalid_name);
            invalid = name;
          }
          if (!AuthFields.email(e)) {
            FormFeedback.error(email, R.string.auth_invalid_email);
            if (invalid == null) invalid = email;
          }
          if (!PasswordPolicy.isValid(p)) {
            FormFeedback.error(password, R.string.reg_msg_weak_pass);
            if (invalid == null) invalid = password;
          }
          if (!p.equals(c) || c.isEmpty()) {
            FormFeedback.error(confirm, R.string.reg_msg_mismatch);
            if (invalid == null) invalid = confirm;
          }
          if (invalid != null) {
            FormFeedback.focus(invalid);
            return;
          }
          if (!terms.isChecked()) {
            message.setText(R.string.auth_terms_required);
            terms.requestFocus();
            return;
          }
          model.submit(
              services.io(),
              () -> services.accountAccess().register(n, e, p),
              R.string.auth_registration_failed);
        });
    model.state.observe(
        this,
        s -> {
          submit.setEnabled(!s.busy);
          back.setEnabled(!s.busy);
          terms.setEnabled(!s.busy);
          for (EditText field : new EditText[] {name, email, password, confirm})
            field.setEnabled(!s.busy);
          message.setText(
              s.busy
                  ? R.string.auth_processing
                  : s.error == 0 ? R.string.auth_empty_text : s.error);
          if (s.ticket != null) {
            AuthTicket ticket = s.ticket;
            if (ticket.profile == null) {
              model.consumed();
              message.setText(R.string.auth_route_error);
              return;
            }
            AuthSession.open(this, ticket.username, ticket.userId, false);
            model.consumed();
            AuthRoutes.open(this, ticket.username, ticket.profile, true);
          }
        });
  }
}
