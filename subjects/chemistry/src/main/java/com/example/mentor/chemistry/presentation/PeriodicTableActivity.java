package com.example.mentor.chemistry.presentation;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mentor.chemistry.ChemistryServices;
import com.example.mentor.chemistry.R;
import com.example.mentor.chemistry.domain.model.ChemicalElement;
import com.example.mentor.chemistry.domain.repository.PeriodicTableRepository;
import com.example.mentor.chemistry.presentation.adapter.PeriodicElementAdapter;
import java.util.List;

public final class PeriodicTableActivity extends AppCompatActivity {
  private static final String STATE_APPLIED_QUERY = "periodic_table.applied_query";
  private PeriodicTableRepository repository;
  private PeriodicElementAdapter adapter;
  private EditText searchInput;
  private TextView countView;
  // Keep the applied filter separate from the editable draft in the search field.
  private String appliedQuery = "";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_science_periodic_table);
    repository = ChemistryServices.periodicTable(this);
    searchInput = findViewById(R.id.etElementSearch);
    countView = findViewById(R.id.tvElementCount);
    if (savedInstanceState != null) {
      appliedQuery = savedInstanceState.getString(STATE_APPLIED_QUERY, "");
      searchInput.setText(savedInstanceState.getString("periodic_table.search_draft", appliedQuery));
    }

    RecyclerView recyclerView = findViewById(R.id.rvPeriodicTable);
    recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
    adapter = new PeriodicElementAdapter(this::showElement);
    recyclerView.setAdapter(adapter);

    findViewById(R.id.btnPeriodicBack).setOnClickListener(view -> finish());
    findViewById(R.id.btnElementSearch)
        .setOnClickListener(
            view -> {
              appliedQuery = searchInput.getText().toString();
              loadElements();
            });
    loadElements();
  }

  private void loadElements() {
    try {
      List<ChemicalElement> elements = repository.search(appliedQuery);
      adapter.submitList(elements);
      countView.setText(getString(R.string.science_elements_count, elements.size()));
    } catch (Exception error) {
      adapter.submitList(java.util.Collections.emptyList());
      countView.setText(getString(R.string.science_tool_periodic_table_data_could_not_be_loaded));
      Toast.makeText(
              this,
              getString(R.string.science_tool_unable_to_load_the_periodic_table),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putString(STATE_APPLIED_QUERY, appliedQuery);
    outState.putString("periodic_table.search_draft", searchInput.getText().toString());
    super.onSaveInstanceState(outState);
  }

  private void showElement(ChemicalElement element) {
    String details =
        getString(
            R.string.science_element_details,
            element.getAtomicNumber(),
            element.getSymbol(),
            element.getAtomicMass(),
            element.getGroup(),
            element.getPeriod(),
            element.getCategory(),
            element.getDescription(),
            element.getCommonUse());
    new AlertDialog.Builder(this)
        .setTitle(element.getName() + " (" + element.getSymbol() + ")")
        .setMessage(details)
        .setPositiveButton(getString(R.string.science_tool_close), null)
        .show();
  }
}
