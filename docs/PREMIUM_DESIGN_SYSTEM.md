# AI Mentor — chuẩn giao diện dùng chung

Nguồn màu Compose: `core/ui-core/.../design/MentorTheme.kt`. Màu XML tương ứng: `app/src/main/res/values/colors.xml` và `values-night/colors.xml`. Các màu của trình sửa hồ sơ tham chiếu lại bảng màu XML này; không tạo một bảng màu độc lập cho từng màn hình.

| Vai trò | Sáng | Tối |
| --- | --- | --- |
| Chính | `#BE185D` | `#F9A8D4` |
| Nền | `#FFF7FB` | `#18111F` |
| Bề mặt | `#FFFFFF` | `#241B2B` |
| Chữ chính | `#25152A` | `#F8EEF7` |
| Chữ phụ | `#6E6071` | `#D5C1D0` |
| Nhấn ấm | `#9A5B00` | `#FFC57E` |

- Dùng `MaterialTheme.colorScheme` cho thành phần Compose và thuộc tính theme Android cho màn XML. Chỉ dùng màu trực tiếp cho chi tiết minh họa như mảng trang trí trên hero.
- `MentorTokens` cung cấp khoảng cách 4/8/12/16/20/24 dp, góc card 20 dp, vùng chạm tối thiểu 48 dp và thời lượng chuyển động mặc định 220 ms. Chữ dùng typography của Material 3, theo tỉ lệ phông hệ thống.
- Tái sử dụng `MentorButton`, `MentorCard`, `MentorProgress`, `MentorSkeleton`. Màn Hồ sơ có bố cục riêng nhưng dùng chung theme và các thành phần này. Chỉ tạo thành phần mới khi có ít nhất hai màn thực sự cần chung.
- Màu trạng thái mang ý nghĩa ổn định: xanh lá là hoàn thành, hồng là bước hiện tại, xám là khóa, vàng ấm là XP hoặc streak. Luôn đi kèm nhãn/trạng thái chữ để không chỉ dựa vào màu.
- Chuyển động ngắn và không cản thao tác; `motionEnabled()` đọc cài đặt hiệu ứng hệ thống. Không chạy hiệu ứng liên tục ngoài lúc tải hoặc thời điểm hoàn thành có ý nghĩa.
- Khi thiếu dữ liệu, hiển thị trạng thái trống và lối đi tiếp theo; không tự tạo XP, huy hiệu, bạn bè hoặc lịch sử học giả.
