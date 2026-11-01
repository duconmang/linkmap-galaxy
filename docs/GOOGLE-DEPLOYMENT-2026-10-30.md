# LinkMap Galaxy — triển khai Google ngày 30/10/2026

## Kết quả Web ↔ Android trên hai client thật

- Đã xác minh Web live trên đúng Chrome Dev (browser 4), account developer@example.com: giao diện Cài đặt hiện email chính xác và “Đã đồng bộ”. Google popup đã hoàn tất; các ghi chú bị kẹt tải bên dưới là lịch sử, không còn là kết luận hiện tại.
- Tạo NOTE `LinkMap Sync Test 3010-WA` trên Web, tự upload Drive. Android nhận đúng title/body nhiều dòng, loại NOTE và danh mục Cá nhân. Ảnh: screenshots/45-web-note-on-android.png.
- Sửa note qua UI Android, thêm `_edited_from_Android`, lưu. Web Sync rồi Tìm kiếm nhận đúng nội dung sửa. Ảnh: screenshots/46-android-note-edit-on-web.png.
- Thêm LINK `https://example.com/?linkmap_test=3010-WA` qua Android Share. Web nhận đúng URL, title metadata “Example Domain” và trạng thái “Truy cập được” từ checker Android. Ảnh: screenshots/47-android-link-status-on-web.png.
- Demo 150 link của Web không được đưa vào Drive; trước khi tạo fixture chỉ có thư viện tài khoản trống, sau note Android hiện 1 mục thật. Không nhập thư viện khách trong bài kiểm thử này.
- **Kiểm thử xóa hai chiều đã qua và fixture đã dọn:** người dùng bấm OK trong JS confirm Chrome; note thử nghiệm xóa trên Web đã biến mất khỏi Android. Link thử nghiệm được chuyển vào Đã xóa qua UI Android; Web Sync nhận tombstone và không còn link. Cuối kiểm thử, UI Web và Android đều hiện 0 mục hoạt động; không xóa vĩnh viễn file Drive hay nội dung có trước. Hai mục thử nghiệm có thể còn trong Đã xóa/journal để hỗ trợ khôi phục và chống hồi sinh bản cũ. Ảnh: screenshots/48-web-after-test-cleanup.png, 49-android-after-test-cleanup.png.

## Cập nhật quyền truy cập Web sau xác nhận của người dùng

- Người dùng chọn “Mở giao diện web công khai để kiểm thử tiếp”. Đã gọi Sites update_site_access access_mode=public; revision 2, ngày 30/10/2026. Không thay đổi dữ liệu Google Drive hoặc OAuth Testing.
- URL https://linkmap-galaxy.duconmang1.chatgpt.site đã tải trực tiếp giao diện app trong Chrome Dev (browser 4), không còn cổng ChatGPT. Ảnh: screenshots/44-public-web-chrome-dev.png.
- Trong lúc đăng nhập, popup từng bị kẹt và có timeout công cụ; sau đó đã hoàn tất chọn account/cấp quyền. Kết luận hiện tại là Web/Android Google sync thật đã qua, bao gồm xóa hai chiều như mục kết quả phía trên.
- Các ghi chú owner-only bên dưới mô tả trạng thái trước xác nhận mở public, đã được thay thế bởi cập nhật này.

## Đã xác minh trên Google Cloud Console

- Chủ tài khoản đang dùng: developer@example.com.
- Tạo project riêng **LinkMap Galaxy**, Project ID `linkmap-galaxy`, project number `881163930052`.
- Google Drive API (`drive.googleapis.com`) đã bật, Console hiển thị Enabled.
- Biểu mẫu Google Auth Platform đã điền: tên LinkMap Galaxy; email hỗ trợ/liên hệ developer@example.com; External, chế độ Testing.

## OAuth đã tạo sau xác nhận của người dùng

Người dùng đã xác nhận chấp thuận Google API Services: User Data Policy, tạo hai client và cấp hồ sơ cơ bản/drive.appdata để kiểm thử. Branding đã tạo thành công, tên LinkMap Galaxy.

- Web client: `881163930052-rqgjt01f96i1h1veof9ilcp8abtv215p.apps.googleusercontent.com`.
- Android Debug client: `881163930052-h46vtkvlv30vr9rnf09tqumdrbn80f49.apps.googleusercontent.com`.
- Đã cấu hình Web Client ID vào gradle.properties và web/dist/config.js; không tải JSON hay lưu Client Secret.
- Data Access: openid, userinfo.email, userinfo.profile, drive.appdata; đều nằm trong nhóm non-sensitive, không xin toàn bộ Drive.
- Test user: developer@example.com. External, Testing; chưa phát hành production.
- Bằng chứng Console: screenshots/38-google-oauth-clients-created.png và 39-google-testing-audience.png.

