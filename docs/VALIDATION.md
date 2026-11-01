# LinkMap 0.1 — xác minh

## Cập nhật 0.7.0 — 30/10/2026

- Cập nhật kiểm thử live Web/Android: đã đăng nhập đúng developer@example.com trên Chrome Dev và emulator, Drive hiện đã đồng bộ. NOTE tạo Web xuất hiện Android, sửa qua UI Android xuất hiện Web; LINK Android Share + URL/query, title metadata Example Domain và health Truy cập được xuất hiện Web. Ảnh 45–47. Site được mở public sau xác nhận, revision 2; OAuth vẫn Testing. Các dòng nói Web popup bị kẹt bên dưới là lịch sử trước khi Google login hoàn tất.
- Xóa hai chiều qua UI thật đã qua: note xóa Web biến mất Android; link soft-delete Android biến mất Web sau Sync. Cuối test cả hai thư viện hoạt động 0 mục. Chỉ tác động hai UUID/URL do kiểm thử tạo; không purge cloud files hoặc mục có trước. Ảnh 48–49. Đã hoàn tất kiểm tra và dọn fixture.
- Google Cloud thật: tạo project linkmap-galaxy, Drive API Enabled, branding LinkMap Galaxy, Web + Android Debug clients, bốn non-sensitive scopes và test user. Không tải/lưu Client Secret vào mã.
- Android thật: Google Credential Manager đăng nhập developer@example.com; AuthorizationClient cấp drive.appdata; UI hiển thị đã đồng bộ. Live GoogleDriveLiveTest qua trong 14,958 giây: link/note upload, pull store độc lập, sửa/sync ngược, tombstone, reopen. Token chỉ dùng trong memory instrumentation, không xuất ra shell/log/file.
- Sửa lỗi account switch ready=false hủy scope Google panel. AccountSwitchTest mới qua. 33 core tests và Lint qua; 26 Android tests qua, live test skip trong lượt thường quy rồi chạy riêng có opt-in như trên. Không chạy lại fixture account tests sau khi đăng nhập thật để tránh đổi namespace đang sync.
- Web: 16 protocol tests qua, syntax checks qua; localhost UI lưu hai note, sửa note thêm URL vẫn NOTE. Web Google popup kẹt tải sau chọn account trong Chrome, nên chưa xác minh Web/Android Google roundtrip; không báo thành công giả.
- Web đã publish: deployment appgdep_6ae42fa7d5c48191841f46eb2d4b4181 succeeded, commit e69e3c3aeffcf6bbfd5e9931a5131a8c1724828d. URL hiện hành https://linkmap-galaxy.duconmang1.chatgpt.site đã thêm vào OAuth origins; giữ site owner-only. Trang live trong Chrome/IAB hiện cần phiên ChatGPT owner, không tự thay quyền truy cập.
- APK versionCode 13, dist/LinkMap-0.7.0-debug.apk, 23.334.178 byte; SHA-256 F7917FABD0EFCBEFAD45A2B47344DF146AF12BBD2F132DD81D96B4B08D2E2CC5. OAuth Testing, chưa mở cho mọi Google account/release signing.
- Bằng chứng: screenshots/38-google-oauth-clients-created.png, 39-google-testing-audience.png, 40-google-scopes-saved.png, 41-google-drive-connected-v070.png. Báo cáo cấu hình và giới hạn: GOOGLE-DEPLOYMENT-2026-10-30.md.

## Cập nhật 0.6.1 — 30/10/2026

