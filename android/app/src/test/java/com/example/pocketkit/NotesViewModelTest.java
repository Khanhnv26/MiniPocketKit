package com.example.pocketkit;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.viewmodel.NotesViewModel;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NotesViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Test
    public void filterAndSearch_returnsMatchingNotes() {
        FakePocketKitRepository repository = new FakePocketKitRepository();
        repository.insertCategory("PRM392");
        repository.insertCategory("SWP391");
        repository.insertNote(Note.builder().title("On thi Room").subject("PRM392").isPinned(1).build());
        repository.insertNote(Note.builder().title("Bao cao SRS").subject("SWP391").isPinned(0).build());

        NotesViewModel viewModel = new NotesViewModel(repository, Runnable::run);
        viewModel.loadNotesAndCategories();

        NotesViewModel.NotesUiState initial = viewModel.getNotesState().getValue();
        assertNotNull(initial);
        assertEquals(2, initial.notes.size());

        viewModel.selectCategory("PRM392");
        NotesViewModel.NotesUiState filteredCat = viewModel.getNotesState().getValue();
        assertNotNull(filteredCat);
        assertEquals(1, filteredCat.notes.size());
        assertTrue(filteredCat.filtered);

        viewModel.deleteCategory("PRM392");
        NotesViewModel.NotesUiState afterDeleteCat = viewModel.getNotesState().getValue();
        assertNotNull(afterDeleteCat);
        assertEquals(DatabaseHelper.FILTER_ALL, afterDeleteCat.selectedCategory);
        assertEquals(2, afterDeleteCat.notes.size());
    }
}
