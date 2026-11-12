# Sprint 4 — Matching hai chiều

Triển khai theo S4-01 đến S4-03 trong [tracking sản phẩm](../Specs/SPRINT/sprint-tracking.md), cập nhật ngày 07/11/2026. Nội dung này thay thế luồng khám phá một chiều cũ của Sprint 4.

## Cách sử dụng

1. Phụ huynh tạo hồ sơ, đồng ý lưu dữ liệu và hoàn thành đủ 6 câu khảo sát. Mỗi hồ sơ học sinh có danh sách và giới hạn riêng.
2. Gia sư hoàn thiện hồ sơ, được admin duyệt và bật hiển thị.
3. Phụ huynh mở **Khám phá gia sư**; gia sư mở **Tìm học sinh**. Mỗi lần hiển thị một thẻ; dùng nút hoặc vuốt phải để OK, vuốt trái để Bỏ qua.
4. OK của một bên đưa thẻ lên đầu danh sách bên kia với tag **♡ Quan tâm bạn**. Cả hai OK mới tạo Match. Bỏ qua đóng cặp cho cả hai, không gửi thông báo lý do.
5. **Đã OK & Match** lưu các lựa chọn đang chờ và Match qua lần tải lại. Thông báo Match được lưu cho cả hai tài khoản, có thể đánh dấu đã đọc. Với phụ huynh có nhiều hồ sơ, **Xem Match** chọn đúng hồ sơ của thông báo.

## Điều kiện và dữ liệu

- Hai chiều cùng dùng `EligibilityPolicy`: gia sư được duyệt, đang hiển thị, tài khoản kích hoạt, nhận tất cả môn cần học, đúng lớp/hình thức, lịch thực sự giao nhau, học phí không vượt ngân sách; học trực tiếp phải cùng tỉnh, học trực tuyến cho phép khác tỉnh.
- Xếp hạng bằng luật: kinh nghiệm, lịch giao nhau và mức học phí; thẻ quan tâm được ưu tiên trước điểm luật. Tối đa 20 thẻ mỗi lần tải, lấy tiếp sau khi xử lý. Hai phía nhận cùng mức phù hợp cho cùng cặp. Không gọi AI trong API matching mới.
- DTO học sinh chỉ chứa lớp, môn, hình thức, tỉnh, lịch, ngân sách và các mô tả học tập ánh xạ từ câu trả lời được cho phép. Không chuyển tên, trường, ảnh, địa chỉ, parent ID, email, số điện thoại hoặc mô tả tự do cho gia sư.
- DTO gia sư là bản sơ lược có huy hiệu danh tính. Luồng mới không mở giấy tờ, liên hệ hay hồ sơ chi tiết trước Match.

## Giới hạn và thời hạn

- Mỗi ngày, mỗi hồ sơ: tối đa **10 OK tổng**, trong đó tối đa **3 OK** vào thẻ “Quan tâm bạn”. Gia sư có một hạn mức; mỗi học sinh của phụ huynh có hạn mức độc lập.
- Ngày được tính theo `Asia/Ho_Chi_Minh`, đặt lại lúc 00:00. Bộ đếm được tăng nguyên tử trong cùng transaction với lựa chọn; Bỏ qua không trừ lượt. Gửi lại cùng lựa chọn không tạo thông báo hoặc trừ lượt lần nữa.
- Hết lượt: server trả 429; nút OK và vuốt phải bị khóa. Thẻ vẫn được giữ, Bỏ qua vẫn dùng được.
- Thời hạn **7 ngày tính từ OK đầu tiên**, không gia hạn bằng tải lại hay gửi lại. Lựa chọn đến muộn bị từ chối ngay; truy cập danh sách cập nhật trạng thái `EXPIRED` và loại cặp khỏi cả hai phía. Không cần tác vụ nền để chặn Match sau hạn.
- Khóa theo thứ tự tài khoản gia sư → hồ sơ gia sư → tài khoản phụ huynh → hồ sơ học sinh → cặp → bộ đếm; kiểm tra tài khoản còn hoạt động để ngăn lượt OK tạo lại cặp sau khi xóa tài khoản. Upsert bộ đếm lấy khóa độc quyền để tránh deadlock khi nhiều yêu cầu OK đồng thời.

