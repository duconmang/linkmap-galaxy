# LinkMap Android 0.3.0 — lưu nhanh, tìm lại dễ

Ngày bàn giao: 29/10/2026. Web được tạm gác theo yêu cầu, không thay đổi hoặc triển khai lại website trong đợt này.

## Trải nghiệm đã triển khai

| Nhu cầu | Hành vi |
|---|---|
| Lưu từ ứng dụng khác | Share hoặc PROCESS_TEXT một URL → lưu ngay → màn xác nhận có Xong, chỉnh sửa, xem bản đồ và hoàn tác. Nhiều URL thì chọn một trước. |
| Link đã lưu | Mở thông tin mục cũ, giữ nguyên tên/ghi chú thay vì tạo bản trùng. |
| Nhập trực tiếp | Một ô URL, Dán và thêm hoặc bàn phím Done; chỉnh thông tin sau. |
| Nhận diện nội dung | Dùng tiêu đề từ Share; nếu chưa có thì lấy title HTML trong lượt kiểm tra. Giữ nguyên tên do người dùng sửa. |
| Ảnh xem trước | Đọc og:image/twitter:image của trang công khai truy cập được; tải HTTPS qua DNS công khai, tối đa 1 MB, không cookie/JavaScript. Hiện ở chi tiết và map khi zoom gần. Không bảo đảm đọc được trang riêng tư. |
| Tìm lại | Tìm đa từ, không phân biệt hoa/thường hoặc dấu tiếng Việt, trên tên/URL/ghi chú/bộ sưu tập/nền tảng. Có ngay trên map. |
| Lọc nhanh | Tất cả, mới lưu, vừa mở, đã ghim, chưa mở qua LinkMap, cần chú ý. |
| Bộ sưu tập | Đặt trong Chỉnh sửa; lọc bằng chip. Ô không thuộc bộ lọc mờ đi tại vị trí cũ; không di chuyển hoặc chia cụm nền tảng. |
| Đọc thư viện | Map/Danh sách, lưu lựa chọn; tab Thư viện luôn cung cấp danh sách có nhãn và nút Mở/Bản đồ. |
| Bản đồ | Mặc định ô khoảng 72dp, không ép vừa toàn bộ thư viện; toàn cảnh bằng nút riêng. Zoom xa thấy icon/trạng thái, gần có nhãn và ảnh khi có. Có Về vị trí trước sau định vị. |
| Ghim và lịch sử | Ghim trong chi tiết, ưu tiên ở danh sách; ghi nhận khi mở thành công qua LinkMap. Không suy đoán hành vi bên ngoài app. |
| Trạng thái | Màu + ký hiệu + lý do + thời điểm; kiểm tra riêng từng link; bỏ qua cảnh báo và theo dõi lại mà không sửa kết quả HTTP. Không tự xóa link đỏ. |
| Xóa/khôi phục | Chuyển vào Đã xóa, snackbar Hoàn tác; danh sách khôi phục trong Cài đặt. Restore tạo revision mới để đồng bộ được với tombstone. |
| Nhập thư viện | JSON LinkMap và HTML bookmark export từ trình duyệt; giới hạn 5 MB/10.000 mục, bỏ qua URL trùng và scheme không phải HTTP(S). Không thay thư viện hiện có. |
| Sao lưu | JSON giữ tên, URL, ghi chú, bộ sưu tập và bổ sung ghim/lần mở/bỏ qua cảnh báo; tương thích bản sao lưu schema 1. Không xuất cache ảnh hoặc credential. |
| Nền | Rõ/Dịu/Tĩnh; giữ thiên hà toàn màn hình, làm dịu sau chữ, không nhấp nháy liên tục. Chữ UI dùng cỡ chữ hệ thống. |
| Widget Android | Widget Lưu link/Tìm lại, hiển thị số mục của thư viện đang mở; không có lịch nền. |

## Dữ liệu và tài khoản

- SQLite schema 4 thêm bảng trạng thái đọc, giữ nguyên bookmark, slot, nhật ký đồng bộ và thư viện schema 1/2/3. Cập nhật APK bình thường, không cần xóa dữ liệu.
- Ghim, lần mở, bỏ qua cảnh báo, camera và nền là thông tin cục bộ từng thiết bị; ghim/lần mở/bỏ qua đi cùng sao lưu JSON. Chúng **chưa đồng bộ qua Drive**. Bộ sưu tập dùng trường category hiện hữu và đi cùng giao thức đồng bộ.
- Cache ảnh nằm trong cache ứng dụng, được thu gọn quanh ngưỡng 40 MB khi tải ảnh mới; tối đa một ảnh 512px sau giải mã và giới hạn kích thước nguồn. Có thể bị Android xóa; nội dung bookmark vẫn còn.
- Tiêu đề tự lấy chỉ cập nhật tên dự phòng. Kết quả cho URL cũ hoặc mục đã xóa không được áp dụng.
- Lịch kiểm tra vẫn là lần mở đầu ngày, lưu từng kết quả, không scheduler nền. Offline giữ trạng thái cũ; không đợi kiểm tra xong mới dùng thư viện.
- Google OAuth/Drive đã có mã và tài liệu cấu hình từ 0.2.0; chưa có Client ID nên chưa bật hoặc kiểm chứng đăng nhập, cấp quyền và đồng bộ thật. Xem [GOOGLE-SETUP.md](GOOGLE-SETUP.md). Không sử dụng đăng nhập Google sẵn trong điện thoại để bỏ qua bước cấp quyền ứng dụng.

## Cách thử nhanh

1. Cài APK, chia sẻ một URL từ trình duyệt vào LinkMap. Bấm Xong để quay lại hoặc chỉnh tên/bộ sưu tập sau khi lưu.
2. Trên map, gõ từ khóa không dấu. Chọn Mở link hoặc Bản đồ trong kết quả.
3. Mở chi tiết, ghim link; dùng chip Đã ghim. Tạo tên bộ sưu tập trong Chỉnh sửa và lọc lại trên map.
4. Xóa thử một mục, dùng Hoàn tác hoặc Cài đặt → Đã xóa → Khôi phục.
5. Trong Cài đặt, thử ba mức nền và nhập HTML bookmark đã export từ trình duyệt.
6. Nhấn giữ màn hình Home của Android → Widgets → LinkMap để đặt widget lưu/tìm nhanh (thao tác tùy launcher).

## Phạm vi chưa thể coi là hoàn tất

- OAuth và Drive thật cần cấu hình Google Cloud/Client ID và tài khoản kiểm thử. Chưa xác minh roundtrip đa thiết bị với Google thật.
- Kiểm thử emulator không thay thế điện thoại vật lý, API 26, mọi OEM/launcher hay phiên thử TalkBack thực tế.
- Chưa tổ chức nghiên cứu với 5–8 người dùng. Mục tiêu tìm lại dưới 10 giây là tiêu chí thử nghiệm, không phải kết quả đã đo ở người dùng.
- AI phân loại/tóm tắt, iOS và phát triển web tiếp theo vẫn nằm ở giai đoạn sau như đề xuất; không đưa AI vào luồng lưu cơ bản của bản này.
- Chưa có compaction nhật ký Drive hoặc giao diện giải quyết toàn bộ xung đột đồng bộ. Không tự xóa lịch sử/thùng rác.

Kết quả build, kiểm thử và ảnh chụp thực tế nằm trong [VALIDATION.md](VALIDATION.md).
