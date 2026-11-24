# Kho đề AI — trạng thái bàn giao cho Mạnh Quỳnh

Ngày kiểm tra: 21/11/2026. Nhánh: feature/exam-pack-foundry.
Base: origin/integration 52c3c0b. Working tree sạch trước khi sửa.
Đã đọc PR #7 (draft, base integration), không lấy các thay đổi luyện AI của PR đó.

## Phạm vi và tiêu chí

Yêu cầu đầy đủ: nhập PDF/ảnh → AI tách câu → xem nguồn → sửa → duyệt → làm bài,
lưu theo tài khoản và mở lại, nâng sáu môn lớp 11 lên ít nhất 50 câu.
Không sửa Xưởng phá mệnh đề, Dự án nhóm, main hoặc dữ liệu production.

**Bản nhánh hiện tại chưa hoàn thành yêu cầu đầy đủ và chưa sẵn sàng nghiệm thu.**

## Mã đã sửa

- ExamPackFoundry là policy trong bộ nhớ, chưa có caller UI production.
- Duyệt trả về bản sao; reviewQuestion thay đúng câu trong gói, gồm đáp án/lời giải đã sửa.
- Chỉ duyệt khi đủ câu, A–D khác nhau, đáp án chuẩn, lời giải, chủ đề và nguồn.
- Nhập mới hủy trạng thái duyệt gói. Làm bài phải gọi playableQuestions để kiểm tra lại
  toàn bộ câu, chủ sở hữu, trang nguồn và trùng lặp.
- Fingerprint sắp nhãn đáp án, giữ chữ hoa/thường, số và đơn vị; không dùng structuralTemplate để xóa câu.
- AI không được cung cấp ID, chủ sở hữu hoặc trạng thái duyệt.
- Edge exampack_extract trước đây được chấp nhận rồi gọi plannerPrompt không hỗ trợ mode này.
  Nay dùng exam_pack.mjs riêng, giới hạn kết quả và giữ nguồn theo metadata request.
  Tên tệp không lấy từ model; trang phải nằm trong pageCount.
- Bộ đọc Edge chỉ trả nháp; chưa có đường ghi DB hoặc giao diện nhập mới.
  pageCount hiện là metadata phía client, chưa được máy chủ tự kiểm chứng từ PDF.

## Chặn và việc còn lại

1. Chưa có kết nối chỉ đọc cloud để kiểm kê trước khi bổ sung. Không coi số 17 do lần kiểm tra
   trước là số hiện tại; chưa thêm 198 câu hoặc tuyên bố câu nào đã được chuyên môn duyệt.
2. Migration 202611200001_new_features_schema.sql dùng public.users và user_id int,
   ép JWT sub sang int. Trong repo không tìm thấy migration tạo public.users;
   SupabaseAccounts xác thực UUID. Không được đổi int thành UUID bằng suy đoán.
   Cần kiểm kê schema thực tế và ánh xạ danh tính đã xác thực trước migration bảo toàn dữ liệu.
3. PR #7 báo CI SQL chặn tại 202611160018: Sinh học chỉ có 12 câu hoàn chỉnh,
   trong khi migration yêu cầu 15. Migration mới đặt sau không thể sửa lỗi replay trước nó.
   Không sửa migration cũ hoặc hạ assertion để che lỗi.
4. Máy local không tìm thấy docker, psql, supabase trong PATH; chưa chạy SQL cô lập.
5. Chưa có UI chọn tệp/xem trang/sửa/duyệt/làm bài, repository Supabase, trạng thái nhập dở,
   retry mất mạng, hoặc kiểm thử vòng đời trên máy ảo. Không coi domain test là E2E.

## Kiểm kê nội dung

| Môn lớp 11 | Lần trước (người dùng cung cấp) | Cloud hiện tại | Sau nhánh này |
|---|---:|---|---|
| Sinh học | 17 | Chưa xác minh | Không ghi cloud |
| GDKT&PL | 17 | Chưa xác minh | Không ghi cloud |
| Tiếng Anh | 17 | Chưa xác minh | Không ghi cloud |
| Địa lí | 17 | Chưa xác minh | Không ghi cloud |
| Lịch sử | 17 | Chưa xác minh | Không ghi cloud |
| Ngữ văn | 17 | Chưa xác minh | Không ghi cloud |

supabase/tests/exam_pack_preflight.sql là truy vấn chỉ đọc: số xuất bản, dữ liệu thiếu,
đáp án sai định dạng, lựa chọn trùng (kể cả D), câu trùng và schema kho đề.
Đây không phải smoke test nghiệm thu. Chưa chạy trên cloud.
Mọi câu AI phải được người dùng duyệt; chưa có danh sách câu mới được duyệt chuyên môn.

## Dữ liệu và rollback

Không migration mới, không deploy, không ghi cloud, không xóa hay reset dữ liệu.
Rollback mã bằng revert commit của nhánh; không cần rollback dữ liệu.
Trước triển khai phần lưu trữ: migration mới phải chứng minh tương thích dữ liệu hiện hữu,
cách ly UUID, chống ghi nhầm gói và có phương án rollback không xóa bản nháp.

## Kiểm chứng

Local: Android assembleDebug, testDebugUnitTest và lintDebug đạt (exit 0); Edge 27/27 test đạt; git diff --check đạt. Không có bằng chứng chạy Edge trên cloud. Các lệnh được ghi dưới đây và trong draft PR.
Android: ./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
Edge: node --test supabase/functions/mentor-ai/*.test.mjs backend/tests/test_gemini_key_pool.mjs backend/tests/test_mentor_ai_handler.mjs
SQL cô lập, cloud và E2E máy ảo: chưa chạy.
