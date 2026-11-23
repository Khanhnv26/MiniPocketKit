package com.example.pocketkit;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketkit.databinding.ActivityNoteEditorBinding;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.ui.FrostUiHelper;
import com.example.pocketkit.viewmodel.NoteEditorViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteEditorActivity extends AppCompatActivity {

    private ActivityNoteEditorBinding binding;
    private NoteEditorViewModel viewModel;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickNoteImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    viewModel.summarizeNoteImage(uri.toString());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoteEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PocketKitApplication app = (PocketKitApplication) getApplication();
        viewModel = new ViewModelProvider(this, app.getViewModelFactory()).get(NoteEditorViewModel.class);

        FrostUiHelper.setupEdgeToEdgeAndBackdrop(
                this,
                binding.editorRoot,
                binding.editorScrollView,
                binding.ambientBackdropView,
                null
        );

        Note noteFromIntent = getIntent().hasExtra(Note.EXTRA_NOTE_ITEM)
                ? IntentCompat.getSerializableExtra(getIntent(), Note.EXTRA_NOTE_ITEM, Note.class)
                : null;
        viewModel.initialize(noteFromIntent, getString(R.string.note_default_category));

        if (savedInstanceState == null && noteFromIntent == null) {
            String initialContent = getIntent().getStringExtra(Note.EXTRA_INITIAL_CONTENT);
            if (initialContent != null && !initialContent.trim().isEmpty()) {
                binding.edtNoteContent.setText(initialContent.trim());
            }
        }

        binding.layoutPinToggle.setOnClickListener(v -> viewModel.togglePinned());
        binding.btnBackEditor.setOnClickListener(v -> finish());
        binding.btnAiSummarizeText.setOnClickListener(v -> viewModel.summarizeNoteText(
                binding.edtNoteTitle.getText().toString(),
                binding.edtNoteContent.getText().toString()
        ));
        binding.btnAiSummarizeImage.setOnClickListener(v ->
                pickNoteImageLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())
        );
        binding.btnSaveNote.setOnClickListener(v -> {
            String pattern = getString(R.string.note_timestamp_format);
            String now = new SimpleDateFormat(pattern, Locale.forLanguageTag("vi-VN")).format(new Date());
            viewModel.saveNote(
                    binding.edtNoteTitle.getText().toString(),
                    binding.edtNoteContent.getText().toString(),
                    now
            );
        });
        binding.btnDeleteNote.setOnClickListener(v -> confirmDelete());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getInitialPopulateEvent().observe(this, event -> {
            Note note = event.getContentIfNotHandled();
            if (note != null) {
                binding.edtNoteTitle.setText(note.getTitle());
                binding.edtNoteContent.setText(note.getContent());
            }
        });

        viewModel.getEditorState().observe(this, state -> {
            if (state.editing) {
                binding.tvEditorHeader.setText(R.string.editor_title_edit);
                binding.btnDeleteNote.setVisibility(View.VISIBLE);
            } else {
                binding.tvEditorHeader.setText(R.string.editor_title_create);
                binding.btnDeleteNote.setVisibility(View.GONE);
            }
            renderCategoryChips(state.categories, state.selectedCategory);
            updatePinUi(state.pinned, state.animatePin);
        });

        viewModel.getAiBusy().observe(this, busy -> {
            boolean isBusy = Boolean.TRUE.equals(busy);
            boolean enabled = !isBusy;
            float alpha = enabled ? 1.0f : 0.55f;
            binding.btnAiSummarizeText.setEnabled(enabled);
            binding.btnAiSummarizeText.setAlpha(alpha);
            binding.btnAiSummarizeText.setText(
                    isBusy ? R.string.editor_ai_busy : R.string.editor_ai_btn_summarize
            );
            binding.btnAiSummarizeImage.setEnabled(enabled);
            binding.btnAiSummarizeImage.setAlpha(alpha);
        });

        viewModel.getAiResultEvent().observe(this, event -> {
            NoteEditorViewModel.AiNoteDraft draft = event.getContentIfNotHandled();
            if (draft != null) {
                showAiDraftPreview(draft);
            }
        });

        viewModel.getEmptyTitleErrorEvent().observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                binding.edtNoteTitle.setError(getString(R.string.editor_error_empty_title));
                binding.edtNoteTitle.requestFocus();
            }
        });

        viewModel.getToastEvent().observe(this, event -> FrostUiHelper.showToast(this, event));

        viewModel.getCloseScreenEvent().observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                finish();
            }
        });
    }

    private void showAiDraftPreview(@NonNull NoteEditorViewModel.AiNoteDraft draft) {
        String message = getString(R.string.editor_ai_preview_message_format, draft.previewSummary);
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.editor_ai_preview_title,
                message,
                R.string.editor_ai_btn_apply,
                false,
                () -> {
                    if (binding.edtNoteTitle.getText().toString().trim().isEmpty()
                            && !draft.suggestedTitle.isEmpty()) {
                        binding.edtNoteTitle.setText(draft.suggestedTitle);
                    }
                    binding.edtNoteContent.setText(draft.formattedBody);
                    viewModel.notifyAiApplied();
                }
        );
    }

    private void renderCategoryChips(@NonNull List<String> categories, @NonNull String selectedCategory) {
        binding.layoutEditorCategories.removeAllViews();

        for (String category : categories) {
            boolean selected = category.equals(selectedCategory);
            TextView chip = FrostUiHelper.createCategoryChip(
                    this,
                    binding.layoutEditorCategories,
                    category,
                    selected,
                    false
            );
            chip.setOnClickListener(v -> viewModel.selectCategory(category));
            binding.layoutEditorCategories.addView(chip);
        }

        TextView addChip = FrostUiHelper.createCategoryChip(
                this,
                binding.layoutEditorCategories,
                getString(R.string.notes_category_add_chip),
                false,
                true
        );
        addChip.setOnClickListener(v -> FrostUiHelper.showAddCategoryDialog(this, name -> viewModel.addCategory(name)));
        binding.layoutEditorCategories.addView(addChip);
    }

    private void updatePinUi(boolean isPinned, boolean animate) {
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

    private void confirmDelete() {
        if (!viewModel.isEditing()) {
            return;
        }
        FrostUiHelper.showConfirmDialog(
                this,
                R.string.notes_delete_note_title,
                getString(R.string.editor_delete_confirm_message),
                R.string.notes_dialog_delete_confirm,
                true,
                () -> viewModel.deleteNote()
        );
    }
}
