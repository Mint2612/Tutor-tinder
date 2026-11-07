# Quản lý hồ sơ và tài khoản

## Các chức năng bổ sung

| Vai trò | Vị trí | Thao tác |
| --- | --- | --- |
| Phụ huynh | Hồ sơ của con | Thêm/sửa hồ sơ, ẩn hoặc bật hiển thị từng con, xóa hồ sơ con |
| Gia sư | Hồ sơ của tôi → Trạng thái nhận học sinh | Tự tạm ẩn/bật nhận học sinh mới |
| Gia sư | Hồ sơ của tôi → Xóa hồ sơ giảng dạy | Xóa riêng hồ sơ, giữ tài khoản để tạo hồ sơ mới |
| Phụ huynh/gia sư | Tài khoản/Hồ sơ của tôi → Xóa tài khoản | Xóa thông tin cá nhân, gỡ hồ sơ và đăng xuất mọi thiết bị |

## Ẩn và hiển thị

- Hồ sơ con đang ẩn không xuất hiện trong thẻ gợi ý của gia sư; phụ huynh vẫn thấy/sửa hồ sơ của mình và có thể bật lại.
- `accepting_students` là lựa chọn của gia sư, tách biệt với `visible` của admin. Tự bật nhận học sinh không thay đổi trạng thái duyệt hay quyền chặn của admin.
- Tạm ẩn không thay đổi revision hoặc bắt duyệt lại. Sửa nội dung hồ sơ vẫn theo quy tắc duyệt lại hiện có.
- Khi tạm ẩn, đề xuất và lượt OK mới bị chặn ở cả UI và API. Match đã có vẫn xuất hiện ở **Đã OK & Match**, nếu các điều kiện khác còn hợp lệ; hạn mức/lựa chọn không bị đặt lại. Thẻ quan tâm đang chờ vẫn hết hạn theo mốc ban đầu.
- Giao diện matching có thông báo đang tạm ẩn và đường dẫn tới phần quản lý hiển thị.

## Xóa

- Xóa hồ sơ con cần nhập `XÓA`; chỉ phụ huynh sở hữu hồ sơ thực hiện được.
- Xóa hồ sơ gia sư hoặc tài khoản cần nhập `XÓA` và mật khẩu hiện tại. Có CSRF, giới hạn thử mật khẩu và xác nhận ngay trong dialog. Admin không tự xóa tài khoản qua chức năng này.
- Dữ liệu cá nhân của hồ sơ bị gỡ; danh sách quản lý và mọi API truy cập hồ sơ loại bản ghi đã xóa. Cặp đang chờ/Match bị đóng, thông báo Match liên quan được gỡ, yêu cầu học thử đang chờ chuyển sang từ chối.
- Lịch sử giữ các ID/bản ghi ẩn danh để không phá khóa ngoại. Với lịch cũ, liên hệ bị thu hồi vĩnh viễn qua `contact_revoked`; tạo lại hồ sơ gia sư không khôi phục việc chia sẻ liên hệ cũ.
- Ảnh/giấy tờ của hồ sơ gia sư bị xóa qua cơ chế dọn object storage sau commit hiện có. Tài khoản đã xóa được thay email/tên bằng giá trị ẩn danh, xóa điện thoại/mật khẩu và thu hồi tất cả session/challenge.
- Xóa tài khoản không thể hoàn tác. Email cũ có thể đăng ký tài khoản mới. Xóa riêng hồ sơ gia sư vẫn giữ tài khoản; hồ sơ tạo lại dùng cùng ID để bảo toàn hạn mức và quyết định cặp, trở về bản nháp và phải gửi minh chứng/duyệt lại. Quyền chặn của admin vẫn được giữ.

## API

Các URL bắt đầu bằng `/api`; tất cả mutation yêu cầu session/CSRF.

| Method | URL | Nội dung |
| --- | --- | --- |
| PUT | `/students/{id}/visibility` | `{visible: boolean}` |
| DELETE | `/students/{id}` | `{confirmation: "DELETE"}` |
| PUT | `/tutor/availability` | `{acceptingStudents: boolean}` |
| DELETE | `/tutor/profile` | `{password, confirmation: "DELETE"}` |
| DELETE | `/account` | `{password, confirmation: "DELETE"}`; thu hồi cookie và mọi phiên |

Student DTO thêm `visible`; Tutor DTO riêng tư thêm `accepting_students`; matching feed thêm `paused`.

## Migration và kiểm thử

`V7__profile_management.sql` thêm trạng thái ẩn/xóa và cờ thu hồi liên hệ lịch sử. Các hồ sơ hiện có mặc định tiếp tục hiển thị/nhận học sinh như trước. Không sửa V1–V6.

Backend có 8 integration tests bổ sung cho quyền sở hữu, role, CSRF, xác nhận, mật khẩu, ẩn/bật, giữ Match/quota, dọn ảnh/giấy tờ, tạo lại hồ sơ, thu hồi phiên và xóa tài khoản cùng lúc có lượt OK. Toàn bộ suite: **87 test pass**.

Playwright: **6/6 cases pass**, kiểm tra các thao tác mới trong luồng thật trên desktop và mobile 360px: công tắc tự ẩn giữ qua tải lại, sửa/ẩn/bật/xóa hồ sơ con, hủy xác nhận, sai mật khẩu, xóa hồ sơ gia sư và xóa tài khoản cả hai vai trò. Chạy trên database/Mailpit/object storage test riêng; các thao tác xóa không dùng tài khoản test ở ứng dụng chính.
