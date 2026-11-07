# Your Sprint Tracking Sheet — TutorMatch

**Phạm vi hiện tại: Sprint 1–4, tổng 13 stories. Sprint 5–6 và AI để sau theo yêu cầu mới.**

Khung ban đầu: 6 sprints × 90 phút; mỗi sprint có 60 phút Build & Test theo slide. Ước lượng này dành cho việc hoàn thiện và kiểm thử trên codebase hiện có, nơi phần lớn core đã được triển khai theo [implementation-status.md](implementation-status.md). Team điều chỉnh cam kết theo nhân sự và năng lực thực tế trong Planning.

**Plan là trần số stories đề xuất cho mỗi sprint. Done trong bảng là mục tiêu hoàn thành đủ số đã chọn, chưa phải kết quả nghiệm thu.** Sau Review, thay Done bằng số thực tế và đánh dấu Goal achieved. Ví dụ: cam kết 4, nghiệm thu được 3 thì ghi `4 / 3`.

| Sprint | Sprint Goal | Goal achieved? | User Stories (Plan/Done mục tiêu) |
| --- | --- | --- | --- |
| Sprint 1 | Phụ huynh và gia sư có thể đăng ký, xác thực, đăng nhập và khôi phục tài khoản đúng vai trò. | ☐ Yes ☐ No | 4 / 4 |
| Sprint 2 | Phụ huynh lưu được hồ sơ và nhu cầu học của con; gia sư hoàn thiện được hồ sơ giảng dạy. | ☐ Yes ☐ No | 3 / 3 |
| Sprint 3 | Admin xác minh được gia sư; chỉ hồ sơ được duyệt mới có thể xuất hiện với phụ huynh. | ☐ Yes ☐ No | 3 / 3 |
| Sprint 4 | Phụ huynh và gia sư nhận thẻ phù hợp hai chiều, chọn OK/Bỏ qua và tạo Match khi cùng quan tâm. | ☐ Yes ☐ No | 3 / 3 |
| Sprint 5 | Phụ huynh gửi được yêu cầu học thử; gia sư phản hồi và hai bên thấy liên hệ sau khi chấp nhận. | ☐ Yes ☐ No | Để sau |
| Sprint 6 | Người dùng hoàn thành được luồng tìm gia sư đến học thử ổn định trên trình duyệt máy tính và điện thoại. | ☐ Yes ☐ No | Để sau |
| **Tổng hiện tại** | **13 stories thuộc Sprint 1–4** | | **13 / 13** |

Sprint 5–6 và story AI bonus được giữ trong backlog tham khảo. Checkbox Sprint 1–4 phản ánh phần source đã hoàn thiện và kiểm thử; team chốt Goal achieved tại Sprint Review.

## Stories cụ thể và điều kiện Done

Mã `S1-01`, `S2-01`… chỉ dùng cho sheet này; các mã US/FR trong Specs được giữ nguyên. Checkbox Sprint 1–4 đã được cập nhật theo kiểm thử implementation; các sprint sau chưa được chốt.

### Sprint 1 — Tài khoản: tối đa 4 stories

- [x] **S1-01 — Đăng ký:** Phụ huynh/gia sư muốn tạo tài khoản để sử dụng hệ thống. **Done:** lưu được tài khoản đúng vai trò; dữ liệu không hợp lệ và tài khoản trùng bị từ chối; không đăng ký công khai được vai trò Admin.
- [x] **S1-02 — Xác thực email:** Người đăng ký muốn xác thực email để kích hoạt tài khoản. **Done:** nhận email qua SMTP, dùng liên kết/mã để kích hoạt; chỉ đăng nhập sau khi xác thực email; mã sai/hết hạn bị từ chối và gửi lại được. Local dùng hộp thư Mailpit; gửi đến hộp thư thật dùng SMTP của nhà cung cấp. SĐT chỉ dùng để liên hệ.
- [x] **S1-03 — Đăng nhập/đăng xuất:** Người dùng muốn vào đúng khu vực của mình và kết thúc phiên an toàn. **Done:** đăng nhập đúng vai trò; sai mật khẩu bị từ chối; không truy cập được chức năng của vai trò khác; đăng xuất làm phiên cũ mất hiệu lực.
- [x] **S1-04 — Quên mật khẩu:** Người dùng muốn lấy lại quyền truy cập khi quên mật khẩu. **Done:** nhận được hướng dẫn khôi phục; đổi được mật khẩu bằng thông tin xác thực hợp lệ; mật khẩu cũ và phiên đăng nhập cũ mất hiệu lực.

### Sprint 2 — Hồ sơ: tối đa 3 stories

