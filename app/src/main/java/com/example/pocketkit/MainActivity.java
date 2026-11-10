package com.example.pocketkit;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pocketkit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainScrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.cardTodayGlance.setOnClickListener(v ->
                showToast("Chi tiết: 2 deadline sắp đến hạn nộp")
        );

        binding.layoutActionSchedule.setOnClickListener(v ->
                showToast("Mở Lịch học: 15:15 - Lập trình Android (Lab 3)")
        );

        binding.btnSettings.setOnClickListener(v ->
                showToast("Cài đặt ứng dụng & Chuyển đổi giao diện Sáng / Tối")
        );

        binding.cardToolNotes.setOnClickListener(v ->
                showToast("Mở Ghi chú & Checklist (Notes)")
        );

        binding.cardToolDeadline.setOnClickListener(v ->
                showToast("Mở Hạn nộp bài (Deadline Reminder)")
        );

        binding.cardToolTimetable.setOnClickListener(v ->
                showToast("Mở Thời khóa biểu (Timetable)")
        );

        binding.cardToolGpa.setOnClickListener(v ->
                showToast("Mở Tính điểm GPA & Dự đoán qua môn")
        );

        binding.cardToolFocus.setOnClickListener(v ->
                showToast("Mở Đồng hồ Focus (Pomodoro 25 phút)")
        );

        binding.cardToolCompress.setOnClickListener(v ->
                showToast("Mở Nén ảnh hồ sơ & CCCD (< 100KB, 500KB)")
        );

        binding.cardToolOcr.setOnClickListener(v ->
                showToast("Mở Quét slide & Trích xuất chữ tiếng Việt")
        );

        binding.cardToolPdf.setOnClickListener(v ->
                showToast("Mở Bộ tiện ích PDF (Gộp ảnh thành PDF)")
        );

        binding.cardToolSplitBill.setOnClickListener(v ->
                showToast("Mở Chia tiền phòng trọ / Điện nước EVN")
        );

        binding.cardToolRandom.setOnClickListener(v ->
                showToast("Mở Vòng quay chọn người thuyết trình")
        );

        binding.cardToolQr.setOnClickListener(v ->
                showToast("Mở Quét & Tạo mã QR tài liệu / WiFi")
        );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
