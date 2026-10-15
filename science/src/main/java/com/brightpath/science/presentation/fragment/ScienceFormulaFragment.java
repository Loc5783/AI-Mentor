package com.brightpath.science.presentation.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.science.R;
import com.brightpath.science.data.local.ScienceDbHelper;
import com.brightpath.science.data.repository.SQLiteScienceNoteRepository;
import com.brightpath.science.presentation.PeriodicTableActivity;
import com.brightpath.science.presentation.ScienceMainActivity;
import com.brightpath.science.presentation.adapter.ScienceModernAdapters;
import com.brightpath.science.util.ScienceUserSession;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ScienceFormulaFragment extends Fragment {
    private RecyclerView recycler;
    private LinearLayout notesContainer;
    private TextView emptyNotes;
    private SQLiteScienceNoteRepository noteRepository;
    private int userId;

    private final List<ScienceModernAdapters.FormulaItem> formulas = new ArrayList<>();

    public ScienceFormulaFragment() { super(R.layout.fragment_science_formula); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userId = new ScienceUserSession(requireContext()).getUserId();
        noteRepository = new SQLiteScienceNoteRepository(new ScienceDbHelper(requireContext()));

        initFormulaList();

        view.findViewById(R.id.buttonScienceFormulaBack).setOnClickListener(v -> requireActivity().finish());
        view.findViewById(R.id.buttonScienceFormulaAction).setOnClickListener(v -> startActivity(new Intent(requireContext(), PeriodicTableActivity.class)));
        view.findViewById(R.id.buttonAddScienceFormula).setOnClickListener(v -> showNoteDialog(null));

        notesContainer = view.findViewById(R.id.containerScienceNotes);
        emptyNotes = view.findViewById(R.id.textScienceNotesEmpty);
        recycler = view.findViewById(R.id.recyclerScienceFormulas);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        String allLabel = getString(R.string.science_all);
        show(allLabel);
        bindChip(view, R.id.chipFormulaAll, allLabel);
        bindChip(view, R.id.chipFormulaPhysics, getString(R.string.science_subj_physics));
        bindChip(view, R.id.chipFormulaChemistry, getString(R.string.science_subj_chemistry));
        bindChip(view, R.id.chipFormulaBiology, getString(R.string.science_subj_biology));
        refreshNotes();
    }

    private void initFormulaList() {
        formulas.clear();
        formulas.add(new ScienceModernAdapters.FormulaItem(getString(R.string.science_formula_newton), "F = m × a", getString(R.string.science_subj_physics)));
        formulas.add(new ScienceModernAdapters.FormulaItem(getString(R.string.science_formula_kinetic), "Wđ = ½mv²", getString(R.string.science_subj_physics)));
        formulas.add(new ScienceModernAdapters.FormulaItem(getString(R.string.science_formula_electric), "A = U × I × t", getString(R.string.science_subj_physics)));
        formulas.add(new ScienceModernAdapters.FormulaItem(getString(R.string.science_formula_moles), "n = m / M", getString(R.string.science_subj_chemistry)));
        formulas.add(new ScienceModernAdapters.FormulaItem(getString(R.string.science_formula_concentration), "Cₘ = n / V", getString(R.string.science_subj_chemistry)));
        formulas.add(new ScienceModernAdapters.FormulaItem(getString(R.string.science_formula_photosynthesis), "6CO₂ + 6H₂O → C₆H₁₂O₆ + 6O₂", getString(R.string.science_subj_biology)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (notesContainer != null) refreshNotes();
    }

    private void showNoteDialog(@Nullable SQLiteScienceNoteRepository.Note note) {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad / 2, pad, 0);

        EditText title = new EditText(requireContext());
        title.setHint(R.string.science_note_title_hint);
        EditText content = new EditText(requireContext());
        content.setHint(R.string.science_note_content_hint);
        content.setMinLines(4);
        content.setGravity(android.view.Gravity.TOP);
        Spinner subject = new Spinner(requireContext());
        String[] subjects = {
                getString(R.string.science_subj_physics),
                getString(R.string.science_subj_chemistry),
                getString(R.string.science_subj_biology),
                getString(R.string.science_subj_others)
        };
        subject.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, subjects));

        if (note != null) {
            title.setText(note.title);
            content.setText(note.content);
            for (int i = 0; i < subjects.length; i++) if (subjects[i].equals(note.subject)) subject.setSelection(i);
        }
        form.addView(title);
        form.addView(subject);
        form.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(note == null ? R.string.science_add_note : R.string.science_edit_note)
                .setView(form)
                .setNegativeButton(R.string.science_cancel, null)
                .setPositiveButton(R.string.science_save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            String c = content.getText().toString().trim();
            if (t.isEmpty()) { title.setError(getString(R.string.science_note_error_title)); return; }
            if (c.isEmpty()) { content.setError(getString(R.string.science_note_error_content)); return; }
            boolean ok;
            if (note == null) ok = noteRepository.insert(userId, t, c, String.valueOf(subject.getSelectedItem())) != -1;
            else ok = noteRepository.update(note.id, userId, t, c, String.valueOf(subject.getSelectedItem()));
            if (ok) {
                dialog.dismiss();
                refreshNotes();
                Toast.makeText(requireContext(), note == null ? R.string.science_note_added : R.string.science_note_updated, Toast.LENGTH_SHORT).show();
            } else Toast.makeText(requireContext(), R.string.science_note_save_error, Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void refreshNotes() {
        List<SQLiteScienceNoteRepository.Note> notes;
        try { notes = noteRepository.getAll(userId); }
        catch (Exception error) { notes = new ArrayList<>(); }
        notesContainer.removeAllViews();
        emptyNotes.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        for (SQLiteScienceNoteRepository.Note note : notes) notesContainer.addView(createNoteCard(note));
    }

    private View createNoteCard(SQLiteScienceNoteRepository.Note note) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = Math.round(10 * getResources().getDisplayMetrics().density);
        card.setLayoutParams(cardParams);
        card.setRadius(16 * getResources().getDisplayMetrics().density);
        card.setCardElevation(2 * getResources().getDisplayMetrics().density);
        card.setContentPadding(18, 14, 18, 14);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText(note.title + " • " + note.subject);
        title.setTextSize(16);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.science_text_primary));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        TextView content = new TextView(requireContext());
        content.setText(note.content);
        content.setTextSize(14);
        content.setTextColor(ContextCompat.getColor(requireContext(), R.color.science_text_secondary));
        content.setPadding(0, 8, 0, 8);
        TextView time = new TextView(requireContext());
        time.setText(getString(R.string.science_note_updated_at, new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(note.updatedAt))));
        time.setTextSize(14);
        time.setTextColor(ContextCompat.getColor(requireContext(), R.color.science_text_secondary));
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setGravity(android.view.Gravity.END);
        TextView edit = actionText(getString(R.string.science_edit));
        TextView delete = actionText(getString(R.string.science_delete));
        edit.setOnClickListener(v -> showNoteDialog(note));
        delete.setOnClickListener(v -> confirmDelete(note));
        actions.addView(edit);
        actions.addView(delete);
        body.addView(title); body.addView(content); body.addView(time); body.addView(actions);
        card.addView(body);
        return card;
    }

    private TextView actionText(String text) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.science_formula_color));
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setPadding(24, 12, 8, 8);
        return view;
    }

    private void confirmDelete(SQLiteScienceNoteRepository.Note note) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.science_note_delete_title)
                .setMessage(getString(R.string.science_note_delete_msg, note.title))
                .setNegativeButton(R.string.science_cancel, null)
                .setPositiveButton(R.string.science_delete, (dialog, which) -> {
                    if (noteRepository.delete(note.id, userId)) {
                        refreshNotes();
                        Toast.makeText(requireContext(), R.string.science_note_delete_success, Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void bindChip(View root, int id, String category) {
        TextView chip = root.findViewById(id);
        chip.setOnClickListener(v -> {
            resetChips(root);
            chip.setBackgroundResource(R.drawable.science_chip_orange);
            chip.setTextColor(Color.WHITE);
            show(category);
        });
    }

    private void resetChips(View root) {
        int[] ids = {R.id.chipFormulaAll, R.id.chipFormulaPhysics, R.id.chipFormulaChemistry, R.id.chipFormulaBiology};
        for (int id : ids) {
            TextView chip = root.findViewById(id);
            chip.setBackgroundResource(R.drawable.science_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.science_text_secondary));
        }
    }

    private void show(String category) {
        List<ScienceModernAdapters.FormulaItem> filtered = new ArrayList<>();
        String allLabel = getString(R.string.science_all);
        for (ScienceModernAdapters.FormulaItem item : formulas) if (allLabel.equals(category) || category.equals(item.category)) filtered.add(item);
        recycler.setAdapter(new ScienceModernAdapters.FormulaAdapter(filtered));
    }

    private ScienceMainActivity main() { return (ScienceMainActivity) requireActivity(); }
}
