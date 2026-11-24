package com.example.pocketkit.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocketkit.network.gateway.CloudGateway;
import com.example.pocketkit.reminder.ReminderGateway;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.tools.ImageCompressGateway;
import com.example.pocketkit.transfer.ScheduleTransferGateway;

import java.util.concurrent.Executor;

public final class PocketKitViewModelFactory implements ViewModelProvider.Factory {

    private final PocketKitRepository repository;
    private final ReminderGateway reminderGateway;
    private final ScheduleTransferGateway transferGateway;
    private final CloudGateway cloudGateway;
    private final ImageCompressGateway imageCompressGateway;
    private final Executor ioExecutor;
    private final Executor networkExecutor;

    public PocketKitViewModelFactory(
            @NonNull PocketKitRepository repository,
            @NonNull ReminderGateway reminderGateway,
            @NonNull ScheduleTransferGateway transferGateway,
            @NonNull CloudGateway cloudGateway,
            @NonNull ImageCompressGateway imageCompressGateway,
            @NonNull Executor ioExecutor,
            @NonNull Executor networkExecutor
    ) {
        this.repository = repository;
        this.reminderGateway = reminderGateway;
        this.transferGateway = transferGateway;
        this.cloudGateway = cloudGateway;
        this.imageCompressGateway = imageCompressGateway;
        this.ioExecutor = ioExecutor;
        this.networkExecutor = networkExecutor;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(repository, reminderGateway, ioExecutor);
        }
        if (modelClass.isAssignableFrom(CalendarViewModel.class)) {
            return (T) new CalendarViewModel(repository, transferGateway, cloudGateway, ioExecutor, networkExecutor);
        }
        if (modelClass.isAssignableFrom(ToolsViewModel.class)) {
            return (T) new ToolsViewModel(repository, cloudGateway, imageCompressGateway, ioExecutor, networkExecutor);
        }
        if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(repository, cloudGateway, reminderGateway, ioExecutor, networkExecutor);
        }
        if (modelClass.isAssignableFrom(NotesViewModel.class)) {
            return (T) new NotesViewModel(repository, ioExecutor);
        }
        if (modelClass.isAssignableFrom(NoteEditorViewModel.class)) {
            return (T) new NoteEditorViewModel(repository, cloudGateway, ioExecutor, networkExecutor);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
