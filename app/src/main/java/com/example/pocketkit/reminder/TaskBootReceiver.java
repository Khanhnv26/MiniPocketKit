package com.example.pocketkit.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskBootReceiver extends BroadcastReceiver {

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        PendingResult pendingResult = goAsync();
        IO_EXECUTOR.execute(() -> {
            try {
                TaskReminderScheduler.rescheduleAllPendingReminders(appContext);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
