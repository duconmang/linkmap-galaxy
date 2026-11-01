# LinkMap Web — triển khai 28/10/2026

- URL đã triển khai và được Sites xác nhận `succeeded`: https://linkmap-galaxy.opalchord20.chatgpt.site
- Audience: owner-private; chưa thay sang public/shared.
- Site: `appgprj_6ae18bd8162881919c6d590f529bb7c5`
- Version: `appgprj_6ae18bd8162881919c6d590f529bb7c5~appgver_6d56bc14fb4c81919502511602f96c8a`
- Deployment: `appgdep_6ae193ee9b9881919b6586c5c097b14b`
- Pushed source commit: `fbe2b19936f796edb03588daabe4fbc2450c920d`
- Archive: `dist/linkmap-web-v020.tgz`
- Android APK: `dist/LinkMap-0.2.0-debug.apk`

Web cục bộ và UI/storage/protocol đã kiểm tra; Google OAuth config vẫn để trống do chưa được cung cấp. Web hoạt động ở chế độ cục bộ, có thông báo chưa cấu hình khi bấm Google Login. Android cũng hiển thị tình trạng này. **Chưa có xác nhận đăng nhập hoặc Drive sync thật**.

Google Cloud/OAuth là một cấu hình của nhà phát triển, không phải tài khoản riêng cho từng người dùng. Xem `GOOGLE-SETUP.md`; script `scripts/configure-google.ps1` ghi cùng Web Client ID công khai cho hai client. Sau cấu hình phải build APK và publish web lại, rồi chạy checklist tích hợp hai thiết bị/tài khoản.