- Hoàn thành NOTE độc lập và Cosmic Modal: nhận diện link/text, giữ toàn văn, chọn URL khi có nhiều link, danh mục nhanh, DÁN điền trước khi lưu, CTA gradient và đóng hộp thoại.
- 33 core tests qua cho nhận diện, lọc, mạng nền tảng và merge. Toàn bộ 25 Android instrumentation tests qua trên Pixel 9 Pro Android 16, 59,463 giây thực thi test. Chạy riêng NoteUiTest để lưu ảnh: qua, 19,692 giây. Build APK/AndroidTest và Lint qua.
- Test note xác minh lưu/sửa/nhiều note, backup/import, migration v4 giữ snapshot, merge giữa hai store, xóa/replay, bỏ qua HTTP; UI xác minh phân loại link/note, lưu danh mục Design, paste không tự lưu, Share text và chọn URL trong nhiều link. Chưa xác minh Google Drive thật hoặc tất cả OEM/IME/TalkBack.
- Ảnh thực tế với tài khoản test tách biệt: `screenshots/35-cosmic-modal-v061.png`, `36-cosmic-note-v061.png`. Native blur phụ thuộc hỗ trợ Android 12+/compositor; nền dim là fallback. Giữ asset thiên hà.
- SQLite schema 5, backup schema 2, journal/2. Web cũ chỉ journal/1 cần nâng cấp trước khi sync note; web không được sửa/publish trong đợt này.
- APK `dist/LinkMap-0.6.1-debug.apk`, versionCode 12, 23.334.178 byte. SHA-256 `E8E52321D106B55BC2C02C63FC54DF988C8A0C1E0C048792A1816D922D3B934C`.

## Cập nhật 0.5.1 — 30/10/2026

- Đổi màu đường nối và nhãn mạng được chọn theo nền tảng: GitHub `#51D9FF`, Facebook `#648BFF`, TikTok `#FF78B7`, Drive `#FFCC66`, OneDrive `#BB92FF`, Google `#4DE0B3`. Website Khác lấy màu HSV ổn định theo platform key hostname, không phụ thuộc thứ tự, tuổi, health hoặc bộ sưu tập.
- Đường nối không còn dùng một gradient cyan/indigo chung; khi chọn chỉ đổi độ đậm và bề dày, giữ hue của nền tảng. Màu trạng thái thân/viền sao và cơ chế mạng nối giữ nguyên. Không đổi nền, top/bottom bar, dữ liệu hoặc Google sync.
- Build APK/AndroidTest và Lint qua. Chạy lại `GalaxyDemoTest` trên 100 link: qua (14,957 giây), gồm kiểm tra chọn mạng, animation, demo tách dữ liệu, zoom và các nút Stitch. Không chạy lại toàn bộ test lưu trữ/core vì đợt này chỉ chỉnh màu hiển thị.
- Ảnh thực tế: `screenshots/31-platform-colors-v051.png`, `32-selected-platform-color-v051.png`. Dữ liệu và health trong ảnh là demo minh họa.
- APK: `dist/LinkMap-0.5.1-debug.apk`, versionCode 10, 23.301.414 byte. SHA-256 `A50DE03166C8E13A125650F863339422A3C3F496C7A2561F5D4EDAAC0DF44ED3`.

## Cập nhật 0.5.0 — 30/10/2026

- `assembleDebug`, `assembleDebugAndroidTest`, `core:test`, `lintDebug` thành công. 28 core tests qua; hai test mới xác minh mạng nối được toàn bộ từng nền tảng, đúng n−1 cạnh, thứ tự input không đổi kết quả và không nối nhầm các hostname Khác.
- 19 Android regression cases qua trên APK 0.5.0 trong lượt đầu. Sau sửa kiểu trả về Unit của helper JUnit, `GalaxyDemoTest` qua riêng (14,81 giây), tổng 20 case đã qua. Không thay app để bỏ qua assert. Test mới kiểm tra vị trí zoom ở nửa dưới/phía phải màn hình, chọn GitHub, giữ dữ liệu demo tách biệt, chuông mở link cần chú ý và hồ sơ mở Cài đặt.
- Tiếp tục kiểm tra animation đủ biên độ sáng và tắt được; luồng 150/1.000 mục, Share, migration, preview và khôi phục vẫn qua. Chưa xác minh mọi OEM/TalkBack hoặc Google roundtrip thật.
- Ảnh thật trên Pixel 9 Pro Android 16: `screenshots/29-stitch-chrome-v050.png` và `screenshots/30-platform-network-v050.png`. Ảnh dùng 100 link demo, health minh họa. Khi chọn Compose Samples, thẻ hiện 16 link GitHub theo dữ liệu thực trong demo và mạng GitHub được làm sáng.
- StarField và asset tinh vân không thay đổi. Không sửa giao thức sync, database schema hoặc web. Đã đọc code.html và screen.png của Stitch làm tài liệu thiết kế, không chạy JavaScript/CDN trong app.
- APK: `dist/LinkMap-0.5.0-debug.apk`, versionCode 9, 23.301.414 byte. SHA-256 `72369845A2C7F3B9488146B608810EC1427BA58776032CB49B3986446A40F2AF`.

