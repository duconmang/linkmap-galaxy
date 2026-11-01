# LinkMap Web 0.8.1 — phòng DC và camera thống nhất

Ngày 30/10/2026. Website: https://duconmang.github.io/linkmap-galaxy/

Đã xuất bản qua Chrome lên GitHub Pages, commit `fe38a7506889055f7b0d13e6fe69236734cbf8be`. Đối chiếu HTTP SHA-256: index.html, app.js, style.css, map-model.js và map-view.js khớp bản cục bộ. Chrome live hiện GALAXY 0.8.1, demo 150 mục / 144 cạnh / 6 nền tảng, zoom tới 0.9x rồi fit lại; không có lỗi console. Ảnh: `screenshots/57-dc-room-081-live.png`, `screenshots/58-dc-zoom-081-live.png`.

## Sửa lỗi

Ở bản 0.8.0, ảnh rack trang trí trong CSS đứng yên theo viewport, trong khi rack chứa bookmark được vẽ trên canvas theo pan/zoom. Hai lớp không có cùng hệ tọa độ, tạo cảm giác tủ trượt khỏi vị trí khi zoom.

0.8.1 dựng toàn bộ phòng DC bằng canvas: sàn kỹ thuật, tường sau, đèn viền, ô đặt tủ, thân/mặt bên/nóc tủ, lối bảo trì và các ô bookmark. Mọi thành phần đều dùng cùng camera world → screen. Lớp nền CSS chỉ còn ánh sáng môi trường, không còn ảnh tủ rack cố định. Ngắt cache asset DC cũ và tăng version service worker.

## Bố cục và kết nối

- Năm vị trí đặt tủ cố định mỗi hàng; mỗi tủ có 30 ô. Tủ mới không làm tủ cũ đổi chỗ khi thêm hàng. Chế độ xem toàn bản đồ tính theo ranh giới phòng.
- Chọn ô dùng khoảng cách gần nhất để tránh chọn nhầm các ô có vùng chạm chồng lấn khi thu nhỏ.
- Các nền tảng vẫn tạo mạng n−1 cạnh như Thiên hà, giữ nguyên màu nền tảng. Cáp nối cong nhẹ với độ lệch riêng từng nền tảng giúp tách các đường đi chung hàng. Khi chọn link, mạng nền tảng đó được làm nổi bật.
- Nút lưới điều khiển lưới sàn DC. Zoom/kéo áp dụng đồng thời lên phòng, tủ, ô link và các đường nối.
- Không thay đổi dữ liệu bookmark/note hoặc quyền/cơ chế Google đồng bộ.

## Kiểm thử

24 Node tests đạt, gồm bốn regression tests mới: vị trí tủ không đổi khi tăng số tủ; tương quan phòng/tủ/đèn khi zoom .1–5 và pan; giữ điểm neo zoom và chọn ô gần nhất; giữ mạng màu theo nền tảng trong DC. Các tests protocol/merge/note/xóa vẫn đạt.

Chrome preview với 150 link: chuyển sang DC, fit, zoom in/out, kéo phòng và xác nhận canvas không có lỗi console. Kiểm tra bản live sau deployment và lưu ảnh bằng chứng trong screenshots/57 trở đi. Google OAuth và Android không đổi nên không lặp lại bài kiểm thử cấp quyền hay ghi dữ liệu Drive.
