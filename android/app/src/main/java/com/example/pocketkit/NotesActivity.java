package com.example.pocketkit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pocketkit.adapter.NoteAdapter;
import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.databinding.ActivityNotesBinding;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.ui.FrostUiHelper;
import com.example.pocketkit.ui.MotionHelper;
import com.example.pocketkit.viewmodel.NotesViewModel;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity implements NoteAdapter.OnNoteInteractionListener {

    private ActivityNotesBinding binding;
    private NotesViewModel viewModel;
    private NoteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PocketKitApplication app = (PocketKitApplication) getApplication();
        viewModel = new ViewModelProvider(this, app.getViewModelFactory()).get(NotesViewModel.class);

        FrostUiHelper.setupEdgeToEdgeAndBackdrop(
                this,
                binding.notesRoot,
                binding.notesContentContainer,
                binding.ambientBackdropView,
                binding.btnAddNote
        );

        binding.rvNotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(new ArrayList<>(), this);
        binding.rvNotes.setAdapter(adapter);

        MotionHelper.registerClosePopTransition(this);
        MotionHelper.attachButtonPress(binding.btnBackFromNotes, binding.btnAddNote, binding.btnClearSearch);

        binding.btnBackFromNotes.setOnClickListener(v -> finish());
        binding.btnAddNote.setOnClickListener(v ->
                MotionHelper.startActivityPush(this, new Intent(this, NoteEditorActivity.class))
        );
        binding.btnClearSearch.setOnClickListener(v -> binding.edtSearchNote.setText(""));

        binding.edtSearchNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasQuery = s != null && s.length() > 0;
                binding.btnClearSearch.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                viewModel.updateSearchQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        viewModel.getNotesState().observe(this, this::renderState);
        viewModel.getToastEvent().observe(this, event -> FrostUiHelper.showToast(this, event));
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadNotesAndCategories();
    }

    private void renderState(@NonNull NotesViewModel.NotesUiState state) {
        renderCategoryChips(state.categories, state.selectedCategory);
        adapter.setNotes(state.notes);
        binding.tvNotesSummaryCount.setText(
                getResources().getQuantityString(R.plurals.notes_count_badge_format, state.notes.size(), state.notes.size())
        );

        if (state.notes.isEmpty()) {
            binding.tvEmptyNotes.setText(state.filtered ? R.string.notes_empty_search : R.string.notes_empty_default);
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            binding.emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void renderCategoryChips(@NonNull List<String> categories, @NonNull String selectedCategory) {
        binding.layoutCategories.removeAllViews();
        addFilterChip(getString(R.string.notes_filter_all), DatabaseHelper.FILTER_ALL, selectedCategory, false);
        addFilterChip(getString(R.string.notes_filter_pinned), DatabaseHelper.FILTER_PINNED, selectedCategory, false);
        for (String cat : categories) {
            addFilterChip(cat, cat, selectedCategory, true);
        }
        addActionChip(getString(R.string.notes_category_add_chip));
    }

    private void addFilterChip(
            @NonNull String label,
            @NonNull String value,
            @NonNull String selectedCategory,
            boolean allowDelete
    ) {
        boolean selected = selectedCategory.equals(value);
        TextView chip = FrostUiHelper.createCategoryChip(
                this,
                binding.layoutCategories,
                label,
                selected,
                false
        );
        chip.setOnClickListener(v -> viewModel.selectCategory(value));
        if (allowDelete) {
            chip.setOnLongClickListener(v -> {
                confirmDeleteCategory(value);
                return true;
            });
        }
        binding.layoutCategories.addView(chip);
    }

    private void addActionChip(@NonNull String label) {
        TextView chip = FrostUiHelper.createCategoryChip(
                this,
                binding.layoutCategories,
                label,
                false,
                true
        );
        chip.setOnClickListener(v -> FrostUiHelper.showAddCategoryDialog(this, name -> viewModel.addCategory(name)));
        binding.layoutCategories.addView(chip);
    }

    private void confirmDeleteCategory(@NonNull String categoryName) {
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.notes_category_delete_title,
                getString(R.string.notes_category_delete_message, categoryName),
                R.string.notes_dialog_delete_confirm,
                true,
                () -> viewModel.deleteCategory(categoryName)
        );
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra(Note.EXTRA_NOTE_ITEM, note);
        MotionHelper.startActivityPush(this, intent);
    }

    @Override
    public void finish() {
        super.finish();
        MotionHelper.applyLegacyClosePopTransition(this);
    }

    @Override
    public void onNoteLongClick(Note note) {
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.notes_delete_note_title,
                getString(R.string.notes_delete_note_message, note.getTitle()),
                R.string.notes_dialog_delete_confirm,
                true,
                () -> viewModel.deleteNote(note)
        );
    }

    @Override
    public void onNoteCheckChanged(Note note, boolean isChecked) {
        viewModel.toggleNoteDone(note, isChecked);
    }
}
