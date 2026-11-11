package com.example.pocketkit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class NotesActivity extends AppCompatActivity {

    private RecyclerView rvNotes;
    private EditText edtSearch;
    private TextView tvEmpty, btnAdd, btnBack, btnAddCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        rvNotes = findViewById(R.id.rvNotes);
        edtSearch = findViewById(R.id.edtSearchNote);
        tvEmpty = findViewById(R.id.tvEmptyNotes);
        btnAdd = findViewById(R.id.btnAddNote);
        btnBack = findViewById(R.id.btnBackFromNotes);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditorActivity.class);
            startActivity(intent);
        });

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void showAddCategoryDialog() {
        final EditText input = new EditText(this);
        input.setHint("Tên phân loại (VD: MAS291, CLB...)");
        input.setPadding(40, 30, 40, 30);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Thêm phân loại mới")
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String categoryName = input.getText().toString().trim();
                    if (!categoryName.isEmpty()) {
                        android.widget.Toast.makeText(this, "Đã thêm phân loại: " + categoryName, android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
