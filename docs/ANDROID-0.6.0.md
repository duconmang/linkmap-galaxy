# LinkMap Android 0.6.0 — liên kết và ghi chú

Ngày: 30/10/2026. APK: `dist/LinkMap-0.6.0-debug.apk`, versionCode 11. Cài cập nhật trên bản cũ; migration giữ toàn bộ bookmark, trạng thái, slot và nhật ký đồng bộ hiện có.

## Nhập nội dung

- Nút + mở một trường nhiều dòng: Nhập hoặc dán link / ghi chú. Nút Dán và thêm đọc clipboard khi người dùng bấm, không giám sát clipboard nền.
- Có HTTP(S), www/domain phổ biến trong văn bản hoặc một URL nhập riêng: nhận diện liên kết, tự chọn icon nền tảng.
- Không tìm thấy link: tạo mục NOTE với biểu tượng ghi chú, tên tự lấy từ dòng đầu; hỗ trợ tiêu đề riêng khi chỉnh sửa.
- Đoạn văn có link giữ nguyên toàn bộ chữ trong ghi chú kèm link. Khi có nhiều URL, người dùng chọn một link trước khi lưu; URL khác và phần chữ vẫn nằm trong nội dung kèm theo. Resource key, query, fragment không bị loại bỏ.
- Chia sẻ text hoặc link qua Android Share/PROCESS_TEXT cũng áp dụng cùng quy tắc. Ghi chú trùng nội dung vẫn có ID riêng; URL trùng vẫn mở mục đã có.
- Tối đa 64.000 ký tự mỗi nội dung; vượt giới hạn sẽ báo chưa lưu, không cắt bớt để lưu âm thầm.

## Dùng note

- Note có sao/icon ghi chú màu ấm, cùng cơ chế tuổi/kích thước và lấp lánh. Không gán nền tảng giả hoặc tạo URL giả.
- Đọc, sửa, chọn/sao chép chữ, ghim, tìm không dấu, bộ sưu tập, xóa/hoàn tác/khôi phục như bookmark. Có bộ lọc Ghi chú và Liên kết.
- Note không tham gia kiểm tra HTTP, không có màu sống/chết, không vào Cần chú ý vì lỗi link và không bị nối vào mạng nền tảng.
- Khi chỉnh sửa một note đã tạo, thêm URL vào nội dung không tự biến nó thành bookmark. Việc nhận diện tự động áp dụng lúc tạo mới; cách này tránh đổi ID/loại và làm mất note khi sửa.
- Vẫn là ghi chú văn bản thuần: chưa có tệp đính kèm, ảnh trong note, checklist hoặc rich text.

## Lưu trữ và sao lưu

- SQLite schema 5 dùng kind LINK/NOTE; URL của NOTE là SQL NULL, nội dung ở trường note. Nhiều note không va chạm chỉ mục URL unique.
- Nâng cấp sao chép bảng trong transaction và giữ bảng `bookmarks_before_notes_v4` làm snapshot an toàn trước migration; không drop dữ liệu người dùng.
- Backup JSON schema 2 giữ kind, ID, ngày thêm và nội dung đầy đủ; vẫn đọc được schema 1. Nhập cùng backup note không tạo bản lặp ID. Sao lưu có giới hạn 25 MB ở cả xuất và nhập; bản vượt giới hạn bị từ chối rõ ràng, dữ liệu local vẫn giữ.
- Phần note cũ gắn trong bookmark không bị đổi thành NOTE hoặc mất trong migration.

## Đồng bộ chuẩn bị cho bước Google

- Journal mới `linkmap.journal/2`, appProperties `linkmapProtocol=2`, hỗ trợ kind, note UUID, nội dung tới 64.000 ký tự và tombstone.
- Android 0.6 đọc cả journal/1 và journal/2; ghi journal/2. Upload được chặn theo tổng byte khoảng 3 MB/lô, không chỉ đếm số mục. Không ghi đè file journal cũ.
- Đã test merge giữa hai store cục bộ, sửa note, replay và xóa. Chưa cấu hình OAuth Client ID nên chưa test Google Drive roundtrip thật.
- **Các client cũ, gồm web hiện đang tạm gác, chỉ hỗ trợ journal/1. Cần nâng cấp chúng trước khi bật đồng bộ note đa nền tảng.** Không có tuyên bố web/live Google đã hỗ trợ note trong đợt này; không publish lại web.

## Tài khoản OAuth

Dùng Google Account Dev hiện có; không cần Gmail mới chỉ để tạo OAuth. Nên dùng project Cloud dành riêng cho LinkMap (tái sử dụng nếu đã có), cùng project chứa Android/Web client và iOS client về sau. Các client trong một project dùng chung branding. Tách project development/testing và production khi triển khai thực tế.

Nguồn: [Google Sign-In best practices](https://developers.google.com/identity/siwg/best-practices), [OAuth policies](https://developers.google.com/identity/protocols/oauth2/policies). Cấu hình cụ thể ở `GOOGLE-SETUP.md`.
