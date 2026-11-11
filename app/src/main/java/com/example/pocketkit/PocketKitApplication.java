package com.example.pocketkit;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class PocketKitApplication extends Application {

    private static final int[] THEME_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(THEME_MODES[getThemeChoice()]);
    }

    int getThemeChoice() {
        int choice = getSharedPreferences("appearance", MODE_PRIVATE).getInt("theme_choice", 0);
        return choice >= 0 && choice < THEME_MODES.length ? choice : 0;
    }

    void setThemeChoice(int choice) {
        getSharedPreferences("appearance", MODE_PRIVATE)
                .edit()
                .putInt("theme_choice", choice)
                .apply();
        AppCompatDelegate.setDefaultNightMode(THEME_MODES[choice]);
    }
}
