# LinkMap Web 0.8.1

Static ES modules, không có app backend, không cần npm install.

~~~powershell
node server.mjs
node --test tests/*.test.mjs
~~~

Preview: http://127.0.0.1:4173. Website: https://duconmang.github.io/linkmap-galaxy/.

- dist/ là mã web chuẩn: Thiên hà sao 5 cánh và Data Center với phòng/rack/link chung camera.
- IndexedDB giữ thư viện khách/tài khoản riêng. Google Identity Services và Drive appDataFolder đồng bộ journal/2, tương thích Android 0.7.
- Demo 150 link chỉ trong bộ nhớ, không xuất hoặc đưa vào hàng đợi Drive.
- Token Google chỉ trong bộ nhớ; OAuth Client ID ở dist/config.js là công khai, không có Client Secret.
- Ghi chú không bị kiểm tra HTTP; Web dùng kết quả checker từ Android.
- OAuth hiện Testing; cần cấu hình origin / test users khi dùng Google Cloud project riêng.
- Chưa E2EE; xem ../docs/WEB-SECURITY.md và dist/privacy.html.

GitHub Pages hiện đọc main / root. Từ thư mục gốc chạy:

~~~powershell
node scripts/sync-web-pages.mjs
node scripts/sync-web-pages.mjs --check
~~~

Commit cả mã web/dist lẫn bản sao static ở root. Các assets/fonts/icons giao diện được phục vụ nội bộ; Google SDK tải từ Google.
