package com.example.pocketkit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketkit.adapter.NoteAdapter;
import com.example.pocketkit.dto.NoteRequestDto;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.repository.NoteRepository;

import java.util.ArrayList;

public class NotesActivity extends AppCompatActivity implements NoteAdapter.OnNoteInteractionListener {

    private RecyclerView rvNotes;
    private EditText edtSearch;
    private TextView tvEmpty, btnAdd, btnBack;

    private NoteRepository noteRepository;
    private NoteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        noteRepository = new NoteRepository(this);

        rvNotes = findViewById(R.id.rvNotes);
        edtSearch = findViewById(R.id.edtSearchNote);
        tvEmpty = findViewById(R.id.tvEmptyNotes);
        btnAdd = findViewById(R.id.btnAddNote);
        btnBack = findViewById(R.id.btnBackFromNotes);

        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(new ArrayList<>(), this);
        rvNotes.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, NoteEditorActivity.class);
            startActivity(intent);
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(edtSearch.getText().toString());
    }

    private void loadNotes(String keyword) {
        noteRepository.getNotes(keyword, (notes, isFromCloud) -> {
            adapter.setNotes(notes);
            tvEmpty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("note_item", note);
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa ghi chú")
                .setMessage("Bạn muốn xóa '" + note.getTitle() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    noteRepository.deleteNote(note.getId());
                    loadNotes(edtSearch.getText().toString());
                    Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onNoteCheckChanged(Note note, boolean isChecked) {
        NoteRequestDto dto = NoteRequestDto.builder()
                .title(note.getTitle())
                .content(note.getContent())
                .subject(note.getSubject())
                .isPinned(note.getIsPinned())
                .isDone(isChecked ? 1 : 0)
                .updateAt(note.getUpdateAt())
                .build();
        noteRepository.updateNote(note.getId(), dto);
        rvNotes.post(() -> loadNotes(edtSearch.getText().toString()));
    }
}
