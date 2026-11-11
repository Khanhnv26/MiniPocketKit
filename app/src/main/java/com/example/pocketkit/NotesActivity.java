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
    private TextView tvEmpty, btnAdd, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        rvNotes = findViewById(R.id.rvNotes);
        edtSearch = findViewById(R.id.edtSearchNote);
        tvEmpty = findViewById(R.id.tvEmptyNotes);
        btnAdd = findViewById(R.id.btnAddNote);
        btnBack = findViewById(R.id.btnBackFromNotes);

        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditorActivity.class);
            startActivity(intent);
        });
    }
}
