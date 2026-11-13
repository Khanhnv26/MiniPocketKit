package com.example.pocketkit;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.databinding.ActivityNoteEditorBinding;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.ui.FrostUiHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class NoteEditorActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_CATEGORY = "state_editor_category";
    private static final String STATE_PINNED = "state_editor_pinned";

    private ActivityNoteEditorBinding binding;
    private DatabaseHelper dbHelper;
    private Note existingNote;
    private String selectedCategory;
    private boolean isPinned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoteEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        selectedCategory = getString(R.string.note_default_category);

        FrostUiHelper.setupEdgeToEdgeAndBackdrop(
                this,
                binding.editorRoot,
                binding.editorScrollView,
                binding.ambientBackdrop.ambientBackdropView,
                null
        );

        if (getIntent().hasExtra(Note.EXTRA_NOTE_ITEM)) {
            existingNote = IntentCompat.getSerializableExtra(getIntent(), Note.EXTRA_NOTE_ITEM, Note.class);
            if (existingNote != null) {
                binding.tvEditorHeader.setText(R.string.editor_title_edit);
                binding.edtNoteTitle.setText(existingNote.getTitle());
                binding.edtNoteContent.setText(existingNote.getContent());
                if (existingNote.getSubject() != null && !existingNote.getSubject().trim().isEmpty()) {
                    selectedCategory = existingNote.getSubject().trim();
                }
                isPinned = existingNote.getIsPinned() == 1;
                binding.btnDeleteNote.setVisibility(View.VISIBLE);
            }
        }

        if (savedInstanceState != null) {
            selectedCategory = savedInstanceState.getString(STATE_SELECTED_CATEGORY, selectedCategory);
            isPinned = savedInstanceState.getBoolean(STATE_PINNED, isPinned);
        }

        renderCategoryChips();
        updatePinUi(false);

        binding.layoutPinToggle.setOnClickListener(v -> {
            isPinned = !isPinned;
            updatePinUi(true);
        });
        binding.btnBackEditor.setOnClickListener(v -> finish());
        binding.btnSaveNote.setOnClickListener(v -> saveNote());
        binding.btnDeleteNote.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_SELECTED_CATEGORY, selectedCategory);
        outState.putBoolean(STATE_PINNED, isPinned);
        super.onSaveInstanceState(outState);
    }

    private void renderCategoryChips() {
        binding.layoutEditorCategories.removeAllViews();

        String defaultCategory = getString(R.string.note_default_category);
        Set<String> uniqueCategories = new LinkedHashSet<>();
        uniqueCategories.add(defaultCategory);
        uniqueCategories.addAll(dbHelper.getAllCategories());
        if (selectedCategory != null && !selectedCategory.trim().isEmpty()) {
            uniqueCategories.add(selectedCategory.trim());
        }

        for (String category : uniqueCategories) {
            boolean selected = category.equals(selectedCategory);
            TextView chip = FrostUiHelper.createCategoryChip(
                    this,
                    binding.layoutEditorCategories,
                    category,
                    selected,
                    false
            );
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                renderCategoryChips();
            });
            binding.layoutEditorCategories.addView(chip);
        }

        TextView addChip = FrostUiHelper.createCategoryChip(
                this,
                binding.layoutEditorCategories,
                getString(R.string.notes_category_add_chip),
                false,
                true
        );
        addChip.setOnClickListener(v -> FrostUiHelper.showAddCategoryDialog(this, name -> {
            dbHelper.insertCategory(name);
            selectedCategory = name;
            renderCategoryChips();
            Toast.makeText(this, getString(R.string.notes_category_added_toast, name), Toast.LENGTH_SHORT).show();
        }));
        binding.layoutEditorCategories.addView(addChip);
    }

    private void updatePinUi(boolean animate) {
        float targetX = isPinned ? FrostUiHelper.dpToPx(this, 22) : 0f;
        binding.pinSwitchTrack.setBackgroundResource(
                isPinned ? R.drawable.bg_switch_track_on : R.drawable.bg_switch_track_off
        );
        binding.ivEditorPinIcon.setColorFilter(
                ContextCompat.getColor(this, isPinned ? R.color.accent_sapphire : R.color.ink_secondary)
        );
        if (animate && FrostUiHelper.areAnimationsEnabled(this)) {
            binding.pinSwitchThumb.animate().translationX(targetX).setDuration(180L).start();
        } else {
            binding.pinSwitchThumb.setTranslationX(targetX);
        }
    }

    private void saveNote() {
        String title = binding.edtNoteTitle.getText().toString().trim();
        String content = binding.edtNoteContent.getText().toString().trim();
        String subject = selectedCategory != null && !selectedCategory.trim().isEmpty()
                ? selectedCategory.trim()
                : getString(R.string.note_default_category);
        int pinnedFlag = isPinned ? 1 : 0;

        if (title.isEmpty()) {
            binding.edtNoteTitle.setError(getString(R.string.editor_error_empty_title));
            binding.edtNoteTitle.requestFocus();
            return;
        }

        String pattern = getString(R.string.note_timestamp_format);
        String now = new SimpleDateFormat(pattern, Locale.forLanguageTag("vi-VN")).format(new Date());

        if (existingNote == null) {
            Note note = Note.builder()
                    .title(title)
                    .content(content)
                    .subject(subject)
                    .isPinned(pinnedFlag)
                    .isDone(0)
                    .updatedAt(now)
                    .build();
            dbHelper.insertNote(note);
            Toast.makeText(this, R.string.editor_toast_created, Toast.LENGTH_SHORT).show();
        } else {
            existingNote.setTitle(title);
            existingNote.setSubject(subject);
            existingNote.setContent(content);
            existingNote.setIsPinned(pinnedFlag);
            existingNote.setUpdatedAt(now);
            dbHelper.updateNote(existingNote);
            Toast.makeText(this, R.string.editor_toast_updated, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void confirmDelete() {
        if (existingNote == null) {
            return;
        }
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.notes_delete_note_title,
                getString(R.string.editor_delete_confirm_message),
                R.string.notes_dialog_delete_confirm,
                true,
                () -> {
                    dbHelper.deleteNote(existingNote.getId());
                    Toast.makeText(this, R.string.notes_note_deleted_toast, Toast.LENGTH_SHORT).show();
                    finish();
                }
        );
    }
}
