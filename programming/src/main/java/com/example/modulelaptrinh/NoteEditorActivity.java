package com.example.modulelaptrinh;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modulelaptrinh.database.ProgrammingDbHelper;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private ImageView btnBack;
    private TextView btnSave;
    private TextView tvToolbarTitle;
    private ProgrammingDbHelper dbHelper;
    private int noteId = -1;
    private boolean isSaved = false;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        dbHelper = new ProgrammingDbHelper(this);
        userId = Math.max(0, getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, 0));

        etNoteTitle = findViewById(R.id.et_note_title);
        etNoteContent = findViewById(R.id.et_note_content);
        btnBack = findViewById(R.id.iv_back);
        btnSave = findViewById(R.id.btn_save);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);

        noteId = getIntent().getIntExtra("NOTE_ID", -1);
        String title = getIntent().getStringExtra("NOTE_TITLE");
        String content = getIntent().getStringExtra("NOTE_CONTENT");

        if (noteId != -1 && title != null) {
            tvToolbarTitle.setText(R.string.note_edit_title);
            etNoteTitle.setText(title);
            etNoteContent.setText(content);
        } else {
            tvToolbarTitle.setText(R.string.note_new_title);
        }

        btnBack.setOnClickListener(v -> {
            saveNoteIfChanged();
            setResult(RESULT_OK);
            finish();
        });

        btnSave.setOnClickListener(v -> {
            saveNote();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveNoteIfChanged();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void saveNoteIfChanged() {
        if (isSaved) return;
        
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            return;
        }

        if (noteId != -1) {
            dbHelper.updateNote(userId, noteId, title, content);
        } else {
            long id = dbHelper.insertNote(userId, title, content);
            if (id != -1) {
                noteId = (int) id;
            }
        }
        isSaved = true;
        setResult(RESULT_OK);
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, R.string.note_empty_warning, Toast.LENGTH_SHORT).show();
            return;
        }

        if (noteId != -1) {
            dbHelper.updateNote(userId, noteId, title, content);
            Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show();
        } else {
            long id = dbHelper.insertNote(userId, title, content);
            if (id != -1) {
                noteId = (int) id;
                Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
            }
        }
        isSaved = true;
        setResult(RESULT_OK);
        finish();
    }
}