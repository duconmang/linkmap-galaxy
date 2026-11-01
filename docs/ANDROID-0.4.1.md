# LinkMap Android 0.4.1 — thiên hà trung tâm, viền sáng rõ

Ngày: 30/10/2026. APK: `dist/LinkMap-0.4.1-debug.apk`, versionCode 8. Cài cập nhật trên bản cũ, không cần xóa thư viện.

## Hai thay đổi chính

1. **Mở map là thấy toàn cảnh.** Giảm khoảng cách rải sao, bỏ năm vị trí neo xa tâm của 0.4.0; dùng một cụm hình elip theo góc vàng. Camera tự căn vừa các sao vào vùng giữa màn hình với khoảng đệm quanh mép. Không chọn sao hoặc hiện thẻ URL che cụm khi mới mở demo. Nút Về tâm thiên hà trở về toàn cảnh; tìm kiếm/định vị vẫn phóng gần và có Về vị trí trước.
2. **Viền có nhịp sáng dễ thấy hơn.** Lõi viền chuyển từ màu trạng thái tối sang gần trắng ở đỉnh nhịp, dày khoảng 1–1,8dp, kèm quầng sáng màu. Thân sao giữ màu health. Pha mỗi ID được trộn hash để các ID liên tiếp của demo không sáng cùng một nhịp. Chu kỳ vẫn 8–14 giây, không chớp nhanh.

Kích thước theo tuổi giữ nguyên. Đổi bố cục không thay URL, ngày thêm, health hoặc slot trong database. Sau thay đổi bố cục của phiên bản này, lọc/đổi tuổi vẫn giữ tọa độ của mỗi slot.

## Điều khiển hiệu ứng

- Viền sao có công tắc riêng tại Cài đặt → Viền sao lấp lánh chậm.
- Chọn nền Tĩnh không còn âm thầm vô hiệu hóa công tắc lấp lánh. Nền và hiệu ứng bookmark được điều khiển độc lập.
- Khi ứng dụng ra nền, hoặc hệ thống đặt animator duration scale bằng 0, đồng hồ animation dừng. Vẫn dùng một đồng hồ vẽ chung 10 Hz và điểm sáng khi zoom rất xa.

## Demo và kiểm chứng

- Giữ 100 URL demo tích hợp, ngày thêm và trạng thái minh họa; không tự ghi/sync vào thư viện thật.
- Kiểm thử mới bảo đảm phạm vi phân bố của 100 sao gọn hơn bản cũ. Kiểm thử ảnh yêu cầu nhiều điểm có chênh lệch tổng RGB lớn hơn 90 giữa hai khung hình cách nhau hai giây, thay cho việc chỉ đếm vài pixel khác nhau.
- Video `docs/screenshots/galaxy-v041.mp4` được quay trực tiếp từ emulator trong 12 giây, không phải ảnh concept hoặc animation dựng riêng.
- Kết quả kiểm thử và checksum APK nằm trong `VALIDATION.md`. Google OAuth và web giữ nguyên phạm vi bản trước.
