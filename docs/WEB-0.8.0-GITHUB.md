# LinkMap Web 0.8.0 — GitHub Pages

Hoàn tất ngày 30/10/2026.

- Website: https://duconmang.github.io/linkmap-galaxy/
- Demo: https://duconmang.github.io/linkmap-galaxy/?demo=150
- Repository: https://github.com/duconmang/linkmap-galaxy
- Commit: c487050f41a5f18341af0887cae93be37f20d1a2
- GitHub Pages: main, / (root), HTTPS bắt buộc. Tạo repo/upload/commit/bật Pages qua Chrome của người dùng, tài khoản duconmang.
- Không triển khai bản 0.8 lên hosting ChatGPT. Site cũ không bị xóa hay thay đổi quyền truy cập.

## Giao diện

Theo bố cục Stitch: header thương hiệu/search/tài khoản/đồng bộ/chuyển không gian, hàng chip bộ sưu tập, dock trái, canvas trung tâm, điều khiển zoom bên phải, legend và số liệu thật dưới cùng. Thiết bị hẹp có CSS dock dưới và header tìm kiếm hai hàng.

Thiên hà: giữ nguyên nebula.png, ngôi sao 5 cánh với icon nền tảng, kích thước giảm theo tuổi (có mức sàn), viền sáng chậm khác pha 8–14 giây; bố cục xoắn ốc tập trung. Mỗi nền tảng một màu đường nối và một cây kết nối n−1 cạnh; vị trí không phân cụm lại theo nền tảng. Note độc lập màu vàng, không bị check HTTP.

Data Center: nền SVG nội bộ mô tả phòng rack, canvas dựng rack có khay server và ô đèn tín hiệu với icon; 30 mục/rack. Demo 150 mục tương ứng 5 rack. Cùng thư viện, bộ lọc, thao tác mở/sửa/xóa và mạng nền tảng với chế độ Thiên hà.

Có tìm kiếm bỏ dấu tiếng Việt, bộ lọc nội dung/nền tảng/danh mục, trang mạng lưới, popup nhập link/note và chọn URL nếu nội dung có nhiều link, xác nhận xóa trong HTML dialog, xuất/nhập JSON, trang privacy. Hiệu ứng có thể tắt và tôn trọng prefers-reduced-motion; dừng bộ đếm nhấp nháy khi tab ẩn.

## Đồng bộ và Google

Giữ nguyên Google Cloud project linkmap-galaxy, Web Client ID hiện tại, drive.appdata, journal/2 tương thích Android 0.7. Thêm origin https://duconmang.github.io vào client Web, không tạo client secret hoặc yêu cầu scope rộng hơn. OAuth vẫn Testing; không tự chuyển production.

Địa chỉ mới có origin khác hosting cũ nên IndexedDB không tự chuyển. Dữ liệu đã lên Drive được tải lại sau đăng nhập/cấp quyền cũ; thư viện khách chưa đồng bộ cần xuất JSON từ địa chỉ cũ và nhập tại địa chỉ mới. Các project GitHub Pages cùng username có chung origin: ưu tiên domain/subdomain riêng trước khi phát hành rộng.

## Kiểm thử đã thực hiện

- 20 Node tests PASS: URL/protocol/note/merge/tombstone/health/legacy journal, tuổi sao/bố cục ổn định/pulse/MST/note isolation/rack slots/tìm tiếng Việt.
- Chrome trên website GitHub Pages: demo 150 mục, 144 đường nối, 6 nền tảng; chuyển hai chế độ; mở thư viện; Google Identity Services và Drive hoạt động, không lỗi console trong bài kiểm thử.
- Local guest: thêm note nhiều dòng có chuỗi `<img src=x onerror=alert(1)>`; nội dung hiển thị dưới dạng chữ, không thực thi. Xóa note qua HTML dialog, thư viện trở lại 0.
- Live Google: developer@example.com đăng nhập trên GitHub Pages, nhận quyền Drive đã cấp trước. Không mở cổng ChatGPT, không dùng IAB.
- Live Web → Android: tạo NOTE `LinkMap GitHub Sync Test 3010` với nội dung tiếng Việt; Android nhận đúng kind/body/category.
- Live Android → Web: sửa body, thêm `_edited_from_Android`; Web nhận đúng nội dung sau đồng bộ.
- Live delete: chuyển note thử nghiệm vào Đã xóa từ Web; Android Sync nhận dấu xóa, cả hai thư viện có 0 mục hoạt động. Không purge Drive hoặc lịch sử/tombstone.
- Đối chiếu SHA-256 nội dung HTTP thực tế: index.html, app.js, google.js, style.css, map-model.js, map-view.js, nebula.png, datacenter.svg khớp file cục bộ.

Ảnh bằng chứng trong screenshots/50–56. Chưa kiểm thử responsive trên thiết bị Web vật lý; thao tác viewport override không cho kích thước như yêu cầu nên không tính là kiểm thử mobile thành công. Android APK không đổi trong đợt này.

## Bảo mật và việc còn lại

Xem WEB-SECURITY.md. Đã thêm CSP meta, bỏ CDN UI/font/icon, render text an toàn, giữ token trong bộ nhớ và giới hạn tải JSON theo byte. Chưa có E2EE. Không cam kết chống được mã first-party bị thay hoặc thiết bị bị chiếm quyền. Cần vận hành MFA/kiểm soát deploy/domain riêng và kế hoạch vault mã hóa tương thích Android/Web trước khi dùng cho nội dung nhạy cảm. OAuth Testing cần được hoàn tất cấu hình phát hành trước khi mọi tài khoản Google có thể đăng nhập.
