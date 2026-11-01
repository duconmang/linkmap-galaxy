# Ứng dụng quản lý bookmark — nghiên cứu và phương án

> Cập nhật UI: người dùng đã làm rõ hướng bản đồ ô vuông, mỗi ô là một bookmark và các ô cùng nền tảng tạo thành cụm. Xem [hướng UI bản đồ mới](HUONG-UI-BAN-DO.md). Đề xuất giao diện thư viện A/B/C dưới đây là lịch sử tham khảo, không còn là hướng UI khuyến nghị hiện tại. Backend chưa được chốt.

Ngày nghiên cứu: 27/10/2026. Trạng thái: đề xuất để lựa chọn; chưa khởi tạo ứng dụng hoặc triển khai dịch vụ cloud. Thư mục dự án ban đầu trống.

## 1. Hướng đề xuất

Android dùng Kotlin + Jetpack Compose; iOS sau này dùng SwiftUI. Hai ứng dụng dùng chung định danh người dùng, mô hình dữ liệu và giao thức đồng bộ. Không cần dùng chung mã giao diện để đồng bộ được.

Khuyến nghị: giao diện A (Thư viện), có thể chuyển giữa ô vuông và danh sách có chấm tròn; triển khai Android offline trước trong kiến trúc có đường mở cho Firebase. Nếu muốn dùng nhiều thiết bị ngay ở bản đầu, chọn Firebase ngay từ đầu. Nếu ưu tiên PostgreSQL và khả năng tự vận hành, chọn Supabase thay thế.

Tách hai lựa chọn độc lập: A/B/C là phong cách giao diện; T1/T2/T3 là kiến trúc dữ liệu. Bất kỳ giao diện nào cũng có thể kết hợp với bất kỳ kiến trúc nào.

## 2. Phạm vi chức năng

### Bản Android đầu tiên

- Tạo/sửa/xóa bookmark: URL, tiêu đề, ghi chú, nhóm, ghim.
- Tạo/sửa/sắp xếp nhóm, chọn biểu tượng và màu. Có nhóm mặc định theo dịch vụ: Facebook, Google Drive, OneDrive, TikTok, GitHub, Khác.
- Nhận URL qua nút Chia sẻ của Android, ngoài cách dán URL trong ứng dụng. Xác thực URL từ dữ liệu được chia sẻ trước khi lưu.
- Tự gợi ý dịch vụ từ hostname, cho phép đổi nhóm. So khớp đúng hostname/subdomain, tránh nhận nhầm tên miền giả.
- Phân biệt `provider` (dịch vụ của URL) với `collection` (nhóm do người dùng đặt). Ví dụ link GitHub vẫn có thể nằm trong nhóm Công việc.
- Đề xuất mỗi bookmark có một nhóm chính, nhiều tag triển khai sau. Phát hiện URL trùng và gợi ý mở bản đã lưu.
- Tìm theo tiêu đề, URL và ghi chú; lọc theo nhóm, trạng thái, ghim.
- Chọn chế độ lưới hoặc danh sách. Chạm để mở bằng trình duyệt/ứng dụng phù hợp; menu để sửa, sao chép, chuyển nhóm, kiểm tra lại.
- Kiểm tra một link hoặc cả thư viện; hiển thị tiến độ và thời điểm kiểm tra gần nhất.
- Lịch mặc định mỗi 24 giờ, tùy chọn tắt/6 giờ/12 giờ/24 giờ/3 ngày/7 ngày; chỉ Wi-Fi; tránh khi pin yếu.
- Xuất/nhập JSON để sao lưu; HTML bookmark để chuyển từ trình duyệt ở giai đoạn tiếp theo. CSV là tùy chọn.
- Giao diện sáng/tối; chữ có thể phóng to; màu trạng thái luôn đi kèm biểu tượng và nhãn.

### Sau bản đầu

Đăng nhập, đồng bộ nhiều thiết bị, thùng rác và phục hồi, lịch sử kiểm tra, ứng dụng SwiftUI, web cho máy tính, browser extension, widget Android. Chia sẻ nhóm giữa nhiều người và kiểm tra bằng OAuth là phạm vi riêng, không mặc định đưa vào MVP.

## 3. Kiểm tra link: mô hình đúng và giới hạn

Không dùng ICMP ping để kết luận một bookmark tồn tại. Ping có thể chỉ nói về máy chủ hoặc bị chặn; không xác nhận bài viết, video hoặc file cụ thể. Dùng HTTP(S) và phân tích kết quả có giới hạn.

