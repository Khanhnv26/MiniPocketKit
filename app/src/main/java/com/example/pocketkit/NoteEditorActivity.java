package com.example.pocketkit;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText edtTitle, edtContent;
    private Spinner spnSubject;
    private CheckBox cbPin;
    private TextView tvHeader, btnSave, btnDelete, btnBack, btnAddCategoryInEditor;

    private DatabaseHelper dbHelper;
    private Note existingNote = null;
    private final List<String> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        dbHelper = new DatabaseHelper(this);

        edtTitle = findViewById(R.id.edtNoteTitle);
        spnSubject = findViewById(R.id.spnNoteSubject);
        edtContent = findViewById(R.id.edtNoteContent);
        cbPin = findViewById(R.id.cbPinNote);
        tvHeader = findViewById(R.id.tvEditorHeader);
        btnSave = findViewById(R.id.btnSaveNote);
        btnDelete = findViewById(R.id.btnDeleteNote);
        btnBack = findViewById(R.id.btnBackEditor);
        btnAddCategoryInEditor = findViewById(R.id.btnAddCategoryInEditor);

        String initialCategory = null;
        if (getIntent().hasExtra("note_item")) {
            existingNote = IntentCompat.getSerializableExtra(getIntent(), "note_item", Note.class);
            if (existingNote != null) {
                tvHeader.setText("Chỉnh sửa ghi chú");
                edtTitle.setText(existingNote.getTitle());
                initialCategory = existingNote.getSubject();
                edtContent.setText(existingNote.getContent());
                cbPin.setChecked(existingNote.getIsPinned() == 1);
                btnDelete.setVisibility(View.VISIBLE);
            }
        }

        setupCategorySpinner(initialCategory);

        btnAddCategoryInEditor.setOnClickListener(v -> showAddCategoryDialog());
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveNote());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupCategorySpinner(String selectedCategory) {
        loadCategories(selectedCategory);

        spnSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = categoryList.get(position);
                if ("+ Thêm phân loại mới...".equals(item)) {
                    showAddCategoryDialog();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadCategories(String preselect) {
        categoryList.clear();
        Set<String> unique = new LinkedHashSet<>();
        unique.add("Chung");
        unique.addAll(dbHelper.getAllCategories());
        categoryList.addAll(unique);
        categoryList.add("+ Thêm phân loại mới...");

        categoryAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, categoryList);
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spnSubject.setAdapter(categoryAdapter);

        if (preselect != null && !preselect.trim().isEmpty()) {
            int pos = categoryList.indexOf(preselect.trim());
            if (pos >= 0) {
                spnSubject.setSelection(pos);
            } else {
                int insertPos = Math.max(0, categoryList.size() - 1);
                categoryList.add(insertPos, preselect.trim());
                categoryAdapter.notifyDataSetChanged();
                spnSubject.setSelection(insertPos);
            }
        }
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Tên phân loại (VD: MAS291, PRN211...)");
        int pad = dpToPx(20);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Thêm phân loại mới")
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        dbHelper.insertCategory(name);
                        loadCategories(name);
                        Toast.makeText(this, "Đã thêm môn: " + name, Toast.LENGTH_SHORT).show();
                    } else {
                        spnSubject.setSelection(0);
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> spnSubject.setSelection(0))
                .setOnCancelListener(dialog -> spnSubject.setSelection(0))
                .show();
    }

    private void saveNote() {
        String title = edtTitle.getText().toString().trim();
        String content = edtContent.getText().toString().trim();
        int isPinned = cbPin.isChecked() ? 1 : 0;

        String subject = "Chung";
        if (spnSubject.getSelectedItem() != null) {
            String selected = spnSubject.getSelectedItem().toString();
            if (!"+ Thêm phân loại mới...".equals(selected) && !selected.trim().isEmpty()) {
                subject = selected.trim();
            }
        }

        if (title.isEmpty()) {
            edtTitle.setError("Tiêu đề không được để trống!");
            edtTitle.requestFocus();
            return;
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
