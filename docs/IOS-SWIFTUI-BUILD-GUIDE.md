# LinkMap Galaxy — Bản bàn giao xây dựng iOS native bằng SwiftUI

**Cập nhật:** 01/11/2026. **Mục đích:** dùng trực tiếp làm đặc tả triển khai cho developer hoặc coding agent trên Mac. Đọc trọn file trước khi tạo project.

Tài liệu này mô tả ứng dụng iOS cần xây dựng; **không khẳng định repository đã có project iOS, IPA, provisioning hoặc kết quả build Xcode**. Tại thời điểm viết, đã có Android 0.7.1 và Web 0.8.1; chưa tạo iOS OAuth client hay đăng ký Bundle ID/App Group với Apple.

## Mục lục

1. [Sản phẩm và những quyết định đã chốt](#1-sản-phẩm-và-những-quyết-định-đã-chốt)
2. [Nguồn tham chiếu trong repository](#2-nguồn-tham-chiếu-trong-repository)
3. [Nền tảng, project và kiến trúc](#3-nền-tảng-project-và-kiến-trúc)
4. [Model dữ liệu và lưu trữ](#4-model-dữ-liệu-và-lưu-trữ)
5. [Nhận diện nội dung, URL và ID](#5-nhận-diện-nội-dung-url-và-id)
6. [Hợp đồng đồng bộ Android–Web–iOS](#6-hợp-đồng-đồng-bộ-androidwebios)
7. [Google login và Drive authorization](#7-google-login-và-drive-authorization)
8. [UI/UX SwiftUI native](#8-uiux-swiftui-native)
9. [Camera, Thiên hà và Data Center](#9-camera-thiên-hà-và-data-center)
10. [Kiểm tra link theo ngày](#10-kiểm-tra-link-theo-ngày)
11. [Paste, Share Extension và deep link](#11-paste-share-extension-và-deep-link)
12. [Backup, tài khoản và quyền riêng tư](#12-backup-tài-khoản-và-quyền-riêng-tư)
13. [Demo, assets và hiệu năng](#13-demo-assets-và-hiệu-năng)
14. [Khởi tạo, build và chạy trên Mac](#14-khởi-tạo-build-và-chạy-trên-mac)
15. [TestFlight và App Store](#15-testflight-và-app-store)
16. [Kế hoạch triển khai và tiêu chí nghiệm thu](#16-kế-hoạch-triển-khai-và-tiêu-chí-nghiệm-thu)
17. [Ma trận kiểm thử](#17-ma-trận-kiểm-thử)
18. [Prompt bàn giao cho coding agent](#18-prompt-bàn-giao-cho-coding-agent)
19. [Tài liệu chính thức](#19-tài-liệu-chính-thức)

## 1. Sản phẩm và những quyết định đã chốt

LinkMap là nơi lưu **liên kết và ghi chú cá nhân** dưới dạng một bản đồ trực quan. Dự án phi lợi nhuận; không dựng database nội dung người dùng trên server LinkMap. App dùng được hoàn toàn cục bộ, không bắt buộc đăng nhập. Khi được người dùng cho phép, nội dung đồng bộ trực tiếp qua Google Drive của chính họ.

Các yêu cầu bắt buộc của bản iOS mục tiêu:

- SwiftUI native; không dùng WKWebView để bọc website, không Flutter/React Native, không port UI bằng ảnh chụp.
- Một ô nhập: paste/gõ có URL → LINK; chỉ có văn bản → NOTE. Tự nhận diện nền tảng/icon. Đoạn văn chứa URL phải giữ nguyên phần văn bản.
- Chế độ **Thiên hà:** ngôi sao 5 cánh, icon nền tảng ở giữa, trạng thái link thể hiện bằng màu; sao cũ nhỏ dần; viền sáng chậm, khác pha.
- Chế độ **Data Center:** ô vuông tín hiệu nằm trên mặt rack; phòng, tủ, ô link và đường nối dùng chung camera khi kéo/zoom. Đưa cả hai chế độ vào bản mục tiêu; nếu chia milestone phải báo rõ phần DC còn thiếu.
- Các link cùng nền tảng nối thành một mạng mảnh, **mỗi nền tảng một màu**. Không gom lại thành cụm không gian. Danh mục là bộ lọc, không phải tọa độ vật lý.
- Thêm/sửa/xóa/khôi phục; tìm kiếm không dấu; bộ sưu tập; ghi chú nhiều dòng; thư viện dạng danh sách có trợ năng.
- Google login tùy chọn; đọc/ghi journal/2 tương thích Android/Web, đọc được journal/1.
- Kiểm tra link vào lần mở đầu ngày; không hứa chạy nền đúng giờ. NOTE không bị kiểm tra HTTP.
- Khoảng 100 mục demo trong app, tách khỏi dữ liệu thật và Drive. Có thể có chế độ xem 150 mục tương đương Web để test bố cục.
- Share Extension nhận link/text, nút paste theo thao tác người dùng, JSON import/export và trạng thái offline rõ ràng.

Không xây backend đồng bộ, Firebase database, CloudKit mirror hay hệ thống tài khoản LinkMap riêng ở bản đầu. Không đồng bộ camera/slot, không tự hợp nhất thư viện guest với tài khoản Google. Chưa E2EE; không tự đổi định dạng journal để thêm mã hóa chỉ trên iOS.

## 2. Nguồn tham chiếu trong repository

Repository: https://github.com/duconmang/linkmap-galaxy. Mốc nguồn đã đối chiếu: `9cf3795923f4145d6beb24c6076fa66e8325c9cb`.

| Phần cần đọc | File / thư mục |
|---|---|
| Model, URL, trạng thái, ngày | `core/src/main/kotlin/vn/linkmap/core/Models.kt` |
| Text → LINK/NOTE | `core/src/main/kotlin/vn/linkmap/core/IncomingContent.kt` |
| Merge và ID | `core/src/main/kotlin/vn/linkmap/core/Sync.kt` |
| Wire JSON Android | `app/src/main/java/vn/linkmap/data/SyncJson.kt` |
| Drive transport | `app/src/main/java/vn/linkmap/data/DriveJournal.kt`, `web/dist/google.js` |
| Transaction/outbox | `app/src/main/java/vn/linkmap/data/BookmarkStore.kt`, `web/dist/storage.js` |
| Validator và merge Web | `web/dist/protocol.js` |
| Thiên hà Android | `core/src/main/kotlin/vn/linkmap/core/GalaxyPresentation.kt`, `app/src/main/java/vn/linkmap/ui/GalaxyMap.kt` |
| Mạng nền tảng | `core/src/main/kotlin/vn/linkmap/core/PlatformNetwork.kt`, `app/src/main/java/vn/linkmap/ui/PlatformNetworkColors.kt` |
| DC đã sửa camera | `web/dist/map-model.js`, `web/dist/map-view.js`, `docs/WEB-0.8.1-DC.md` |
| Kiểm tra link | `app/src/main/java/vn/linkmap/data/LinkChecker.kt`, `HealthRules` và `HtmlSignals` trong `Models.kt` |
| Assets | `app/src/main/res/drawable-nodpi/nebula_cloud.png`, `web/dist/assets/` |
| Kiểm thử tương thích | `core/src/test/`, `web/tests/`, `app/src/androidTest/` |
| Google và bảo mật | `docs/GOOGLE-SETUP.md`, `docs/WEB-SECURITY.md` |

`docs/SYNC-PROTOCOL.md` còn phần lịch sử journal/1 ở đầu; **bản ghi mới dùng journal/2**. Khi tài liệu lịch sử mâu thuẫn với source ở mốc trên, đối chiếu encoder, validator, merge và tests; không tự phát minh protocol thứ ba.

UI tham chiếu trực tiếp: https://duconmang.github.io/linkmap-galaxy/?demo=150. Ảnh chụp tài khoản không nằm trong Git; không phụ thuộc các đường dẫn ảnh tạm trên máy Windows. Đây không phải yêu cầu tạo/đọc file Figma.

## 3. Nền tảng, project và kiến trúc

### 3.1 Baseline đề xuất

- iPhone và iPad, deployment target **iOS 17.0+**; đây là lựa chọn sản phẩm, không phải yêu cầu bắt buộc của Google SDK.
- Swift 6 language mode, strict concurrency; Xcode/SDK đang được Apple chấp nhận tại lúc archive. Ghi phiên bản thực tế vào README iOS và log CI, không đoán số Xcode trên máy.
- Native: SwiftUI, Foundation, CryptoKit, SQLite3, UniformTypeIdentifiers; UIKit chỉ cho các API hệ thống cần bridge như presenter, paste control, Share Extension.
- Google Sign-In qua Swift Package Manager. Tài liệu Google tại ngày đối chiếu đang nêu `9.0.0`; xác minh bản tương thích thực tế, pin dependency và commit `Package.resolved`, không tự dùng nhánh `main`.
- UI brand dùng named colors trong Asset Catalog; hệ thống dùng semantic colors, SF Symbols và Dynamic Type. Không cần font/CDN/icon proxy bên ngoài.

### 3.2 Định danh đề xuất, chưa đăng ký

| Thành phần | Giá trị đề xuất |
|---|---|
| App / scheme | `LinkMap` |
| Main bundle ID | `vn.linkmap.ios` |
| Share Extension bundle ID | `vn.linkmap.ios.share` |
| App Group | `group.vn.linkmap` |
| Google iOS OAuth client name | `LinkMap Galaxy iOS` |
| Google Cloud project phải dùng | `linkmap-galaxy` |

Bundle ID, App Group và Apple Team phải được kiểm tra quyền sử dụng trên Apple Developer account. iOS client ID cần tạo theo Bundle ID đã chốt; **không dùng Android client ID hoặc Web client ID làm iOS client ID**. Không tạo tài khoản/credential mới chỉ vì đang viết project; nếu chưa có cấu hình, local mode phải chạy được và hiển thị trạng thái Google chưa cấu hình.

### 3.3 Cấu trúc mong muốn

```text
ios/
  README.md
  LinkMap.xcodeproj/
  Config/
    Public.example.xcconfig
    Local.xcconfig                  # ignored; Team / config riêng của máy
    ExportOptions.example.plist
  Packages/LinkMapCore/
    Package.swift
    Sources/LinkMapCore/
      Models/ URL/ Sync/ Geometry/ Health/
    Tests/LinkMapCoreTests/
      Fixtures/
  LinkMap/
    App/                           # App entry, dependency graph, scene lifecycle
    Features/Map/ Library/ Editor/ Detail/ Settings/ Accounts/
    Infrastructure/Storage/ Google/ Network/ ImportExport/ ShareInbox/
    Resources/Assets.xcassets/ Localizable.xcstrings
    Info.plist
    PrivacyInfo.xcprivacy
  LinkMapShare/
    ShareViewController.swift      # hosting SwiftUI composer
    Info.plist
    LinkMapShare.entitlements
  LinkMapTests/
  LinkMapUITests/
```

`LinkMapCore` không phụ thuộc SwiftUI, UIKit hoặc Google SDK; DTO và geometry phải test được bằng `swift test`. App target chứa adapters và UI. Share target không phụ thuộc Google session/token của app.

### 3.4 Trách nhiệm và concurrency

- `@MainActor AppModel`: state hiển thị, account selection, sheet/navigation; không chứa SQL hay vòng lặp HTTP.
- `actor LibraryStore`: một writer tuần tự cho mỗi namespace; quản lý SQLite, migration, clock, outbox và raw sync state. Không đưa `sqlite3*` ra ngoài actor.
- `actor SyncCoordinator`: single-flight, không chạy hai sync song song cho cùng thư viện; giữ account epoch để hủy kết quả khi đổi account.
- `@MainActor GoogleSession`: SDK/presenting view controller; lấy token còn hạn qua SDK rồi chuyển một snapshot cần thiết cho transport. Không chuyển nguyên đối tượng SDK không Sendable qua actor tùy tiện.
- `actor LinkCheckCoordinator`: hàng đợi, per-host limiter và ngày hoàn tất.
- `MapGeometryEngine`: tính layout/edges từ snapshot bất biến; cache theo data revision. Không chạy MST trên main actor mỗi frame.
- Phân biệt thao tác bị hủy, offline, hết hạn phiên, từ chối quyền và journal hỏng trong UI; không gom tất cả thành “link chết”.

## 4. Model dữ liệu và lưu trữ

### 4.1 Wire DTO chuẩn

Các field sau là tên JSON chính xác, phân biệt chữ hoa/thường. Dùng `Codable`, enum raw values giữ nguyên, thời gian và counter là `Int64`.

| Field | Kiểu / quy tắc |
|---|---|
| `id` | LINK: `url-` + 64 hex SHA-256 chữ thường; NOTE: `note-` + UUID chữ thường |
| `kind` | `LINK` hoặc `NOTE`; legacy thiếu field → `LINK` |
| `url` | LINK: URL đã normalize; NOTE: chuỗi rỗng **trong journal** |
| `title` | String, tối đa 200 UTF-16 code units |
| `note` | String, tối đa 64.000 UTF-16 code units; NOTE phải có nội dung không trắng |
| `category` | String, tối đa 50 UTF-16 code units; mặc định `Cá nhân` khi nhập legacy thiếu danh mục |
| `createdAt` | Unix epoch **milliseconds**, không phải seconds |
| `counter` | Số nguyên 0…9.007.199.254.740.000 |
| `device` | String dài 1…128 UTF-16 code units; iOS tự sinh installation UUID chữ thường, lưu bền |
| `deleted` | Bool, dấu xóa nội dung |
| `health` | `UNCHECKED`, `REACHABLE`, `UNAVAILABLE`, `UNCERTAIN` |
| `reason` | String, tối đa 500 UTF-16 code units |
| `checkedAt`, `notFoundAt` | Unix milliseconds, >= 0; 0 nghĩa chưa có |
| `httpCode` | Int hoặc JSON `null`; giữ lại khi encode kể cả giá trị null |
| `healthDevice` | String <= 128 UTF-16 code units; NOTE ghi `""` |

`provider`, `slot`, camera, pin, recent/openedAt, preview cache, ignored, check-day, sync status và demo flag **không phải field nội dung journal/2 hiện tại**. Không thêm trường rồi mặc định các client khác sẽ đồng bộ chúng. Provider luôn derive từ URL.

Swift `String.count` đếm grapheme clusters nên không dùng để kiểm tra các giới hạn trên; dùng `text.utf16.count`. Khi cắt title/category theo UTF-16 phải không tạo surrogate lẻ. Các timestamp gửi sang JavaScript cũng phải nằm trong miền integer biểu diễn chính xác; không emit NaN, Infinity, số âm hoặc Int64 cực đại.

### 4.2 Database cục bộ

Chọn SQLite3 hệ thống để có transaction rõ ràng cho record + outbox. Không cần thêm một cloud SDK/database. Schema gợi ý dưới đây là thiết kế iOS, không phải yêu cầu mở trực tiếp file SQLite Android:

```sql
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE sync_records (
  id TEXT PRIMARY KEY,
  kind TEXT NOT NULL,
  url TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  deleted INTEGER NOT NULL,
  payload BLOB NOT NULL
);
CREATE TABLE spatial_slots (
  id TEXT PRIMARY KEY REFERENCES sync_records(id),
  slot INTEGER NOT NULL UNIQUE
);
CREATE TABLE local_reading (
  id TEXT PRIMARY KEY REFERENCES sync_records(id),
  pinned INTEGER NOT NULL DEFAULT 0,
  opened_at INTEGER NOT NULL DEFAULT 0,
  ignored INTEGER NOT NULL DEFAULT 0,
  preview_path TEXT
);
CREATE TABLE outbox (
  seq INTEGER PRIMARY KEY AUTOINCREMENT,
  record_id TEXT NOT NULL,
  payload BLOB NOT NULL
);
CREATE TABLE seen_files (file_id TEXT PRIMARY KEY);
CREATE TABLE conflicts (id TEXT PRIMARY KEY, payload BLOB NOT NULL);
CREATE TABLE imported_drafts (draft_id TEXT PRIMARY KEY, imported_at INTEGER NOT NULL);
CREATE INDEX active_created ON sync_records(deleted, created_at, id);
```

- `payload` là snapshot wire đầy đủ, không phải patch. Các cột index phải cập nhật cùng payload trong transaction.
- `meta`: clock, deviceId, nextSlot, migration version, lastCompletedCheckDay. Outbox seq không được tái dùng nếu còn snapshot pending.
- Không đặt UNIQUE cho URL trong bảng raw: cần giữ cả record xung đột, tombstone và những ID khác nhau có cùng URL.
- `BEGIN IMMEDIATE` → cập nhật record/clock/slot → INSERT snapshot outbox → COMMIT. Rollback toàn bộ khi lỗi; UI chỉ báo đã lưu sau commit.
- Merge một remote batch và thêm `seen_files` trong cùng transaction. Không mark file seen trước khi validate/commit thành công.
- SQL binding có tham số, không nối text/note/URL vào câu SQL. Migration qua `PRAGMA user_version`, có rollback; không xóa DB để “fix migration”.

Namespace: `guest` và `google:<verified-sub>`. Mỗi namespace có DB riêng dưới Application Support; tên thư mục account dùng SHA-256 của namespace thay vì email/raw subject. Tạo đường dẫn từ identifier đã kiểm soát; không ghép path do journal cung cấp. Logout đóng DB đang dùng, hủy tasks, xóa state hiển thị rồi mở guest. Cache account cũ có thể giữ riêng; UI phải nói rõ logout không xóa dữ liệu trên máy.

Đặt iOS Data Protection phù hợp cho DB/WAL/SHM, ví dụ `completeUntilFirstUserAuthentication`; không coi đây là E2EE. Loại dữ liệu thư viện và credentials khỏi backup iCloud mặc định nếu thiết kế chọn chỉ backup thủ công/Drive; xử lý cả sidecar WAL/SHM và Shared Inbox. Preferences không nhạy cảm có thể để UserDefaults.

## 5. Nhận diện nội dung, URL và ID

### 5.1 Luồng nhập

1. Trim khoảng trắng ngoài; không làm mất xuống dòng bên trong; reject rỗng hoặc >64.000 UTF-16 units trước khi lưu.
2. Thử toàn bộ input như một URL, bổ sung `https://` nếu là domain thiếu scheme.
3. Nếu là đoạn văn, nhận diện URL HTTP(S) và domain thiếu scheme; loại email, số thập phân và domain nằm trong query của URL đã nhận diện.
4. Dedupe URL theo chuỗi normalize, giữ thứ tự xuất hiện.
5. 0 URL → NOTE. 1 URL → LINK. Nhiều URL → hiện chooser, chưa được tự bỏ các link còn lại khỏi văn bản gốc.
6. Lưu mixed text trong `note` của LINK. Không dùng title ngắn để thay toàn bộ note.
7. Sửa NOTE có thêm URL vẫn giữ kind NOTE; chuyển loại phải là thao tác riêng có xác nhận. Sửa LINK thành text không URL phải báo lỗi, không tự đổi ID/kind.
8. Kiểm tra trùng URL; thông báo “Đã có” và cho xem mục cũ. Không silently ghi đè note/title của record khác.

`NSDataDetector` dùng được để tìm candidate nhưng không được là toàn bộ hợp đồng normalize; detector của OS có thể nhận diện khác regex Android/Web. Chốt bằng fixtures chung.

### 5.2 Normalize tối thiểu, giữ tài nguyên nguyên vẹn

- Chỉ lưu `http`/`https`, không có user/password trong authority; host hợp lệ; port 1…65535 nếu có; input URL không chứa whitespace/control characters, tối đa 8.192 UTF-16 units.
- Lowercase **scheme**, giữ nguyên phần còn lại của chuỗi input hợp lệ. Không tự thêm `/`, xóa fragment, sort query, bỏ resourcekey, decode/re-encode `%2F`, đổi host case hoặc Unicode-normalize nội dung trước khi hash.
- `URL`/`URLComponents` chỉ để validation/phân tích; **không lấy `absoluteString` làm chuỗi lưu/hash sau khi Foundation đã canonicalize**. Reject URL mà parser tự sửa một lỗi cú pháp thay vì coi nó tương đương input.
- Lowercase host riêng khi nhận diện provider; match đúng domain hoặc hậu tố `.` + domain, tránh `github.com.attacker.example`.
- Nếu edge case khác giữa source Android/Web, thêm fixture và xác minh cả hai client trước; không tự đổi ID của mục đã đồng bộ.

### 5.3 ID

```swift
import CryptoKit
import Foundation

func newLinkID(normalizedURL: String) -> String {
    let hash = SHA256.hash(data: Data(normalizedURL.utf8))
    return "url-" + hash.map { String(format: "%02x", $0) }.joined()
}

func newNoteID() -> String {
    "note-" + UUID().uuidString.lowercased()
}
```

Đây là helper độc lập; không thay cho validator/store. ID link được tính **khi tạo mới**, không tính lại khi sửa URL của record đã có. NOTE không hash body: hai ghi chú giống nhau vẫn là hai mục độc lập.

Golden vectors đã tính từ SHA-256 UTF-8 của source hiện tại:

| URL normalize | ID |
|---|---|
| `https://example.com` | `url-100680ad546ce6a577f42f52df33b4cfdca756859e664b8d7de329b150d09ce9` |
| `https://example.com/` | `url-0f115db062b7c0dd030b16878c99dea5c354b49dc37b38eb8846179c7783e9d7` |
| `https://github.com/android/compose-samples` | `url-8454038479af2950bc30aba01a96bc84da1fbd601e0ac1cc22ee295a84ca59a6` |
| `https://drive.google.com/file/d/demo/view?resourcekey=demo#view` | `url-e05404f350bcd1f76f04ed37c0f485178a3272505fc68fcfbd4cc9b174623863` |

## 6. Hợp đồng đồng bộ Android–Web–iOS

### 6.1 Wire example

```json
{
  "protocol": "linkmap.journal/2",
  "records": [
    {
      "id": "url-8454038479af2950bc30aba01a96bc84da1fbd601e0ac1cc22ee295a84ca59a6",
      "kind": "LINK",
      "url": "https://github.com/android/compose-samples",
      "title": "Compose samples",
      "note": "Mẫu tham khảo Android",
      "category": "Dev & GitHub",
      "createdAt": 1793494800000,
      "counter": 1,
      "device": "ios-installation-001",
      "deleted": false,
      "health": "UNCHECKED",
      "reason": "Chưa kiểm tra",
      "checkedAt": 0,
      "httpCode": null,
      "notFoundAt": 0,
      "healthDevice": ""
    },
    {
      "id": "note-00000000-0000-0000-0000-000000000001",
      "kind": "NOTE",
      "url": "",
      "title": "Ý tưởng",
      "note": "Dòng thứ nhất\nDòng thứ hai",
      "category": "Cá nhân",
      "createdAt": 1793494801000,
      "counter": 2,
      "device": "ios-installation-001",
      "deleted": false,
      "health": "UNCHECKED",
      "reason": "",
      "checkedAt": 0,
      "httpCode": null,
      "notFoundAt": 0,
      "healthDevice": ""
    }
  ]
}
```

JSON key order không có ý nghĩa. Swift synthesized Codable thường bỏ optional nil; wire encoder phải emit `httpCode: null` để giữ hợp đồng hiện có. Không encode Swift `Date` thành ISO string hoặc epoch seconds trong wire DTO.

### 6.2 Merge nội dung

Revision là `(counter, device)`, so sánh counter trước rồi chuỗi device theo thứ tự ordinal UTF-16, không localized/natural sort. Device mới tự sinh ASCII UUID để tránh khác biệt collation giữa Swift/Kotlin/JS.

```text
receive(remote): localClock = max(localClock, remote.counter)
local edit/delete/restore: localClock += 1
new revision = (localClock, thisInstallationDeviceID)
contentWinner = record có revision lớn hơn; nếu bằng nhau giữ bản hiện có
```

Giữ `createdAt` khi sửa/xóa/restore. Đồng hồ thực không quyết định thắng/thua nội dung. Hai snapshot cùng ID/revision nhưng nội dung khác là dữ liệu bất thường: giữ để chẩn đoán/conflict, không tuyên bố merge hoàn hảo trường hợp này.

### 6.3 Merge health độc lập

1. Chọn contentWinner theo revision.
2. Nếu winner là NOTE, trả winner và `healthDevice=""`; không lấy health từ link.
3. Với LINK, chỉ xét observation ở các record có URL **trùng chính xác URL của winner**.
4. Chọn observation lớn nhất theo `(checkedAt, healthDevice, content revision)`.
5. Chỉ copy `health`, `reason`, `checkedAt`, `httpCode`, `notFoundAt`, `healthDevice`; không copy title/note/category/deleted từ observation đó.

Local checker phải ghi kết quả dựa trên state hiện tại trong transaction, kiểm tra ID vẫn tồn tại, chưa deleted và URL chưa đổi. Health-only observation không tăng content revision; enqueue snapshot có cùng content revision và healthDevice hiện tại. Nếu metadata checker muốn đổi title, đó phải là content edit riêng, không đè title người dùng đã chỉnh.

### 6.4 Xóa, khôi phục, trùng URL

Xóa tạo `deleted=true` với revision mới rồi enqueue; không xóa raw sync record hoặc slot. Replay dữ liệu cũ không làm mục sống lại. Restore có chủ ý tạo revision mới cao hơn; không đơn giản bỏ một cờ UI local.

Giữ các ID raw trùng URL sau chỉnh sửa đồng thời. Có thể chỉ hiển thị một primary theo `(createdAt,id)` ổn định và đưa phần còn lại vào màn xử lý xung đột. Không xóa bản ghi khác, đổi ID theo URL mới hoặc loại record trước khi merge. Không xóa/compact các file journal cũ ở bản iOS đầu.

### 6.5 Drive transport

Đọc từ appDataFolder, không tạo file thư viện JSON dùng chung bị overwrite:

```http
GET https://www.googleapis.com/drive/v3/files
  ?spaces=appDataFolder
  &q=trashed = false and (appProperties has { key='linkmapProtocol' and value='1' } or appProperties has { key='linkmapProtocol' and value='2' })
  &fields=nextPageToken,files(id,size)
  &pageSize=1000
```

Đây là dạng dễ đọc; URL thực phải encode query đúng một lần bằng URLComponents/URLQueryItem. Theo `nextPageToken`; tối đa 100 trang mỗi lượt. File chưa seen đọc qua `GET /drive/v3/files/{validated-file-id}?alt=media`.

Ghi batch **bất biến** qua multipart/related, không dùng multipart/form-data:

```http
POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id
Content-Type: multipart/related; boundary=<random-boundary>
```

```json
{
  "name": "linkmap-<random-uuid>.json",
  "mimeType": "application/json",
  "parents": ["appDataFolder"],
  "appProperties": {"linkmapProtocol": "2"}
}
```

Part đầu là metadata JSON ở trên; part sau là journal/2. CRLF/boundary theo multipart spec; tên file không chứa title/note/email của người dùng.

### 6.6 Sync transaction và giới hạn

```text
1. Chụp account namespace + auth epoch; xác minh token thuộc Google sub này.
2. Pull các file chưa seen; stream với bound → validate cả batch → merge + seen trong transaction.
3. Chụp các seq outbox cần gửi (không giữ SQL transaction mở trong HTTP).
4. POST immutable batch; chỉ sau response thành công có file ID mới tiến hành ACK.
5. Transaction: merge snapshot đã gửi/seen upload ID và ACK đúng danh sách seq vừa gửi.
6. Chạy tiếp bounded lượt nếu còn pending; kết thúc có trạng thái rõ ràng.
```

- Sender: tối đa 100 records/batch và khoảng 3 MiB bytes UTF-8 phần payload, tính thêm overhead để không vượt reader bound. Không chọn batch rỗng rồi lặp vô hạn.
- Reader: tối đa 5 MiB **bytes thực nhận**/file trước JSON.parse/decode, tối đa 1.000 records. Content-Length chỉ là kiểm tra sớm; không tin header là đủ.
- Đọc cả `linkmap.journal/1` và `/2`; chỉ ghi `/2`. Protocol không biết, batch lỗi hoặc một record không hợp lệ: không mark seen và không apply nửa batch.
- Snapshot gửi có thể cũ hơn local state mới: merge không rollback local edit; ACK chỉ xóa đúng seq đã gửi, không `DELETE outbox` toàn bảng.
- Crash sau upload trước ACK có thể upload lặp; merge phải idempotent. File name/Drive modifiedTime không phải revision.
- Remote merge không tự tạo outbox echo. Sau foreground, mạng trở lại, thao tác local và nút Sync có thể schedule sync debounce; không polling mỗi giây.
- 401: refresh một lần qua SDK rồi yêu cầu reconnect nếu vẫn thất bại; 403 không mặc định là session hết hạn; 429/5xx backoff có jitter, không bỏ outbox. Offline không chặn việc lưu local.
- Logout/account switch: tăng epoch, cancel pending requests; response cũ không được commit vào account mới hoặc reset busy của phiên mới.
- `seen_files` là tối ưu, không được coi mất file cũ là yêu cầu xóa toàn thư viện. Không tự dọn Drive khi logout, không gọi DELETE trên journal hiện có.

## 7. Google login và Drive authorization

### 7.1 Cấu hình đúng project

Google Cloud project hiện có: `linkmap-galaxy`. Public Web Client ID đang dùng cho Android/Web:

```text
881163930052-rqgjt01f96i1h1veof9ilcp8abtv215p.apps.googleusercontent.com
```

Tạo **iOS OAuth client riêng** với bundle ID đã xác nhận, trong cùng project. Không đổi project hoặc scope của Android/Web để làm iOS chạy được. Không có Client Secret trong app, không cần backend exchange chỉ để gọi Drive trực tiếp.

Cấu hình app Info.plist bằng build settings công khai:

```xml
<key>GIDClientID</key>
<string>$(GOOGLE_IOS_CLIENT_ID)</string>
<key>CFBundleURLTypes</key>
<array><dict>
  <key>CFBundleURLSchemes</key>
  <array><string>$(GOOGLE_REVERSED_CLIENT_ID)</string></array>
</dict></array>
```

`GOOGLE_IOS_CLIENT_ID` và reversed URL scheme lấy từ **iOS client vừa tạo**, không điền giá trị Web ở trên vào hai field này. Ví dụ reversed scheme có dạng `com.googleusercontent.apps.<ios-client-prefix>`. `GIDServerClientID` chỉ cấu hình khi có nhu cầu backend/ID-token audience được thiết kế rõ; mô hình direct Drive hiện tại không bắt buộc thêm nó.

XML trên là các key bổ sung, không phải toàn bộ Info.plist để thay file Xcode đang sinh. Đảm bảo build settings được resolve thành giá trị thật trong app bundle.

SPM products: `GoogleSignIn` và `GoogleSignInSwift`. Dùng nút Google theo SDK. Trong `.onOpenURL`, chuyển OAuth callback cho `GIDSignIn.sharedInstance.handle(url)` trước; không log URL callback. Presenter lấy từ window scene foreground phù hợp, không force unwrap một rootViewController toàn cục.

### 7.2 Luồng người dùng

1. App mở: dùng guest ngay, thử restore phiên SDK ở nền của luồng khởi động; UI không bị kẹt chờ Google.
2. Người dùng bấm đăng nhập: gọi Google SDK với presenter. Sau thành công, xác minh userinfo `sub`/email verified, mở namespace tương ứng. Email chỉ để hiển thị, không làm khóa dữ liệu.
3. Khi người dùng bật Drive: kiểm tra `grantedScopes`, xin thêm đúng `https://www.googleapis.com/auth/drive.appdata` nếu chưa có. Không xin `drive`, `drive.readonly` hoặc quyền đọc toàn bộ thư viện.
4. Trước Google API: dùng `refreshTokensIfNeeded`; bearer access token chỉ tới Google API endpoint cố định, không gửi tới URL bookmark, preview hoặc analytics.
5. Khi mở namespace Google đã có quyền Drive, sync incremental; guest chỉ chuyển sang Google qua một thao tác “Đưa thư viện trên máy vào tài khoản” có preview số lượng.
6. Logout gọi SDK `signOut`, xóa phiên in-memory và về guest; không tự restore lại ngay sau thao tác logout. “Ngắt liên kết Google” dùng disconnect/revoke theo yêu cầu riêng. Không tuyên bố logout đã xóa mọi cache trên máy.

SDK có thể giữ credential state trong Keychain để restore. App không tự chép token vào UserDefaults, SQLite, Shared Inbox, JSON backup hoặc log; không khẳng định “không có token lưu bền” nếu SDK đang dùng Keychain. Không chia Keychain credentials cho Share Extension trong thiết kế đầu.

Theo [hướng dẫn Google iOS](https://developers.google.com/identity/sign-in/ios/start-integrating) và [quyền Google API](https://developers.google.com/identity/sign-in/ios/api-access). OAuth hiện **Testing**; khi test ba nền tảng cần account trong Test users. Quyền App Store/TestFlight và trạng thái Google OAuth là hai quy trình khác nhau.

## 8. UI/UX SwiftUI native

### 8.1 Điều hướng và adaptive layout

- iPhone: `TabView` gồm **Bản đồ / Thư viện / Cài đặt**, bên trong dùng `NavigationStack`. Nút `+` dễ chạm trên map và toolbar của thư viện.
- iPad: dùng `NavigationSplitView` hoặc sidebar thích ứng; map không bị kéo giãn bằng các tọa độ thiết kế điện thoại. Hỗ trợ rotate và multitasking, không khóa orientation để tránh giải quyết layout.
- Nền thiên hà phủ full-screen, cả vùng dưới toolbar/tab bar với material thích hợp. Safe area áp dụng cho controls, không tạo một dải nền opaque vô tình cắt ảnh.
- Không sao chép status bar, Dynamic Island, home indicator hoặc device frame vào assets. Dùng font hệ thống SF, Dynamic Type và các biểu tượng điều khiển SF Symbols; logo nền tảng là assets riêng.
- Trên iOS mới có thể dùng glass API với availability guard; baseline iOS 17 dùng materials hệ thống. Không gọi API chỉ có ở OS mới mà thiếu fallback.
- Toàn bộ controls có accessibilityLabel/Hint, tap target tối thiểu khoảng 44 pt; tên trạng thái phải có chữ/icon, không dựa riêng vào đỏ/xanh.

### 8.2 Màn bản đồ

Header thể hiện LinkMap, tìm kiếm, account/sync status; hàng chip bộ sưu tập nằm dưới. Nút chuông nếu dùng phải mở danh sách **link cần chú ý**, không phải icon trang trí chết. Nút hồ sơ mở đúng màn account.

Canvas là trung tâm; controls zoom `+`, `−`, Fit và Reset ở góc dưới bên phải, phía trên tab bar. Giữ ổn định vị trí controls khi mở bàn phím/sheet. Bộ chọn **Thiên hà / Data Center** dùng segmented picker hoặc menu rõ tên trên điện thoại hẹp; không thay bằng 3D giả.

Chạm node → selection card có title, loại nội dung, trạng thái, **Mở / Chi tiết**. NOTE mở reader; LINK chỉ mở URL sau thao tác rõ ràng. Thư viện có thao tác **Xem trên bản đồ** để focus một node. Empty state có Thêm đầu tiên và Xem demo. Hero giới thiệu được thu gọn; không che hết map khi xoay ngang.

Số mục/đường nối/nền tảng là số thật của tập đang hiển thị; nếu tắt edges thì số đường đang vẽ bằng 0. Demo có nhãn rõ trạng thái minh họa. Tìm kiếm/lọc giữ tọa độ, có thể làm mờ node không khớp; không cấp lại slot mỗi lần gõ.

### 8.3 Thư viện và chi tiết

- `List`/`LazyVStack` có LINK/NOTE, title, URL hoặc preview note, danh mục và status; search bỏ dấu tiếng Việt, `đ` → `d`, không đổi dữ liệu gốc.
- Filter Tất cả / Link / Note / Cần chú ý, danh mục, nền tảng; có Xóa bộ lọc và trạng thái không có kết quả.
- Detail: note dùng text selection, giữ xuống dòng, `Text(verbatim:)` để không thực thi/diễn giải HTML. Link có URL đầy đủ, thời điểm kiểm tra, reason và nút mở/copy.
- Sửa, ghim, bỏ ghim, xóa có xác nhận, xem Đã xóa/restore, và lịch sử mở là thao tác thật. Pin/history/ignored hiện là local-only, UI không quảng cáo chúng đã sync nếu protocol chưa hỗ trợ.
- Preview metadata là phụ trợ, tải có giới hạn; không overwrite title/note người dùng đã đặt. Không fetch favicon bằng dịch vụ proxy bên thứ ba cho mọi URL.

### 8.4 Cosmic editor

SwiftUI `.sheet`/presentation phù hợp iOS, có dismiss, keyboard avoidance và interactive dismissal an toàn nếu đang có nội dung chưa lưu. Trên iPad có thể dùng form sheet. Visual brand: thẻ bo mềm, viền cyan nhẹ, badge link/note, material tối; không dựng lại blur bằng hàng chục shadow.

Thứ tự nội dung:

1. Tiêu đề **Thêm vào LinkMap** và nút đóng.
2. TextEditor nhập link/note nhiều dòng, placeholder, nút **DÁN** theo cơ chế paste hệ thống.
3. Số lượng UTF-16 units / 64.000; chip **Link · GitHub** hoặc **Ghi chú**; chooser nếu nhiều URL.
4. Chip danh mục: Thiên hà chung, Design, Dev & GitHub, Cá nhân, nhóm đã có; chọn Nhóm khác khi cần.
5. Khi sửa có title và note đi kèm link; khi thêm mới không bắt nhập form dài.
6. **Hủy / Thêm vào bản đồ** hoặc **Lưu ghi chú**. Disable khi đang commit, không mất input khi lỗi; chỉ dismiss khi local save thành công.

Paste chỉ điền input để người dùng xem trước, không tự mở link hay đồng ý quyền Google. Bấm Save offline vẫn lưu local và hiển thị pending sync; không chờ Drive để đóng editor.

### 8.5 Cài đặt

Các nhóm: Google/account; sync status và Sync ngay; chuyển thư viện guest có preview; bản đồ/motion; JSON backup; check links; local storage/privacy; hướng dẫn xóa dữ liệu; phiên bản app. Avatar có thể dùng initials, không cần tải ảnh tài khoản để app hoạt động.

Không dùng chữ “Đã đồng bộ” chỉ vì có token. Phân biệt `localOnly`, `signedIn`, `drivePermissionMissing`, `syncing`, `syncedAt`, `pendingCount`, `offline`, `reconnectRequired`, `error`. Không hiển thị mật khẩu/token hoặc raw payload nhật ký trong UI lỗi.

## 9. Camera, Thiên hà và Data Center

### 9.1 Một camera cho toàn scene

Tách `MapCamera` (scale, translation, viewport) khỏi layout world. Công thức áp dụng cho background hình học, node, edges và hit-test:

```text
screen = world * zoom + pan
world  = (screen - pan) / zoom

khi zoom từ z sang z' quanh điểm neo a trong viewport:
pan' = a - (a - pan) * z' / z
```

`pan` và anchor dùng cùng coordinate space của canvas, không trộn tọa độ toàn màn hình với safe-area-local. Snapshot camera ở lúc bắt đầu gesture; magnification tính từ snapshot, không nhân dồn vào scale đã cập nhật mỗi event. Pan và pinch phối hợp không nhảy tâm. Clamp scale (gợi ý 0.1…5), mỗi mode giữ camera riêng.

Fit tính từ world bounds + inset dành cho controls/hero. Resize/rotate giữ world point tại tâm viewport; không tự reset layout. Tap tìm node gần nhất trong hit radius, đổi screen → world đúng; không lấy node đầu tiên trong mảng khi nhiều vùng chạm chồng nhau ở zoom nhỏ.

### 9.2 Thiên hà

Canvas SwiftUI kết hợp `TimelineView` cho animation; sao là Path 10 đỉnh xen kẽ outer/inner radius, tạo **5 cánh**, không dùng sparkle 4 cánh thay thế. Icon nền tảng ở giữa. Ngôi sao NOTE có màu riêng và không mang badge lỗi HTTP.

Slot cấp một lần trong local transaction và giữ qua filter/sort/edit/restore. Hình elip tập trung theo Android:

```text
slot = 0 → (0,0)
angle = slot * 2.399963229728653
radius = sqrt(slot + 1) * 30 + 15
x = cos(angle) * radius
y = sin(angle) * radius * 1.28
```

Web dùng khoảng cách/aspect khác để phù hợp desktop; tọa độ không nằm trong protocol, nên không cần iOS có cùng pixel với Web. Giữ ưu tiên nhìn được cả map trong một vùng trung tâm trên điện thoại.

```text
days = max(0, nowMilliseconds - createdAt) / 86_400_000
ageScale = 0.34 + 0.66 * exp(-days / 120)
```

Áp dụng vào kích thước hiển thị, có sàn tap target riêng. Kích thước không đổi slot, ID, createdAt hoặc health. Không shrink theo “lần mở gần nhất”.

Viền sáng chậm 8…14 giây, pha ổn định theo ID. Dùng hash deterministic (ví dụ port FNV-1a từ Web), **không dùng Swift `Hasher`/`hashValue`** vì seed đổi giữa các lần chạy. Không cần Android/Web/iOS nhấp nháy đồng pha. Halo có lõi rõ nhưng không phủ mờ icon. Tắt animation khi scene không active, đang ở tab khác, Reduce Motion bật hoặc người dùng tắt lấp lánh.

### 9.3 Mạng nền tảng

Nhóm key theo known provider hoặc hostname lowercase đã bỏ `www.`; NOTE không tham gia mạng. Dùng Prim/MST để n node cùng nền tảng có n−1 cạnh; sort ID ổn định trước khi tính. Không vẽ full mesh. Geometry có thể khác mỗi thiết bị nhưng tất cả link trong cùng nhóm phải được kết nối.

| Nền tảng | Màu đường nối |
|---|---|
| GitHub | `#51D9FF` |
| Facebook | `#648BFF` |
| TikTok | `#FF78B7` |
| Google Drive | `#FFCC66` |
| OneDrive | `#BB92FF` |
| Google | `#4DE0B3` |

Hostname khác dùng hue từ deterministic hash. Màu mạng tách biệt màu trạng thái: reachable vẫn xanh dù đường GitHub cyan. Chọn node làm sáng mạng nền tảng đó và làm dịu mạng khác, không đổi tất cả cạnh sang cùng một màu.

### 9.4 Data Center

Port ý tưởng hình học từ Web 0.8.1, không đặt ảnh có tủ rack cố định dưới một canvas có camera khác. Vẽ sàn, tường, ánh sáng viền, pad vị trí tủ, mặt bên/nóc/tủ, khay server và ô link trong **cùng world space**. Nền ambience bên ngoài có thể là gradient viewport; không chứa đối tượng cần ăn khớp với tủ.

Constants tham chiếu hiện có:

```text
rackCapacity = 30; rackWidth = 230; rackHeight = 450
horizontalGap = 70; serviceAisle = 190; fixedColumns = 5
rackIndex = slot / 30; localSlot = slot % 30
rackX = (rackIndex % 5) * 300
rackY = floor(rackIndex / 5) * 640
nodeX = rackX + 49 + (localSlot % 3) * 66
nodeY = rackY + 80 + floor(localSlot / 3) * 35
```

Không tính số cột từ số lượng hiện tại hoặc width màn hình rồi di chuyển tủ cũ khi thêm link/rotate. Fit theo room bounds, đổi camera khi cần. Mạng nền tảng vẫn có đầy đủ cạnh, cong nhẹ với lane riêng để không chồng hết lên một hàng. Room/hit-test/edge endpoints phải dùng cùng phép biến đổi ở §9.1.

Trên iPhone nhỏ, toàn cảnh DC có thể nhỏ: cho pinch/focus rõ ràng, giữ thư viện làm đường truy cập thay thế. Không phóng 150 ô đến mức chỉ thấy một góc ngay khi vào scene. Không tự phân loại mỗi rack thành một nền tảng: slot hỗn hợp giữ nguyên.

### 9.5 Màu trạng thái và trợ năng

Gợi ý Asset Catalog: reachable `#25D6A0`, unavailable `#FB648A`, uncertain `#F3C458`, unchecked `#8393AF`, note `#EFBA6C`. Đây là brand/status tokens; các label/background hệ thống vẫn dùng semantic colors thích hợp.

Canvas không tự tạo accessibility tree cho từng node. Cần accessible selection overlay và thư viện List có đầy đủ thao tác; không đọc 10.000 node qua VoiceOver mỗi frame. Test Dynamic Type lớn, high contrast, Reduce Motion, color differentiation, VoiceOver và iPad keyboard/pointer.

## 10. Kiểm tra link theo ngày

### 10.1 Chính sách

- Ngày tính theo Gregorian date tại timezone hiện tại của thiết bị, lưu dạng `yyyy-MM-dd`; không dùng locale Buddhist calendar hoặc lấy UTC day làm ngày người dùng.
- Khi app chuyển `.active`, nếu lastCompletedCheckDay khác today, khởi động **một** lượt check; bỏ mục đã có checkedAt thuộc today. Dùng single-flight để scene active lặp không nhân số lượt.
- Lưu kết quả từng mục ngay; chỉ đánh dấu hoàn tất ngày khi lượt tương ứng không còn pending. Bị suspend/kill/offline thì lần mở sau tiếp tục phần còn thiếu.
- Link mới/sửa URL về UNCHECKED và có hàng đợi riêng dù lượt ngày đã xong. NOTE/deleted không vào hàng đợi. Có Check lại thủ công.
- Giới hạn 4 request đồng thời toàn app, 1 request/host; nếu iOS và Android cùng check thì health merge chọn observation theo wire rule, không ghi đè nội dung.
- Không dùng background timer/BGTaskScheduler để hứa “đúng 00:00 mỗi ngày”. Bản đầu ưu tiên foreground, dừng/persist khi background. [Tài liệu Apple về background strategies](https://developer.apple.com/documentation/backgroundtasks/choosing-background-strategies-for-your-app).

### 10.2 Transport và phân loại

iOS native dùng URLSession, không bị CORS như Web. Tách session checker khỏi Google session; dùng cấu hình ephemeral, không lưu/chia sẻ cookie, không gắn token/cookie Google vào request bookmark. Giữ TLS verification mặc định; không thêm `NSAllowsArbitraryLoads` để vượt ATS.

Mục tiêu tương đương Android: HTTPS công khai; GET streaming, tối đa 16 KiB HTML để xem title/login signal; không download toàn file/video, không chạy JavaScript. Khoảng 5 giây cho kết nối/đọc, 8 giây một call, deadline tổng khoảng 18 giây, tối đa 5 redirect; tắt redirect tự động để kiểm tra target từng hop. Phân biệt network error với task cancellation.

| Kết quả | Health |
|---|---|
| 2xx, không có login/error signal | REACHABLE; chỉ chứng minh server phản hồi |
| 410 | UNAVAILABLE |
| 404 ở Facebook/TikTok/GitHub/Drive/OneDrive | UNCERTAIN vì có thể bị ẩn quyền |
| 404 ở nguồn khác, lặp lại sau >=6 giờ | UNAVAILABLE, giữ notFoundAt của lần đầu |
| 404 lần đầu | UNCERTAIN, ghi notFoundAt |
| 401/403, 429, 5xx, login/CAPTCHA, TLS/DNS/timeout | UNCERTAIN |
| Chưa có kết quả | UNCHECKED |

HTTP URL vẫn lưu/mở bằng browser theo thao tác người dùng, nhưng auto-check báo UNCERTAIN và đề nghị HTTPS. Không kết luận mọi 200 là tài nguyên riêng tư còn sống, không gắn NOTE vào trạng thái lỗi link.

### 10.3 Chặn truy cập mạng nội bộ: điều kiện trước khi bật auto-check rộng

Reject credentials trong URL, loopback, link-local, RFC1918, IPv6 ULA, multicast, metadata endpoints và các cách biểu diễn IP tương đương; kiểm tra cả host đầu và mọi redirect. Không xin quyền Local Network để “làm checker chạy được”.

URLSession có thể resolve DNS riêng sau một lần DNS preflight. **Preflight không tự giải quyết DNS rebinding.** Người triển khai phải chứng minh transport/host policy chặn địa chỉ không công khai tại kết nối thực, có tests IPv4/IPv6/redirect và không phá hostname TLS/SNI.

Nếu chưa đạt gate này, chạy iOS ở `receiveOnly` hoặc chỉ probe một allowlist public hosts được kiểm soát, giữ URL khác UNCERTAIN; vẫn nhận kết quả từ Android qua Drive. Không gọi chức năng check đầy đủ là đã hoàn thiện khi chỉ mới có URLSession.data(for:). Không tự viết TLS bypass hay browser scraping để tránh gate.

## 11. Paste, Share Extension và deep link

### 11.1 Paste có chủ đích

Dùng `PasteButton` hoặc bridge `UIPasteControl` cho nút DÁN; chỉ nhận sau hành động của người dùng. Không đọc UIPasteboard khi onAppear/foreground để tìm mọi link họ vừa copy, không polling clipboard và không hứa chèn menu toàn cục vào app khác. Khi paste bị từ chối, để nhập tay/standard paste menu hoạt động. [UIPasteControl](https://developer.apple.com/documentation/uikit/uipastecontrol).

### 11.2 Share Extension

Target riêng nhận `UTType.url` và `UTType.plainText`; activation rule có giới hạn, không ship `TRUEPREDICATE`. Content phải đọc qua NSItemProvider theo async API, validate kích thước/loại; không serialize object của host app hoặc lấy nội dung khác ngoài phần họ Share.

Thiết kế an toàn bản đầu: extension có SwiftUI composer qua UIHostingController, nhận diện link/note và lưu **Shared Inbox draft** vào App Group. Không mở main SQLite hoặc xin/giữ token Google trong extension.

```json
{
  "version": 1,
  "draftID": "lowercase-uuid",
  "content": "Nội dung người dùng vừa Share",
  "suggestedCategory": "Cá nhân",
  "capturedAt": 1793494800000
}
```

- Một file draft/UUID, ghi temp + atomic rename; file protection và bound thực nhận. Không ghi tất cả drafts vào một mảng UserDefaults dễ race giữa hai process.
- Main app đọc Inbox khi active, cho xác nhận thư viện đích; atomic claim draft với hash namespace đích trước khi ghi DB. Retry của draft đã claim luôn dùng đúng thư viện đó, không tự đổi sang account vừa đăng nhập. Commit nội dung + imported draftID trong cùng local transaction, rồi mới ACK/xóa file draft; crash/retry không tạo NOTE trùng. Nếu thư viện đích chưa mở được thì giữ draft chờ, không ghi sang guest hoặc account khác.
- Extension chỉ báo **“Đã lưu vào hộp nhận; mở LinkMap để thêm vào thư viện”**, không nói đã lên Drive. Nếu disk full/quota, giữ composer báo lỗi, không completeRequest giả thành công.
- Nếu chưa mở app lần nào/không có App Group container, hiện trạng thái cấu hình cần hoàn tất; không force unwrap containerURL.
- Không ép mở main app bằng private API/responder-chain hack, không khởi chạy OAuth trong Share Extension. Người dùng mở app bình thường để xử lý Inbox.

Đây là khác biệt hành vi được thiết kế rõ so với Android Share lưu trực tiếp. Nếu muốn lưu thẳng DB từ extension ở phase sau, cần thiết kế multi-process transaction, account binding và conflict handling riêng; không chỉ đổi một dòng để share cùng connection SQLite.

### 11.3 Deep link, widget

`linkmap://capture`, `linkmap://search`, `linkmap://item/<validated-id>` có thể dùng để điều hướng. Chỉ whitelist route, không thực thi URL/SQL trong query; không tự lưu/xóa hoặc mở link bên ngoài chỉ vì nhận deep link.

WidgetKit/App Intents có thể triển khai sau các flow chính: shortcut Thêm/Tìm kiếm, snapshot hạn chế dữ liệu. Không đưa toàn note/URL nhạy cảm lên lock screen mặc định; không nhận token Google trong widget.

## 12. Backup, tài khoản và quyền riêng tư

### 12.1 Backup không phải journal

Đọc backup schema 1/2; ghi schema 2. Giới hạn 25 MiB UTF-8 và 10.000 entries; validate toàn bộ trước khi viết, import add-only mặc định, không xóa/ghi đè thư viện hiện tại. Dùng fileImporter/UTType.json và security-scoped URL khi cần; fileExporter tạo file JSON rõ ràng cho người dùng tự chọn nơi lưu.

```json
{
  "schemaVersion": 2,
  "exportedAt": 1793494800000,
  "bookmarks": [
    {
      "id": "note-00000000-0000-0000-0000-000000000001",
      "kind": "NOTE",
      "url": null,
      "title": "Ý tưởng",
      "note": "Nội dung nhiều dòng\nDòng tiếp theo",
      "category": "Cá nhân",
      "createdAt": 1793494800000,
      "pinned": false,
      "openedAt": 0,
      "ignored": false
    }
  ]
}
```

Khác biệt quan trọng: Android backup NOTE dùng `url:null`, Web có thể xuất `url:""`; importer iOS phải nhận cả hai cho NOTE, **encoder journal vẫn phải dùng `url:""`**. Backup legacy thiếu kind là LINK. Bản xuất Web có thể chứa thêm health/revision fields; không dùng revision từ backup để ghi đè clock hiện tại.

Tạo revision local mới khi nhập vào thư viện; giữ id hợp lệ/createdAt khi không xung đột, cấp slot mới nếu chưa có. Cho preview số lượng thêm/trùng/lỗi. Không import demo hoặc khôi phục tombstone ngoài ý muốn chỉ vì file backup có cùng URL; cần flow restore rõ ràng. Không encode token, account email/sub, outbox, seen-files, paths preview hoặc credentials trong backup.

### 12.2 Các hành động khác nhau phải có tên khác nhau

| Hành động | Kết quả mong muốn |
|---|---|
| Đăng xuất | Dừng phiên/tasks, về guest; có giải thích cache account còn trên máy |
| Xóa dữ liệu account trên thiết bị | Xóa đúng namespace/cache được chọn sau export warning; không tuyên bố đã xóa Drive |
| Ngắt liên kết Google | SDK disconnect/revoke theo yêu cầu, dừng sync; không xóa tài khoản Google |
| Xóa một mục | Tombstone sync, hỗ trợ restore trong Đã xóa |
| Xóa vĩnh viễn toàn bộ dữ liệu cloud | Chưa thuộc sync tự động; cần thiết kế và xác nhận riêng |

Không tự purge appDataFolder, compact lịch sử hoặc reset seen-files để “khắc phục sync”. Việc purge cloud khi client Android/Web offline còn cache có thể làm dữ liệu được upload lại; phải phối hợp tất cả client hoặc nâng protocol trước khi quảng cáo một nút “xóa sạch mọi thiết bị”. Các file appDataFolder không dùng Trash như file Drive thông thường. Xem [Google appDataFolder](https://developers.google.com/workspace/drive/api/guides/appdata).

### 12.3 Ranh giới bảo mật

Nội dung nằm trong sandbox iOS + Drive cá nhân, **chưa E2EE**. Keychain/Data Protection/TLS không thay cho mã hóa đầu cuối. Không tuyên bố “người phát triển không thể đọc” trong trường hợp mã app/site hoặc thiết bị bị chiếm quyền. E2EE là phase riêng, cần nâng đồng thời Android/Web/iOS, versioned envelope, key recovery và không plaintext fallback.

Không có analytics/ads/crash service thu nội dung mặc định. Logging chỉ event code, count, latency và mã lỗi đã redacted; URL có thể chứa resourcekey hoặc token nên cũng không log toàn URL. Không gửi note tới AI, metadata service hoặc link scanner bên ngoài để “tự phân loại”.

Không yêu cầu Photos/Contacts/Location/Local Network nếu feature không dùng. File import dùng system picker. Không dùng accessibility/overlay thủ thuật để đọc nội dung app khác. Các chuỗi HTML/Markdown trong note chỉ là dữ liệu.

### 12.4 Git hygiene của phần iOS

Commit `.xcodeproj`, shared schemes, Swift sources, tests/fixtures giả lập, assets, `Package.swift`, `Package.resolved`, example config và PrivacyInfo.xcprivacy đã kiểm tra. Không commit `xcuserdata`, DerivedData, `.build`, `.xcarchive`, IPA, provisioning profiles, p12/private certificates, API keys `.p8`, `AuthKey_*`, Local.xcconfig, user databases, token dumps hoặc ảnh chụp tài khoản.

Public OAuth client ID và reversed scheme không phải secret; vẫn không commit credential JSON tải về mà chưa kiểm tra. Thêm ignore pattern iOS có phạm vi phù hợp; **không dùng `*.swift` hoặc bỏ cả `.xcodeproj`**. SHA-1 Android không phải cấu hình signing iOS; không mang Android `.signing/` sang project iOS.

## 13. Demo, assets và hiệu năng

### 13.1 Demo tách hẳn thư viện thật

Đọc `app/src/main/assets/demo-bookmarks.json` để port bộ 100 demo hoặc tạo fixture tương đương trong bundle. Không giả định JSON này là wire journal; dùng adapter DemoRecord riêng. Demo cần nhiều nền tảng, tuổi khác nhau, đủ bốn trạng thái minh họa và một vài NOTE.

Demo nằm trong memory-only `DemoLibrary`, không đi qua LibraryStore/outbox/export/Google APIs. Màn hình luôn ghi “Dữ liệu minh họa, không đồng bộ”. Tắt demo quay lại thư viện thật nguyên vẹn; bấm Thêm thoát demo và lưu mục thật sau xác nhận. Không tự import 100 demo vào lần sync đầu của account.

### 13.2 Assets

- Nền chính: copy `nebula_cloud.png` vào Asset Catalog có tên `GalaxyNebula`, giữ nguyên ảnh, không thay bằng gradient đơn thuần. Downsample/render hợp lý theo viewport, không load bản lớn lặp mỗi frame.
- Brand SVG từ `web/dist/assets/` có thể chuyển thành PDF vector hoặc asset format Xcode hỗ trợ, giữ hình logo và third-party notices. Không dùng emoji để thay logo GitHub/TikTok/Drive.
- Named colors: AccentColor cyan và các token nền tảng/status ở §9. Dùng material/system palette cho controls để Dynamic Type/contrast vẫn đúng.
- Ảnh preview remote nếu có: HTTPS, bound kích thước, cache giới hạn/dọn theo LRU; không đặt vào backup/sync và không tự tải cả media từ bookmark.

### 13.3 Ngân sách hiệu năng cần đo, không tự tuyên bố đạt

- 100/150 node: thao tác pan/zoom phải mượt trên iPhone thật; animation viền có thể update khoảng 10–15 Hz, gesture drawing theo frame OS. Giảm/đơn giản halo trong lúc tương tác nếu cần.
- Đo CPU khi map idle, khi sang tab khác, khi app background; không để Timer giữ renderer sống sau dismiss.
- 1.000+ mục: cull node ngoài viewport, label theo level-of-detail, tính edges bất đồng bộ theo snapshot; không vẽ text/shadow cho mọi node.
- MST O(n²) không được chạy trong TimelineView.body; với thư viện rất lớn cần worker/cancellation/strategy được benchmark. Có thể giảm số label hoặc chi tiết vẽ, nhưng không xóa dữ liệu hoặc báo giả số lượng.
- Memory ổn định sau 20 lần vào/ra scene, 20 lần đổi mode và import lớn. Đo bằng Instruments, không chỉ dựa vào simulator FPS.

## 14. Khởi tạo, build và chạy trên Mac

### 14.1 Lấy source và công cụ

```bash
git clone https://github.com/duconmang/linkmap-galaxy.git
cd linkmap-galaxy
git pull --ff-only
xcodebuild -version
xcodebuild -showsdks
swift --version
```

Cài Xcode từ nguồn Apple, chọn command line tools tương ứng, chấp nhận license trên máy của chủ dự án nếu được yêu cầu. Windows không có Xcode nên không dùng kết quả Gradle/Node để tuyên bố iOS build đã qua.

Tạo project trong `ios/`, không xóa/sửa Android hoặc Web để thay bằng iOS. Nếu dùng XcodeGen/Tuist, phải commit spec + cách generate/pin tool; vẫn đảm bảo người nhận có thể tái tạo project. Baseline khuyến nghị là project Xcode trực tiếp, shared scheme `LinkMap` và Swift Package local `LinkMapCore`.

Thiết lập:

1. Main app target SwiftUI, iOS deployment 17.0, Swift 6, iPhone+iPad; display name LinkMap.
2. Library package unit-testable; link SQLite3 cho app adapter và CryptoKit cho core hashing.
3. Add SPM Google SDK vào main target, không tự add SDK vào extension.
4. Add Share Extension, các Bundle ID/Team và App Group phù hợp. Entitlements phải khớp provisioning, không dùng App Group giả để che lỗi setup.
5. Asset Catalog, localization `vi`/`en`, app icon, privacy manifest và URL schemes.
6. Local.xcconfig tạo từ example: Team ID, Bundle IDs, iOS client ID/reversed scheme. Commit example không có Team/private secrets; kiểm tra các biến thực sự được Info.plist resolve.
7. Main app vẫn chạy guest/local khi Google config trống; Google button hiển thị cấu hình chưa có, không crash vì placeholder.

### 14.2 Unit tests và simulator

Sau khi đã tạo package/project:

```bash
swift test --package-path ios/Packages/LinkMapCore
xcodebuild -resolvePackageDependencies -project ios/LinkMap.xcodeproj -scheme LinkMap
xcodebuild -showdestinations -project ios/LinkMap.xcodeproj -scheme LinkMap
```

Chọn một simulator ID thực từ lệnh cuối, không hardcode tên thiết bị không cài trên máy:

```bash
xcodebuild \
  -project ios/LinkMap.xcodeproj -scheme LinkMap \
  -destination 'platform=iOS Simulator,id=<SIMULATOR_UDID>' \
  -derivedDataPath .tmp/ios-derived-data \
  test
```

`<SIMULATOR_UDID>` là placeholder phải thay. Scheme/test plan cần bao gồm đúng targets unit/UI; lỗi thiếu scheme không được sửa bằng bỏ hết tests. Kết quả build/test log chứa đường dẫn cá nhân hoặc account cần giữ local/redact trước khi publish.

Với iPhone thật: chọn Team/provisioning hợp lệ, bật Developer Mode theo luồng hệ thống nếu cần, Run từ Xcode. Google login cần test riêng trên thiết bị; không xóa dữ liệu app/account hiện có để né lỗi signing mà chưa có backup và sự đồng ý của chủ thiết bị.

### 14.3 Archive và export

Ví dụ sau chỉ chạy khi signing/capabilities đã cấu hình đúng:

```bash
xcodebuild \
  -project ios/LinkMap.xcodeproj -scheme LinkMap \
  -configuration Release -destination 'generic/platform=iOS' \
  -archivePath .tmp/LinkMap.xcarchive \
  archive
```

Upload bằng Xcode Organizer hoặc pipeline App Store Connect được cấp quyền. Nếu export CLI, tạo ExportOptions.plist theo phương thức Xcode đang hỗ trợ trên máy; không copy một giá trị `method` cũ rồi mặc định hợp lệ với mọi Xcode. Chỉ dùng automatic provisioning update nếu chủ Team đã cho phép thao tác Apple account tương ứng.

Build number phải tăng mỗi lần upload cùng version; ví dụ iOS marketing version đầu `0.1.0`, build `1`, độc lập Android versionCode 14. Không giả cùng version number là cùng binary hay cùng bundle identity.

IPA ký App Store không phải file APK có thể cài trực tiếp cho mọi người từ GitHub. Phân phối beta qua TestFlight; ad hoc/development cần profile và thiết bị phù hợp. Không hứa “pull repo trên Windows là build được IPA”.

## 15. TestFlight và App Store

### 15.1 Điều kiện phát hành

- Apple Developer Team, Bundle IDs/App Group và signing/profiles hợp lệ; App Store Connect record đúng Bundle ID.
- Xcode/SDK đáp ứng yêu cầu upload của Apple tại ngày archive. Kiểm tra lại [Submitting](https://developer.apple.com/app-store/submitting/), không coi deployment target iOS 17 là SDK build.
- Mở được guest mode; Google iOS client đúng project, URL scheme, scopes và cấu hình consent. OAuth Testing cần kế hoạch để reviewer/test users có thể kiểm tra tính năng.
- Privacy Policy URL công khai, support/contact do chủ dự án cung cấp, app icon/screenshots thật, age rating và export-compliance khai theo chức năng/thư viện thực tế.
- App Privacy disclosures phải phản ánh cả SDK Google và luồng Drive, không chọn “không thu thập gì” chỉ vì không có database riêng. Privacy manifest/required-reason APIs kiểm tra cả app và dependencies; không bịa reason code.
- Chưa đăng ký/publish bất kỳ mục App Store nào chỉ vì có tài liệu này. Các thông tin Team, support URL và quyền upload phải do chủ dự án xác nhận lúc triển khai.

### 15.2 Gate về dịch vụ đăng nhập

[App Review Guidelines 4.8](https://developer.apple.com/app-store/review/guidelines/#login-services) đặt điều kiện khi dùng third-party login cho primary account. Không tự kết luận Google-only được miễn vì app phi lợi nhuận hoặc không có backend.

Thiết kế ưu tiên local-first, **Kết nối Google Drive** là tùy chọn storage. Trước submission, đánh giá trường hợp thực tế và review notes. Nếu cần lựa chọn tương đương như Sign in with Apple, triển khai flow thật; Apple identity không cấp quyền Google Drive và không được thay Google `sub` trong namespace đã sync. Không gắn nút Apple đăng nhập giả để qua checklist.

[Guideline 5.1.1](https://developer.apple.com/app-store/review/guidelines/#privacy) có yêu cầu liên quan account creation/deletion và thu hồi kết nối. Trước Production phải giải quyết đúng phạm vi app hỗ trợ; logout, local clear và xóa Google account là ba việc khác nhau. Nếu flow deletion/Drive lifecycle chưa đáp ứng, ghi là blocker thay vì tuyên bố đã sẵn sàng App Store.

### 15.3 TestFlight

Archive → validate → upload → xử lý build trong App Store Connect → khai thông tin beta → phân phối internal/external theo quyền/review hiện hành của Apple. Người test cài bằng TestFlight. Không áp quy tắc 12 testers/14 ngày của Google Play thành quy định TestFlight; đọc điều kiện Apple tại lúc tạo đợt test.

Review notes nên giải thích local mode, cách mở demo, cách kết nối Drive, dữ liệu nằm ở đâu, NOTE không check HTTP và các giới hạn đã biết. Không gửi mật khẩu Google cá nhân cho reviewer; chuẩn bị phương án review hợp lệ và các account/test access được chủ dự án cho phép.

## 16. Kế hoạch triển khai và tiêu chí nghiệm thu

| Milestone | Công việc | Điều kiện hoàn thành |
|---|---|---|
| M0 — scaffold | Project, package, configs, assets, targets/schemes | Fresh clone mở Xcode, local app chạy; chưa cần Google |
| M1 — core + persistence | Model, URL/text, ID, merge, SQL, outbox, backup | Unit fixtures đúng Android/Web, transaction/crash tests qua |
| M2 — local UI | Editor, library, detail, delete/restore, settings | Lưu note/link offline, relaunch không mất; không có nút chết |
| M3 — maps | Galaxy, slow twinkle/age, colored MST, DC, camera | 100/150 demo; pan/pinch/filter/rotate không lệch; VoiceOver có đường truy cập |
| M4 — intake | Paste, Share Inbox, deep links | Không tự đọc clipboard; không rơi draft, không duplicate khi crash |
| M5 — Google sync | iOS client, SDK, scopes, Drive journal | Create/edit/delete ba nền tảng đúng; account switch/offline không rò dữ liệu |
| M6 — checker | First-open daily, bounds, host policy | Không quét NOTE/LAN; không mark red vì thiếu quyền; resume đúng ngày |
| M7 — release | Signing, privacy/account lifecycle, TestFlight | Archive thật, beta smoke thật, disclosure/review gates được giải quyết |

Không đánh dấu milestone bằng ảnh UI tĩnh hoặc mock sync. Nếu chỉ làm M0–M3, gọi đúng là local prototype. Nếu checker chỉ receiveOnly, ghi rõ chưa đạt auto-check đầy đủ. Không tự sửa protocol trên Android/Web để làm một test iOS dễ hơn.

Definition of Done cho bản iOS đủ chức năng:

1. Có source `ios/`, shared scheme, dependency lock, example config và hướng dẫn fresh-clone build.
2. LINK/NOTE offline bền vững, không mất nội dung nhiều dòng/emoji/resource keys; import/export tương thích.
3. Hai scene dùng camera đúng, màu mạng riêng, không chia cụm nền tảng; sao 5 cánh thật và motion settings hiệu lực.
4. Demo không xuất hiện trong outbox, backup hoặc account thật sau mọi flow.
5. Google login/reconnect/revoke được thử thật; iOS↔Android↔Web sync và tombstone qua các ca ở §17.
6. Share/paste, account switching, lỗi mạng, app suspension và storage errors không làm mất dữ liệu.
7. Tests pass có log; Xcode archive và smoke trên thiết bị được ghi bằng chứng, không chỉ nói “code có vẻ compile”.
8. Không commit signing material/tokens/private captures; các blocker App Store/Google được nêu thật.

## 17. Ma trận kiểm thử

### 17.1 Core / protocol, chạy không cần account

| Ca | Kết quả bắt buộc |
|---|---|
| URL không slash và có slash | Khác ID như golden vectors; không tự canonicalize |
| `%2F`, `%2f`, query order, fragment/resourcekey | Giữ nguyên bytes của URL lưu/hash |
| `javascript:`, `file:`, `data:`, credentials, port sai | Không thành LINK hợp lệ |
| `github.com.attacker.example` | Không phải GitHub |
| Bare domain trong văn bản, email, `1.2.3` | Đúng LINK/NOTE; không nhận domain bên trong query lần hai |
| Hai note cùng text | Hai UUID độc lập; multiline/emoji nguyên vẹn |
| 64.000/64.001 UTF-16 units, emoji surrogate | Bound đúng, không truncate body, title không tạo surrogate lẻ |
| Record thiếu kind trong journal/1 | Đọc thành LINK |
| NOTE journal url=null | Reject; NOTE backup null/empty đều import được |
| Edit URL của ID cũ | Giữ ID; health cũ không áp lên URL mới |
| Hai edits đồng thời | Cùng kết quả theo Lamport/device khi đổi thứ tự pull |
| Delete rồi replay snapshot cũ | Không sống lại; restore chủ ý revision cao hơn |
| Health mới + note/title edit cũ/mới | Health không thay nội dung/deleted |
| Ack khi có edit mới đang phát sinh | Chỉ bỏ seq đã gửi |
| Apply file lặp, upload thành công trước crash | Không duplicate nội dung hoặc tăng outbox echo |
| Lỗi một record trong batch | Không mark seen/commit nửa batch |
| 5 MiB/record count/counter vượt miền JS | Reject có lỗi rõ, không phá thư viện |
| Backup vượt 25 MiB/10.000 mục | Reject trước khi ghi; không xóa dữ liệu hiện tại |

Tạo fixtures JSON từ source/tests hiện có, kiểm tra bằng Node validator rồi dùng nguyên fixtures đó trong Swift tests. Fixture metadata phải ghi version và expected result. Không dùng token thật hoặc URLs chứa bí mật làm fixture.

### 17.2 Map / UI

Test 0, 1, 100, 150, 1.000 nodes; sao mới/cũ/tương lai; fit/zoom anchor/nearest hit; giảm motion; đổi mode và quay lại; filter không đổi slot; rotate/multitasking; 20 lần vào ra map. DC phải kiểm tra tương quan pixel/geometry giữa room, rack origin, LED và edge endpoints ở zoom 0.1, 0.5, 1, 2, 5 và khi pan.

VoiceOver đọc/mở/sửa/xóa bằng thư viện; Dynamic Type lớn không làm CTA biến mất; iPhone màn nhỏ/landscape, iPad split view, dark/light environment nếu hỗ trợ. Nội dung `<img src=x onerror=...>` hiện như text, không thành HTML.

### 17.3 End-to-end ba nền tảng, chỉ account thử nghiệm được cho phép

1. Tạo NOTE trên iOS → Sync → Android và Web thấy đúng title/body/category/kind.
2. Android sửa note, Web sync, iOS sync → nguyên nội dung tiếng Việt/emoji/nhiều dòng.
3. Web tạo LINK → iOS nhận đúng URL, query/fragment không đổi.
4. iOS check LINK → Web/Android nhận observation; chỉnh URL đồng thời không mang health cũ sang URL mới.
5. iOS xóa NOTE → hai client kia ẩn; replay offline client cũ không hồi sinh. Restore chủ ý trên một client → xuất hiện lại đúng revision.
6. Mỗi client sửa offline rồi đổi thứ tự sync → cùng winner; không tăng lặp journal vì echo.
7. Tạo fixture guest → Google login không tự nhập guest. Logout/switch A→B trong lúc HTTP đang về → không có mục A trong B.
8. Thu hồi Drive scope, hết token, offline/429, background/kill giữa upload/ACK → local data/outbox đúng.
9. Mở demo, export và sync → không có demo trong dữ liệu thật/Drive.
10. Dọn **chỉ** fixture vừa tạo bằng tombstone, xác minh ba client không còn mục thử nghiệm hoạt động. Không purge journal hoặc dữ liệu có trước.

### 17.4 Báo cáo nghiệm thu phải ghi

Commit, macOS/Xcode/SDK/Swift versions, simulator và thiết bị thật, bundle IDs, dependency versions; tests đã chạy và skipped; ảnh/video đã redact; trạng thái Google iOS client/Testing; TestFlight build number; các blocker. Không ghi token, password, Google sub/email cá nhân hoặc provisioning vào báo cáo công khai.

## 18. Prompt bàn giao cho coding agent

Có thể dùng nguyên khối sau cùng repository này trên Mac:

```text
Hãy triển khai LinkMap Galaxy iOS native bằng SwiftUI trong thư mục ios/.
Đọc toàn bộ docs/IOS-SWIFTUI-BUILD-GUIDE.md và các source tham chiếu trước khi sửa.

Giữ Android và Web đang hoạt động; không thay chúng bằng project iOS.
Không dùng WKWebView/Flutter/React Native cho UI chính. Dùng local-first storage,
LINK/NOTE, Canvas Galaxy/DC, camera chung, stars 5 cánh/age/twinkle,
colored platform networks, native editor/library/settings, paste và Share Inbox.

Hợp đồng liên nền tảng phải đúng journal/2, đọc journal/1, UTF-16 limits,
URL/ID nguyên vẹn, Lamport content merge, independent health merge,
tombstone, transaction/outbox/ACK và namespace Google sub. Không tự đổi protocol.

Tạo project/schemes/package/tests/fixtures và example config có thể build từ fresh clone.
Local mode phải chạy nếu Google chưa cấu hình. iOS OAuth client phải là loại iOS,
cùng Google Cloud project; không nhúng Client Secret hay token.
Không tạo credential, mua dịch vụ, chấp thuận điều khoản hoặc publish App Store
ngoài phạm vi chủ dự án đã cho phép. Không thu thập dữ liệu cá nhân để làm demo.

Thực hiện theo milestones và kiểm chứng từng bước. Chỉ báo build/tests/Google sync
thành công khi đã chạy thật. Nếu thiếu Apple Team, Mac, Bundle ID, OAuth client,
hoặc checker security gate thì hoàn thiện phần không phụ thuộc và ghi blocker chính xác.

Cuối cùng trả source, hướng dẫn chạy, kết quả tests/build/archive,
bằng chứng UI đã redact, trạng thái sync 3 nền tảng và danh sách phần chưa hoàn tất.
Không xóa dữ liệu người dùng hay app hiện có trên thiết bị để vượt lỗi kiểm thử.
```

## 19. Tài liệu chính thức

Các liên kết dưới đây được dùng để kiểm tra hướng tích hợp; yêu cầu SDK/App Store có thể thay đổi nên phải xem lại khi archive. Thuật toán dữ liệu và thiết kế ứng dụng ở trên dựa trên source LinkMap, không phải template của Google/Apple.

- [Google Sign-In iOS: setup, client ID, SPM, URL scheme](https://developers.google.com/identity/sign-in/ios/start-integrating)
- [Google Sign-In: SwiftUI lifecycle và restore session](https://developers.google.com/identity/sign-in/ios/sign-in)
- [Google API scopes và refresh token qua SDK](https://developers.google.com/identity/sign-in/ios/api-access)
- [Google Drive appDataFolder](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Apple SwiftUI](https://developer.apple.com/documentation/swiftui)
- [Apple Canvas](https://developer.apple.com/documentation/swiftui/canvas)
- [Apple TimelineView](https://developer.apple.com/documentation/swiftui/timelineview)
- [Apple UIPasteControl](https://developer.apple.com/documentation/uikit/uipastecontrol)
- [Apple Background strategies](https://developer.apple.com/documentation/backgroundtasks/choosing-background-strategies-for-your-app)
- [Apple App Extension Programming Guide](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/)
- [Apple App Groups](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.security.application-groups)
- [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
- [Apple App Store submission](https://developer.apple.com/app-store/submitting/)
- [Apple TestFlight](https://developer.apple.com/testflight/)

**Trạng thái của file này:** tài liệu thiết kế/triển khai đã đối chiếu Android 0.7.1 và Web 0.8.1; không phải chứng nhận iOS đã compile, đã sync thật hoặc đã được App Store duyệt.