| Trạng thái | Hiển thị | Ý nghĩa và quy tắc đề xuất |
|---|---|---|
| Reachable | Xanh + dấu ✓ | HTTP thành công, không phát hiện trang đăng nhập/trang lỗi đã biết; chỉ là quan sát gần nhất |
| Unavailable | Đỏ + dấu × | Có bằng chứng không truy cập được, như 410 hoặc 404 lặp lại trên tài nguyên công khai; nhãn ghi rõ lý do, không khẳng định đã xóa vĩnh viễn |
| Uncertain | Vàng + khóa/dấu ? | 401/403, CAPTCHA, redirect đến đăng nhập, giới hạn truy cập, timeout, DNS/TLS hoặc lỗi tạm thời; cần kiểm tra lại |
| Unchecked | Xám | Chưa kiểm tra; kết quả cũ vẫn có thời gian và nhãn quá hạn riêng |
| Checking | Vòng tiến độ | Trạng thái chạy tạm thời, không ghi đè kết quả cuối cùng trước khi xong |

RFC 9110 cho phép máy chủ dùng 404 để che tài nguyên bị cấm truy cập. Google Drive API cũng nêu rõ 404 có thể là thiếu quyền hoặc file không tồn tại. Do đó không áp dụng quy tắc “404 = đã chết” cho mọi dịch vụ. [HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html), [Drive errors](https://developers.google.com/workspace/drive/api/guides/handle-errors).

Facebook/TikTok/Drive/OneDrive và repo riêng tư cần cách xử lý theo dịch vụ. Không có bộ kiểm tra HTTP chung nào bảo đảm nhận biết đúng mọi nội dung. HTTP 200 vẫn có thể chỉ là trang đăng nhập hoặc trang báo lỗi mềm. MVP dùng kiểm tra công khai theo kiểu best effort; không tự thu thập cookie hay mật khẩu của người dùng. Tích hợp API/OAuth từng nhà cung cấp chỉ làm khi thật sự cần và sau khi đánh giá quyền, quota, điều kiện sử dụng.

### Quy trình đề xuất

1. Chỉ nhận URL HTTP(S) hợp lệ; giữ URL gốc. Không tự bỏ query/fragment vì có thể là tham số truy cập, resource key hoặc đường dẫn trong ứng dụng web.
2. Kiểm tra điều kiện mạng; mất mạng toàn thiết bị thì dừng đợt kiểm tra, không chuyển toàn bộ bookmark sang đỏ.
3. Thử HEAD; nếu phương thức không được hỗ trợ hoặc kết quả không đủ tin cậy thì dùng GET streaming với giới hạn byte. Không tải nguyên video/file, không thực thi JavaScript trong MVP.
4. Redirect tối đa 5 bước, giữ URL đích để giải thích kết quả nhưng không tự thay URL gốc. Với backend, kiểm tra an toàn từng đích trước khi kết nối.
5. Điểm khởi đầu để hiệu chỉnh: timeout tổng mỗi link 15 giây, đọc tối đa 64 KB, tối đa 4 request đồng thời và 1–2 request/host.
6. 429: tôn trọng Retry-After; 5xx/timeout: retry có backoff và jitter, giới hạn số lần. 401/403: yêu cầu xác minh, không thử lại dồn dập.
7. Với 404 công khai chưa rõ: vàng “không tìm thấy, chờ kiểm tra lại”; đỏ sau hai lần quan sát cách nhau một khoảng chờ, ví dụ ít nhất 6 giờ. Quy tắc riêng của dịch vụ có quyền ẩn tài nguyên được ưu tiên hơn quy tắc chung này.
8. Lưu mã HTTP, lý do, thời gian, nguồn kiểm tra (máy/server), kết quả thành công gần nhất và số lần lỗi liên tiếp. Chỉ báo khi có thay đổi đáng chú ý; không báo lặp mỗi ngày.

Đỏ nghĩa là lỗi truy cập đã quan sát, không đồng nghĩa nên xóa bookmark. App không tự xóa link lỗi.

### Chấm nhấp nháy

Giữ lựa chọn chấm tròn/ô vuông như yêu cầu. Đề xuất màu ổn định khi nghỉ; chấm nhấp nháy nhẹ chỉ lúc đang kiểm tra hoặc ngay khi đổi trạng thái. Có cài đặt bật/tắt hiệu ứng; tôn trọng chế độ giảm chuyển động. Không tạo cảm giác đang giám sát trực tiếp khi dữ liệu chỉ cập nhật mỗi ngày.

## 4. Lịch chạy nền

Android dùng unique PeriodicWorkRequest của WorkManager; đổi cài đặt thì cập nhật lịch hiện có, không tạo worker trùng. Lịch 24 giờ là chu kỳ mục tiêu, không phải cam kết chạy đúng một giờ cố định. WorkManager có chu kỳ tối thiểu 15 phút và chịu giới hạn pin/Doze. Khi người dùng force-stop ứng dụng, không hứa tác vụ vẫn chạy. Khi mở lại, kiểm tra các mục đến hạn. [Android WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

iOS dùng BackgroundTasks nhưng hệ điều hành quyết định thời điểm cho chạy. Vì thế muốn kiểm tra khi điện thoại tắt hoặc lâu không mở app thì cần scheduler + worker trên server. Backend vẫn có thể trễ hoặc lỗi, cần retry và hiển thị thời gian thực tế. [Apple background strategies](https://developer.apple.com/documentation/backgroundtasks/choosing-background-strategies-for-your-app).

Nếu bật cloud checking, server là bên kiểm tra định kỳ chính; thiết bị chỉ kiểm tra thủ công/bù khi cần để tránh mỗi máy cùng quét toàn bộ. Lịch kiểm tra và tùy chọn Wi-Fi là hai lớp: Wi-Fi chỉ chi phối việc chạy trên thiết bị. Link riêng tư/nội bộ không được gửi đến cloud checker mặc định.

## 5. Ba phương án triển khai

### T1 — Lưu trên thiết bị trước

Kotlin, Compose Material 3, ViewModel + StateFlow, Coroutines, Room, DataStore, WorkManager, OkHttp. Repository tách dữ liệu lưu trữ khỏi UI và bộ kiểm tra. Room là nguồn dữ liệu cho giao diện.

Ưu điểm: không cần tài khoản hoặc server, dùng offline, ít chi phí vận hành, dễ thử nghiệm cách quản lý bookmark. Hạn chế: chưa có đồng bộ tự động; backup/import là thao tác riêng; lịch kiểm tra phụ thuộc thiết bị.

Phù hợp nếu bro muốn dùng cá nhân trên Android trước. Chuẩn bị UUID, phiên bản bản ghi và dấu xóa ngay trong schema để bổ sung đồng bộ sau; vẫn cần viết và kiểm thử lớp đồng bộ khi nâng cấp.

### T2 — Firebase, ưu tiên ra bản đồng bộ sớm

Compose + Firebase Authentication + Cloud Firestore. SDK Firestore có cache offline trên Android/Apple và tự đồng bộ khi có mạng. Với sửa cùng document, mặc định là last-write-wins. [Firestore offline](https://firebase.google.com/docs/firestore/manage-data/enable-offline).

MVP dùng Firestore qua repository làm nguồn dữ liệu; không thêm Room chỉ để tạo một cache thứ hai. Nếu chọn lộ trình nâng cấp từ T1 thì giữ Room làm nguồn chính và viết outbox/sync adapter rõ ràng, không để hai tầng dữ liệu cạnh tranh nhau.

Cloud Functions/Cloud Run kết hợp scheduler và hàng đợi để quét link công khai theo `nextCheckAt`. Firebase có scheduled functions dùng Cloud Scheduler. [Scheduled functions](https://firebase.google.com/docs/functions/schedule-functions).

Ưu điểm: Android, iOS và web có đường triển khai đồng bộ ngắn hơn; giảm phần backend phải tự xây. Hạn chế: phụ thuộc nền tảng Google, chi phí theo sử dụng, phải kiểm soát lượt đọc/ghi, worker và lưu lịch sử. Cloud checking cần billing phù hợp; không mặc định cam kết miễn phí.

Đây là lựa chọn khuyến nghị nếu đồng bộ là tính năng của bản đầu. Bộ kiểm tra server có thể thêm ở cột mốc sau, độc lập với đồng bộ bookmark.

### T3 — Supabase/PostgreSQL, ưu tiên quyền chủ động

Compose + Room + sync outbox, Supabase Auth/PostgreSQL/RLS, API hoặc client để trao đổi thay đổi, backend worker/scheduler kiểm tra link. iOS SwiftUI dùng cùng backend.

Supabase có thư viện Swift; thư viện Kotlin trong tài liệu là thư viện do cộng đồng duy trì, cần đánh giá trong spike kỹ thuật. RLS phải giới hạn dữ liệu theo chủ sở hữu. [Kotlin client](https://supabase.com/docs/reference/kotlin/introduction), [Swift client](https://supabase.com/docs/reference/swift/installing), [Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security).

Ưu điểm: mô hình quan hệ rõ, SQL thuận tiện, có hướng tự host và chuyển dữ liệu. Hạn chế: realtime không thay thế cơ chế đồng bộ offline; phải tự xử lý hàng đợi ghi, xung đột, phân trang thay đổi, xóa và retry. Tự host tăng trách nhiệm backup, cập nhật và giám sát.

| Tiêu chí | T1 | T2 | T3 |
|---|---|---|---|
| Bản Android dùng cá nhân | Nhanh nhất | Nhanh | Nhiều việc hơn |
| Đồng bộ nhiều máy | Chưa có | SDK hỗ trợ nhiều phần | Tự xây lớp offline sync |
| Kiểm tra khi điện thoại tắt | Không | Có nếu thêm worker server | Có nếu thêm worker server |
| Offline | Room | Cache Firestore hoặc Room + adapter khi nâng cấp T1 | Room + outbox |
| Vận hành | Thấp | Thấp đến vừa | Vừa; cao hơn nếu tự host |
| Chi phí | Không có backend riêng | Theo sử dụng | Hosting/tài nguyên + công vận hành |

Không khuyến nghị đồng bộ một file JSON qua Drive/OneDrive làm cơ chế chính: phải xử lý xung đột cả file, xóa, merge và sửa đồng thời. Có thể dùng làm backup/export sau này.

## 6. Dữ liệu và đồng bộ

Mô hình khởi điểm:

- Collection: id, ownerId, name, icon, color, sortOrder, revision, updatedAt, deletedAt.
- Bookmark: id, ownerId, collectionId, originalUrl, comparisonKey, provider, title, note, pinned, revision, createdAt, updatedAt, deletedAt.
- LinkHealth: bookmarkId, checkedUrlRevision, status, reason, httpCode, checkedAt, lastSuccessAt, nextCheckAt, consecutiveFailures, source.
- CheckHistory: sự kiện kiểm tra có giới hạn lưu trữ, không ghi response HTML/cookie/token.
- UserPreferences: chu kỳ kiểm tra và lựa chọn đồng bộ; các cài đặt gắn thiết bị như Wi-Fi/theme có phạm vi riêng.
- SyncOutbox: chỉ cần cho T1 nâng cấp đồng bộ hoặc T3; operationId, entityId, baseRevision, payload, retry state.

Quy tắc: UUID tạo được khi offline; thao tác có idempotency key; xóa dùng tombstone để không xuất hiện lại khi thiết bị cũ online. Với custom sync, dùng revision/server version thay vì chỉ so đồng hồ của điện thoại. Đồng bộ ghi chú/URL và kết quả kiểm tra tách nhau để worker không ghi đè chỉnh sửa người dùng. Bỏ kết quả kiểm tra cũ nếu URL đã đổi.

Với Firebase MVP có thể chọn last-write-wins và nói rõ giới hạn khi sửa cùng bookmark ở hai máy. Cần lưu thùng rác/lịch sử sửa tối thiểu cho phục hồi và kiểm thử các trường hợp sửa/xóa đồng thời; không tuyên bố SDK mặc định đã giải quyết mọi xung đột sản phẩm.

## 7. Quyền riêng tư và an toàn của bộ kiểm tra

Tài khoản nào chỉ truy cập bookmark tài khoản đó, kiểm tra bằng Security Rules hoặc RLS. Không nhúng quyền quản trị backend trong app. Log cần che query nhạy cảm, token chia sẻ và thông tin đăng nhập. Không gửi link riêng tư lên server chỉ vì người dùng bật đồng bộ nếu chưa chọn cloud checking.

Backend nhận URL tùy ý phải có bộ chống SSRF: giới hạn scheme/port, chặn địa chỉ nội bộ/loopback/link-local/metadata và địa chỉ không public, kiểm tra IPv4/IPv6, pin địa chỉ kết nối đã được xác thực, xác thực lại từng redirect, giới hạn thời gian/kích thước và network egress. Áp dụng cho cả fetch tiêu đề/favicon. [OWASP SSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html).

Mã hóa đầu-cuối toàn bộ URL và để server tự đọc URL kiểm tra là hai yêu cầu xung đột. Nếu sau này cần E2EE, kiểm tra tại thiết bị hoặc thiết kế quyền chia sẻ URL riêng cho checker; không hứa đồng thời cả hai ở MVP.

## 8. Ba hướng giao diện

Ảnh là concept tạo bằng ImageGen, không phải screenshot app đã chạy. Chữ, icon và vị trí trạng thái trong ảnh mang tính minh họa; đặc tả ở trên là chuẩn khi triển khai. Ảnh tĩnh không thể thể hiện nhấp nháy.

### A — Thư viện

![A](concepts/A-thu-vien.png)

Nhóm theo lưới hai cột, bấm vào để xem bookmark dưới dạng ô gần vuông có tiêu đề, favicon, trạng thái và thời gian. Phong cách sáng, nhẹ, dễ dùng hằng ngày. Ưu điểm: cân bằng lưu trữ và tra cứu; hạn chế: số mục nhìn thấy ít hơn danh sách. Đây là hướng khuyến nghị.

### B — Tín hiệu

![B](concepts/B-tin-hieu.png)

Danh sách có chấm tròn trạng thái lớn; nền tối và bộ lọc theo dịch vụ. Trang kiểm tra ưu tiên mục lỗi/cần xác minh và thao tác thử lại. Gần ý tưởng chấm nhấp nháy nhất. Ưu điểm: rà soát sức khỏe link nhanh; hạn chế: dễ khiến trải nghiệm thiên về giám sát hơn lưu trữ nếu đưa quá nhiều cảnh báo.

### C — Không gian làm việc

![C](concepts/C-khong-gian.png)

Giao diện tổ chức theo nhóm, ưu tiên ghi chú và chế độ hai vùng trên tablet/máy tính. Có màn cài đặt lịch kiểm tra rõ ràng. Ưu điểm: hợp thư viện lớn và nhiều thiết bị; hạn chế: nhiều điều khiển hơn. Trên điện thoại tách danh sách nhóm và nội dung thành các màn riêng.

Có thể chọn A làm mặc định, B làm chế độ danh sách/trang kiểm tra, C làm bố cục tablet về sau. Không cần triển khai cả ba sản phẩm riêng.

## 9. Lộ trình và điều kiện nghiệm thu

1. Chốt concept, kiến trúc, quy tắc trạng thái. Dựng prototype Compose với dữ liệu mẫu cho thư viện, nhóm, thêm/sửa, chi tiết, kiểm tra, cài đặt.
2. Lưu bền vững và nhận Share; thêm/sửa/xóa, nhóm, tìm kiếm, nhập/xuất. Mất mạng và khởi động lại vẫn đọc/sửa được dữ liệu đã lưu.
3. Link checker và WorkManager: kiểm thử 200, redirect, HEAD 405, 401/403, 404, 410, 429, 5xx, login HTML 200, timeout, TLS/DNS, offline, hủy và chạy lại. Không chuyển cả thư viện đỏ do mất mạng. Đổi lịch không sinh worker trùng.
4. Nếu chọn cloud: kiểm thử hai máy, offline rồi online, sửa cùng mục, xóa trong lúc máy khác offline, đổi URL khi đang kiểm tra, đăng xuất/đổi tài khoản. Đánh giá Security Rules/RLS và SSRF trước khi mở checker cho URL bên ngoài.
5. Mở rộng SwiftUI dùng chung backend; web/tablet và extension sau khi luồng lưu/tra cứu đã ổn.

Chưa đưa lịch giao hàng cố định vì chưa chọn backend/phạm vi và chưa kiểm tra môi trường Android SDK/build. Không cần khởi tạo cloud hoặc chịu phí để duyệt concept.

Ước lượng tải theo giả định: 1.000 bookmark công khai × 1 lần/ngày ≈ 30.000 lượt kiểm tra logic/tháng cho một người; request thực tế tăng khi fallback GET, redirect hoặc retry. Chi phí còn phụ thuộc số người, cache, DB reads/writes, worker, băng thông và thời gian giữ lịch sử. Cần đo một bản thử trước khi chốt ngân sách; không dùng free tier làm cam kết chi phí dài hạn.

## 10. Các nguồn tham khảo chính

- [Android offline-first architecture](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Android receive shared data](https://developer.android.com/develop/ui/compose/sharing/receive)
- [Android periodic work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Apple background strategies](https://developer.apple.com/documentation/backgroundtasks/choosing-background-strategies-for-your-app)
- [HTTP Semantics RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html)
- [Drive error semantics](https://developers.google.com/workspace/drive/api/guides/handle-errors)
- [Firestore offline and conflict behavior](https://firebase.google.com/docs/firestore/manage-data/enable-offline)
- [Firebase scheduled functions](https://firebase.google.com/docs/functions/schedule-functions)
- [Supabase Kotlin](https://supabase.com/docs/reference/kotlin/introduction), [Swift](https://supabase.com/docs/reference/swift/installing), [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security)
- [OWASP SSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)
