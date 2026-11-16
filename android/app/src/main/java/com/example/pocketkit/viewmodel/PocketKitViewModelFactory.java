package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketkit.reminder.ReminderGateway;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.transfer.ScheduleTransferGateway;

import java.util.concurrent.Executor;

public final class PocketKitViewModelFactory implements ViewModelProvider.Factory {

    private final PocketKitRepository repository;
    private final ReminderGateway reminderGateway;
    private final ScheduleTransferGateway transferGateway;
    private final Executor ioExecutor;

    public PocketKitViewModelFactory(
            @NonNull PocketKitRepository repository,
            @NonNull ReminderGateway reminderGateway,
            @NonNull ScheduleTransferGateway transferGateway,
            @NonNull Executor ioExecutor
    ) {
        this.repository = repository;
        this.reminderGateway = reminderGateway;
        this.transferGateway = transferGateway;
        this.ioExecutor = ioExecutor;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(repository, reminderGateway, ioExecutor);
        }
        if (modelClass.isAssignableFrom(CalendarViewModel.class)) {
            return (T) new CalendarViewModel(repository, transferGateway, ioExecutor);
        }
        if (modelClass.isAssignableFrom(ToolsViewModel.class)) {
            return (T) new ToolsViewModel(repository, ioExecutor);
        }
        if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(repository);
        }
        if (modelClass.isAssignableFrom(NotesViewModel.class)) {
            return (T) new NotesViewModel(repository, ioExecutor);
        }
        if (modelClass.isAssignableFrom(NoteEditorViewModel.class)) {
            return (T) new NoteEditorViewModel(repository, ioExecutor);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
