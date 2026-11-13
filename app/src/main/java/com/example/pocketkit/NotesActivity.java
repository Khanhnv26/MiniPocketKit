package com.example.pocketkit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pocketkit.adapter.NoteAdapter;
import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.databinding.ActivityNotesBinding;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.ui.FrostUiHelper;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity implements NoteAdapter.OnNoteInteractionListener {

    private static final String STATE_SELECTED_CATEGORY = "state_selected_category";

    private ActivityNotesBinding binding;
    private DatabaseHelper dbHelper;
    private NoteAdapter adapter;
    private String currentCategory = DatabaseHelper.FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            currentCategory = savedInstanceState.getString(STATE_SELECTED_CATEGORY, DatabaseHelper.FILTER_ALL);
        }

        dbHelper = new DatabaseHelper(this);

        FrostUiHelper.setupEdgeToEdgeAndBackdrop(
                this,
                binding.notesRoot,
                binding.notesContentContainer,
                binding.ambientBackdrop.ambientBackdropView,
                binding.bottomCtaContainer
        );

        binding.rvNotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(new ArrayList<>(), this);
        binding.rvNotes.setAdapter(adapter);

        binding.btnBackFromNotes.setOnClickListener(v -> finish());
        binding.btnAddNote.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));
        binding.btnClearSearch.setOnClickListener(v -> binding.edtSearchNote.setText(""));

        binding.edtSearchNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasQuery = s != null && s.length() > 0;
                binding.btnClearSearch.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderCategoryChips();
        loadNotes();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_SELECTED_CATEGORY, currentCategory);
        super.onSaveInstanceState(outState);
    }

    private void loadNotes() {
        String query = binding.edtSearchNote.getText().toString();
        List<Note> notes = dbHelper.getNotesFiltered(query, currentCategory);
        adapter.setNotes(notes);
        binding.tvNotesSummaryCount.setText(getString(R.string.notes_count_badge_format, notes.size()));

        if (notes.isEmpty()) {
            boolean filtered = !query.trim().isEmpty() || !DatabaseHelper.FILTER_ALL.equals(currentCategory);
            binding.tvEmptyNotes.setText(filtered ? R.string.notes_empty_search : R.string.notes_empty_default);
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            binding.emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void renderCategoryChips() {
        binding.layoutCategories.removeAllViews();

        List<String> categories = dbHelper.getAllCategories();
        if (!DatabaseHelper.FILTER_ALL.equals(currentCategory)
                && !DatabaseHelper.FILTER_PINNED.equals(currentCategory)
                && !categories.contains(currentCategory)) {
            currentCategory = DatabaseHelper.FILTER_ALL;
        }

        addFilterChip(getString(R.string.notes_filter_all), DatabaseHelper.FILTER_ALL, false);
        addFilterChip(getString(R.string.notes_filter_pinned), DatabaseHelper.FILTER_PINNED, false);
        for (String cat : categories) {
            addFilterChip(cat, cat, true);
        }
        addActionChip(getString(R.string.notes_category_add_chip));
    }

    private void addFilterChip(String label, String value, boolean allowDelete) {
        boolean selected = currentCategory.equals(value);
        TextView chip = FrostUiHelper.createCategoryChip(
                this,
                binding.layoutCategories,
                label,
                selected,
                false
        );
        chip.setOnClickListener(v -> {
            currentCategory = value;
            renderCategoryChips();
            loadNotes();
        });
        if (allowDelete) {
            chip.setOnLongClickListener(v -> {
                confirmDeleteCategory(value);
                return true;
            });
        }
        binding.layoutCategories.addView(chip);
    }

    private void addActionChip(String label) {
        TextView chip = FrostUiHelper.createCategoryChip(
                this,
                binding.layoutCategories,
                label,
                false,
                true
        );
        chip.setOnClickListener(v -> FrostUiHelper.showAddCategoryDialog(this, name -> {
            dbHelper.insertCategory(name);
            currentCategory = name;
            renderCategoryChips();
            loadNotes();
            Toast.makeText(this, getString(R.string.notes_category_added_toast, name), Toast.LENGTH_SHORT).show();
        }));
        binding.layoutCategories.addView(chip);
    }

    private void confirmDeleteCategory(String categoryName) {
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.notes_category_delete_title,
                getString(R.string.notes_category_delete_message, categoryName),
                R.string.notes_dialog_delete_confirm,
                true,
                () -> {
                    dbHelper.deleteCategory(categoryName);
                    if (categoryName.equals(currentCategory)) {
                        currentCategory = DatabaseHelper.FILTER_ALL;
                    }
                    renderCategoryChips();
                    loadNotes();
                    Toast.makeText(this, getString(R.string.notes_category_deleted_toast, categoryName), Toast.LENGTH_SHORT).show();
                }
        );
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra(Note.EXTRA_NOTE_ITEM, note);
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(Note note) {
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.notes_delete_note_title,
                getString(R.string.notes_delete_note_message, note.getTitle()),
                R.string.notes_dialog_delete_confirm,
                true,
                () -> {
                    dbHelper.deleteNote(note.getId());
                    loadNotes();
                    Toast.makeText(this, R.string.notes_note_deleted_toast, Toast.LENGTH_SHORT).show();
                }
        );
    }

    @Override
    public void onNoteCheckChanged(Note note, boolean isChecked) {
        note.setIsDone(isChecked ? 1 : 0);
        dbHelper.updateNote(note);
        binding.rvNotes.post(this::loadNotes);
    }
}
