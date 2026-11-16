package com.example.pocketkit.transfer;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.pocketkit.R;
import com.example.pocketkit.model.ScheduleSlot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public final class AndroidScheduleTransferGateway implements ScheduleTransferGateway {

    private static final int MAX_TEMPLATE_ROWS_PER_DAY = 3;

    private final Context appContext;

    public AndroidScheduleTransferGateway(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public ScheduleTransferHelper.ImportResult parseImport(@NonNull String uriString, @NonNull String fileName)
            throws IOException {
        Uri uri = Uri.parse(uriString);
        try (InputStream is = appContext.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Cannot open input stream");
            }
            return ScheduleTransferHelper.parseSchedules(is, fileName);
        }
    }

    @Override
    public void writeExport(
            @NonNull String uriString,
            @NonNull List<ScheduleSlot> slots,
            @NonNull ScheduleTransferHelper.TransferFormat format
    ) throws IOException {
        Uri uri = Uri.parse(uriString);
        try (OutputStream os = appContext.getContentResolver().openOutputStream(uri)) {
            if (os == null) {
                throw new IOException("Cannot open output stream");
            }
            ScheduleTransferHelper.exportSchedules(slots, format, os);
        }
    }

    @NonNull
    @Override
    public File writeShareFile(
            @NonNull List<ScheduleSlot> slots,
            @NonNull ScheduleTransferHelper.TransferFormat format,
            @NonNull String fileName
    ) throws IOException {
        File exportDir = new File(appContext.getCacheDir(), "exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Cannot create cache export directory");
        }
        File outFile = new File(exportDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            ScheduleTransferHelper.exportSchedules(slots, format, fos);
        }
        return outFile;
    }

    @NonNull
    @Override
    public List<ScheduleSlot> buildTemplateSlots() {
        return ScheduleTransferHelper.buildSampleSlotsFromSeedRows(
                appContext.getResources().getStringArray(R.array.schedule_seed_rows),
                MAX_TEMPLATE_ROWS_PER_DAY
        );
    }
}
