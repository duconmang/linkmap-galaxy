# LinkMap Android 0.6.1 — Cosmic Modal

APK: `dist/LinkMap-0.6.1-debug.apk`, versionCode 12. Cài cập nhật trên bản cũ.

- Thẻ nhập bo góc 26dp, nền gradient tối, viền cyan/indigo và bóng nhẹ, badge liên kết và nút đóng. Giữ ảnh thiên hà hiện có.
- Lớp dim 64%; native blur phía sau trên Android 12+ khi thiết bị/compositor hỗ trợ. Thiết bị không hỗ trợ vẫn dùng lớp dim.
- Ô nhập hỗ trợ link và ghi chú nhiều dòng, nút DÁN và thao tác Dán từ bộ nhớ tạm. Cả hai điền nội dung trước, không tự lưu, để người dùng kiểm tra và chọn danh mục.
- Chip Thiên hà chung, Design, Dev & GitHub, Cá nhân; bổ sung các danh mục đã có và nhập nhóm khác. Nội dung dài cuộn riêng, footer giữ nút Hủy và CTA gradient.
- Text thuần đổi CTA thành Lưu ghi chú; link được nhận diện nền tảng; nhiều URL cần chọn một, giữ toàn văn kèm theo. Không thay đổi quy tắc chỉnh sửa note của 0.6.0.

Tính năng note, migration, backup và giới hạn đồng bộ: [Android 0.6.0](ANDROID-0.6.0.md). Chưa xác minh Google thật vì thiếu OAuth Client ID; web cũ cần nâng giao thức trước khi đồng bộ note.
