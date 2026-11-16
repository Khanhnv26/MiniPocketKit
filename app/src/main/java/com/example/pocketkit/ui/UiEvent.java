package com.example.pocketkit.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UiEvent<T> {

    private final T content;
    private boolean handled = false;

    public UiEvent(@NonNull T content) {
        this.content = content;
    }

    @Nullable
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    @NonNull
    public T peekContent() {
        return content;
    }
}
