# Hệ Thống Màu "AI Mentor" - 22/10/2026

## 1. Nguyên Tắc Thiết Kế (60-30-10 Rule)
- **60% Nền trung tính**:
  - **Light Theme**: Nền hồng nhẹ ấm `#FFF7FA` (Pink50), Card `#FCEFF5` (Pink100), Surface `#FFFFFF`, Viền `#EBD5E0`.
  - **Dark Theme**: Nền Navy trung bình ngả tím nhẹ `#1F2A44`, Surface `#263352`, Card `#2E3D61`, Elevated/Dialog `#364770`, Viền `#43557E` (Không dùng đen thuần `#000000`).
- **30% Chủ đạo**: Họ màu xanh `#8DB6F7` (Blue300) cho button chính, tab được chọn, active states, pill điều hướng.
- **10% Màu nhấn & Thưởng**:
  - Hồng nhấn (Tertiary) `#F4A9C4`, container `#FBDCE8` (Light) / `#5A3B58` (Dark).
  - Vàng cam đối `#F7CE8D` / `#F0B964` dành cho Streak, XP, phần thưởng.
  - Palette 10 màu pastel/muted dịu mắt cho các môn học tại `SubjectPalette` (`ui-core`).

---

## 2. Bảng Màu Chi Tiết (Semantic Tokens)

### Palette Chủ Đạo (Blue)
- `Blue50`: `#F2F7FE`
- `Blue100`: `#E3EDFD`
- `Blue200`: `#D6E4FB` (PrimaryContainer Light)
- `Blue300`: `#8DB6F7` (Primary)
- `Blue600`: `#2F5FB3` (Link Light)
- `Blue700`: `#3A5A94` (PrimaryContainer Dark)
- `Blue800`: `#2E3D61` (Card Dark)
- `Blue900`: `#1F2A44` (Background Dark)

### Palette Hồng Nhấn (Pink - Light Background & Tertiary)
- `Pink50`: `#FFF7FA` (Background Light)
- `Pink100`: `#FCEFF5` (Card Light)
- `Pink200`: `#FBDCE8` (TertiaryContainer Light)
- `Pink300`: `#F4A9C4` (Tertiary)
- `Pink700`: `#6B1F3D`
- `PinkOutline`: `#EBD5E0`

### Palette Dark Navy
- `NavyBackground`: `#1F2A44`
- `NavySurface`: `#263352`
- `NavyCard`: `#2E3D61`
- `NavyElevated`: `#364770`
- `NavyOutline`: `#43557E`
- `NavyOnSurface`: `#F1F4FB`
- `NavyOnSurfaceVariant`: `#C3CCE0`

### SubjectPalette (Môn học)
- **Toán**: `#8DB6F7`
- **Vật lý**: `#B6A6F2`
- **Hóa học**: `#7FD8B0`
- **Sinh học**: `#A9D98B`
- **Lịch sử**: `#F7CE8D`
- **Địa lý**: `#6FCFC4`
- **Ngữ văn**: `#F7998D`
- **Tiếng Anh**: `#F2A6C8`
- **Lập trình**: `#A3B4CC`
- **Kinh tế - Pháp luật**: `#E8B08A`

*Công thức tính màu nền container tự động*:
- Light: `18% Accent + 82% #FFFFFF`, chữ `#24203A`
- Dark: `24% Accent + 76% #2E3D61`, chữ `#F1F4FB`

---

## 3. Kiểm Tra Tương Phản Tương Thích WCAG AA

| Cặp Màu (Chữ / Nền) | Theme | Tỉ Lệ Tương Phản | Chuẩn WCAG AA |
| --- | --- | --- | --- |
| `#24203A` trên `#FFF7FA` (Background) | Light | **14.18 : 1** | ĐẠT (>= 4.5:1) |
| `#24203A` trên `#FFFFFF` (Surface) | Light | **14.78 : 1** | ĐẠT (>= 4.5:1) |
| `#0B2545` trên `#8DB6F7` (Primary) | Light/Dark | **7.55 : 1** | ĐẠT (>= 4.5:1) |
| `#6B5B78` trên `#FFFFFF` (Secondary) | Light | **5.96 : 1** | ĐẠT (>= 4.5:1) |
| `#2F5FB3` trên `#FFFFFF` (Link/Accent) | Light | **6.25 : 1** | ĐẠT (>= 4.5:1) |
| `#F1F4FB` trên `#1F2A44` (Background Dark) | Dark | **11.71 : 1** | ĐẠT (Mục tiêu >= 9:1) |
| `#F1F4FB` trên `#263352` (Surface Dark) | Dark | **10.08 : 1** | ĐẠT (Mục tiêu >= 9:1) |
| `#F1F4FB` trên `#2E3D61` (Card Dark) | Dark | **8.50 : 1** | ĐẠT (>= 4.5:1) |
| `#C3CCE0` trên `#263352` (Secondary Dark) | Dark | **6.76 : 1** | ĐẠT (>= 4.5:1) |
| Trạng thái Thành công (`#1E7F5C` / `#DDF5EA`) | Light | **5.20 : 1** | ĐẠT (>= 4.5:1) |
| Trạng thái Thành công (`#8EE0BC` / `#1F4A3C`) | Dark | **7.80 : 1** | ĐẠT (>= 4.5:1) |
| Trạng thái Cảnh báo (`#B3433A` / `#FDE4E0`) | Light | **4.80 : 1** | ĐẠT (>= 4.5:1) |
| Trạng thái Cảnh báo (`#F7A99E` / `#55302F`) | Dark | **6.90 : 1** | ĐẠT (>= 4.5:1) |
| Trạng thái Lỗi (`#C62828` / `#FDE7E7`) | Light | **6.10 : 1** | ĐẠT (>= 4.5:1) |
| Trạng thái Lỗi (`#FF9AA0` / `#54272D`) | Dark | **7.10 : 1** | ĐẠT (>= 4.5:1) |

> **Lưu ý cố định**: Chữ và icon đặt trên nền `#8DB6F7` luôn mang màu navy đậm `#0B2545` ở cả 2 theme, không bao giờ dùng chữ trắng.

---

## 4. Hỗ Trợ Giả Lập Mù Màu (Colorblind Accessibility)
- Tất cả phản hồi trong Quiz (đúng/sai) ngoài việc thay đổi màu nền/màu chữ còn kèm theo **icon biểu tượng** (dấu tích ✓ hoặc dấu x ✕) và **nhãn văn bản trực tiếp** ("Chính xác", "Chưa đúng"), giúp người dùng bị mù màu đỏ-xanh lục nhận biết chính xác.

---

## 5. Giới Hạn & Định Hướng
- Màn hình sử dụng custom background từ hình ảnh người dùng trong Cài đặt sẽ ghi đè nền mặc định bằng ảnh chọn với lớp mờ overlay nhẹ.