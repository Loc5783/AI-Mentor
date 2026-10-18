# Authentication và onboarding — 18/10/2026

## Kiểm tra nguồn và kế hoạch đã áp dụng

- Android native Java, Material 3, Activity/Fragment; SQLite chung AImentor.db được quản lý theo phiên bản từng module.
- Tài khoản hiện có dùng BCrypt, chưa có máy chủ xác thực, email, OAuth hay dịch vụ gửi mã. Tận dụng AppServices, AsyncActionRunner, ThemeHelper/LocaleHelper, AuthColumn và chính sách safe area.
- Kế hoạch: (1) giao diện pastel theo ảnh tham chiếu, (2) dữ liệu/routing onboarding độc lập, (3) onboarding 4 trang và hồ sơ, (4) màn hình recovery + contract email trung thực, (5) kiểm thử migration/luồng mới/cũ/theme/viewport, (6) bàn giao giới hạn backend.

## Giao diện

- Nền hồng sáng nhẹ, card trắng viền hồng, input bo góc, CTA hồng đậm để chữ sáng dễ đọc. Dark mode nền tím than/card tối, accent hồng sáng và chữ tối trên CTA.
- Hệ màu nhận diện áp dụng auth/onboarding và shell; bốn môn giữ màu phân biệt riêng. Hai chế độ và ngôn ngữ dùng thiết lập chung đã có.
- Form có nhãn, hiện/ẩn mật khẩu, focus/error/success, xử lý loading và khóa thao tác khi gửi. Bản nháp và yêu cầu auth đang chạy được giữ bằng ViewModel qua thay đổi cấu hình; mật khẩu không đưa vào Bundle/SharedPreferences.
- Bố cục native cuộn theo bàn phím, tôn trọng thanh hệ thống, giới hạn chiều rộng 560dp khi mở cửa sổ/tablet lớn. Không tạo website/desktop app riêng.

## Tài khoản và dữ liệu

- Đăng nhập bằng email hoặc username cũ, bao gồm student 01/02/03. Email trùng ở dữ liệu cũ không tự chọn một người dùng ngẫu nhiên.
- Đăng ký học sinh: họ tên, email hợp lệ bất kỳ nhà cung cấp, mật khẩu/xác nhận và chấp thuận điều khoản. Không thu số điện thoại/địa chỉ.
- Tài khoản học sinh mới dùng email chuẩn hóa làm username. BCrypt giữ nguyên; từ chối mật khẩu quá 72 byte để tránh lỗi/cắt ngắn. Không giả login bằng dữ liệu tĩnh.
- Module user_db tăng từ v2 lên v3. Thêm learner_profiles với khóa user_id, step, display_name, grade, subjects, goal, minutes, consented_at. Có foreign key cascade; đăng ký account + profile cùng transaction, lỗi ghi phải rollback cả hai.
- Người dùng trước phiên bản này được ghi step=5 khi migrate để không buộc xem intro lại. API addUser/UserRegistration.register cũ dành cho compatibility/import/demo tiếp tục tạo classic account đã hoàn tất; form học sinh mới bắt buộc dùng LearnerRegistration.registerLearner, khởi tạo step=0.
- Dữ liệu học, quiz bank, EXP, streak và ID người dùng không bị viết lại.

## Điều hướng và trải nghiệm lần đầu

- Step 0–3: bốn trang giới thiệu; step 4: thiết lập hồ sơ; step 5: hoàn tất.
- Ghi bước vào SQLite theo từng account, tăng đơn điệu. Thoát và đăng nhập lại đi đến đúng bước; bỏ qua intro vẫn phải thiết lập hồ sơ. Bản nháp chưa hoàn tất được lưu khi rời màn hình, không được ghi đè profile đã hoàn tất bằng bản nháp đến trễ.
- Hồ sơ chọn lớp 10–12, ít nhất một trong bốn môn, mục tiêu; thời lượng mỗi ngày tùy chọn, không phải countdown quiz. Dashboard hiển thị tên/lớp/mục tiêu thực đã chọn.
- Hồ sơ học có thể sửa và hướng dẫn có thể xem lại trong Cài đặt. Xem lại không đặt lại trạng thái onboarding.
- Home kiểm tra account hiện hành và trạng thái profile trước khi vào. Các Activity nội bộ không export. Đăng xuất xóa ActiveUser và remembered-session metadata.
- Remember me chỉ lưu metadata local username/id/flag, không lưu mật khẩu. Đây là tài khoản local, chưa thay thế được access/refresh token hay kiểm tra quyền của backend.

## Quên mật khẩu, reset và xác minh

- Đã có UI nhập email, mã/token, mật khẩu mới/xác nhận, trạng thái và điều hướng giữa ba màn hình trong AuthRecoveryActivity.
- RecoveryService là contract cho máy chủ thật; hiện gắn UnavailableRecoveryService. Nút gửi/xác minh/reset bị vô hiệu hóa và giải thích dịch vụ chưa được nối. Không tuyên bố đã gửi mail/xác minh/thay mật khẩu.
- Không có Google/Facebook login vì nguồn chưa có OAuth provider. Không giả mã OTP, token hoặc email thành công.
- Khi nối backend: token một lần/hết hạn, resend/rate-limit ở server, thông báo không tiết lộ account, HTTPS/app links có allowlist, trạng thái xác minh và reset dựa vào server. Màn hình đã có validation và giới hạn gửi lại sơ bộ, nhưng không thay thế chống spam phía server.
- Intro không quảng cáo chức năng kiểm tra đầu vào hay đề xuất tự động chưa tồn tại. Lớp/mục tiêu hiện là metadata hiển thị; chưa có engine cá nhân hóa nội dung.

## Phân tách mã

- accounts/: canonical identity, auth session, ViewModel form, recovery contract/provider unavailable, routing.
- onboarding/: policy thuần Java, profile DTO/repository SQLite và ViewModel hồ sơ.
- activities/: Login/Register, Onboarding, Profile Setup và Recovery.
- utils/: AuthChrome và AuthFields dùng chung; giữ ThemeHelper/LocaleHelper/safe area.
- res/: màu/theme light-dark, nền/card, vector minh họa, layout và nội dung VI/EN.

## Kiểm chứng

- Debug, release, APK kiểm thử, unit và lint đều build thành công: auth-onboarding-verified-build-verification.log. 125 unit tests, không thất bại; lint 0 lỗi, 526 cảnh báo trên toàn bộ module.
- Máy giả lập QA riêng Android 37, màn hình 320dp, chữ 1,3, điều hướng ba nút: runner kết thúc OK (76 tests), gồm 68 ca chạy thành công và 8 ca API thật chủ động bỏ qua vì key trống. Kết quả: auth-onboarding-full-runtime-verification.log. Cả 9 ca auth/onboarding và 5 ca smoke luồng chính đều thành công.
- Đã xem ảnh hồ sơ trên màn hình nhỏ; form cuộn, nội dung nằm trên thanh điều hướng. Ảnh sáng/tối, VI/EN và recovery được lưu trong ../AIMentor_QA/auth-onboarding-small. Chưa kiểm thử điện thoại thật hay email server.
- 10 trường API key vẫn trống; không gọi AI thật. APK QA: ../AIMentor_QA/AImentor-debug.apk.