- [x] **S2-01 — Hồ sơ học sinh:** Phụ huynh muốn tạo/sửa thông tin học của con để tìm gia sư đúng nhu cầu. **Done:** lưu được môn, lớp, hình thức học, khu vực, lịch và ngân sách khi đã đồng ý lưu dữ liệu; ghi nhận đồng ý; tải lại vẫn có dữ liệu; phụ huynh khác không xem/sửa được.
- [x] **S2-02 — Khảo sát nhu cầu:** Phụ huynh muốn mô tả cách học và nhu cầu của con để nhận gợi ý phù hợp hơn. **Done:** trả lời được 6 câu khảo sát và mô tả tự do; lưu đúng hồ sơ học sinh; mở lại xem/sửa được.
- [x] **S2-03 — Hồ sơ gia sư:** Gia sư muốn giới thiệu khả năng và điều kiện dạy để được tìm thấy. **Done:** lưu/sửa được môn, lớp, hình thức, khu vực, lịch rảnh, học phí, kinh nghiệm và phương pháp dạy; dữ liệu sai bị báo lỗi; tải lại vẫn có dữ liệu.

### Sprint 3 — Xác minh: tối đa 3 stories

- [ ] **S3-01 — Nộp hồ sơ xác minh:** Gia sư muốn gửi giấy tờ để Admin xem xét danh tính. **Done:** tải được tệp hợp lệ và gửi duyệt; tệp sai loại/quá giới hạn bị từ chối; trạng thái chờ duyệt được lưu; người không có quyền không đọc được giấy tờ.
- [ ] **S3-02 — Duyệt/từ chối:** Admin muốn xem bằng chứng của lần nộp hiện tại trước khi quyết định. **Done:** phải mở bằng chứng hiện tại trước khi duyệt; duyệt hoặc từ chối kèm lý do được lưu; gia sư xem được kết quả; có lịch sử quyết định gắn mã lần nộp và phiên bản hồ sơ.
- [ ] **S3-03 — Hiển thị đáng tin cậy:** Phụ huynh muốn chỉ thấy gia sư đủ điều kiện (xác minh cấp 2 trở lên) đã được duyệt. **Done:** chỉ tài khoản đã kích hoạt, hồ sơ đã duyệt và đang bật hiển thị mới được đưa vào khám phá; có huy hiệu xác minh; hồ sơ chưa duyệt/bị ẩn không xuất hiện; gia sư lưu hồ sơ hoặc đổi/xóa ảnh, thay đổi giấy tờ thì thu hồi duyệt và phải gửi duyệt lại.

Các tiêu chí Sprint 3 cần team demo/nghiệm thu trước khi đánh dấu checkbox và Goal achieved; kết quả kiểm thử kỹ thuật ghi tại [verification.md](verification.md).

### Sprint 4 — Matching hai chiều: tối đa 3 stories

- [ ] **S4-01 — Gợi ý hai chiều theo điều kiện:** Phụ huynh và gia sư muốn có danh sách thẻ đáp ứng điều kiện bắt buộc của cả hai bên. **Done:** lọc hai chiều đúng môn, lớp, hình thức, lịch, học phí/ngân sách và cùng tỉnh khi học trực tiếp; phụ huynh có danh sách riêng cho từng hồ sơ học sinh, gia sư có danh sách thẻ học sinh; chỉ gia sư đã duyệt và đang bật hiển thị mới tham gia; xếp hạng bằng luật; có thông báo khi không có kết quả.
- [ ] **S4-02 — Thẻ sơ lược và tag:** Mỗi bên muốn xem nhanh thông tin sơ lược của thẻ và biết thẻ do hệ thống đề xuất hay do bên kia quan tâm. **Done:** thẻ gia sư hiển thị môn, huy hiệu xác minh, kinh nghiệm tóm tắt, học phí, khu vực; thẻ học sinh cho gia sư chủ yếu là thông tin học sinh, không có tên, trường, địa chỉ, ảnh hay liên hệ phụ huynh; thẻ có tag "Đề xuất" hoặc "Quan tâm bạn" (chữ kèm icon).
- [ ] **S4-03 — OK/Bỏ qua và Match:** Mỗi bên muốn chủ động chọn và chỉ kết nối khi cả hai cùng quan tâm. **Done:** thao tác được bằng vuốt và nút tương đương; lựa chọn lưu ở backend; OK của một bên đưa thẻ lên đầu danh sách bên kia với tag "Quan tâm bạn"; cả hai OK thì tạo Match và thông báo; Bỏ qua ẩn cặp khỏi cả hai danh sách mà không báo lý do; giới hạn x = 3 lượt OK vào thẻ "Quan tâm bạn" và 10 lượt OK tổng mỗi ngày mỗi hồ sơ, hết giới hạn thì khóa OK nhưng giữ thẻ; thẻ "Quan tâm bạn" hết hạn sau 7 ngày; tải lại kiểm tra được.


**Cập nhật kỹ thuật 07/11/2026:** Đã triển khai S4-01/S4-02/S4-03 và xác minh 3/3 bằng 79 kiểm thử backend, 6 kiểm thử trình duyệt desktop/mobile, lint và build frontend. Chi tiết tại [bằng chứng Sprint 4](sprint4-matching.md). Checkbox nghiệm thu và Goal achieved vẫn để team chốt ở Sprint Review; chưa ghi nhận thay Product Owner.


