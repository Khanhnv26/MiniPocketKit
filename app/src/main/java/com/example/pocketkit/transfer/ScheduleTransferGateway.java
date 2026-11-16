package com.example.pocketkit.transfer;

import androidx.annotation.NonNull;

import com.example.pocketkit.model.ScheduleSlot;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ScheduleTransferGateway {

    @NonNull
    ScheduleTransferHelper.ImportResult parseImport(@NonNull String uriString, @NonNull String fileName)
            throws IOException;

    void writeExport(
            @NonNull String uriString,
            @NonNull List<ScheduleSlot> slots,
            @NonNull ScheduleTransferHelper.TransferFormat format
    ) throws IOException;

    @NonNull
    File writeShareFile(
            @NonNull List<ScheduleSlot> slots,
            @NonNull ScheduleTransferHelper.TransferFormat format,
            @NonNull String fileName
    ) throws IOException;

    @NonNull
    List<ScheduleSlot> buildTemplateSlots();
}
