package com.example.mathmodule.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mathmodule.R;
import com.example.mathmodule.adapter.MathNoteAdapter;
import com.example.mathmodule.databases.MathNoteItem;
import com.example.mathmodule.databases.SqliteDbHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MathFormulaFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private MathNoteAdapter adapter;
    private SqliteDbHelper dbHelper;
    private final List<MathNoteItem> fullNoteList = new ArrayList<>();
    private final List<MathNoteItem> displayList = new ArrayList<>();
    private String currentFilter = "ALL";
    private Button btnAll;
    private Button btnAlgebra;
    private Button btnGeometry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_math_formula, container, false);
        view.findViewById(R.id.btn_math_module_back)
                .setOnClickListener(v -> requireActivity().finish());

        recyclerView = view.findViewById(R.id.recycler_notes);
        emptyView = view.findViewById(R.id.txt_empty_notes);
        btnAll = view.findViewById(R.id.btn_filter_all_note);
        btnAlgebra = view.findViewById(R.id.btn_filter_algebra);
        btnGeometry = view.findViewById(R.id.btn_filter_geometry);
        FloatingActionButton btnAddNote = view.findViewById(R.id.btn_add_note);

        dbHelper = new SqliteDbHelper(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MathNoteAdapter(displayList, new MathNoteAdapter.NoteActionListener() {
            @Override
            public void onPin(MathNoteItem item) {
                dbHelper.updatePinStatus(item.getId(), item.getIsPinned() == 1 ? 0 : 1);
                loadNotesFromDatabase();
            }

            @Override
            public void onEdit(MathNoteItem item) {
                showNoteDialog(item);
            }

            @Override
            public void onDelete(MathNoteItem item) {
                confirmDelete(item);
            }
        });
        recyclerView.setAdapter(adapter);

        setupFilters();
        btnAddNote.setOnClickListener(v -> showNoteDialog(null));
        loadNotesFromDatabase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) loadNotesFromDatabase();
    }

    private void showNoteDialog(@Nullable MathNoteItem existing) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_note, null, false);
        TextView dialogTitle = dialogView.findViewById(R.id.txt_note_dialog_title);
        EditText edtTitle = dialogView.findViewById(R.id.edt_note_title);
        EditText edtFormula = dialogView.findViewById(R.id.edt_note_formula);
        RadioGroup categoryGroup = dialogView.findViewById(R.id.rg_category);

        boolean editing = existing != null;
        dialogTitle.setText(editing ? R.string.math_formula_edit_title : R.string.math_formula_add_title);
        if (editing) {
            edtTitle.setText(existing.getTitle());
            edtFormula.setText(existing.getFormula());
            categoryGroup.check(getString(R.string.math_formula_filter_geometry).equals(existing.getCategory())
                    ? R.id.rb_geometry : R.id.rb_algebra);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(editing ? R.string.math_formula_edit_title : R.string.math_formula_add_title)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.math_formula_btn_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = edtTitle.getText().toString().trim();
                    String formula = edtFormula.getText().toString().trim();
                    String category = categoryGroup.getCheckedRadioButtonId() == R.id.rb_geometry
                            ? getString(R.string.math_formula_filter_geometry) : getString(R.string.math_formula_filter_algebra);

                    if (title.isEmpty()) {
                        edtTitle.setError(getString(R.string.math_formula_hint_title));
                        edtTitle.requestFocus();
                        return;
                    }
                    if (formula.isEmpty()) {
                        edtFormula.setError(getString(R.string.math_formula_hint_content));
                        edtFormula.requestFocus();
                        return;
                    }

                    boolean success;
                    if (editing) {
                        success = dbHelper.updateNote(existing.getId(), title, formula, category);
                    } else {
                        success = dbHelper.insertNote(title, formula, category) > 0;
                    }

                    if (!success) {
                        Toast.makeText(requireContext(), R.string.math_formula_error_save, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(requireContext(),
                            editing ? R.string.math_formula_success_update : R.string.math_formula_success_add,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadNotesFromDatabase();
                }));
        dialog.show();
    }

    private void confirmDelete(MathNoteItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.math_formula_delete_title)
                .setMessage(getString(R.string.math_formula_delete_msg, item.getTitle()))
                .setPositiveButton(R.string.math_formula_delete_confirm, (dialog, which) -> {
                    boolean deleted = dbHelper.deleteNote(item.getId());
                    Toast.makeText(requireContext(),
                            deleted ? R.string.math_formula_delete_success : R.string.math_formula_delete_error,
                            Toast.LENGTH_SHORT).show();
                    loadNotesFromDatabase();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadNotesFromDatabase() {
        fullNoteList.clear();
        fullNoteList.addAll(dbHelper.getAllNotes());
        filterData(currentFilter);
    }

    private void setupFilters() {
        btnAll.setOnClickListener(v -> selectFilter("ALL", btnAll, btnAlgebra, btnGeometry));
        btnAlgebra.setOnClickListener(v -> selectFilter("ALGEBRA", btnAlgebra, btnAll, btnGeometry));
        btnGeometry.setOnClickListener(v -> selectFilter("GEOMETRY", btnGeometry, btnAll, btnAlgebra));
        selectFilter("ALL", btnAll, btnAlgebra, btnGeometry);
    }

    private void selectFilter(String filter, Button active, Button... inactive) {
        currentFilter = filter;
        updateButtonColors(active, inactive);
        filterData(filter);
    }

    private void filterData(String filterType) {
        displayList.clear();
        String algebra = getString(R.string.math_formula_filter_algebra);
        String geometry = getString(R.string.math_formula_filter_geometry);
        for (MathNoteItem item : fullNoteList) {
            if ("ALL".equals(filterType)
                    || ("ALGEBRA".equals(filterType) && algebra.equals(item.getCategory()))
                    || ("GEOMETRY".equals(filterType) && geometry.equals(item.getCategory()))) {
                displayList.add(item);
            }
        }
        adapter.updateData(displayList);
        boolean empty = displayList.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateButtonColors(Button active, Button... inactives) {
        active.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.math_formula_icon)));
        active.setTextColor(Color.WHITE);
        for (Button inactive : inactives) {
            inactive.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.math_button_inactive_bg)));
            inactive.setTextColor(ContextCompat.getColor(requireContext(), R.color.math_button_inactive_text));
        }
    }
}