### Sprint 5 — Học thử: tối đa 4 stories

- [ ] **S5-01 — Gửi yêu cầu:** Phụ huynh muốn đề xuất buổi học thử với gia sư đã chọn. **Done:** gửi được hồ sơ học sinh thuộc mình, thời gian đề xuất và lời nhắn; yêu cầu hợp lệ được lưu ở trạng thái chờ và xuất hiện trong danh sách hai bên.
- [ ] **S5-02 — Phản hồi yêu cầu:** Gia sư muốn chấp nhận hoặc từ chối yêu cầu của mình. **Done:** chỉ gia sư nhận yêu cầu được phản hồi khi yêu cầu còn chờ và chưa hết hạn; trạng thái cập nhật; phụ huynh thấy kết quả khi làm mới danh sách.
- [ ] **S5-03 — Chia sẻ liên hệ:** Hai bên muốn liên hệ để thống nhất buổi học sau khi chấp nhận. **Done:** chỉ phụ huynh sở hữu yêu cầu và gia sư liên quan thấy liên hệ khi trạng thái đã chấp nhận; trạng thái chờ/từ chối/hết hạn chưa lộ liên hệ.
- [ ] **S5-04 — Yêu cầu còn hiệu lực:** Phụ huynh muốn phân biệt yêu cầu đang chờ và đã hết hạn để tiếp tục tìm gia sư. **Done:** chặn yêu cầu chờ trùng giữa cùng học sinh và gia sư; yêu cầu quá hạn hiện trạng thái hết hạn khi truy cập; không thể chấp nhận yêu cầu đã hết hạn hay phản hồi lần hai.

### Sprint 6 — Hoàn thiện: tối đa 2 stories cam kết + 1 bonus

- [ ] **S6-01 — Sử dụng trên máy tính/điện thoại:** Người dùng muốn hoàn thành luồng chính trên thiết bị của mình. **Done:** nghiệm thu trên trình duyệt máy tính và điện thoại từ đăng nhập, hồ sơ, duyệt gia sư, khám phá đến học thử; kiểm tra thêm đăng ký/xác thực/khôi phục theo Sprint 1; không có lỗi chặn luồng, nội dung bị che hoặc thao tác không sử dụng được; vuốt và nút đều hoạt động.
- [ ] **S6-02 — Trạng thái rõ ràng và thử lại:** Người dùng muốn biết thao tác đang xử lý, thành công hay thất bại để tiếp tục sử dụng. **Done:** các màn hình chính có trạng thái tải/trống/lỗi phù hợp; thông báo tiếng Việt dễ hiểu; chặn gửi lặp khi đang xử lý; thao tác lỗi có thể thử lại sau khi nguyên nhân được khắc phục.
- [ ] **S6-B01 — AI gợi ý có giải thích (bonus):** Phụ huynh muốn hiểu vì sao một gia sư phù hợp. **Done:** dùng Ollama chạy local để xếp hạng tối đa 20 ứng viên đã lọc, trả tối đa 5 kết quả kèm giải thích; chỉ gửi dữ liệu ẩn danh được cho phép; đầu ra được kiểm tra; lỗi/đầu ra sai/quá thời gian chuyển sang luật trong dưới 10 giây và vẫn giữ các điều kiện bắt buộc.

## Cách ghi nhận kết quả

**After Planning:** Chốt Sprint Goal và điền số user stories team cam kết vào Plan.

**After Review:** Demo phần mềm hoạt động, đánh dấu stories đã nghiệm thu, thay Done mục tiêu bằng số thực tế và chọn Yes hoặc No cho Sprint Goal. Số stories hoàn thành và việc đạt Sprint Goal được đánh giá riêng. Story chưa đủ tiêu chí chưa được tính Done; nếu chuyển sang sprint sau phải được chọn lại trong Planning và không tính hoàn thành hai lần.

**After Retro:** Ghi ít nhất một vấn đề, một hành động cải thiện và người phụ trách vào bảng bên dưới. Thử hành động đó trong sprint tiếp theo.

## Retrospective Log

| Sprint | Problem | Action | Owner |
| --- | --- | --- | --- |
| Sprint 1 | | | |
| Sprint 2 | | | |
| Sprint 3 | | | |
| Sprint 4 | | | |
| Sprint 5 | | | |
| Sprint 6 | | | |

Cập nhật sheet ngay sau mỗi sự kiện. Goal achieved và Retrospective Log chờ team ghi nhận tại sự kiện Scrum; Sprint 1–4 đã hoàn thiện trong source. Những phần đã có code vẫn cần kiểm tra các tiêu chí còn thiếu trước khi tính Done, đặc biệt SMTP email, AI local và nghiệm thu trình duyệt/thiết bị.
