package com.example.pocketkit;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pocketkit.network.gateway.CloudGateway;
import com.example.pocketkit.network.gateway.RetrofitCloudGateway;
import com.example.pocketkit.network.session.SessionStore;
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
    private CloudGateway cloudGateway;
    private ExecutorService sharedIoExecutor;
    private ExecutorService networkExecutor;
    private PocketKitViewModelFactory viewModelFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        TaskNotificationHelper.createNotificationChannel(this);

        this.repository = new LocalPocketKitRepository(this);
        this.reminderGateway = new SystemReminderGateway(this);
        this.scheduleTransferGateway = new AndroidScheduleTransferGateway(this);
        this.cloudGateway = new RetrofitCloudGateway(this, BuildConfig.API_BASE_URL, new SessionStore(this));
        this.sharedIoExecutor = Executors.newSingleThreadExecutor();
        this.networkExecutor = Executors.newFixedThreadPool(2);
        this.viewModelFactory = new PocketKitViewModelFactory(
                repository,
                reminderGateway,
                scheduleTransferGateway,
                cloudGateway,
                sharedIoExecutor,
                networkExecutor
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