## API

Tất cả yêu cầu cần phiên đăng nhập; thay đổi cần CSRF.

| Method | Đường dẫn `/api` | Vai trò / kết quả |
| --- | --- | --- |
| GET | `/students/{id}/matching` | Phụ huynh sở hữu hồ sơ: thẻ, hạn mức, Match và thẻ đã OK |
| GET | `/tutor/matching` | Gia sư đã được duyệt và hiển thị: thẻ học sinh ẩn danh và hạn mức |
| POST | `/matching/actions` | Phụ huynh/gia sư: `{studentId,tutorId,interested:boolean}`; chỉ thao tác cặp của mình |
| GET | `/matching/notifications` | Thông báo Match chưa đọc của tài khoản hiện tại |
| POST | `/matching/notifications/{id}/read` | Đánh dấu thông báo của chính tài khoản là đã đọc |

Feed: `{cards, limits: {totalRemaining,incomingRemaining,resetsAt}, matches, savedCards}`. Thẻ có `tag` là `SUGGESTED` hoặc `INTERESTED_IN_YOU`, `canLike`, `expiresAt`, `status`, mức phù hợp và giải thích. `ruleScore` là điểm luật nội bộ, không phải tỷ lệ thành công học tập.

## Migration và tương thích

`V6__two_way_matching.sql` bổ sung `matching_pairs`, `matching_daily_usage`, `matching_notifications`. Không sửa migration đã có hoặc xóa dữ liệu. Các lượt thích/bỏ qua cũ được chuyển sang quyết định của phụ huynh; không tự tạo Match. Lượt thích cũ quá 7 ngày thành hết hạn, lượt OK cũ được tính vào ngày tương ứng.

API `/swipes` cũ dùng cùng kiểm tra hạn mức và trạng thái để tránh bỏ qua luật qua endpoint cũ. Dữ liệu lựa chọn của phụ huynh vẫn được ghi tương thích vào `swipe_interactions`; xóa quan tâm cũ đóng cặp, không hoàn lượt hoặc làm cặp xuất hiện lại.

Hồ sơ chi tiết sau Match, xác nhận kết nối hai lần và lịch học thử theo quy tắc mới thuộc Sprint 5. Các API chi tiết/lesson cũ được giữ để tương thích, chưa được chuyển sang mô hình kết nối mới; không coi đây là phần đã nghiệm thu của Sprint 5. AI bonus cũng chưa được triển khai trong luồng mới.

## Bằng chứng kiểm thử

- `npm run lint` và `npm run build`: pass.
- `./mvnw -B -ntp verify` qua container test: **79 tests pass** sau khi ghép upstream `3a7e3df`, gồm **10 matching integration tests**.
- Playwright: **6 tests pass trên bản final đã merge** ở Chromium desktop và mobile 360px: email/xét duyệt thật qua Mailpit local, khảo sát, vuốt/nút, tag, hai bên OK, thông báo, tải lại, chuyển hồ sơ, hết lượt OK vẫn Bỏ qua được, đăng nhập/reset, tên pháp lý riêng tư và lỗi mạng.
- Kiểm thử backend bao gồm lọc/ẩn hồ sơ không hợp lệ, dữ liệu ẩn danh, phân quyền/CSRF, cả hai thứ tự OK, Bỏ qua, hết hạn, quota 3/10 hai phía, reset ngày, hai bên OK đồng thời và nhiều yêu cầu đồng thời sát hạn mức.
- Database kiểm thử dùng tmpfs riêng; không chạy integration test trên dữ liệu ứng dụng. Mobile là thiết bị giả lập Chromium, không phải nghiệm thu mọi máy thật.

Team có thể demo theo các bước trên để chốt Sprint Review. Kết quả kỹ thuật không thay cho xác nhận nghiệm thu của Product Owner.
