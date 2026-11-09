# CampusKit (MiniPocketKit) — Everyday Student Utilities

> **"Everything you need, in your pocket."**  
> Bộ tiện ích di động bỏ túi dành riêng cho học sinh, sinh viên Việt Nam: giải quyết nhanh các nhu cầu học tập, hồ sơ và sinh hoạt hàng ngày mà không cần mạng Internet hay đăng nhập phức tạp.

---

## 🎯 Giới thiệu sản phẩm

CampusKit (tên repo: `MiniPocketKit`) được xây dựng theo nguyên tắc:
1. **Single-task & Fast**: Mở app → chọn công cụ → dùng ngay lập tức.
2. **100% Offline-First**: Không bắt buộc tạo tài khoản, không phụ thuộc server/cloud, bảo mật dữ liệu tuyệt đối trên máy người dùng (SQLite).
3. **Kiến trúc Java thuần + XML Layouts + SQLite Database.

---

## 📱 Các nhóm tính năng (3 Modules cốt lõi)

### 1. Study Essentials (Tiện ích Học tập hàng ngày)
- **Ghi chú nhanh (Notes & Checklist)**: Tạo note, checklist việc cần làm, ghim ghi chú quan trọng, phân loại theo môn học.
- **Nhắc nhở hạn nộp (Deadline Reminder)**: Đặt deadline bài tập, lịch nộp dự án kèm đếm ngược ngày (D-Day).
- **Thời khóa biểu (Timetable)**: Lịch học tuần trực quan, hiển thị phòng học và nhắc giờ vào lớp.
- **Tính GPA & Điểm cần đạt**: Tính điểm học kỳ, hỗ trợ dự đoán điểm thi cuối kỳ cần đạt để qua môn.
- **Đồng hồ tập trung (Focus Timer)**: Pomodoro 25/5 phút giúp duy trì tập trung khi ôn thi.

### 2. Document Utilities (Tiện ích Hồ sơ & Tài liệu)
- **Nén ảnh hồ sơ (Image Compressor)**: Nén ảnh thẻ, căn cước, bằng cấp xuống < 100KB, 500KB hoặc 1MB để nộp web trường/tuyển dụng.
- **Quét & Trích xuất chữ (Scan & OCR)**: Chụp slide bài giảng, scan tài liệu và trích xuất chữ tiếng Việt bằng Google ML Kit.
- **Bộ công cụ PDF (PDF Toolkit)**: Gộp nhiều ảnh thành file PDF, ghép/tách tài liệu nhanh gọn.

### 3. Quick Tools (Tiện ích Sinh hoạt & Đời sống)
- **Chia tiền phòng / Ăn nhóm (Split Bill)**: Chia tiền điện nước phòng trọ theo bậc giá mới, chia hóa đơn ăn uống nhóm.
- **Vòng quay ngẫu nhiên (Random Picker)**: Chọn người thuyết trình, bốc thăm may mắn trong nhóm.
- **Tạo & Quét mã QR (QR Tool)**: Quét link tài liệu môn học, tạo mã QR WiFi hoặc chuyển khoản nhanh.

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

- **Ngôn ngữ**: Java 11
- **Giao diện**: XML Layouts (Material Design 3, ConstraintLayout, MaterialCardView, RecyclerView)
- **Cơ sở dữ liệu**: SQLite (Room / SQLiteOpenHelper offline)
- **Target SDK**: Android 37 (Min SDK 24 - hỗ trợ > 95% thiết bị Android tại VN)
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## 🚀 Cài đặt & Chạy dự án

```bash
# Clone repository
git clone https://github.com/Khanhnv26/MiniPocketKit.git

# Mở dự án trong Android Studio (Ladybug / Iguana / Giraffe)
# Sync Gradle và bấm Run trên thiết bị máy ảo hoặc máy thật Android
```

---


