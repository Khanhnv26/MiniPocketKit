package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.ui.UiEvent;
import com.example.pocketkit.ui.UiMessage;

import java.util.List;
import java.util.concurrent.Executor;

public class NotesViewModel extends ViewModel {

    public static final class NotesUiState {
        @NonNull
        public final String selectedCategory;
        @NonNull
        public final List<String> categories;
        @NonNull
        public final List<Note> notes;
        public final boolean filtered;

        public NotesUiState(
                @NonNull String selectedCategory,
                @NonNull List<String> categories,
                @NonNull List<Note> notes,
                boolean filtered
        ) {
            this.selectedCategory = selectedCategory;
            this.categories = categories;
            this.notes = notes;
            this.filtered = filtered;
        }
    }

    private final PocketKitRepository repository;
    private final Executor ioExecutor;

    private final MutableLiveData<NotesUiState> notesState = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();

    private String currentCategory = DatabaseHelper.FILTER_ALL;
    private String currentQuery = "";

    public NotesViewModel(@NonNull PocketKitRepository repository, @NonNull Executor ioExecutor) {
        this.repository = repository;
        this.ioExecutor = ioExecutor;
    }

    @NonNull
    public LiveData<NotesUiState> getNotesState() {
        return notesState;
    }

    @NonNull
    public LiveData<UiEvent<UiMessage>> getToastEvent() {
        return toastEvent;
    }

    public void loadNotesAndCategories() {
        final String querySnapshot = currentQuery;
        ioExecutor.execute(() -> postSnapshot(querySnapshot));
    }

    public void selectCategory(@NonNull String category) {
        currentCategory = category;
        loadNotesAndCategories();
    }

    public void updateSearchQuery(@NonNull String query) {
        currentQuery = query;
        loadNotesAndCategories();
    }

    public void addCategory(@NonNull String name) {
        ioExecutor.execute(() -> {
            repository.insertCategory(name);
            currentCategory = name;
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.notes_category_added_toast, name)));
            postSnapshot(currentQuery);
        });
    }

    public void deleteCategory(@NonNull String categoryName) {
        ioExecutor.execute(() -> {
            repository.deleteCategory(categoryName);
            if (categoryName.equals(currentCategory)) {
                currentCategory = DatabaseHelper.FILTER_ALL;
            }
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.notes_category_deleted_toast, categoryName)));
            postSnapshot(currentQuery);
        });
    }

    public void deleteNote(@NonNull Note note) {
        ioExecutor.execute(() -> {
            repository.deleteNote(note.getId());
            toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.notes_note_deleted_toast)));
            postSnapshot(currentQuery);
        });
    }

    public void toggleNoteDone(@NonNull Note note, boolean isChecked) {
        final Note updated = Note.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .subject(note.getSubject())
                .isPinned(note.getIsPinned())
                .isDone(isChecked ? 1 : 0)
                .updatedAt(note.getUpdatedAt())
                .build();
        ioExecutor.execute(() -> {
            repository.updateNote(updated);
            postSnapshot(currentQuery);
        });
    }

    private void postSnapshot(@NonNull String querySnapshot) {
        List<String> categories = repository.getAllCategories();
        if (!DatabaseHelper.FILTER_ALL.equals(currentCategory)
                && !DatabaseHelper.FILTER_PINNED.equals(currentCategory)
                && !categories.contains(currentCategory)) {
            currentCategory = DatabaseHelper.FILTER_ALL;
        }
        List<Note> notes = repository.getNotesFiltered(querySnapshot, currentCategory);
        boolean filtered = !querySnapshot.trim().isEmpty() || !DatabaseHelper.FILTER_ALL.equals(currentCategory);
        notesState.postValue(new NotesUiState(currentCategory, categories, notes, filtered));
    }
}
