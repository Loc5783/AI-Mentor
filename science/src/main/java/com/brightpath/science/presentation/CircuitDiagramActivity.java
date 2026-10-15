package com.brightpath.science.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.brightpath.science.R;
import com.brightpath.science.presentation.view.CircuitDiagramView;
import com.brightpath.science.util.ScienceUiFactory;

public final class CircuitDiagramActivity extends AppCompatActivity {
    private CircuitDiagramView diagramView;
    private CheckBox switchCheckBox;
    private TextView explanationView;
    private boolean parallel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_science_circuit_diagram);
        diagramView = findViewById(R.id.circuitDiagramView);
        switchCheckBox = findViewById(R.id.cbCircuitSwitch);
        explanationView = findViewById(R.id.tvCircuitExplanation);
        Spinner typeSpinner = findViewById(R.id.spCircuitType);
        typeSpinner.setAdapter(ScienceUiFactory.spinnerAdapter(this,
                new String[]{getString(R.string.science_circuit_series), getString(R.string.science_circuit_parallel)}));
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                parallel = position == 1;
                updateDiagram();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        switchCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateDiagram());
        findViewById(R.id.btnCircuitBack).setOnClickListener(view -> finish());
        updateDiagram();
    }

    private void updateDiagram() {
        boolean closed = switchCheckBox.isChecked();
        diagramView.setParallel(parallel);
        diagramView.setSwitchClosed(closed);
        String circuitText = parallel
                ? getString(R.string.science_circuit_parallel_desc)
                : getString(R.string.science_circuit_series_desc);
        String switchText = closed
                ? getString(R.string.science_circuit_switch_closed_desc)
                : getString(R.string.science_circuit_switch_open_desc);
        explanationView.setText(circuitText + switchText
                + getString(R.string.science_circuit_disclaimer_desc));
    }
}
