# LinkMap journal v1

Hai client dùng cùng giao thức JSON `linkmap.journal/1`, lưu vào Google Drive appDataFolder của tài khoản được cấp quyền. Không có backend trung gian hoặc file thư viện chung bị hai máy ghi đè.

Mỗi file Drive là một batch **bất biến**, chứa tối đa 100 sự kiện do client gửi (client nhận giới hạn 1.000), có `appProperties.linkmapProtocol=1`. Một batch chứa đầy đủ bản ghi tại thời điểm ghi. File đã đọc được ghi nhận cục bộ bằng Drive file ID. Nếu upload thành công nhưng ACK bị gián đoạn, upload lại có thể tạo file trùng sự kiện; merge vẫn idempotent. Không tự xóa hoặc compact nhật ký trong bản đầu.

```json
{
  "protocol":"linkmap.journal/1",
  "records":[{
    "id":"url-<64 lowercase hex SHA-256>","url":"https://example.com", "title":"Example",
    "note":"", "category":"Cá nhân", "createdAt":1793149200000,
    "counter":1,"device":"installation-uuid","deleted":false,
    "health":"UNCHECKED","reason":"Chưa kiểm tra","checkedAt":0,
    "httpCode":null,"notFoundAt":0,"healthDevice":""
  }]
}
```

`id` của link mới là SHA-256 của URL đã chuẩn hóa tối thiểu, không bỏ query/fragment; ID được giữ khi sửa URL. Cùng một URL thêm độc lập ở hai nền tảng có cùng ID. Không gộp tất cả biến thể URL semantic như URL rút gọn/tracking, không gọi backend để canonicalize.

Nội dung dùng Lamport revision `(counter, device)` so sánh tăng dần; khi nhận remote, nâng clock cục bộ lên ít nhất counter đã nhận; lần sửa tiếp theo dùng counter cao hơn. Tie-break bằng device ID ASCII, không dùng đồng hồ điện thoại để chọn bản ghi nội dung. Sửa đồng thời cùng một bookmark là last Lamport writer wins; nhật ký cũ vẫn tồn tại trong Drive, chưa có UI phục hồi lịch sử phiên bản.

Xóa là tombstone có revision, không xóa ngay bản sao SQLite/sync_state. Replay hoặc kết quả kiểm tra cũ không thể làm sống lại bookmark đã xóa. Tạo lại có chủ ý sinh revision mới cao hơn.

Kết quả kiểm tra là kênh độc lập, dùng `(checkedAt, healthDevice)` và chỉ áp dụng nếu URL trùng nội dung đang thắng. Vì vậy kiểm tra cũ không ghi đè ghi chú hoặc kết quả cho URL mới. Lịch kiểm tra và camera/vị trí slot không đồng bộ; mỗi thiết bị cấp slot riêng không trùng, thư viện/nội dung/trạng thái được đồng bộ. Clock thiết bị vẫn ảnh hưởng thứ tự kết quả health (giới hạn của bản đầu); không ảnh hưởng nội dung/tombstone.

Android giữ outbox trong transaction cùng thao tác ghi; web dùng IndexedDB readwrite transaction. ACK xóa đúng các sequence đã upload, không xóa thay đổi vừa phát sinh. Merge remote không tạo echo outbox. Hạn chế response 5 MB/file và phân trang tối đa 100 trang/lượt để không tải vô hạn. Nhật ký tăng theo sử dụng; cần bổ sung compaction có checkpoint/ack trước khi dùng ở quy mô lớn dài hạn.

Mỗi tài khoản Google có database/namespace riêng. Đăng nhập không tự gộp thư viện khách. Google access token phải được xác minh subject qua Google userinfo trước khi đọc/ghi Drive và chỉ được giữ trong bộ nhớ client. Client ID công khai không phải secret. LinkMap không lưu Google password, client secret hoặc refresh token.

Android nếu một URL bị sửa đồng thời thành URL của bản ghi khác: giữ bản xung đột trong sync_state và giữ nguyên dữ liệu hiển thị hiện hữu, ghi thông báo xung đột. Web deduplicate URL ở hiển thị nhưng giữ bản ghi thô. Cần UI giải quyết trường hợp URL đụng nhau và phục hồi nhật ký ở bước tiếp theo; không tuyên bố merge hoàn hảo mọi tình huống.
# Journal/2 — 0.7.0

Web đọc cả linkmap.journal/1 và linkmap.journal/2; ghi linkmap.journal/2 với appProperties linkmapProtocol=2. kind mặc định LINK cho bản ghi legacy; NOTE dùng UUID note-..., URL rỗng và body 1–64.000 ký tự. LWW áp dụng toàn bộ note, không nhập observation HTTP từ bản khác. Nhiều note không bị gộp theo URL rỗng. Tombstone giữ nguyên ID; mỗi lô tối đa 100 records và khoảng 3 MB payload. Scope vẫn chỉ drive.appdata và hồ sơ cơ bản.
