package com.example.pocketkit.network.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.network.dto.ApiModels;

public class SessionStore {
    private static final String PREFS_NAME = "pocketkit_auth_prefs";
    private static final String KEY_TOKEN = "key_access_token";
    private static final String KEY_EXPIRES_AT = "key_expires_at";
    private static final String KEY_USER_ID = "key_user_id";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_FULL_NAME = "key_full_name";
    private static final String KEY_LAST_BACKUP_EPOCH = "key_last_backup_epoch";

    private final SharedPreferences prefs;

    public SessionStore(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public void saveAuth(@NonNull ApiModels.AuthResponse authResponse, long nowMillis) {
        long expiresAt = authResponse.getExpiresInMs() > 0L ? nowMillis + authResponse.getExpiresInMs() : 0L;

        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_TOKEN, authResponse.getAccessToken())
                .putLong(KEY_EXPIRES_AT, expiresAt);

        ApiModels.UserSummary user = authResponse.getUser();

        if(user != null) {
            editor.putLong(KEY_USER_ID, user.getId() != null ? user.getId() : 0L)
                    .putString(KEY_EMAIL, user.getEmail())
                    .putString(KEY_FULL_NAME, user.getFullName());
        }

        editor.apply();
    }

    public void saveLastBackupEpoch(long epochMillis) {
        prefs.edit().putLong(KEY_LAST_BACKUP_EPOCH, Math.max(0L, epochMillis)).apply();
    }

    public boolean isLoggedIn() {
        String token = prefs.getString(KEY_TOKEN, null);

        if(token == null || token.trim().isEmpty()) {
            return false;
        }

        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L);
        return expiresAt <= 0L || System.currentTimeMillis() < expiresAt;
    }

    @Nullable
    public String getAccessToken() {
        return isLoggedIn() ? prefs.getString(KEY_TOKEN, null) : null;
    }

    @NonNull
    public String getEmail() {
        String value = prefs.getString(KEY_EMAIL, null);
        return value != null ? value : "";
    }

    public String getFullName() {
        String value = prefs.getString(KEY_FULL_NAME, null);
        return value != null ? value : "";
    }

    public long getLastBackupEpoch() {
        return prefs.getLong(KEY_LAST_BACKUP_EPOCH, 0L);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }







}
