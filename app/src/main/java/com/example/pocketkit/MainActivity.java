package com.example.pocketkit;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pocketkit.databinding.ActivityMainBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        // 1. Thẻ Hôm nay & Lịch học
        binding.cardTodayGlance.setOnClickListener(v ->
                showToast("Chi tiết: 2 deadline sắp đến hạn nộp")
        );

        binding.layoutActionSchedule.setOnClickListener(v ->
                showToast("Mở Lịch học và công việc chi tiết")
        );

        binding.btnSettings.setOnClickListener(v ->
                showToast("Cài đặt ứng dụng & Chuyển đổi giao diện Sáng / Tối")
        );

        // 2. Lưới 3 cột tiện ích chính
        binding.btnToolNotes.setOnClickListener(v ->
                showToast("Mở Ghi chú (Notes)")
        );

        binding.btnToolDeadline.setOnClickListener(v ->
                showToast("Mở Hạn nộp (Deadline Reminder)")
        );

        binding.btnToolTimetable.setOnClickListener(v ->
                showToast("Mở Lịch học (Timetable)")
        );

        binding.btnToolCompress.setOnClickListener(v ->
                showToast("Mở Nén ảnh hồ sơ (Image Compressor)")
        );

        binding.btnToolOcr.setOnClickListener(v ->
                showToast("Mở Quét slide & OCR chữ (Scan OCR)")
        );

        binding.btnToolGpa.setOnClickListener(v ->
                showToast("Mở Tính GPA & Điểm cần đạt")
        );

        binding.btnToolPdf.setOnClickListener(v ->
                showToast("Mở Bộ công cụ PDF (PDF Toolkit)")
        );

        binding.btnToolFocus.setOnClickListener(v ->
                showToast("Mở Đồng hồ Pomodoro (Focus Timer)")
        );

        // 3. Nút Tất cả: Mở Bottom Sheet mở rộng
        binding.btnToolAll.setOnClickListener(v -> showAllToolsBottomSheet());
    }

    private void showAllToolsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_all_tools, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btnSheetSplitBill).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Mở Chia tiền phòng trọ / Điện nước EVN");
        });

        sheetView.findViewById(R.id.btnSheetRandom).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Mở Vòng quay chọn người thuyết trình");
        });

        sheetView.findViewById(R.id.btnSheetQr).setOnClickListener(v -> {
            dialog.dismiss();
            showToast("Mở Quét & Tạo mã QR tài liệu");
        });

        dialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
