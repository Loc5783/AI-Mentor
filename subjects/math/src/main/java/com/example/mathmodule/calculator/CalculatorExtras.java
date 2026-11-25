package com.example.mathmodule.calculator;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.example.mathmodule.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

/** Matrix and statistics panels inside the calculator dialog. */
final class CalculatorExtras {
  private CalculatorExtras() {}

  private static int dp(Activity a, int value) {
    return Math.round(value * a.getResources().getDisplayMetrics().density);
  }

  private static LinearLayout column(Activity a) {
    LinearLayout layout = new LinearLayout(a);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 12));
    return layout;
  }

  private static TextView label(Activity a, String text) {
    TextView view = new TextView(a);
    view.setText(text);
    view.setTextSize(16);
    view.setTypeface(null, Typeface.BOLD);
    view.setTextColor(MaterialColors.getColor(a, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
    view.setPadding(0, dp(a, 8), 0, dp(a, 4));
    return view;
  }

  private static TextView result(Activity a) {
    TextView view = new TextView(a);
    view.setTextSize(16);
    view.setTypeface(Typeface.MONOSPACE);
    view.setTextIsSelectable(true);
    view.setTextColor(MaterialColors.getColor(a, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
    view.setPadding(dp(a, 8), dp(a, 10), dp(a, 8), dp(a, 10));
    view.setMinHeight(dp(a, 60));
    return view;
  }

  private static MaterialButton button(Activity a, String text, Runnable onClick) {
    MaterialButton button = new MaterialButton(a);
    button.setText(text);
    button.setTextSize(14);
    button.setOnClickListener(v -> onClick.run());
    return button;
  }

  static View matrixPanel(Activity a) {
    LinearLayout root = column(a);
    root.addView(label(a, a.getString(R.string.math_matrix_size)));
    Spinner size = new Spinner(a);
    size.setAdapter(new ArrayAdapter<>(a, android.R.layout.simple_spinner_dropdown_item, new String[] {"2×2", "3×3"}));
    root.addView(size);
    EditText[][] first = new EditText[3][3], second = new EditText[3][3];
    View[] thirdRows = new View[2];
    LinearLayout[] grids = new LinearLayout[2];
    for (int matrix = 0; matrix < 2; matrix++) {
      root.addView(label(a, matrix == 0 ? "A" : "B"));
      grids[matrix] = new LinearLayout(a);
      grids[matrix].setOrientation(LinearLayout.VERTICAL);
      EditText[][] fields = matrix == 0 ? first : second;
      for (int row = 0; row < 3; row++) {
        LinearLayout line = new LinearLayout(a);
        for (int col = 0; col < 3; col++) {
          EditText input = new EditText(a);
          input.setSingleLine(true);
          input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
          input.setText("0");
          input.setSelectAllOnFocus(true);
          input.setTextSize(17);
          input.setGravity(Gravity.CENTER);
          input.setContentDescription((matrix == 0 ? "A" : "B") + (row + 1) + (col + 1));
          fields[row][col] = input;
          line.addView(input, new LinearLayout.LayoutParams(0, dp(a, 48), 1));
        }
        if (row == 2) thirdRows[matrix] = line;
        grids[matrix].addView(line);
      }
      root.addView(grids[matrix]);
    }
    size.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int index, long id) {
        boolean three = index == 1;
        for (int matrix = 0; matrix < 2; matrix++) {
          thirdRows[matrix].setVisibility(three ? View.VISIBLE : View.GONE);
        }
        for (int row = 0; row < 2; row++) {
          first[row][2].setVisibility(three ? View.VISIBLE : View.GONE);
          second[row][2].setVisibility(three ? View.VISIBLE : View.GONE);
        }
      }
      @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    });
    TextView output = result(a);
    output.setText(R.string.math_matrix_hint);
    String[] operations = {"A+B", "A−B", "A×B", "det A", "det B", "A⁻¹", "B⁻¹", "Aᵀ", "Bᵀ"};
    for (int row = 0; row < 3; row++) {
      LinearLayout line = new LinearLayout(a);
      for (int col = 0; col < 3; col++) {
        String operation = operations[row * 3 + col];
        MaterialButton control = button(a, operation, () -> {
          try {
            int n = size.getSelectedItemPosition() == 0 ? 2 : 3;
            boolean usesA = !operation.equals("det B") && !operation.equals("B⁻¹") && !operation.equals("Bᵀ");
            boolean usesB = operation.equals("A+B") || operation.equals("A−B") || operation.equals("A×B")
                || operation.equals("det B") || operation.equals("B⁻¹") || operation.equals("Bᵀ");
            double[][] aa = usesA ? read(first, n) : null;
            double[][] bb = usesB ? read(second, n) : null;
            String value;
            switch (operation) {
              case "A+B": value = MatrixEngine.format(MatrixEngine.add(aa, bb)); break;
              case "A−B": value = MatrixEngine.format(MatrixEngine.subtract(aa, bb)); break;
              case "A×B": value = MatrixEngine.format(MatrixEngine.multiply(aa, bb)); break;
              case "det A": value = CalculatorEngine.format(MatrixEngine.determinant(aa)); break;
              case "det B": value = CalculatorEngine.format(MatrixEngine.determinant(bb)); break;
              case "A⁻¹": value = MatrixEngine.format(MatrixEngine.inverse(aa)); break;
              case "B⁻¹": value = MatrixEngine.format(MatrixEngine.inverse(bb)); break;
              case "Aᵀ": value = MatrixEngine.format(MatrixEngine.transpose(aa)); break;
              default: value = MatrixEngine.format(MatrixEngine.transpose(bb));
            }
            output.setText(operation + " =\n" + value);
          } catch (IllegalArgumentException invalid) { output.setText(R.string.math_matrix_error); }
        });
        line.addView(control, new LinearLayout.LayoutParams(0, -2, 1));
      }
      root.addView(line);
    }
    root.addView(output);
    return root;
  }

  private static double[][] read(EditText[][] fields, int n) {
    double[][] values = new double[n][n];
    for (int row = 0; row < n; row++) for (int col = 0; col < n; col++) {
      try { values[row][col] = Double.parseDouble(fields[row][col].getText().toString().trim()); }
      catch (NumberFormatException invalid) { throw new IllegalArgumentException("Invalid cell", invalid); }
    }
    return MatrixEngine.checked(values);
  }

  static View statisticsPanel(Activity a) {
    LinearLayout root = column(a);
    Spinner mode = new Spinner(a);
    mode.setAdapter(new ArrayAdapter<>(a, android.R.layout.simple_spinner_dropdown_item,
        new String[] {a.getString(R.string.math_stats_single), a.getString(R.string.math_stats_pairs)}));
    root.addView(mode);
    root.addView(label(a, a.getString(R.string.math_stats_data)));
    EditText input = new EditText(a);
    input.setMinLines(4);
    input.setMaxLines(8);
    input.setGravity(Gravity.TOP);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setHint(R.string.math_stats_single_hint);
    root.addView(input, new LinearLayout.LayoutParams(-1, -2));
    String[] drafts = {"", ""};
    int[] current = {0};
    mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int index, long id) {
        drafts[current[0]] = input.getText().toString();
        current[0] = index;
        input.setText(drafts[index]);
        input.setHint(index == 0 ? R.string.math_stats_single_hint : R.string.math_stats_pairs_hint);
      }
      @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    });
    TextView output = result(a);
    root.addView(button(a, a.getString(R.string.math_stats_calculate), () -> {
      try {
        StringBuilder text = new StringBuilder();
        if (mode.getSelectedItemPosition() == 0) {
          StatisticsEngine.Summary s = StatisticsEngine.summarize(StatisticsEngine.parseValues(input.getText().toString()));
          text.append("n = ").append(s.count)
              .append("\nΣx = ").append(CalculatorEngine.format(s.sum))
              .append("\nx̄ = ").append(CalculatorEngine.format(s.mean))
              .append("\n").append(a.getString(R.string.math_stats_median)).append(" = ").append(CalculatorEngine.format(s.median))
              .append("\n").append(a.getString(R.string.math_stats_min_max)).append(" = ")
              .append(CalculatorEngine.format(s.minimum)).append(" / ").append(CalculatorEngine.format(s.maximum))
              .append("\nσ² = ").append(CalculatorEngine.format(s.populationVariance))
              .append("\nσ = ").append(CalculatorEngine.format(s.populationStandardDeviation()))
              .append("\ns² = ").append(s.count > 1 ? CalculatorEngine.format(s.sampleVariance) : "—")
              .append("\ns = ").append(s.count > 1 ? CalculatorEngine.format(s.sampleStandardDeviation()) : "—");
        } else {
          StatisticsEngine.Regression r = StatisticsEngine.regress(StatisticsEngine.parsePairs(input.getText().toString()));
          text.append("n = ").append(r.count)
              .append("\ny = ax + b")
              .append("\na = ").append(CalculatorEngine.format(r.slope))
              .append("\nb = ").append(CalculatorEngine.format(r.intercept))
              .append("\nr = ").append(Double.isNaN(r.correlation) ? "—" : CalculatorEngine.format(r.correlation));
        }
        output.setText(text.toString());
      } catch (IllegalArgumentException invalid) { output.setText(R.string.math_stats_error); }
    }));
    root.addView(output);
    return root;
  }
}
