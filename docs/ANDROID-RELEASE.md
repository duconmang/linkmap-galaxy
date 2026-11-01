# Android release 0.7.1

Package vn.linkmap, versionName 0.7.1, versionCode 14. Release là build không debuggable, ký bằng chứng thư riêng thay vì debug key. Đây là bản đóng gói có ký số, không đồng nghĩa đã được Google Play phê duyệt Production.

## Build

PowerShell 7, JDK 21, Android SDK 36:

```powershell
# Chỉ chạy một lần trên dự án mới chưa có khóa ký:
.\scripts\init-release-signing.ps1
# Các lần build sau luôn dùng lại khóa:
.\scripts\build-release.ps1
```

Build script tạo APK, AAB, chạy unit tests core và lintRelease. Không tự cài đè/xóa app trên thiết bị và không upload lên Play Console.

Máy CI / hệ điều hành khác có thể cung cấp biến môi trường LINKMAP_KEYSTORE_FILE, LINKMAP_KEYSTORE_PASSWORD, LINKMAP_KEY_ALIAS, LINKMAP_KEY_PASSWORD, rồi chạy assembleRelease/bundleRelease. Secrets phải lấy từ kho bí mật của CI, không ghi vào Gradle properties hoặc repository. Build release bị chặn nếu chưa cấu hình ký.

## Khóa ký và backup

Khóa cục bộ nằm trong .signing/linkmap-upload.p12; mật khẩu ngẫu nhiên được bảo vệ bằng DPAPI ở .signing/linkmap-upload.password.dpapi. Thư mục được giới hạn quyền người dùng Windows hiện tại và SYSTEM, bị loại khỏi Git. Không upload thư mục này cùng GitHub Release.

DPAPI chỉ giải mã được bằng tài khoản Windows hiện tại trên môi trường tương ứng. Trước khi đổi/cài lại máy, chủ dự án cần sao lưu keystore và lưu mật khẩu được giải mã vào nơi riêng an toàn. Chỉ sao chép file DPAPI sang máy mới không đủ để khôi phục khóa. Không tạo khóa mới để thay khóa đã phát hành.

Khóa này ký APK cài trực tiếp và có thể làm upload key cho AAB. Nếu Play App Signing tạo chứng thư phân phối khác, phải đăng ký SHA-1 của chứng thư app-signing trên Play cho Google OAuth, không chỉ SHA-1 upload key. Muốn APK GitHub và Play nâng cấp qua lại được cần lên kế hoạch dùng cùng app-signing certificate khi cấu hình Play; không mặc định hai chứng thư trùng nhau.

## Google đăng nhập và dữ liệu

Chứng thư release khác debug: cần Android OAuth client cho package vn.linkmap và SHA-1 release trong cùng Cloud project với Web client. Không tăng quyền Drive; vẫn hồ sơ cơ bản + drive.appdata. OAuth Testing và Play closed testing là hai cơ chế riêng biệt.

Đã đăng ký client `LinkMap Galaxy Android (Release)` trong project hiện tại sau xác nhận của chủ dự án. Public client ID: `881163930052-pannbs0jagqn1i3mdp7esbht6vi2hffv.apps.googleusercontent.com`. Chứng thư APK GitHub / upload key có SHA-1 `DD:3A:96:69:4C:AC:17:53:A5:02:DF:BF:27:A5:EE:F1:9C:5D:A9:B3`. Đây là thông tin công khai; không có private key/client secret trong source.

Kiểm tra bản đóng gói ngày 01/11/2026: assembleRelease + bundleRelease + 33 core tests + lintRelease thành công; APK Signature Scheme v2 xác minh hợp lệ; RSA 3072; non-debuggable; API min 26 / target 36; APK zipalign 16 KB đạt; chữ ký JAR của AAB xác minh hợp lệ. Chưa cài APK release lên emulator đang giữ bản debug để tránh gỡ app hoặc xóa dữ liệu; chưa tuyên bố Google roundtrip trên chính APK release đã được thử trực tiếp. Đăng ký OAuth server-side không cần nhúng Android client ID vào APK hoặc đổi Web client ID hiện có.

APK release không cài đè lên APK debug ký bằng chứng thư khác. Xuất hoặc đồng bộ dữ liệu của bản debug trước khi tự gỡ bản cũ. Script không tự gỡ app hay xóa dữ liệu.

APK để cài trực tiếp từ GitHub. AAB để upload vào Play Console, không cài trực tiếp như APK. Tài khoản Play cá nhân tạo sau 13/11/2023 phải closed test tối thiểu 12 người opted-in liên tục ít nhất 14 ngày trước khi xin Production; APK tải từ GitHub không thay thế closed test này. Dự án phi lợi nhuận không tự được miễn yêu cầu theo loại tài khoản.

Nguồn: https://support.google.com/googleplay/android-developer/answer/14151465 và https://developer.android.com/studio/publish/app-signing