## Cập nhật 0.4.1 — 30/10/2026

- Build APK/AndroidTest, 26 core tests và Lint qua. Toàn bộ 20 Android instrumentation tests qua trong một lượt, 48,26 giây, trên Pixel 9 Pro emulator Android 16.
- Bố cục 100 sao được thu gọn, mặc định fit vào vùng trung tâm với khoảng đệm; không chọn sao hoặc mở thẻ URL che bản đồ khi mới mở demo. Home/compass trở về toàn cảnh thay vì zoom sao đầu tiên. Unit test mới chặn hồi quy về phạm vi rải quá rộng.
- Viền dùng nét có kích thước theo dp, lõi gần trắng ở đỉnh nhịp và quầng màu biên độ lớn hơn. Pha hash được trộn, các ID demo liên tiếp không cùng nhịp. Test ảnh yêu cầu hơn 35 điểm lấy mẫu có tổng chênh RGB >90 sau hai giây; đã qua. Tắt hiệu ứng vẫn cho bản đồ đứng yên, đã qua.
- Tùy chọn nền Tĩnh và công tắc lấp lánh được tách độc lập. Lifecycle và system animator scale bằng 0 vẫn dừng chuyển động.
- Đã quay trực tiếp `screenshots/galaxy-v041.mp4`: 720×1600, thời lượng đọc từ MP4 12,12 giây, 593.945 byte. Không dựng hiệu ứng riêng cho video. Ảnh: `26-galaxy-central-v041.png`, `27-galaxy-bright-v041.png`, `28-galaxy-overview-v041.png`.
- Giữ 100 link demo, tuổi và health minh họa; các kiểm tra không ghi demo vào bookmark/reading/outbox thật và đổi demo/thư viện vẫn qua. Không sửa backend, Google config hoặc web.
- APK: `dist/LinkMap-0.4.1-debug.apk`, versionCode 8, 23.268.642 byte. SHA-256 `6592C19A8A9F4F7BC5676E7DC674BA377AFCE5CFCBF1D26B16C280593594E8BE`.

## Cập nhật 0.4.0 — 29/10/2026

- Build APK/AndroidTest và Lint thành công. APK: `dist/LinkMap-0.4.0-debug.apk`, versionCode 7, 23.489.326 byte; SHA-256 `7EC5E1768ED781EF53A74CB38D27D34B277993B6341FEB3C972CAF75A833D678`.
- 25 core tests qua, gồm ba kiểm thử mới: tuổi lớn hơn làm sao nhỏ dần với mức sàn, ngày tương lai không phóng to vượt mức, 1.000 vị trí không trùng và không xê dịch khi lọc/đổi tuổi, pha lấp lánh độc lập và liên tục.
- 19 Android regression cases (Share, 150/1.000 mục, preview, store) qua trên APK chốt. Case mới `GalaxyDemoTest` qua riêng ở lượt cuối (15,97 giây); tổng cộng 20 case đã qua. Case này xác minh 100 URL duy nhất/hợp lệ, hơn 80 ngày thêm khác nhau, pixel viền thay đổi theo thời gian và đứng yên khi tắt hiệu ứng, xem/ghim demo không tạo bookmark/reading/outbox thật, chuyển demo → thư viện trống → demo và mở lại vẫn đúng chế độ.
- Trong thử nghiệm đã xử lý snapshot GPU emulator bị đen màn hình bằng cold boot không wipe dữ liệu. Helper UI được sửa để không chờ main queue idle vô hạn khi Canvas đang animation, đồng thời làm mới cache accessibility sau đổi chế độ; không thay điều kiện kiểm tra UI để bỏ qua lỗi.
- Toàn cảnh giảm chi tiết thành điểm sáng khi sao dưới 12 pixel, đồng hồ chung 10 Hz; không tạo 100 coroutine hoặc đổi màu health theo nhịp sáng. Chỉ chạy khi map ở RESUMED; có nút tắt và tôn trọng system animator scale bằng 0.
- Ảnh thật: `screenshots/23-galaxy-v040.png`, `24-galaxy-twinkle-v040.png` (khung hình sau), `25-galaxy-100-overview-v040.png`. Dữ liệu, ngày thêm và health là demo được ghi nhãn rõ. Đây không phải kết quả kiểm tra HTTP thật.
- Bộ 100 link được đóng gói trong assets và hiện qua lớp trình bày bộ nhớ; không seed DB người dùng hoặc gửi lên Google. Chỉ hành động Lưu link này vào thư viện của tôi mới tạo mục thật, ngày thêm hiện tại và health chưa kiểm tra. Thư viện cũ không bị thay bằng demo.
- Chưa đo pin/nhiệt trên máy thật, chưa xác minh mọi OEM/TalkBack hoặc toàn bộ CDN; OAuth/Drive thật vẫn chưa cấu hình như bản trước. Web giữ nguyên. Xem `ANDROID-0.4.0.md`.

