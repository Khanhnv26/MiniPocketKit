package com.example.pocketkit;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText edtTitle, edtSubject, edtContent;
    private CheckBox cbPin;
    private TextView tvHeader, btnSave, btnDelete, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        edtTitle = findViewById(R.id.edtNoteTitle);
        edtSubject = findViewById(R.id.edtNoteSubject);
        edtContent = findViewById(R.id.edtNoteContent);
        cbPin = findViewById(R.id.cbPinNote);
        tvHeader = findViewById(R.id.tvEditorHeader);
        btnSave = findViewById(R.id.btnSaveNote);
        btnDelete = findViewById(R.id.btnDeleteNote);
        btnBack = findViewById(R.id.btnBackEditor);

        btnBack.setOnClickListener(v -> finish());
    }
}
