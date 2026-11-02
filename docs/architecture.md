# Kiến trúc

- `frontend/`: React JavaScript + Vite, CSS thuần, Oxlint theo template chính thức.
- `backend/`: Java 21 + Spring Boot 4.1.2, Maven Wrapper, Spring MVC, Spring Data JPA, Validation, Flyway, MariaDB JDBC và Actuator.
- `compose.yaml`: MariaDB 11.4; profile `app` chạy thêm backend/frontend; profile `test` chạy kiểm thử backend với DB riêng.

Frontend gọi `/api/...`; Vite proxy khi phát triển, nginx proxy khi chạy Docker. Không cần mở CORS toàn bộ. Chưa có router hoặc UI kit; thêm khi xây màn hình thực tế.

Backend tổ chức theo tính năng (`user`, `health`, `matching`), cấu hình và seed ở `config`. Tính năng tiếp theo thêm controller → service → repository, dùng DTO cho đầu vào/đầu ra; không trả thẳng entity chứa dữ liệu riêng tư.

Flyway là nguồn quản lý schema. Hibernate chỉ `validate`, không tự sửa bảng. Migration đã chạy thì không sửa lại; tạo `V2__...sql`, `V3__...sql` cho thay đổi mới. Các bảng hồ sơ/học thử/đánh giá đã có schema; hiện chỉ `User` có JPA entity/repository mẫu. Các entity và API còn lại phát triển theo tính năng.

Vector embedding lưu JSON cùng tên model; ứng dụng đọc thành `double[]` để tính cosine. Không cần vector DB riêng với quy mô MVP. Chưa tích hợp AI provider.

## Phạm vi sản phẩm

Dùng bản v0.4 trong `Specs/README.md` và `Specs/Requirement.md` làm cơ sở tạm thời: PARENT, TUTOR, ADMIN; học sinh là hồ sơ của phụ huynh; không lưu giấy tờ. PO cần thống nhất khác biệt với `README-easyToMem.md` trước khi phát triển tính năng liên quan.

## Thứ tự chia việc

1. FR-01: Spring Security, phương thức đăng nhập, lưu mật khẩu đã hash, session và phân quyền. ADMIN không do người dùng tự chọn.
2. FR-02/03: entity/DTO/service/controller cho hồ sơ, consent, danh mục môn/lớp/khu vực và lịch.
3. FR-04: admin duyệt, kiểm tra quyền tại backend; không upload giấy tờ.
4. FR-05: SQL lọc điều kiện bắt buộc → cosine top 10 → fallback; chỉ tutor đã duyệt và có vector hợp lệ.
5. FR-06/07: chi tiết gia sư và yêu cầu học thử; kiểm tra chủ sở hữu và chuyển trạng thái.
6. FR-09: đánh giá sau học thử, giải thích top 3 bằng LLM.

Chưa có Spring Security/auth nên chưa có API đọc dữ liệu người dùng; chỉ có health. Actuator chỉ công khai health. Không thêm API hồ sơ trước khi có kiểm tra quyền.

## AI

Ẩn danh dữ liệu trước cả embedding và LLM; chỉ gọi AI từ backend. Tạo lại vector khi hồ sơ đổi, tái sử dụng khi tìm kiếm. Không so vector khác model/kích thước. Tutor chưa có vector vẫn ẩn; cần tác vụ retry khi triển khai provider.

NFR-01 muốn danh sách trong 3 giây, NFR-08 cho API chờ 10 giây: cần thống nhất timeout. Fallback vẫn phải giữ điều kiện bắt buộc. Phép tính cosine hiện chỉ là nền tảng, chưa kiểm chứng chất lượng ghép thực tế.
