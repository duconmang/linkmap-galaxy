# LinkMap Android 0.4.0 — thiên hà theo ảnh tham chiếu

APK: `dist/LinkMap-0.4.0-debug.apk`. Cài cập nhật trực tiếp lên bản cũ; versionCode 7, không cần xóa dữ liệu. Web không thay đổi trong đợt này.

## Giao diện và thao tác

- Thanh tìm kiếm gọn phía trên, nút đổi map/danh sách bên trái, nút về tâm thiên hà bên phải. Bộ sưu tập dùng chip ngang; các bộ lọc ghim/chưa mở/cần chú ý nằm sau nút Bộ lọc.
- Thanh dưới chứa Bản đồ, Thư viện, Cài đặt; nút Thêm tách riêng bên phải theo ảnh mẫu.
- Link được rải trong không gian, không xếp lưới hoặc chia cụm nền tảng. Năm slot đầu tạo một chòm sao quanh tâm; các slot sau dùng phân bố góc vàng. Slot/bản ghi SQLite không đổi. Sau lần đổi bố cục này, tuổi link hoặc đổi bộ lọc không làm các sao còn lại di chuyển.
- Chạm một sao để chọn: hiện vòng quỹ đạo và thẻ URL/trạng thái với Mở, Chi tiết. Chạm lại sao đã chọn để mở URL. Nhấn giữ để mở chi tiết; kéo/chụm để di chuyển/zoom.
- Các đường nối rất mảnh chỉ giúp định hướng; không tạo nhóm dữ liệu mới. Lọc bộ sưu tập làm các sao khác mờ đi tại chỗ.
- Icon và tên vẫn được giữ; ảnh xem trước nội dung có trong bảng chi tiết khi đã tải được.

## Kích thước theo ngày thêm

Dùng `createdAt`, không dùng lần mở hoặc lần kiểm tra HTTP. Hệ số kích thước:

`0.34 + 0.66 × exp(-số_ngày_đã_lưu / 120)`

- Mới lưu: 100%; khoảng 30 ngày: 85%; 180 ngày: 49%; một năm: 37%.
- Mức nhỏ nhất 34% để link lâu năm không biến mất. Ngày nằm trong tương lai được coi là mới lưu.
- Kích thước được cập nhật theo thời gian khi app hoạt động. Vị trí và màu health không đổi theo tuổi.
- Vùng chọn có đường kính tối thiểu 48dp ở mức zoom đang dùng; khi nhiều vùng chạm chồng nhau, lấy sao gần điểm chạm nhất. Có thể tìm qua Thư viện để chọn chính xác.

## Viền lấp lánh

- Mỗi ID có pha và chu kỳ riêng 8–14 giây, dạng sóng sin liên tục; chỉ độ sáng viền/quầng thay đổi. Không đổi màu health, không chớp tắt thân sao, không co giãn kích thước theo animation.
- Một đồng hồ chung khoảng 10 cập nhật/giây chỉ được đọc trong bước vẽ Canvas, tránh tạo coroutine hoặc animation riêng cho từng bookmark. Khi zoom rất xa, sao dưới 12 pixel được vẽ thành điểm sáng thay vì đường viền nhiều cánh.
- Dừng đồng hồ khi màn map không ở trạng thái RESUMED. Không chạy lịch nền hoặc dịch vụ animation.
- Tắt tại Cài đặt → Viền sao lấp lánh chậm. Nền Tĩnh hoặc system animator duration scale bằng 0 cũng vô hiệu hóa chuyển động.

## 100 link demo có sẵn trong APK

- File đóng gói: `app/src/main/assets/demo-bookmarks.json`, gồm đúng 100 URL khác nhau: GitHub, Figma, Notion, Anthropic, Hugging Face, Facebook, TikTok, Google, Drive, OneDrive.
- Các URL Drive/OneDrive trong demo là trang tìm kiếm; không chứa tài liệu riêng tư hoặc thông tin đăng nhập. Không cam kết mọi trang public trong demo truy cập được ở mọi quốc gia/tài khoản.
- Ngày thêm được mô phỏng từ mới lưu đến gần hai năm trước để xem khác biệt kích thước. Health là dữ liệu minh họa, có nhãn rõ trên UI và trong chi tiết, không phải kết quả kiểm tra HTTP.
- Thư viện mới trống tự mở demo. Với thư viện cũ, bấm Xem 100 link demo hoặc vào Cài đặt → Khám phá 100 link demo.
- Demo ở bộ nhớ, không seed vào SQLite, không tạo outbox Drive, không tự xuất vào JSON và không được đưa vào daily checker. Ghim/lịch sử demo chỉ tồn tại trong phiên xem.
- Bấm Demo · 100 sao • Về thư viện để dùng dữ liệu thật. Nút Thêm chuyển về thư viện thật trước khi nhập link.
- Chỉ khi chủ động bấm Lưu link này vào thư viện của tôi trong chi tiết demo, một bookmark thật mới được lưu với ngày thêm hiện tại và trạng thái chưa kiểm tra. Nếu URL đã có thì mở mục hiện hữu.

## Xác minh

Xem `VALIDATION.md` cho kết quả build/core/instrumentation và ảnh chụp. Việc so sánh hai khung hình kiểm tra được animation đang chạy; khi tắt, vùng bản đồ không thay đổi pixel. Demo và các test giao diện dùng thư viện kiểm thử tách biệt dữ liệu thật.

Google OAuth/Drive thật vẫn cần cấu hình Client ID như bản trước. Thay đổi này không triển khai backend mới, không sửa giao thức đồng bộ hoặc bật đăng nhập giả.
