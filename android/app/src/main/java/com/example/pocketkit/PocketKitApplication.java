package com.example.pocketkit;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pocketkit.reminder.ReminderGateway;
import com.example.pocketkit.reminder.SystemReminderGateway;
import com.example.pocketkit.reminder.TaskNotificationHelper;
import com.example.pocketkit.repository.LocalPocketKitRepository;
import com.example.pocketkit.repository.PocketKitRepository;
import com.example.pocketkit.transfer.AndroidScheduleTransferGateway;
import com.example.pocketkit.transfer.ScheduleTransferGateway;
import com.example.pocketkit.viewmodel.PocketKitViewModelFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PocketKitApplication extends Application {

    private PocketKitRepository repository;
    private ReminderGateway reminderGateway;
    private ScheduleTransferGateway scheduleTransferGateway;
    private ExecutorService sharedIoExecutor;
    private PocketKitViewModelFactory viewModelFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        TaskNotificationHelper.createNotificationChannel(this);

        this.repository = new LocalPocketKitRepository(this);
        this.reminderGateway = new SystemReminderGateway(this);
        this.scheduleTransferGateway = new AndroidScheduleTransferGateway(this);
        this.sharedIoExecutor = Executors.newSingleThreadExecutor();
        this.viewModelFactory = new PocketKitViewModelFactory(
                repository,
                reminderGateway,
                scheduleTransferGateway,
                sharedIoExecutor
        );
    }

    @NonNull
    public PocketKitRepository getRepository() {
        return repository;
    }

    @NonNull
    public PocketKitViewModelFactory getViewModelFactory() {
        return viewModelFactory;
    }

    int getThemeChoice() {
        return 1;
    }

    void setThemeChoice(int choice) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