## Cập nhật 0.3.1 — 29/10/2026

- Thử hình sao 5 cánh trên map, cánh bo nhẹ và phần giữa rộng cho logo. Giữ bounding box tương tác của ô cũ để dễ chạm. Ảnh xem trước được clip theo ngôi sao, nền/viền giữ màu trạng thái.
- Cài đặt cho phép đổi Sao 5 cánh / Ô vuông, lưu theo thư viện trên thiết bị, không đổi vị trí hoặc bản ghi bookmark. Hình sao là mặc định nếu chưa lưu lựa chọn.
- `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` qua. Chạy lại luồng UI 150 link: tìm không dấu → ghim → định vị → về vị trí trước, qua (14,731 giây). Không chạy lại toàn bộ bộ kiểm thử lưu trữ vì đợt này chỉ sửa hiển thị và tùy chọn giao diện.
- Ảnh chụp thật trên emulator: `screenshots/20-android-star5-v031.png`, `21-android-star5-overview-v031.png`, `22-android-star5-close-v031.png`. Dữ liệu và trạng thái trong ảnh là fixture tổng hợp, không phải thư viện thật hoặc kết quả HTTP thật.
- APK: `dist/LinkMap-0.3.1-debug.apk`, versionCode 6. Web chưa thay đổi.

## Cập nhật 0.3.0 — 29/10/2026

