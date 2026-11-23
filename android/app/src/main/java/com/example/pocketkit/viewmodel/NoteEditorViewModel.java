package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocketkit.R;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.exception.CloudException;
import com.example.pocketkit.network.gateway.CloudGateway;
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

    public static final class AiNoteDraft {
        @NonNull
        public final String suggestedTitle;
        @NonNull
        public final String previewSummary;
        @NonNull
        public final String formattedBody;

        public AiNoteDraft(
                @NonNull String suggestedTitle,
                @NonNull String previewSummary,
                @NonNull String formattedBody
        ) {
            this.suggestedTitle = suggestedTitle;
            this.previewSummary = previewSummary;
            this.formattedBody = formattedBody;
        }
    }

    @FunctionalInterface
    private interface AiSummaryCall {
        @NonNull
        ApiModels.NoteSummarizeResponse call() throws CloudException;
    }

    private final PocketKitRepository repository;
    private final CloudGateway cloudGateway;
    private final Executor ioExecutor;
    private final Executor networkExecutor;

    private final MutableLiveData<EditorUiState> editorState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> aiBusy = new MutableLiveData<>(false);
    private final MutableLiveData<UiEvent<Note>> initialPopulateEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<AiNoteDraft>> aiResultEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> emptyTitleErrorEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<UiMessage>> toastEvent = new MutableLiveData<>();
    private final MutableLiveData<UiEvent<Boolean>> closeScreenEvent = new MutableLiveData<>();

    private Note existingNote;
    private String selectedCategory = "";
    private String defaultCategory = "";
    private boolean isPinned = false;
    private boolean initialized = false;
    private volatile boolean aiRequestRunning = false;

    public NoteEditorViewModel(
            @NonNull PocketKitRepository repository,
            @NonNull CloudGateway cloudGateway,
            @NonNull Executor ioExecutor,
            @NonNull Executor networkExecutor
    ) {
        this.repository = repository;
        this.cloudGateway = cloudGateway;
        this.ioExecutor = ioExecutor;
        this.networkExecutor = networkExecutor;
    }

    @NonNull
    public LiveData<EditorUiState> getEditorState() {
        return editorState;
    }

    @NonNull
    public LiveData<Boolean> getAiBusy() {
        return aiBusy;
    }

    @NonNull
    public LiveData<UiEvent<Note>> getInitialPopulateEvent() {
        return initialPopulateEvent;
    }

    @NonNull
    public LiveData<UiEvent<AiNoteDraft>> getAiResultEvent() {
        return aiResultEvent;
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

    public void summarizeNoteText(@NonNull String rawTitle, @NonNull String rawContent) {
        String content = rawContent.trim();
        if (content.isEmpty()) {
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.editor_ai_empty_content_error)));
            return;
        }
        String title = rawTitle.trim();
        runAiSummary(() -> cloudGateway.summarizeNote(title, content));
    }

    public void summarizeNoteImage(@NonNull String imageUriString) {
        runAiSummary(() -> cloudGateway.summarizeNoteImage(imageUriString));
    }

    public void notifyAiApplied() {
        toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.editor_ai_applied_toast)));
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

    @NonNull
    public static String formatAiNoteBody(@NonNull ApiModels.NoteSummarizeResponse res) {
        StringBuilder sb = new StringBuilder();
        String summary = safeTrim(res.getSummary());
        if (!summary.isEmpty()) {
            sb.append("Tóm tắt: ").append(summary);
        }

        List<String> keyPoints = res.getKeyPoints();
        if (keyPoints != null && !keyPoints.isEmpty()) {
            StringBuilder pointsBuilder = new StringBuilder();
            for (String point : keyPoints) {
                String clean = safeTrim(point);
                if (!clean.isEmpty()) {
                    if (pointsBuilder.length() > 0) {
                        pointsBuilder.append('\n');
                    }
                    pointsBuilder.append("• ").append(clean);
                }
            }
            if (pointsBuilder.length() > 0) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append("Ý chính:\n").append(pointsBuilder);
            }
        }

        String formatted = safeTrim(res.getFormattedContent());
        if (!formatted.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Chi tiết:\n").append(formatted);
        }
        return sb.toString();
    }

    private void runAiSummary(@NonNull AiSummaryCall call) {
        if (aiRequestRunning) {
            return;
        }
        if (!cloudGateway.isLoggedIn()) {
            toastEvent.setValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_error_login_required)));
            return;
        }
        aiRequestRunning = true;
        aiBusy.postValue(true);
        networkExecutor.execute(() -> {
            try {
                ApiModels.NoteSummarizeResponse response = call.call();
                String body = formatAiNoteBody(response);
                String title = safeTrim(response.getTitle());
                String preview = safeTrim(response.getSummary());
                if (preview.isEmpty()) {
                    preview = body;
                }
                aiRequestRunning = false;
                aiBusy.postValue(false);
                if (body.isEmpty()) {
                    toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.editor_ai_empty_result_error)));
                    return;
                }
                aiResultEvent.postValue(new UiEvent<>(new AiNoteDraft(title, preview, body)));
            } catch (CloudException ex) {
                aiRequestRunning = false;
                aiBusy.postValue(false);
                toastEvent.postValue(new UiEvent<>(UiMessage.shortToast(R.string.cloud_message_raw, ex.getMessage())));
            }
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

    @NonNull
    private static String safeTrim(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
