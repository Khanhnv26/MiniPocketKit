package com.example.pocketkit;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class PocketKitApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    int getThemeChoice() {
        return 1;
    }

    void setThemeChoice(int choice) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
