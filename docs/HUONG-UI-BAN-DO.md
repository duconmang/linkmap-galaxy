# LinkMap — hướng UI bản đồ ô vuông

**Cập nhật 28/10/2026:** Người dùng đã thay đổi hướng bố cục: nền thiên hà phủ toàn màn hình, Top/Bottom Bar trong suốt; bỏ cụm nền tảng, tất cả bookmark nằm trên bản đồ chung và phân biệt bằng icon. Thêm link chỉ cần URL, tự tạo tên/chọn icon. Bản 0.1.2 thực hiện hướng này; các đề xuất cụm/đảo bên dưới được giữ làm lịch sử thiết kế.

Ngày: 27/10/2026. Đây là hướng giao diện mới theo phản hồi của chủ dự án, thay thế đề xuất giao diện thư viện A/B/C trước đó. Các lựa chọn backend trong bản nghiên cứu cũ vẫn là phương án, chưa được chốt.

**Đã chốt:** phương án **03 — Thiên hà ô vuông**. Triển khai Android Jetpack Compose, lưu trên thiết bị, kiểm tra lần mở app đầu mỗi ngày; không triển khai backend riêng. Xem `README.md` và `docs/VALIDATION.md` cho trạng thái bản 0.1. Các mục đề xuất bên dưới là định hướng UX dài hạn, không phải tất cả đã có trong MVP.

## Nguyên tắc đã xác nhận

- Bản đồ là màn hình chính và là điểm khác biệt của sản phẩm.
- Một bookmark = một ô vuông riêng biệt. Ô chứa logo/icon của nền tảng.
- Nền ô thể hiện kết quả kiểm tra link, không lấy màu thương hiệu làm màu trạng thái.
- Nhiều bookmark cùng nền tảng có cùng logo lặp lại và nằm gần nhau trong một cụm.
- Một cụm Facebook 20 bookmark phải có 20 ô Facebook khi đang xem ở mức chi tiết đủ lớn; không thay bằng một thẻ thư mục duy nhất.
- Màu xanh = truy cập được ở lần kiểm tra gần nhất; đỏ = lỗi truy cập có bằng chứng; đề xuất thêm vàng = cần xác minh và xám = chưa kiểm tra.
- Ảnh tham chiếu của người dùng là hướng thẩm mỹ và trải nghiệm, không phải yêu cầu giữ nguyên các nút tròn trong ảnh. Yêu cầu mới ưu tiên ô vuông.

## Ba phương án demo

### 01 — Quần đảo số

Mỗi nền tảng là một đảo có đường viền nhẹ, ghép bởi các ô vuông theo hình không đều. Các đảo cách nhau bằng khoảng trống trên nền bản đồ tối. Nhãn nền tảng nằm ngoài cụm, không chiếm một ô bookmark. Không dùng đường nối nếu không có quan hệ dữ liệu thực sự.

Phù hợp nhất để cân bằng khác biệt và dễ dùng. Có thể mở rộng đảo khi thêm bookmark mà không phải biến màn hình thành danh sách hoặc bộ thẻ nhóm.

Ảnh: `concepts/map-v2/01-quan-dao-so.png`.

### 02 — Thành phố số

Các ô bookmark ghép thành khối phố trên mặt phẳng tọa độ. Mỗi nền tảng có một khu vực riêng. Khoảng trống giữa khối đóng vai trò đường đi để mắt dễ định vị; không ám chỉ liên kết mạng giữa các URL. Dùng nền sáng, đường lưới mảnh và trạng thái xanh/đỏ/vàng/xám ngay trên nền từng ô.

Ưu điểm: dễ nhớ vị trí, quy tắc sắp xếp rõ, thư viện nhiều mục vẫn có cấu trúc. Đánh đổi: bớt cảm giác tự do so với quần đảo hoặc thiên hà. Tránh trang trí thành mô hình thành phố 3D vì che khuất ô và khó chạm.

Ảnh: `concepts/map-v2/02-thanh-pho-so.png`.

### 03 — Thiên hà ô vuông

Giữ tinh thần không gian của ảnh người dùng: nền xanh đen, quầng sáng nhẹ, cụm bookmark cách nhau như các chòm sao. Mỗi bookmark vẫn là một ô vuông; logo cùng nền tảng lặp lại trong cụm. Độ sáng vừa đủ để nhận màu, không rải nhiều hạt trang trí dễ bị nhầm là dữ liệu.

Ưu điểm: gần ảnh tham chiếu nhất, tạo nhận diện thị giác mạnh. Đánh đổi: nếu nền và chuyển động quá nhiều sẽ khó đọc, tốn tài nguyên và gây mỏi; ưu tiên chế độ hiệu ứng nhẹ.

Ảnh: `concepts/map-v2/03-thien-ha-o-vuong.png`.

## Đề xuất lựa chọn

Chọn bố cục 01 và có thể dùng chất nền/ánh sáng vừa phải của 03. Nếu ưu tiên sự gọn gàng và định vị rất rõ, chọn 02. Ba phương án khác nhau ở bố cục và cảm giác không gian; cùng sử dụng một mô hình bookmark.

## Tương tác cốt lõi