- APK Android và AndroidTest build thành công; Lint: 0 lỗi, 26 cảnh báo (gồm chuỗi widget viết trực tiếp, khuyến nghị dependency/API). APK bàn giao: `dist/LinkMap-0.3.0-debug.apk`, versionCode 5, 23.294.736 byte.
- SHA-256 APK: `6DE2F5CD9F25B8852BADBC2C1F09685096961883A2553DE77CD431BBD53B887E`.
- 22 Kotlin core tests qua: 17 cũ + 5 cho tìm tiếng Việt đa từ, bộ lọc đọc/cảnh báo, HTML import, metadata giữ đúng chữ và hình học 1.000 mục.
- Toàn bộ 19 Android instrumentation tests qua trên Pixel 9 Pro emulator Android 16, 42,231 giây ở lượt chốt. Gồm 14 test store, 2 luồng UI 150/1.000 link, 1 Share/PROCESS_TEXT/widget registration, 2 test ảnh.
- Store tests dùng database riêng: migration schema 1→4 và 3→4, khôi phục giữ slot/ghi chú, ghim/lịch sử qua restart, nhập HTML không ghi đè, kết quả metadata cũ không áp dụng sau sửa URL/xóa, user title không bị thay và hidden URL collision không thay mất mục cũ.
- Nhập 1.000 bookmark HTML: một lượt đo 1.051 ms trên emulator; không đại diện hiệu năng mọi thiết bị. Kiểm tra lưu/mở lại đủ 1.000 mục, 1.000 slot riêng và tìm đúng mục 999.
- UI tests mở tài khoản tổng hợp riêng, luôn trả lại guest và xóa database fixture trong finally. Thử toàn cảnh → thư viện → tìm `do an android` → đúng `Đồ án Android Compose` → ghim → định vị map → có Về vị trí trước. Đã qua cả 150 và 1.000 mục.
- Thử lại luồng 150 link với font_scale 1.5 thành công; cài đặt emulator được trả về 1.0. Có ảnh xác minh chữ lớn. Chưa thử phiên TalkBack thực tế.
- Share một URL lưu ngay, giữ EXTRA_SUBJECT làm tên; PROCESS_TEXT cùng URL hiện mục cũ và không tạo thêm. Đã kiểm tra widget có đăng ký trong AppWidgetManager; chưa kiểm chứng đặt/resize trên mọi launcher.
- Ảnh xem trước: test giải mã ảnh 1.000px xuống không quá 512px, từ chối dữ liệu giả và dữ liệu quá 1 MB; HTTP, IPv4/IPv6 loopback không được tải. UI metadata/ảnh từ website riêng tư hoặc mọi website công khai không thể bảo đảm; chưa test tích hợp mạng mọi CDN.
- Ảnh 13–19 trong `screenshots/` là giao diện thật trên dữ liệu kiểm thử tổng hợp; màu health ở fixture là minh họa, không phải kết quả kiểm tra sống/chết của các URL đó. Fixture không trộn với thư viện thật.
- Không sửa/publish web trong đợt này. Giao thức journal/1 giữ nguyên. Ghim/lịch sử/bỏ qua cảnh báo cục bộ, có trong JSON backup; bộ sưu tập/title đi cùng giao thức sync hiện có.
- Chưa có OAuth Client ID nên chưa kiểm chứng Google login/consent/Drive roundtrip thật; chưa test máy vật lý/API 26, nghiên cứu 5–8 người dùng hoặc đo tỷ lệ tìm lại dưới 10 giây. Xem `ANDROID-0.3.0.md` cho phạm vi bàn giao và giới hạn.

## Cập nhật 0.2.0 — Web + Google/Drive sync

- Android APK build và Lint qua. 17 Kotlin core tests qua, gồm merge đồng thời, health độc lập, URL thay đổi và tombstone.
- 8 Android instrumentation tests qua trên Android 16, database kiểm thử riêng. Bao gồm replay nhật ký cũ sau tombstone, hai store offline sửa nội dung và nhận health, không tạo echo outbox, migration schema 1→2→3 giữ dữ liệu.
- 13 Node tests qua cho JavaScript protocol: normalize, nhận diện hostname, SHA-256 ID, replay idempotent, tính hội tụ/associative, health tie và giới hạn dữ liệu nhận.
- Browser QA trên localhost qua: thêm một URL không có scheme → đúng icon → lưu IndexedDB → reload dữ liệu còn → tìm kiếm → mở đúng chi tiết. Kiểm tra thiếu Client ID hiển thị thông báo rõ, không báo đăng nhập thành công giả. UI responsive được quan sát ở viewport hẹp của IAB.
- Android UI đã mở màn Google, hiển thị rõ build chưa cấu hình Client ID. Screenshot: `screenshots/09-android-google-v020.png`.
- Web screenshot: `screenshots/10-web-account-v020.png`, `screenshots/11-web-map-v020.png`.
- Chưa có OAuth Client ID/Google Cloud configuration nên **chưa test đăng nhập thật, consent Drive hoặc đồng bộ giữa tài khoản Google thật trên hai nền tảng**. Protocol/storage tests không thay thế kiểm thử tích hợp thật. Cấu hình một lần và checklist nằm trong `GOOGLE-SETUP.md`.
- Nhật ký Drive append-only chưa có compaction, UI lịch sử/khôi phục xung đột chưa có. Slot/camera của map là cục bộ từng thiết bị. Web hiển thị health từ Android; không kiểm tra giả bằng no-cors.

## Cập nhật 0.1.2 — 28/10/2026