## Android đã tích hợp và kiểm thử thật

- Tự tái sử dụng quyền Drive đã cấp qua AuthorizationClient khi mở lại app, có mạng hoặc token hết hạn; nếu cần consent thì báo kết nối lại, không tự mở popup quyền từ lifecycle callback.
- Đồng bộ khi thay đổi dữ liệu, khi vào app/có mạng, mỗi 60 giây khi app đang hiển thị. Dừng lịch khi app ra nền.
- GoogleDriveLiveTest chỉ chạy với instrumentation argument `liveGoogle=true`: tạo UUID link/note test, upload, tải về bằng store độc lập, sửa, đồng bộ ngược, tombstone mục test và kiểm tra sau mở lại. Không log token hoặc nội dung người dùng; không xóa file cloud hay mục có trước.
- Google login + cấp quyền Drive thật qua trên emulator với developer@example.com. Ảnh: screenshots/41-google-drive-connected-v070.png.
- Live roundtrip GoogleDriveLiveTest chạy có opt-in: **qua**, 14,958 giây. Kiểm tra cả LINK và NOTE, upload/download, sửa từ store thứ hai, đồng bộ ngược, tombstone và mở lại. Lần khởi động process instrumentation đã tự khôi phục profile/quyền Drive trước test, không cần consent lại.
- Phát hiện và sửa lỗi chuyển account đặt ready=false làm UI root biến mất, hủy coroutine đăng nhập trước khi ghi profile. Store mới được load trước khi publish; AccountSwitchTest bảo vệ ready không bị hạ xuống khi chuyển thư viện đã khởi tạo.
- Build APK/AndroidTest, Lint và 33 core tests qua. Bộ Android thường quy: 26 tests qua, 1 live test bỏ qua khi không có opt-in. Live test được chạy riêng và qua như trên.
- APK 0.7.0, versionCode 13: dist/LinkMap-0.7.0-debug.apk, 23.334.178 byte, SHA-256 F7917FABD0EFCBEFAD45A2B47344DF146AF12BBD2F132DD81D96B4B08D2E2CC5.

## Web tương thích

- Đã cấu hình Client ID thật, đọc journal/1 + journal/2, ghi journal/2; body tối đa 64.000 ký tự, upload chặn theo byte, backup schema 2. Notes có UUID độc lập, không gộp theo URL rỗng.
- UI localhost kiểm tra tạo hai note độc lập, nhiều dòng, sửa note có thêm URL vẫn giữ NOTE; 16 protocol tests qua. Giữ giao diện web hiện tại.
- Google login/Drive trên Web live đã xác minh bằng cùng tài khoản Android; các luồng tạo note, sửa ngược, link/metadata/health và xóa hai chiều đã quan sát qua UI hai phía.
- Web đã publish thành công, source commit `e69e3c3aeffcf6bbfd5e9931a5131a8c1724828d`, saved version `appgprj_6ae18bd8162881919c6d590f529bb7c5~appgver_9542b95309288191abc4adf8de5170b1`, deployment `appgdep_6ae42fa7d5c48191841f46eb2d4b4181` (succeeded).
- URL trả về bởi hosting: **https://linkmap-galaxy.duconmang1.chatgpt.site** (đổi route so với URL lịch sử opalchord20). Đã bổ sung origin này vào Google Web client, Console xác nhận OAuth client saved.
- Audience ban đầu là owner-only; người dùng đã xác nhận đổi public (revision 2). Site hiện vào trực tiếp, không yêu cầu cổng ChatGPT. Quyền Google Drive và dữ liệu của từng tài khoản vẫn độc lập với quyền truy cập giao diện website.

## Cấu hình dự kiến

- Android: package `vn.linkmap`; SHA-1 đã đối chiếu APK bằng apksigner: E0:F6:91:04:D8:97:36:AE:77:F5:1B:E3:7F:7D:B0:CC:8F:5D:12:C6.
- Web: dùng cho audience của Credential Manager Android và web. Origins: http://127.0.0.1:4173, http://localhost:4173, URL lịch sử https://linkmap-galaxy.opalchord20.chatgpt.site và URL hiện hành https://linkmap-galaxy.duconmang1.chatgpt.site.
- Không lưu Client Secret trong Android hoặc web.
- Project hiện phục vụ testing. Khi phát hành production cần project/môi trường release riêng, chứng chỉ release và các trang/brand/domain phù hợp; chưa tuyên bố phát hành công khai.
