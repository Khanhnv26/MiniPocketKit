package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public class NoteEditorViewModel extends ViewModel {

    public static final class EditorUiState {
        public final boolean editing;
        @NonNull
        public final String selectedCategory;
        public final boolean pinned;
        public final boolean animatePin;
        @NonNull
        public final List<String> categories;

        public EditorUiState(
                boolean editing,
                @NonNull String selectedCategory,
                boolean pinned,
                boolean animatePin,
                @NonNull List<String> categories
        ) {
            this.editing = editing;
            this.selectedCategory = selectedCategory;
            this.pinned = pinned;
            this.animatePin = animatePin;
            this.categories = categories;
        }
    }

    private final PocketKitRepository repository;
    private final Executor ioExecutor;

    private final MutableLiveData<EditorUiState> editorState = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Note>> initialPopulateEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> emptyTitleErrorEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> closeScreenEvent = new MutableLiveData<>();

    private Note existingNote;
    private String selectedCategory = "";
    private String defaultCategory = "";
    private boolean isPinned = false;
    private boolean initialized = false;

    public NoteEditorViewModel(@NonNull PocketKitRepository repository, @NonNull Executor ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
    }

    @NonNull
    public LiveData<EditorUiState> getEditorState() {
        return editorState;
    }

    @NonNull
    public LiveData<UiEvent<Note>> getInitialPopulateEvent() {
        return initialPopulateEvent;
    }

    @NonNull
    public LiveData<UiEvent<Boolean>> getEmptyTitleErrorEvent() {
        return emptyTitleErrorEvent;
    }

    @NonNull
    public LiveData<UiEvent<UiMessage>> getToastEvent() {
        return toastEvent;
    }

    @NonNull
    public LiveData<UiEvent<Boolean>> getCloseScreenEvent() {
        return closeScreenEvent;
    }

    public boolean isEditing() {
        return existingNote != null;
    }

    public void initialize(@Nullable Note noteFromIntent, @NonNull String fallbackCategory) {
        this.defaultCategory = fallbackCategory;
        if (!initialized) {
            initialized = true;
            this.existingNote = noteFromIntent;
            this.selectedCategory = fallbackCategory;
            if (noteFromIntent != null) {
                if (noteFromIntent.getSubject() != null && !noteFromIntent.getSubject().trim().isEmpty()) {
                    this.selectedCategory = noteFromIntent.getSubject().trim();
                }
                this.isPinned = noteFromIntent.getIsPinned() == 1;
                initialPopulateEvent.setValue(new UiEvent<>(noteFromIntent));
            }
        }
        refreshCategories(false);
    }

    public void selectCategory(@NonNull String category) {
        selectedCategory = category;
        refreshCategories(false);
    }

    public void addCategory(@NonNull String categoryName) {
        ioExecutor.execute(() -> {
            repository.insertCategory(categoryName);
            selectedCategory = categoryName;
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.notes_category_added_toast, categoryName)));
            postState(false);
        });
    }

    public void togglePinned() {
        isPinned = !isPinned;
        refreshCategories(true);
    }

    public void saveNote(@NonNull String rawTitle, @NonNull String rawContent, @NonNull String timestampText) {
        String title = rawTitle.trim();
        if (title.isEmpty()) {
            emptyTitleErrorEvent.setValue(new UiEvent<>(true));
            return;
        }
        String content = rawContent.trim();
        String subject = selectedCategory != null && !selectedCategory.trim().isEmpty()
                ? selectedCategory.trim()
                : defaultCategory;
        int pinnedFlag = isPinned ? 1 : 0;

        ioExecutor.execute(() -> {
            if (existingNote == null) {
                Note note = Note.builder()
                        .title(title)
                        .content(content)
                        .subject(subject)
                        .isPinned(pinnedFlag)
                        .isDone(0)
                        .updatedAt(timestampText)
                        .build();
                repository.insertNote(note);
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.editor_toast_created)));
            } else {
                existingNote.setTitle(title);
                existingNote.setSubject(subject);
                existingNote.setContent(content);
                existingNote.setIsPinned(pinnedFlag);
                existingNote.setUpdatedAt(timestampText);
                repository.updateNote(existingNote);
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.editor_toast_updated)));
            }
            closeScreenEvent.postValue(new UiEvent<>(true));
        });
    }

    public void deleteNote() {
        if (existingNote == null) {
            return;
        }
        final int targetId = existingNote.getId();
        ioExecutor.execute(() -> {
            repository.deleteNote(targetId);
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.notes_note_deleted_toast)));
            closeScreenEvent.postValue(new UiEvent<>(true));
        });
    }

    private void refreshCategories(boolean animatePin) {
        ioExecutor.execute(() -> postState(animatePin));
    }

    private void postState(boolean animatePin) {
        Set<String> uniqueCategories = new LinkedHashSet<>();
        if (!defaultCategory.isEmpty()) {
            uniqueCategories.add(defaultCategory);
        }
        uniqueCategories.addAll(repository.getAllCategories());
        if (selectedCategory != null && !selectedCategory.trim().isEmpty()) {
            uniqueCategories.add(selectedCategory.trim());
        }
        editorState.postValue(new EditorUiState(
                existingNote != null,
                selectedCategory,
                isPinned,
                animatePin,
                new ArrayList<>(uniqueCategories)
        ));
    }
}