- Một lớp nền thiên hà phủ toàn bộ cửa sổ, Top/Bottom Bar và Scaffold trong suốt, danh sách tìm kiếm cũng có nền trong suốt. Màu chữ/icon foreground được đặt rõ để đọc được trên nền tối.
- Bỏ tất cả vùng, nhãn và màu tinh vân phân biệt nền tảng. Các ô nằm trên một trường tọa độ chung, vị trí không phụ thuộc loại platform.
- SQLite schema v2 chuyển slot cũ (cục bộ từng nền tảng) thành slot toàn cục theo thứ tự tạo; giữ nguyên ID, URL, title, note, category, trạng thái, timestamp, metadata. Lần sau xóa/thêm/đổi nền tảng không di chuyển các ô hiện hữu.
- Form thêm mới chỉ có một EditText URL, tự nhận provider/icon, bổ sung HTTPS nếu nhập tên miền, tự tạo tên khi lưu. Dán và thêm là một hành động; gõ link có thể dùng nút Thêm hoặc IME Done. Các trường chi tiết chỉ hiện trong Chỉnh sửa.
- 15 core unit tests qua, gồm nhận URL không có scheme, giữ query/fragment, vị trí không phụ thuộc provider và không xê dịch khi xóa/thêm ô.
- 6 Android instrumentation tests qua trên emulator Android 16 bằng `am instrument` (database kiểm thử riêng, không xuất database chính); gồm migration v1→v2 và thêm URL nhanh với icon/slot toàn cục.
- Smoke test UI: form nhận URL GitHub có đúng 1 ô nhập và hiển thị icon tự động; bấm Thêm lưu được mà không điền tên/nhóm. Ảnh: `screenshots/08-quick-add-v012.png`.
- Screenshot toàn màn hình: `screenshots/07-fullscreen-v012.png`. APK build và Android Lint qua, còn các cảnh báo khuyến nghị cũ.

## Cập nhật 0.1.1 — nền thiên hà và tinh vân

- Thêm texture tinh vân RGBA có vân bụi, render màu theo nền tảng ở phía sau các ô. Mây gắn vào tọa độ cụm nên di chuyển và phóng to cùng bản đồ.
- Nền xa có dải mây xanh tím và 850 sao nhiều mức sáng, một số sao có quầng và tia mảnh. Không dùng animation/loop liên tục.
- Tăng độ rõ nền, giữ các ô trạng thái nằm trên cùng; cắt nền đúng biên bản đồ để không tràn vào thanh đầu/trước điều hướng; thêm nền tối dưới chú giải.
- Build APK và Android Lint thành công; kiểm tra screenshot thực tế trên emulator Android 16, không có crash. Các thay đổi chỉ liên quan hiển thị.
- Screenshot mới: `screenshots/06-nebula-v011.png`. Texture và prompt gốc: `app/src/main/res/drawable-nodpi/nebula_cloud.png`, `docs/concepts/map-v2/NEBULA-PROMPT.md` (built-in ImageGen).

Ngày 27/10/2026. Thiết bị chạy thử: Pixel 9 Pro emulator, Android 16/API 36, 1280×2856. Chưa kiểm tra điện thoại vật lý hoặc mọi OEM.

## Build và kiểm thử

- `:app:assembleDebug`: thành công, APK debug ký bằng debug keystore.
- `:core:test`: 13 test, 0 lỗi. Bao phủ ngày qua nửa đêm theo Asia/Ho_Chi_Minh, mở lại cùng ngày, URL có resource key/fragment, scheme/credentials không hợp lệ, hostname giả, 404 tài nguyên riêng tư, lỗi tạm thời, HTML login/CAPTCHA, 404 lặp lại, 410, hình học cụm và slot không trùng.
- `:app:connectedDebugAndroidTest`: 4 test SQLite chạy trên Android 16, 0 lỗi. Kiểm tra lưu và mở lại kết quả/mốc ngày, bỏ kết quả cho URL cũ và link đã xóa, import nguyên tử không ghi đè link hiện hữu, xóa không đánh lại số slot, chặn trùng.
- `:app:lintDebug`: không lỗi. Còn các cảnh báo khuyến nghị phiên bản dependency mới hơn, dùng KTX và cấu hình backup Android 12+. Bản đầu ghim dependency phù hợp SDK 36 sẵn có; không nâng toolchain để chỉ xóa cảnh báo.

