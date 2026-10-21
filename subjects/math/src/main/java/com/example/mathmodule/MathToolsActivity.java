package com.example.mathmodule;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mathmodule.calculator.MathCalculator;

/** Subject-owned calculator entry. Lessons are provided by the shared learning engine. */
public final class MathToolsActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    int padding = Math.round(20 * getResources().getDisplayMetrics().density);
    content.setPadding(padding, padding, padding, padding);
    TextView title = new TextView(this);
    title.setText(R.string.math_calculator_title);
    title.setTextSize(24);
    content.addView(title);
    TextView hint = new TextView(this);
    hint.setText(R.string.math_calculator_hint);
    hint.setTextSize(16);
    content.addView(hint);
    Button open = new Button(this);
    open.setText(R.string.math_calculator_title);
    open.setOnClickListener(v -> MathCalculator.show(this));
    content.addView(open);
    setContentView(content);
  }
}
