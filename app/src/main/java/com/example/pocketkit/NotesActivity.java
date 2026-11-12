package com.example.pocketkit;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketkit.adapter.NoteAdapter;
import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity implements NoteAdapter.OnNoteInteractionListener {

    private RecyclerView rvNotes;
    private EditText edtSearch;
    private TextView tvEmpty, btnAdd, btnBack;
    private LinearLayout layoutCategories;

    private DatabaseHelper dbHelper;
    private NoteAdapter adapter;
    private String currentCategory = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        dbHelper = new DatabaseHelper(this);

        rvNotes = findViewById(R.id.rvNotes);
        edtSearch = findViewById(R.id.edtSearchNote);
        tvEmpty = findViewById(R.id.tvEmptyNotes);
        btnAdd = findViewById(R.id.btnAddNote);
        btnBack = findViewById(R.id.btnBackFromNotes);
        layoutCategories = findViewById(R.id.layoutCategories);

        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(new ArrayList<>(), this);
        rvNotes.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderCategoryChips();
        loadNotes();
    }

    private void loadNotes() {
        List<Note> notes = dbHelper.getNotesFiltered(edtSearch.getText().toString(), currentCategory);
        adapter.setNotes(notes);
        tvEmpty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderCategoryChips() {
        layoutCategories.removeAllViews();
        addChip("Tất cả", "ALL", false);
        addChip("Đã ghim", "PINNED", false);
        for (String cat : dbHelper.getAllCategories()) {
            addChip(cat, cat, false);
        }
        addChip("+ Thêm", "ADD_NEW", true);
    }

    private void addChip(String label, String value, boolean isAddButton) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundResource(R.drawable.dashboard_pill);
        chip.setMinHeight(dpToPx(36));
        chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (layoutCategories.getChildCount() > 0) {
            params.setMarginStart(dpToPx(6));
        }
        chip.setLayoutParams(params);

        if (isAddButton) {
            chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dashboard_segment)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.dashboard_accent));
            chip.setOnClickListener(v -> showAddCategoryDialog());
        } else {
            boolean selected = currentCategory.equals(value);
            chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                    selected ? R.color.dashboard_text : R.color.dashboard_segment)));
            chip.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.dashboard_surface : R.color.dashboard_text));
            chip.setOnClickListener(v -> {
                currentCategory = value;
                renderCategoryChips();
                loadNotes();
            });
        }
        layoutCategories.addView(chip);
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Tên phân loại (VD: MAS291, CLB...)");
        int pad = dpToPx(20);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Thêm phân loại mới")
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        dbHelper.insertCategory(name);
                        currentCategory = name;
                        renderCategoryChips();
                        loadNotes();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
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
                    dbHelper.deleteNote(note.getId());
                    loadNotes();
                    Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onNoteCheckChanged(Note note, boolean isChecked) {
        note.setIsDone(isChecked ? 1 : 0);
        dbHelper.updateNote(note);
        rvNotes.post(this::loadNotes);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}