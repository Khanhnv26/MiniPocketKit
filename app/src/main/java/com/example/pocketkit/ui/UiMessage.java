package com.example.pocketkit.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public final class UiMessage {

    public static final class StringRefArg {
        @StringRes
        public final int resId;

        public StringRefArg(@StringRes int resId) {
            this.resId = resId;
        }
    }

    @StringRes
    public final int resId;
    public final boolean longDuration;
    @NonNull
    public final Object[] formatArgs;

    public UiMessage(@StringRes int resId, boolean longDuration, @NonNull Object... formatArgs) {
        this.resId = resId;
        this.longDuration = longDuration;
        this.formatArgs = formatArgs;
    }

    @NonNull
    public static UiMessage shortToast(@StringRes int resId, @NonNull Object... args) {
        return new UiMessage(resId, false, args);
    }

    @NonNull
    public static UiMessage longToast(@StringRes int resId, @NonNull Object... args) {
        return new UiMessage(resId, true, args);
    }

    @NonNull
    public static StringRefArg strRef(@StringRes int resId) {
        return new StringRefArg(resId);
    }

    @NonNull
    public String resolve(@NonNull Context context) {
        if (formatArgs.length == 0) {
            return context.getString(resId);
        }
        Object[] resolved = new Object[formatArgs.length];
        for (int i = 0; i < formatArgs.length; i++) {
            Object arg = formatArgs[i];
            if (arg instanceof StringRefArg) {
                resolved[i] = context.getString(((StringRefArg) arg).resId);
            } else {
                resolved[i] = arg;
            }
        }
        return context.getString(resId, resolved);
    }
}
