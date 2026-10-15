package com.example.modulelaptrinh.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modulelaptrinh.NoteEditorActivity;
import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.adapters.ProgrammingNoteAdapter;
import com.example.modulelaptrinh.database.ProgrammingDbHelper;
import com.example.modulelaptrinh.database.ProgrammingNote;

import java.util.List;

public class NoteFragment extends Fragment {

    private RecyclerView rvNotes;
    private EditText etSearchNote;
    private ImageView ivAddNote, ivExportNotes;
    private ProgrammingNoteAdapter adapter;
    private ProgrammingDbHelper dbHelper;
    private List<ProgrammingNote> noteList;
    private int userId;

    // FIX: Dùng ActivityResultLauncher để nhận kết quả
    private final ActivityResultLauncher<Intent> noteEditorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // FIX: Reload lại danh sách sau khi lưu
                    loadNotes();
                    Toast.makeText(getContext(), "Đã cập nhật danh sách ghi chú", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);
        userId = ((com.example.modulelaptrinh.MainActivity) requireActivity()).getCurrentUserId();

        rvNotes = view.findViewById(R.id.rv_notes);
        etSearchNote = view.findViewById(R.id.et_search_note);
        ivAddNote = view.findViewById(R.id.iv_add_note);
        ivExportNotes = view.findViewById(R.id.iv_export_notes);

        dbHelper = new ProgrammingDbHelper(getContext());

        setupRecyclerView();
        loadNotes();

        ivAddNote.setOnClickListener(v -> openNoteEditor(null));

        ivExportNotes.setOnClickListener(v -> shareNotes());

        etSearchNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchNotes(s.toString());
            }
        });

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ProgrammingNoteAdapter(null, new ProgrammingNoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(ProgrammingNote note) {
                openNoteEditor(note);
            }

            @Override
            public void onDeleteClick(int position, ProgrammingNote note) {
                dbHelper.deleteNote(userId, note.getId());
                loadNotes();
                Toast.makeText(getContext(), "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
            }
        });

        rvNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotes.setAdapter(adapter);
    }

    private void loadNotes() {
        noteList = dbHelper.getAllNotes(userId);
        if (adapter != null) {
            adapter.updateData(noteList);
        }
    }

    private void searchNotes(String keyword) {
        if (keyword.isEmpty()) {
            loadNotes();
        } else {
            noteList = dbHelper.searchNotes(userId, keyword);
            if (adapter != null) {
                adapter.updateData(noteList);
            }
        }
    }

    private void shareNotes() {
        if (noteList == null || noteList.isEmpty()) {
            Toast.makeText(getContext(), R.string.note_share_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder content = new StringBuilder(getString(R.string.note_share_header) + "\n\n");
        for (ProgrammingNote note : noteList) {
            content.append("• ").append(note.getTitle() == null ? getString(R.string.note_share_default_title) : note.getTitle())
                    .append("\n")
                    .append(note.getContent() == null ? "" : note.getContent())
                    .append("\n\n");
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.note_share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, content.toString().trim());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.note_share_chooser_title)));
    }

    private void openNoteEditor(ProgrammingNote existingNote) {
        Intent intent = new Intent(getActivity(), NoteEditorActivity.class);
        intent.putExtra(com.example.modulelaptrinh.MainActivity.EXTRA_USER_ID, userId);
        if (existingNote != null) {
            // FIX: Dùng putExtra với int
            intent.putExtra("NOTE_ID", existingNote.getId());
            intent.putExtra("NOTE_TITLE", existingNote.getTitle());
            intent.putExtra("NOTE_CONTENT", existingNote.getContent());
        }
        // FIX: Dùng launcher thay vì startActivityForResult
        noteEditorLauncher.launch(intent);
    }
    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadNotes();
        }
    }

}