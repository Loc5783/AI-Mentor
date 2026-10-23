package com.example.asm_final.utils;

import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.example.asm_final.R;
import com.google.android.material.textfield.TextInputLayout;
import java.util.function.*;

public final class AuthFields {
  private AuthFields() {}

  public static TextInputLayout container(EditText field) {
    ViewParent p = field.getParent();
    while (p != null) {
      if (p instanceof TextInputLayout) return (TextInputLayout) p;
      p = p.getParent();
    }
    throw new IllegalStateException("Missing field container");
  }

  public static void draft(EditText field, Consumer<String> changed) {
    field.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            changed.accept(s.toString());
          }

          public void afterTextChanged(Editable e) {}
        });
  }

  public static void live(EditText field, Predicate<String> valid, int error, int helper) {
    Runnable update =
        () -> {
          String text = field.getText().toString();
          TextInputLayout box = container(field);
          boolean success = !text.isEmpty() && valid.test(text);
          box.setHelperTextColor(
              android.content.res.ColorStateList.valueOf(
                  ContextCompat.getColor(
                      field.getContext(), success ? com.example.mentor.ui.R.color.auth_success : com.example.mentor.ui.R.color.auth_muted)));
          box.setHelperText(
              helper == 0
                  ? null
                  : success
                      ? field.getContext().getString(R.string.auth_valid)
                      : field.getContext().getString(helper));
          box.setBoxStrokeColorStateList(
              success
                  ? android.content.res.ColorStateList.valueOf(
                      ContextCompat.getColor(field.getContext(), com.example.mentor.ui.R.color.auth_success))
                  : androidx.appcompat.content.res.AppCompatResources.getColorStateList(
                      field.getContext(), com.example.mentor.ui.R.color.auth_border));
          if (success || text.isEmpty()) box.setError(null);
          else if (!field.hasFocus() || box.getError() != null)
            box.setError(field.getContext().getString(error));
        };
    draft(field, s -> update.run());
    field.setOnFocusChangeListener(
        (v, focus) -> {
          if (!focus) update.run();
        });
  }

  public static boolean email(String value) {
    return value != null && android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches();
  }

  public static String text(EditText field) {
    return field.getText().toString();
  }

  public static void collapseWhenEmpty(TextView message) {
    message.setVisibility(message.getText().length() == 0 ? View.GONE : View.VISIBLE);
    message.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            message.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
          }

          public void afterTextChanged(Editable e) {}
        });
  }

  public static void confirmation(EditText password, EditText confirm) {
    live(confirm, s -> s.equals(text(password)), R.string.reg_msg_mismatch, 0);
    draft(
        password,
        s -> {
          if (text(confirm).isEmpty()) return;
          if (s.equals(text(confirm))) FormFeedback.clear(confirm);
          else FormFeedback.error(confirm, R.string.reg_msg_mismatch);
        });
  }
}
