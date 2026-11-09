package com.example.pocketkit;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pocketkit.databinding.ActivityMainBinding;

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
        // 1. Dual Hero Live Widgets
        binding.cardLiveDeadline.setOnClickListener(v ->
                showToast("Chi tiết hạn chót: Assignment 1 PRM392 (Còn 18 giờ)")
        );

        binding.cardLiveClass.setOnClickListener(v ->
                showToast("Tiết học kế tiếp: Lập trình Android (15:15 - Phòng AL-L203)")
        );

        // 2. Góc học tập (Study Essentials)
        binding.cardToolNotes.setOnClickListener(v ->
                showToast("Tiện ích Ghi chú nhanh & Checklist")
        );

        binding.cardToolGpa.setOnClickListener(v ->
                showToast("Tiện ích Tính GPA & Dự đoán điểm qua môn")
        );

        binding.cardToolPomodoro.setOnClickListener(v ->
                showToast("Đồng hồ tập trung học Pomodoro 25 phút")
        );

        // 3. Hồ sơ & Tài liệu (Document Utilities)
        binding.cardToolCompress.setOnClickListener(v ->
                showToast("Công cụ cứu nguy: Nén ảnh thẻ & CCCD (<100KB, <500KB)")
        );

        binding.cardToolOcr.setOnClickListener(v ->
                showToast("Quét slide & Trích xuất chữ tiếng Việt (ML Kit Offline)")
        );

        binding.cardToolPdf.setOnClickListener(v ->
                showToast("Bộ tiện ích PDF: Gộp ảnh bài tập thành file PDF")
        );

        // 4. Tiện ích đời sống (Quick Tools)
        binding.cardToolSplitBill.setOnClickListener(v ->
                showToast("Chia tiền phòng trọ & Điện nước EVN 5 bậc")
        );

        binding.cardToolRandom.setOnClickListener(v ->
                showToast("Vòng quay bốc thăm chọn người thuyết trình")
        );

        binding.cardToolQr.setOnClickListener(v ->
                showToast("Quét & Tạo mã QR tài liệu / WiFi")
        );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
