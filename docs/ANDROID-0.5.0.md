# LinkMap Android 0.5.0 — thanh điều hướng Stitch và mạng nền tảng

Ngày: 30/10/2026. APK: `dist/LinkMap-0.5.0-debug.apk`, versionCode 9. Cài cập nhật lên bản cũ, giữ nguyên thư viện. Web không thay đổi.

## Giao diện theo bản Stitch được cung cấp

- Header nhận diện LinkMap / GALAXY, phụ đề Knowledge Universe, biểu tượng mạng, chuông và hồ sơ.
- Thanh tìm kiếm dạng kính tối, bộ lọc bên trong, nút định hướng riêng bên phải; các chip bộ sưu tập nằm ngang, tên không xuống dòng như một số chỗ trong ảnh render HTML.
- Thanh dưới dạng pill ba tab, tab hiện tại có quầng cyan. Nút thêm riêng có viền chuyển sắc cyan–indigo.
- Zoom + / − / về toàn cảnh được gom trong một pill dọc ở góc dưới phải, phía trên hướng dẫn cử chỉ và bottom bar.
- Nền thiên hà, texture tinh vân, bố cục trung tâm, tuổi/kích thước sao và hiệu ứng viền 8–14 giây được giữ nguyên. HTML/Tailwind được dùng làm tham chiếu; ứng dụng vẫn là Jetpack Compose native, không nhúng WebView hoặc tải script/font CDN.

## Mạng liên kết cùng nền tảng

- Các link GitHub thuộc cùng một mạng GitHub; tương tự Facebook, TikTok, Drive, OneDrive và Google. Không di chuyển sao hoặc chia cụm nền tảng.
- Mỗi mạng gồm các cạnh nối ngắn tạo cây khung tối thiểu: n link có n−1 cạnh, nối được toàn bộ mạng mà không vẽ mọi cặp. Đường nối giữ nét mảnh cyan/indigo; khi chọn sao, toàn bộ mạng tương ứng được làm sáng hơn.
- Các website chưa có Provider riêng được tách theo hostname (bỏ www). Ví dụ Figma và Notion không bị nối chỉ vì cùng nhãn Khác; hostname giả như figma.com.evil.test không nhập chung vào Figma.
- Không còn dùng bộ sưu tập hoặc bốn sao gần nhất bất kỳ để tạo cạnh. Bộ sưu tập vẫn là bộ lọc độc lập; tuổi/health không quyết định cạnh.
- Thẻ sao đã chọn hiển thị số link cùng nền tảng thực tế. Mạng được tính lại khi danh sách/URL thay đổi, không chạy thuật toán mỗi khung hình animation và không gọi thêm API website.

## Các nút mới hoạt động thế nào

- Chuông mở danh sách Cần chú ý, dùng health và tùy chọn bỏ qua cảnh báo hiện có. Dấu chấm chỉ xuất hiện khi có mục cần chú ý; không xin quyền thông báo Android mới.
- Hồ sơ mở Cài đặt/Tài khoản; hiện LM khi chưa đăng nhập, chữ đầu tên tài khoản khi có profile đã lưu. Không báo đăng nhập Google thành công nếu chưa cấu hình OAuth.
- Biểu tượng mạng cạnh tên app chuyển map/danh sách. La bàn vẫn đưa về toàn cảnh.
- Demo vẫn có nhãn minh họa và được tách khỏi thư viện thật/Drive.

Xem `VALIDATION.md` cho kết quả build, kiểm thử và checksum. Ảnh: `screenshots/29-stitch-chrome-v050.png`, `screenshots/30-platform-network-v050.png`.