## Smoke test bằng UI thật

- Cài APK, mở MainActivity, kiểm tra empty state, không thấy AndroidRuntime crash.
- Gửi 8 URL qua `ACTION_SEND` và 1 URL qua `ACTION_PROCESS_TEXT` bằng ADB rồi bấm Lưu qua UI. Cả 9 URL được lưu và chia thành đúng cụm nền tảng. Script tái hiện: `scripts/smoke-share.ps1` (chỉ chạy trên emulator test; script thêm dữ liệu mẫu).
- Đợt kiểm tra thực tế hoàn tất và SQLite có `completedDay=2026-10-27`, `checkedAt` cho cả 9 mục. Ví dụ HTTP 410 thực tế từ httpbin được hiển thị đỏ; Drive cần xác minh được hiển thị vàng.
- Sau khi force-stop và mở lại cùng ngày, so sánh bản sao SQLite trước/sau: cả 9 ID, timestamp và trạng thái giữ nguyên. Không quét lại các mục đã kiểm tra hôm đó.
- Nút kiểm tra lại thủ công chạy đợt 9 mục rồi trở về trạng thái hoàn tất.
- Tìm kiếm → chọn ComposeSamples → camera định vị ô → chạm ô → sheet chi tiết đúng URL, trạng thái và thời điểm kiểm tra.
- Sửa lỗi nhãn tên chồng nhau phát hiện trên screenshot: chỉ hiện tên khi zoom gần, rút gọn theo đúng chiều rộng ô.
- Sửa nhận biết CAPTCHA: tham chiếu script captcha trên trang công khai không còn tự động làm kết quả thành cần xác minh.

## Ảnh chụp thật

- `screenshots/01-empty.png`: lần mở đầu, không có bookmark.
- `screenshots/02-map.png`: bản đồ có 9 URL mẫu, kết quả kiểm tra thực tế, không giả lập màu.
- `screenshots/03-search.png`: tìm kiếm/danh sách.
- `screenshots/04-settings.png`: kiểm tra trong ngày và backup.
- `screenshots/05-detail.png`: chi tiết sau khi tìm kiếm định vị.

Ảnh do ADB screencap từ emulator, khác với ba ảnh concept ImageGen trước đó. Dữ liệu mẫu chỉ được thêm vào emulator chạy thử, không được seed trong APK.

## Giới hạn còn lại

- Mới là MVP Android; chưa phát hành Play Store, chưa có bản SwiftUI hoặc web.
- Đã thử receiver PROCESS_TEXT bằng intent; chưa thử menu của mọi app nguồn/OEM hoặc toàn bộ Chrome selection UX. Việc menu hiển thị do app nguồn quyết định. Không có quyền theo dõi toàn bộ clipboard nền.
- Chưa chạy trực tiếp API 26, offline giữa từng bước mạng, nhiều nghìn link, mọi tình huống đổi timezone hoặc font lớn. Logic ngày và hình học có unit test; vẫn cần đo trên máy thật.
- Bộ kiểm tra HTTP công khai là best effort; dấu hiệu HTML không thể bao phủ mọi trang đăng nhập/lỗi mềm hoặc nội dung cần JavaScript. HTTP 2xx không bảo đảm nội dung riêng tư tồn tại.
- Đã test serializer/import DB; trình chọn Google Drive thực tế chưa kiểm tra do emulator không cấu hình tài khoản Drive. Không tuyên bố có đồng bộ Google tự động.
- Cụm giữ slot của ô, không di chuyển khi đổi trạng thái. Khi cụm tăng kích thước, các cụm ở hàng phía sau có thể được đẩy ra để tránh chồng; chưa có kéo tự sắp xếp cụm hoặc tọa độ tùy chỉnh.
- Ánh sáng là hiệu ứng nền tĩnh có thể tắt; chưa có nhấp nháy trạng thái riêng từng ô.
- Kiểm tra thường nhật dùng GET streaming thay vì HEAD để có cơ hội nhận ra trang login/lỗi mềm; giới hạn prefix HTML 16 KB, không tải file lớn.
