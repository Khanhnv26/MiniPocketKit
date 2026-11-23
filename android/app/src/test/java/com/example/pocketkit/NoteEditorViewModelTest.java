package com.example.pocketkit;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.model.Note;
import com.example.pocketkit.network.utils.ImageUploadHelper;
import com.example.pocketkit.viewmodel.NoteEditorViewModel;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NoteEditorViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Test
    public void saveNote_emptyTitle_emitsValidationEventWithoutSaving() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        NoteEditorViewModel viewModel = new NoteEditorViewModel(repository, new FakeCloudGateway(), Runnable::run, Runnable::run);
        viewModel.initialize(null, "Chung");

        viewModel.saveNote("   ", "Noi dung", "16/11 18:00");

        assertNotNull(viewModel.getEmptyTitleErrorEvent().getValue().getContentIfNotHandled());
        assertEquals(0, repository.getTotalNotesCount());
    }

    @Test
    public void initializeTwiceAfterRotation_preservesUserToggledPinAndCategory() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertCategory("PRM392");
        Note existing = Note.builder()
                .id(1)
                .title("Ghi chu cu")
                .content("Chi tiet")
                .subject("Chung")
                .isPinned(0)
                .build();
        repository.notes.add(existing);

        NoteEditorViewModel viewModel = new NoteEditorViewModel(repository, new FakeCloudGateway(), Runnable::run, Runnable::run);
        viewModel.initialize(existing, "Chung");
        viewModel.selectCategory("PRM392");
        viewModel.togglePinned();

        viewModel.initialize(existing, "Chung");
        NoteEditorViewModel.EditorUiState state = viewModel.getEditorState().getValue();
        assertNotNull(state);
        assertEquals("PRM392", state.selectedCategory);
        assertTrue(state.pinned);

        viewModel.saveNote("Ghi chu moi", "Da cap nhat", "16/11 18:10");
        assertEquals(1, repository.notes.get(0).getIsPinned());
        assertEquals("PRM392", repository.notes.get(0).getSubject());
        assertNotNull(viewModel.getCloseScreenEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void summarizeNoteText_emitsFormattedDraftWhenLoggedIn() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        cloudGateway.loggedIn = true;
        NoteEditorViewModel viewModel = new NoteEditorViewModel(repository, cloudGateway, Runnable::run, Runnable::run);

        viewModel.summarizeNoteText("Spring JWT", "Spring Security stateless dung Bearer token");

        NoteEditorViewModel.AiNoteDraft draft = viewModel.getAiResultEvent().getValue().getContentIfNotHandled();
        assertNotNull(draft);
        assertEquals("Spring JWT", draft.suggestedTitle);
        assertTrue(draft.formattedBody.contains("Tóm tắt: Tóm tắt ngắn gọn."));
        assertTrue(draft.formattedBody.contains("• Ý chính 1"));
        assertEquals("Spring Security stateless dung Bearer token", cloudGateway.lastSummarizedContent);
    }

    @Test
    public void summarizeNoteText_blankContent_emitsToastWithoutCallingCloud() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        cloudGateway.loggedIn = true;
        NoteEditorViewModel viewModel = new NoteEditorViewModel(repository, cloudGateway, Runnable::run, Runnable::run);

        viewModel.summarizeNoteText("Tieu de", "   ");

        assertNull(cloudGateway.lastSummarizedContent);
        assertNotNull(viewModel.getToastEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void summarizeNoteImage_emitsFormattedDraftWhenLoggedIn() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        FakeCloudGateway cloudGateway = new FakeCloudGateway();
        cloudGateway.loggedIn = true;
        NoteEditorViewModel viewModel = new NoteEditorViewModel(repository, cloudGateway, Runnable::run, Runnable::run);

        viewModel.summarizeNoteImage("content://media/note.jpg");

        NoteEditorViewModel.AiNoteDraft draft = viewModel.getAiResultEvent().getValue().getContentIfNotHandled();
        assertNotNull(draft);
        assertEquals("Ghi chú từ ảnh", draft.suggestedTitle);
        assertEquals("content://media/note.jpg", cloudGateway.lastSummarizedImageUri);
    }

    @Test
    public void imageUploadHelper_computeInSampleSize_scalesDownOversizedImages() {
        assertEquals(1, ImageUploadHelper.computeInSampleSize(1920, 1080, 2048));
        assertEquals(2, ImageUploadHelper.computeInSampleSize(4000, 3000, 2048));
        assertEquals(4, ImageUploadHelper.computeInSampleSize(6000, 4000, 2048));
    }
}
