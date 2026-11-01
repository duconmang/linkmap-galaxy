# LinkMap Galaxy

Ứng dụng lưu bookmark và ghi chú trên Android và Web, hiển thị dưới dạng bản đồ Thiên hà hoặc Data Center. Dữ liệu lưu cục bộ; người dùng có thể đăng nhập Google để đồng bộ trực tiếp vào vùng dữ liệu riêng của ứng dụng trên Drive của chính họ.

**Website:** https://duconmang.github.io/linkmap-galaxy/

**Demo 150 link:** https://duconmang.github.io/linkmap-galaxy/?demo=150

**Phiên bản mã hiện tại:** Android 0.7.0 · Web 0.8.1.

## Tính năng

- Nhập một link hoặc đoạn văn: tự nhận diện URL, nền tảng và icon; văn bản thuần thành note. Nếu có nhiều URL, người dùng chọn một link và giữ toàn văn.
- Android Jetpack Compose: thiên hà sao 5 cánh, kích thước theo tuổi, viền sáng chậm và đường nối cùng nền tảng theo màu.
- Web: chuyển giữa Thiên hà và Data Center; phòng DC, rack, ô link và đường nối dùng chung camera khi kéo/zoom.
- Tìm kiếm, bộ sưu tập, ghim và quản lý link/note; sao lưu JSON; Share / selected text trên Android.
- Android kiểm tra link vào lần mở đầu ngày. Web hiển thị kết quả đã đồng bộ; không suy đoán trạng thái HTTP bằng no-cors.
- Đồng bộ journal/2, hỗ trợ xung đột chỉnh sửa và dấu xóa. Demo không tự đưa vào thư viện hoặc Drive.

## Cấu trúc repository

| Đường dẫn | Nội dung |
|---|---|
| app/ | Android Kotlin / Compose, SQLite, checker, Google login, Drive sync, widget và instrumentation tests |
| core/ | Logic Kotlin dùng chung và unit tests: URL, note, merge, health, bố cục, mạng nền tảng |
| gradle/, gradlew, gradlew.bat | Gradle Wrapper |
| web/ | Mã web tĩnh, preview server và Node tests |
| web/dist/ | Bản web chuẩn để chỉnh sửa và triển khai |
| index.html, *.js, style.css, assets/ | Bản sao web/dist được GitHub Pages phục vụ từ main / root |
| scripts/ | Build Android, cấu hình public OAuth Client ID, smoke test và đồng bộ artifact Pages |
| docs/ | Hướng dẫn, giao thức, thiết kế và báo cáo kiểm thử; không kèm ảnh chụp tài khoản |

## Chạy Android

Yêu cầu JDK 21, Android SDK 36 và thiết bị Android 8.0/API 26 trở lên. Mở thư mục gốc bằng Android Studio rồi Run module app.

Trên Windows:

~~~powershell
.\scripts\build.ps1
~~~

Hoặc với JAVA_HOME / ANDROID_HOME đã đặt:

~~~powershell
.\gradlew.bat :app:assembleDebug :core:test :app:lintDebug
~~~

APK debug được tạo tại app/build/outputs/apk/debug/app-debug.apk. APK và output build không được commit.

Instrumentation tests cần emulator/thiết bị riêng:

~~~powershell
.\scripts\build.ps1 -DeviceTests
~~~

Bài kiểm thử Drive thật chỉ chạy khi bật opt-in liveGoogle và kết nối Drive trong app; không chạy bài này trên tài khoản/dữ liệu không dùng để kiểm thử.

## Chạy Web

Node.js hiện đại, không cần npm install hoặc app backend:

~~~powershell
cd web
node server.mjs
node --test tests/*.test.mjs
~~~

Preview tại http://127.0.0.1:4173.

GitHub Pages đang phục vụ main / root. Sau khi sửa web/dist, đồng bộ artifact từ thư mục gốc:

~~~powershell
node scripts/sync-web-pages.mjs
node scripts/sync-web-pages.mjs --check
~~~

Commit cả web/dist và các artifact ở root. Không thay nguồn Pages sang repository khác. Xem [Web](web/README.md) và [DC 0.8.1](docs/WEB-0.8.1-DC.md).

## Google OAuth và quyền riêng tư

Public Web OAuth Client ID có trong gradle.properties và web/dist/config.js; đây là định danh công khai, không phải Client Secret. Các client Android/Web phải cùng Google Cloud project để đồng bộ vào cùng appDataFolder. Android cần đăng ký package và SHA-1 của chứng thư ký của người build; không đưa keystore lên GitHub.

Để dùng project của bạn:

~~~powershell
.\scripts\configure-google.ps1 -ClientId "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
node scripts/sync-web-pages.mjs
~~~

Đăng ký đúng JavaScript origin của website trong OAuth client. OAuth của bản hiện tại ở chế độ Testing; không mặc định mọi tài khoản đều đăng nhập được.

Nội dung nằm trong IndexedDB / SQLite và Google Drive của người dùng; không có database nội dung của LinkMap phía server. Hiện **chưa mã hóa đầu cuối**. Mã website bị thay hoặc thiết bị bị chiếm quyền có thể đọc thư viện đang mở. Logout giữ cache cục bộ. Xem [hướng dẫn Google](docs/GOOGLE-SETUP.md), [giao thức đồng bộ](docs/SYNC-PROTOCOL.md) và [bảo mật](docs/WEB-SECURITY.md).

Không commit token, OAuth secret, service-account key, keystore, .env, local.properties, database người dùng, backup cá nhân, ảnh chụp tài khoản, APK hoặc cache. Các assets hình nền/icon và demo mẫu là tài nguyên ứng dụng, được giữ trong mã nguồn. Logo thương hiệu và thư viện bên thứ ba: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
