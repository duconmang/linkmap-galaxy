# Cấu hình Google cho LinkMap — một lần cho ứng dụng

**Trạng thái mới nhất 0.7.0:** đã tạo project LinkMap Galaxy (`linkmap-galaxy`), Web + Android OAuth clients và test user developer@example.com; Android Google/Drive thật đã qua kiểm thử. ID công khai đã có trong gradle.properties và web/dist/config.js. Web đã nâng journal/2; kiểm thử popup Google trên Chrome còn kẹt tải. Đây là OAuth Testing, chưa phải phát hành production. [Báo cáo thực tế](GOOGLE-DEPLOYMENT-2026-10-30.md). Các dòng “chưa có Client ID” bên dưới là hướng dẫn/trạng thái lịch sử trước 0.7.0.

**Cập nhật 0.6.0:** Google Account Dev hiện có là đủ, không cần Gmail mới. Tạo/tái sử dụng project riêng cho LinkMap; Android/Web/iOS client cùng ứng dụng dùng cùng project và branding. Google yêu cầu tách project theo môi trường development/testing/production. [Google best practices](https://developers.google.com/identity/siwg/best-practices).

Android 0.6.0 dùng journal/2 cho LINK/NOTE và đọc legacy journal/1. Web hiện tại chỉ đọc journal/1; cần nâng cấp protocol/storage/UI của web trước khi bật đồng bộ note đa nền tảng. Hướng dẫn dưới đây là phần OAuth; có Client ID không tự bảo đảm mọi client đã tương thích note.

Google Cloud project và OAuth Client ID là cấu hình của **nhà phát triển LinkMap**. Người dùng cuối chỉ đăng nhập tài khoản Google và cấp quyền; họ không phải tạo project hay Client ID. Không có backend LinkMap để giữ bookmark hoặc refresh token.

## Trạng thái hiện tại

Mã Android và web đã có đăng nhập Google, cấp quyền Drive riêng, thư viện cục bộ theo tài khoản và giao thức đồng bộ chung. **Chưa có Client ID được cung cấp cho dự án**, vì vậy bản build mặc định chưa bật đăng nhập thật. Không dùng ID giả, không báo đăng nhập/đồng bộ thành công khi chưa qua Google.

## 1. Project dùng chung

1. Tạo hoặc chọn một project trong [Google Cloud Console](https://console.cloud.google.com/).
2. Bật **Google Drive API**.
3. Cấu hình **Google Auth Platform / Branding, Audience, Data Access** cho LinkMap. Trong giai đoạn Testing, thêm các tài khoản sẽ dùng để kiểm thử vào Test users.
4. Khai báo các scope: `openid`, `email`, `profile`, `https://www.googleapis.com/auth/drive.appdata`. Không xin quyền toàn bộ Drive.

Đăng nhập xác định danh tính; bật đồng bộ yêu cầu thêm quyền Drive. Việc tài khoản đã đăng nhập trong Chrome/Android không tự cấp quyền Drive cho LinkMap.

## 2. Web OAuth client

Tạo OAuth client loại **Web application** trong cùng project. Thêm JavaScript origins:

```text
http://127.0.0.1:4173
http://localhost:4173
https://linkmap-galaxy.opalchord20.chatgpt.site
```

Nếu chuyển sang domain khác, thêm origin chính xác của domain đó. GIS token popup dùng trong bản này không cần Client Secret hoặc backend đổi authorization code. Thông tin callback do thư viện GIS xử lý; không thêm Client Secret vào web/Android.

Client ID dạng `...apps.googleusercontent.com` là thông tin công khai của app. Điền cùng Web Client ID vào cả hai nền tảng:

```powershell
.\scripts\configure-google.ps1 -ClientId 'CLIENT_ID.apps.googleusercontent.com'
.\scripts\build.ps1
```

Script cập nhật `googleWebClientId` trong root `gradle.properties` và `web/dist/config.js`. Cần publish lại web sau khi thay config.

Hoặc sửa thủ công:

- Android: `gradle.properties`, dòng `googleWebClientId=...` (cũng có thể truyền `-PgoogleWebClientId=...`).
- Web: `web/dist/config.js`, `window.LINKMAP_CONFIG.googleClientId`.

## 3. Android OAuth client

Tạo thêm OAuth client loại **Android**, vẫn trong cùng project:

```text
Package name: vn.linkmap
Debug SHA-1: E0:F6:91:04:D8:97:36:AE:77:F5:1B:E3:7F:7D:B0:CC:8F:5D:12:C6
```

SHA-1 trên là debug signing key của máy phát triển hiện tại, không phải secret. Khi ký release/Play App Signing, đăng ký client hoặc certificate fingerprint tương ứng; debug client không thay thế release client.

Đặt **Web Client ID** vào cấu hình Android như mục 2. Credential Manager dùng ID này làm audience; Android client được Google nhận diện qua package + certificate. Chạy trên máy có Google Play services và tài khoản Google.

## 4. Kiểm thử tích hợp thật

1. Đăng nhập cùng một Google account trên web và Android.
2. Trên từng nền tảng, bấm **Bật đồng bộ Drive** và cấp quyền.
3. Thư viện khách không tự tải lên. Nếu muốn, bấm **Đưa thư viện trên máy vào tài khoản này**.
4. Thêm link trên web → đồng bộ → mở lại Android hoặc bấm Đồng bộ ngay. Kiểm tra đúng URL, tên, ghi chú.
5. Android kiểm tra link và đồng bộ kết quả; web nhận kết quả. Thử sửa ghi chú đồng thời và xóa khi một thiết bị offline; kết nối lại xác nhận không hồi sinh link cũ.
6. Đăng xuất, đăng nhập một account khác: không thấy thư viện account trước; quay lại account ban đầu vẫn giữ dữ liệu riêng.
7. Kiểm tra hết hạn/hủy cấp quyền, popup bị chặn, offline giữa upload và ACK; dữ liệu cục bộ phải còn nguyên.

Các bước này chưa thể xác nhận với Google thật trước khi có OAuth client. Các test tự động kiểm tra merge, replay, tombstone, migration và tách database độc lập với dịch vụ OAuth.

## Ghi chú triển khai

- Site hiện được tạo với quyền riêng tư của chủ sở hữu theo mặc định Sites. Lớp bảo vệ Site và đăng nhập Google cho dữ liệu Drive là hai lớp khác nhau. Trước khi phát hành rộng rãi cần cấu hình audience/domain của sản phẩm, trang chính sách và OAuth production phù hợp; chưa tự thay đổi audience của Site.
- Web lưu dữ liệu trong IndexedDB; token Google chỉ nằm trong bộ nhớ phiên. Reload hoặc hết hạn có thể cần kết nối lại, không có refresh token do LinkMap lưu trên server.
- Android dùng Credential Manager cho đăng nhập và AuthorizationClient cho quyền Drive. Token chỉ giữ trong bộ nhớ ứng dụng; SDK Google quản lý cache riêng của nó.
- Drive appDataFolder gắn với app/project. Hai OAuth client phải ở cùng project để cùng thấy nhật ký LinkMap.
- Web không tự kiểm tra mọi link do CORS; không suy diễn trạng thái từ opaque response. Android là checker theo ngày như thiết kế đã chốt.

Nguồn: [Android authentication](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), [Android authorization](https://developer.android.com/identity/authorization), [GIS web token model](https://developers.google.com/identity/oauth2/web/guides/use-token-model), [Drive appDataFolder](https://developers.google.com/workspace/drive/api/guides/appdata).
