package com.example.asm_final;

import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

/** Debug-only host for calculator interaction tests. */
public final class CalculatorHostActivity extends AppCompatActivity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(new FrameLayout(this));
    com.example.mathmodule.calculator.MathCalculator.attach(this);
  }

  @Override protected void onDestroy() {
    com.example.mathmodule.calculator.MathCalculator.detach(this);
    super.onDestroy();
  }
}
