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
        // 1. Thẻ Hôm nay
        binding.cardTodayGlance.setOnClickListener(v -> 
            showToast("Dữ liệu tổng quan: 2 deadline sắp tới, 1 tiết học chiều nay.")
        );

        // 2. Study Essentials
        binding.cardToolNotes.setOnClickListener(v -> 
            showToast("Tiện ích Ghi chú nhanh (Notes)")
        );

        binding.cardToolDeadline.setOnClickListener(v -> 
            showToast("Tiện ích Nhắc nhở hạn nộp (Deadline Reminder)")
        );

        binding.cardToolTimetable.setOnClickListener(v -> 
            showToast("Tiện ích Thời khóa biểu (Timetable)")
        );

        binding.cardToolGpa.setOnClickListener(v -> 
            showToast("Tiện ích Tính GPA & Dự đoán điểm")
        );

        binding.cardToolPomodoro.setOnClickListener(v -> 
            showToast("Tiện ích Đồng hồ học tập (Focus Timer)")
        );

        // 3. Document Utilities
        binding.cardToolCompress.setOnClickListener(v -> 
            showToast("Tiện ích Nén ảnh hồ sơ (Image Compressor)")
        );

        binding.cardToolOcr.setOnClickListener(v -> 
            showToast("Tiện ích Quét slide & OCR chữ (Scan OCR)")
        );

        binding.cardToolPdf.setOnClickListener(v -> 
            showToast("Tiện ích Ghép/Tách PDF (PDF Toolkit)")
        );

        // 4. Quick Tools
        binding.cardToolSplitBill.setOnClickListener(v -> 
            showToast("Tiện ích Chia tiền trọ / Ăn nhóm (Group Bill)")
        );

        binding.cardToolRandom.setOnClickListener(v -> 
            showToast("Tiện ích Vòng quay ngẫu nhiên (Random Picker)")
        );

        binding.cardToolQr.setOnClickListener(v -> 
            showToast("Tiện ích Quét & Tạo mã QR (QR Tool)")
        );
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
