# LinkMap: bảo mật Web và dữ liệu người dùng

Cập nhật 30/10/2026. Phạm vi: Web 0.8, Android 0.7, journal/2.

## Kiến trúc hiện tại

Website là file tĩnh. Không có database link/note của LinkMap trên hosting. IndexedDB giữ nội dung cục bộ theo thư viện khách hoặc Google subject. Google Identity Services cấp token trong bộ nhớ; đồng bộ trực tiếp với Drive appDataFolder của người đăng nhập, cùng Google Cloud project với Android. Chỉ yêu cầu hồ sơ cơ bản và drive.appdata. Demo không ghi vào IndexedDB/outbox và không xuất cùng thư viện thật.

Google giới hạn appDataFolder cho ứng dụng tạo nó; đây là phân quyền, không phải mã hóa đầu cuối. Nội dung JSON trên Drive và IndexedDB chưa được ứng dụng mã hóa. Logout xóa phiên token trong bộ nhớ, giữ cache cục bộ để người dùng truy cập lại.

## Rủi ro cần hiểu đúng

| Tình huống | Hiện trạng / giới hạn |
|---|---|
| Hosting bị đọc trộm file tĩnh | Không có nội dung người dùng hoặc client secret trong gói web. Public OAuth client ID là cấu hình công khai. |
| Website bị thay mã JavaScript | Mã độc cùng origin có thể đọc IndexedDB và token trong phiên, rồi đọc Drive trong phạm vi được cấp. Backendless không giải quyết được nguy cơ này. |
| XSS qua tiêu đề/note/URL | Render bằng textContent/createElement; chỉ cho URL HTTP(S), không nhận credentials trong URL. CSP hạn chế nguồn script; không dùng innerHTML cho dữ liệu. |
| Tài khoản Google khác | Namespace tách theo subject và kiểm tra subject khi cấp Drive; appDataFolder thuộc chính tài khoản cấp token. Namespace không chống được XSS cùng origin. |
| Máy dùng chung / thiết bị bị chiếm quyền | Cache vẫn còn sau logout. Người dùng cần xóa dữ liệu website hoặc dùng profile riêng. |
| Nhật ký đồng bộ bị hỏng / quá lớn | Kiểm tra schema/version/record, giới hạn kích thước nhận theo byte trước JSON.parse, kiểm tra ID và kích thước file. Không import hoặc thực thi HTML. |
| Link cần đăng nhập / CORS | Web không suy đoán sống/chết qua no-cors; đọc kết quả Android kiểm tra. Reachable không có nghĩa là an toàn. |

## Đã đưa vào mã 0.8

- Không dùng Tailwind CDN, unpkg icon runtime, Google Fonts, favicon proxy hoặc analytics. Icon/font giao diện dùng nội bộ. Google SDK vẫn tải trực tiếp từ Google đúng hướng dẫn nhà cung cấp.
- CSP meta hạn chế script/connect/frame/img; referrer policy; chặn object/base/form. Không có mã script inline.
- Token chỉ trong bộ nhớ, không ghi localStorage/IndexedDB/URL/log. Bearer API wrapper chỉ gửi tới origin Google API cố định.
- Dữ liệu người dùng được render dưới dạng text; mở liên kết ngoài với noopener/noreferrer.
- Hộp xác nhận xóa là HTML dialog, không phụ thuộc native confirm.
- Demo trạng thái được ghi rõ là minh họa; chỉ số UI lấy từ dữ liệu hiện tại.
- Service worker cache file ứng dụng cùng origin, không cache Google API hoặc payload dữ liệu người dùng.

CSP là phòng vệ bổ sung. Một deployment độc hại có thể thay chính CSP và JavaScript. Meta CSP không thiết lập được frame-ancestors, HSTS hay Cross-Origin-Opener-Policy.

## Vận hành tên miền và hosting

1. Bật MFA/passkey cho GitHub, Google Cloud, email quản trị và nhà đăng ký domain; khóa chuyển domain, giới hạn người có quyền deploy.
2. GitHub Pages phục vụ HTTPS; chỉ upload artifact web đã kiểm tra, không upload .env, token, keystore, backup người dùng hoặc toàn bộ thư mục làm việc.
3. Với domain riêng: xác minh domain GitHub để hạn chế takeover, cấu hình DNS theo tài liệu GitHub, enforce HTTPS sau khi certificate sẵn sàng. Dọn DNS khi ngừng dùng site. DNSSEC khi nhà cung cấp hỗ trợ và cấu hình đúng.
4. Muốn HTTP security headers tùy chỉnh: dùng hosting/proxy có hỗ trợ. Đề xuất CSP header, X-Content-Type-Options: nosniff, Referrer-Policy, frame-ancestors 'none', COOP same-origin-allow-popups cho Google popup. Chỉ bật HSTS/includeSubDomains sau khi mọi subdomain đã dùng HTTPS. Không khẳng định các header này đã áp dụng trên GitHub Pages.
5. Không tự động broad-scope, không thu refresh token hoặc client secret vào frontend. Thêm origin chính xác cho GitHub Pages vào Web OAuth client hiện tại; giữ Cloud project để Android và Web dùng chung appDataFolder.
6. OAuth hiện Testing. Mở cho mọi người cần hoàn tất cấu hình consent/publishing phù hợp, privacy URL/domain và yêu cầu Google nếu phát sinh. Không đổi production chỉ để vượt màn thử nghiệm.

GitHub project Pages dùng chung origin `https://duconmang.github.io` với các project Pages khác của tài khoản. Các đường dẫn repository không phải ranh giới bảo mật origin: cần kiểm soát mã của tất cả các site cùng origin. Trước khi phát hành rộng rãi, ưu tiên subdomain riêng (ví dụ linkmap.domain-cua-ban) để tách origin, cập nhật OAuth origin và cho người dùng đăng nhập lại. IndexedDB không tự chuyển giữa hai origin; dữ liệu đã đồng bộ sẽ được tải lại từ Drive sau đăng nhập, thư viện khách cần xuất/nhập.

## Đề xuất giai đoạn tiếp: vault mã hóa đầu cuối

Triển khai đồng thời trên Web và Android qua protocol/envelope mới, có migration và rollback được kiểm thử. Random master key mã hóa AES-256-GCM, nonce duy nhất mỗi lần ghi; bọc khóa bằng khóa dẫn xuất từ mật khẩu riêng, KDF có salt/tham số và ngân sách thiết bị phù hợp. Có recovery secret, kiểm thử mất mật khẩu/thiết bị và cảnh báo rõ mất khóa không thể khôi phục. Google login xác định tài khoản, không thay thế bí mật giải mã. Không lưu master key rõ vào Drive hay localStorage. Không âm thầm fallback plaintext nếu giải mã thất bại. Bản xuất mã hóa cần thiết kế riêng, có version và xác thực.

E2EE bảo vệ blob Drive bị lộ và dữ liệu khóa ở trạng thái nghỉ; không ngăn JavaScript website độc hại đọc plaintext/khóa sau khi người dùng mở vault. Native app được ký, Android Keystore và quy trình phát hành có kiểm soát giúp giảm phụ thuộc vào mã web được tải mỗi lần, nhưng không bảo vệ tuyệt đối thiết bị đã bị chiếm quyền.

## Tài liệu nguồn

- Google appDataFolder: https://developers.google.com/workspace/drive/api/guides/appdata
- Google GIS CSP: https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid#content_security_policy
- OWASP HTML5: https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html
- OWASP CSP: https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html
- OWASP Cryptographic Storage: https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html
- GitHub Pages publishing: https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site
