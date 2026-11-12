package com.example.pocketkit;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText edtTitle, edtSubject, edtContent;
    private CheckBox cbPin;
    private TextView tvHeader, btnSave, btnDelete, btnBack;

    private DatabaseHelper dbHelper;
    private Note existingNote = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        dbHelper = new DatabaseHelper(this);

        edtTitle = findViewById(R.id.edtNoteTitle);
        edtSubject = findViewById(R.id.edtNoteSubject);
        edtContent = findViewById(R.id.edtNoteContent);
        cbPin = findViewById(R.id.cbPinNote);
        tvHeader = findViewById(R.id.tvEditorHeader);
        btnSave = findViewById(R.id.btnSaveNote);
        btnDelete = findViewById(R.id.btnDeleteNote);
        btnBack = findViewById(R.id.btnBackEditor);

        if (getIntent().hasExtra("note_item")) {
            existingNote = IntentCompat.getSerializableExtra(getIntent(), "note_item", Note.class);
            if (existingNote != null) {
                tvHeader.setText("Chỉnh sửa ghi chú");
                edtTitle.setText(existingNote.getTitle());
                edtSubject.setText(existingNote.getSubject());
                edtContent.setText(existingNote.getContent());
                cbPin.setChecked(existingNote.getIsPinned() == 1);
                btnDelete.setVisibility(View.VISIBLE);
            }
        }

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveNote());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void saveNote() {
        String title = edtTitle.getText().toString().trim();
        String subject = edtSubject.getText().toString().trim();
        String content = edtContent.getText().toString().trim();
        int isPinned = cbPin.isChecked() ? 1 : 0;

        if (title.isEmpty()) {
            edtTitle.setError("Tiêu đề không được để trống!");
            edtTitle.requestFocus();
            return;
        }

        if (subject.isEmpty()) {
            subject = "Chung";
        } else {
            dbHelper.insertCategory(subject);
        }

        String now = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());

        if (existingNote == null) {
            Note note = Note.builder()
                    .title(title)
                    .content(content)
                    .subject(subject)
                    .isPinned(isPinned)
                    .isDone(0)
                    .updatedAt(now)
                    .build();
            dbHelper.insertNote(note);
            Toast.makeText(this, "Đã tạo ghi chú mới!", Toast.LENGTH_SHORT).show();
        } else {
            existingNote.setTitle(title);
            existingNote.setSubject(subject);
            existingNote.setContent(content);
            existingNote.setIsPinned(isPinned);
            existingNote.setUpdatedAt(now);
            dbHelper.updateNote(existingNote);
            Toast.makeText(this, "Đã cập nhật ghi chú!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void confirmDelete() {
        if (existingNote == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xóa ghi chú")
                .setMessage("Bạn có chắc muốn xóa ghi chú này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelper.deleteNote(existingNote.getId());
                    Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}