1. Mở ứng dụng: quay về vùng bản đồ và mức zoom lần trước; lần đầu thì căn toàn bộ các cụm.
2. Kéo một ngón để di chuyển; chụm hai ngón để zoom quanh tâm thao tác. Có nút trở về toàn cảnh và nút cộng/trừ hỗ trợ thao tác một tay.
3. Chạm nhãn cụm để căn và zoom vào nền tảng đó. Chạm ô đủ lớn để chọn bookmark.
4. Khi chọn, hiện viền rõ và tên ngắn bên cạnh ô; mở bottom sheet gồm tiêu đề, URL, trạng thái, thời điểm kiểm tra, ghi chú và nút Mở liên kết. Không tự mở ra ngoài chỉ vì chạm nhầm trong lúc kéo bản đồ.
5. Nhấn giữ để vào chế độ sắp xếp; kéo ô trong cụm hoặc kéo cả cụm bằng tay cầm riêng. Khi di chuyển cả cụm thì giữ nguyên tương quan các ô bên trong.
6. Thêm link qua nút + hoặc Share từ app khác. Gợi ý nền tảng, đặt vào vị trí trống gần cụm tương ứng và nháy viền ngắn để người dùng nhận ra ô vừa thêm.
7. Tìm kiếm: kết quả có tên cụ thể; chọn kết quả thì camera đưa đến đúng ô và làm nổi nó. Có thể hiện sheet danh sách kết quả để xử lý nhiều logo giống nhau.
8. Lọc theo trạng thái/danh mục: giữ nguyên vị trí, làm mờ các mục không khớp. Không tự dồn lại các ô sau mỗi lần lọc.

## Nhận diện nhiều link cùng logo

Logo giúp nhận biết nền tảng, không đủ phân biệt từng URL. Vì vậy tên xuất hiện theo mức zoom:

- Toàn cảnh: nhãn cụm + số lượng; các ô chủ yếu cho thấy mật độ và màu trạng thái.
- Mức trung bình: logo từng ô rõ hơn; nhấn cụm để đi sâu.
- Cận cảnh: tên ngắn bên dưới ô hoặc ở ô đang chọn; sheet cung cấp tiêu đề đầy đủ và URL.

Cho phép người dùng đặt tên, ghim hoặc gắn biểu tượng phụ cho bookmark quan trọng. Vẫn giữ logo nền tảng làm yếu tố chính.

## Giữ bản đồ có thể ghi nhớ

- Lưu tọa độ cụm và vị trí ô theo ID ổn định. Kiểm tra link xong chỉ đổi màu trạng thái; không đổi vị trí.
- Không dùng mô phỏng các nút trôi tự do liên tục làm bố cục mặc định.
- Bookmark mới vào ô trống; không xáo lại toàn bộ thư viện.
- Xóa để lại khoảng trống; lệnh Dọn bố cục là thao tác người dùng chủ động, có hoàn tác.
- Tách nền tảng (Facebook/GitHub/Drive) khỏi danh mục người dùng (Công việc/Cá nhân). Lọc danh mục không phá cấu trúc cụm nền tảng đã ghi nhớ.

## Thư viện lớn và khả năng tiếp cận

- Với hàng nghìn bookmark, toàn cảnh không thể là tập hợp hàng nghìn mục chạm nhỏ xíu. Giảm chi tiết theo zoom: xa thì biểu diễn mật độ/tổng trạng thái của cụm, gần thì từng ô có thể chọn.
- Chỉ vẽ phần bản đồ trong vùng nhìn thấy và vùng đệm; tránh hiệu ứng riêng liên tục cho mọi ô.
- Ô nhỏ ở toàn cảnh là dấu hiệu thị giác, không phải mọi ô đều phải chạm trực tiếp. Khi zoom gần, vùng thao tác được thiết kế đủ rộng và không chồng lên nhau.
- Bổ sung dấu ✓, ×, khóa hoặc ? khi zoom gần để phân biệt ngoài màu. Có danh sách hỗ trợ tìm kiếm và trình đọc màn hình.
- Nhấp nháy nhẹ có tùy chọn; trạng thái xanh không làm cả cụm nhấp nháy liên tục. Ưu tiên nhấp khi đang kiểm tra, khi vừa đổi trạng thái hoặc khi tìm đến một ô.
- Không dùng ánh sáng để khẳng định trạng thái realtime; luôn có thời gian kiểm tra gần nhất trong sheet.

## Gợi ý triển khai sau khi chọn concept

Compose Canvas có thể đảm nhiệm lớp nền/lưới/ô và phép biến đổi bản đồ; các thanh điều khiển, sheet và tìm kiếm dùng Compose thông thường. Tách tọa độ thế giới khỏi tọa độ màn hình; hit-test dựa vào tọa độ sau biến đổi; giữ bố cục ổn định theo ID.

Đây là hướng kỹ thuật cần prototype và đo trên máy thật trước khi chốt, đặc biệt với gesture, trình đọc màn hình và thư viện lớn. Chưa khởi tạo code ứng dụng trong bước duyệt ảnh này.

## Lưu ý về ảnh

Ba ảnh tạo bằng built-in ImageGen, dùng ảnh người dùng làm tham chiếu. Đây là concept tĩnh, không phải màn hình ứng dụng đã chạy; chữ/logo/số lượng trong ảnh có thể sai lệch nhỏ. Quy tắc dữ liệu và tương tác trong tài liệu này là cơ sở khi triển khai. Prompt gốc được lưu cùng thư mục ảnh.
