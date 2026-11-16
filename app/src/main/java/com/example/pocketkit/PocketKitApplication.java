package com.example.pocketkit;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.pocketkit.reminder.TaskNotificationHelper;

public class PocketKitApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        TaskNotificationHelper.createNotificationChannel(this);
    }

    int getThemeChoice() {
        return 1;
    }

    void setThemeChoice(int choice) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
