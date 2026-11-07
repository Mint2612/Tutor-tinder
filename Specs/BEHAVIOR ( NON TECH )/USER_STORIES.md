# User Stories: TutorMatch

> Các câu chuyện người dùng của MVP, dùng theo form AI-Ready User Story của team. Yêu cầu tổng quan xem [README](../README.md).
> Tài liệu này hỗ trợ Scrum nhưng không phải thành phần bắt buộc của Scrum, nên giữ ngắn gọn và cập nhật khi team làm rõ thêm.
> Các story mức Should/Could (US-08 đến US-10) sẽ viết sau khi nhóm Must ổn định.

## Mục lục

| ID | Tên story | Vai trò | Ưu tiên | FR |
|---|---|---|---|---|
| [US-01](#us-01-đăng-ký-đăng-nhập-và-phân-quyền) | Đăng ký, đăng nhập và phân quyền | Phụ huynh, Gia sư, Admin | Must | FR-01 |
| [US-02](#us-02-tạo-hồ-sơ-học-sinh-và-làm-khảo-sát) | Tạo hồ sơ học sinh và làm khảo sát | Phụ huynh | Must | FR-02 |
| [US-03](#us-03-gia-sư-tạo-hồ-sơ-và-khai-báo-phương-pháp-dạy) | Gia sư tạo hồ sơ và khai báo phương pháp dạy | Gia sư | Must | FR-03 |
| [US-04](#us-04-xác-minh-hồ-sơ-gia-sư) | Xác minh hồ sơ gia sư | Admin | Must | FR-04 |
| [US-05](#us-05-matching-hai-chiều-dạng-thẻ-kèm-mức-phù-hợp-lấy-cảm-hứng-cách-matching-tinder) | Matching hai chiều dạng thẻ kèm mức phù hợp | Phụ huynh, Gia sư | Must | FR-05 |
| [US-06](#us-06-xem-hồ-sơ-chi-tiết-và-xác-nhận-kết-nối) | Xem hồ sơ chi tiết và xác nhận kết nối | Phụ huynh, Gia sư | Must | FR-06 |
| [US-07](#us-07-đặt-lịch-học-thử-sau-khi-kết-nối) | Đặt lịch học thử sau khi kết nối | Phụ huynh, Gia sư | Must | FR-07 |


## US-01: Đăng ký, đăng nhập và phân quyền

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh, Gia sư, Admin | Must | FR-01 / UC-01 |

### Feature
Đăng ký, đăng nhập và phân quyền theo vai trò

### User Story
Là một người dùng (Phụ huynh hoặc Gia sư),
tôi muốn đăng ký, đăng nhập và được phân quyền theo vai trò,
để tôi truy cập đúng các chức năng dành cho vai trò của mình.

### Context
Phụ huynh cần tin rằng gia sư là người thật, và gia sư làm việc với trẻ em nên danh tính phải rõ ràng. Vì vậy tài khoản Phụ huynh và Gia sư cần email và số điện thoại được xác thực, kèm cam kết thông tin chính xác. Tài khoản Admin không đăng ký công khai mà do hệ thống cấp sẵn để vào Admin Dashboard.

### Functional Requirements
- Hệ thống phải cho người dùng chọn vai trò Phụ huynh hoặc Gia sư khi đăng ký.
- Hệ thống phải yêu cầu họ tên, email, số điện thoại, mật khẩu và cam kết thông tin chính xác, đồng ý điều khoản và chính sách dữ liệu.
- Hệ thống phải gửi mã xác thực đến email và số điện thoại, chỉ kích hoạt tài khoản khi cả hai đã xác thực.
- Hệ thống phải cho đăng nhập bằng email hoặc số điện thoại cùng mật khẩu, và báo lỗi chung khi sai thông tin.
- Hệ thống phải chuyển người dùng tới màn hình chính đúng vai trò sau khi đăng nhập.
- Hệ thống phải chặn truy cập vào chức năng của vai trò khác.
- Hệ thống phải cho đăng xuất, đặt lại mật khẩu qua email đã xác thực, và tự đăng xuất khi phiên hết hạn.

### Business Rules
- Mỗi email và số điện thoại chỉ gắn với một tài khoản.
- Mỗi tài khoản chỉ có một vai trò, không tự đổi vai trò sau khi đăng ký.
- Không đăng ký Admin công khai; Admin do quản trị hệ thống cấp.
- Gia sư đăng nhập được nhưng chỉ hiển thị cho phụ huynh sau khi qua xác minh (US-04).

### Scenarios
#### Scenario 1: Đăng ký thành công
Given người dùng chưa có tài khoản
When họ nhập đủ thông tin hợp lệ và xác thực email, số điện thoại
Then tài khoản được kích hoạt và họ đăng nhập được

#### Scenario 2: Email đã tồn tại
Given đã có tài khoản dùng email này
When người dùng đăng ký với cùng email
Then hệ thống từ chối và hướng người dùng sang đăng nhập hoặc đặt lại mật khẩu

#### Scenario 3: Truy cập trái vai trò
Given một Phụ huynh đã đăng nhập
When họ cố mở trang Admin Dashboard
Then hệ thống từ chối truy cập

### Constraints
- Mật khẩu được mã hóa khi lưu, phiên hết hạn khi không hoạt động (NFR-06).
- Giao diện tiếng Việt, dùng được trên di động (NFR-03).

### Non-Goals
- Đăng nhập bằng Google/Facebook, xác thực hai lớp.
- Học sinh tự có tài khoản riêng.

### Dependencies
- Không phụ thuộc story nào. Tất cả story còn lại phụ thuộc US-01.

### Acceptance Criteria
- [ ] Tài khoản chỉ kích hoạt sau khi xác thực email và số điện thoại.
- [ ] Đăng nhập sai không tiết lộ email hoặc số điện thoại có tồn tại hay không.
- [ ] Mỗi vai trò chỉ vào được màn hình và chức năng của mình.
- [ ] Không có chức năng đăng ký Admin công khai.

---

## US-02: Tạo hồ sơ học sinh và làm khảo sát

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh | Must | FR-02 / UC-02 |

### Feature
Hồ sơ học sinh và khảo sát phong cách học

### User Story
Là một phụ huynh,
tôi muốn tạo hồ sơ cho con và hoàn thành khảo sát về cách con học,
để hệ thống hiểu nhu cầu của con và gợi ý được gia sư phù hợp.

### Context
Học sinh không có tài khoản riêng; mỗi học sinh là một hồ sơ thuộc tài khoản phụ huynh. Dữ liệu này là đầu vào chính cho phần AI (US-05) nên cần đủ rõ nhưng không quá dài. Đây là dữ liệu của trẻ em nên cần sự đồng ý của phụ huynh.

### Functional Requirements
- Hệ thống phải cho phụ huynh tạo hồ sơ học sinh gồm môn cần học, cấp lớp, hình thức học, khu vực, lịch, học phí mong muốn.
- Hệ thống phải hiển thị khảo sát phong cách học (tối đa 15 câu) và cho trả lời từng câu.
- Hệ thống phải cho nhập mô tả tự do về mục tiêu, điểm yếu và tính cách của học sinh.
- Hệ thống phải ghi nhận khảo sát do phụ huynh hay học sinh trả lời.
- Hệ thống phải cho phép sửa hồ sơ và làm lại khảo sát.
- Hệ thống phải cho phép nhiều hồ sơ học sinh dưới một tài khoản (tối thiểu một hồ sơ trong MVP).

### Business Rules
- Phụ huynh phải đồng ý rõ ràng việc lưu dữ liệu học sinh trước khi tạo hồ sơ (NFR-04).
- Mỗi học sinh là một hồ sơ độc lập: có khảo sát, danh sách gợi ý và Match riêng.
- Các thông tin bắt buộc để gợi ý (môn, cấp lớp, hình thức học) không được bỏ trống.
- Hồ sơ chưa hoàn thành khảo sát thì chưa được nhận gợi ý.

### Scenarios
#### Scenario 1: Tạo hồ sơ thành công
Given phụ huynh đã đăng nhập và đồng ý lưu dữ liệu
When họ nhập đủ thông tin và hoàn thành khảo sát
Then hồ sơ được lưu và sẵn sàng để nhận gợi ý

#### Scenario 2: Chưa đồng ý lưu dữ liệu
Given phụ huynh chưa đồng ý chính sách dữ liệu
When họ bấm tạo hồ sơ học sinh
Then hệ thống yêu cầu đồng ý trước và không lưu gì

### Constraints
- Khảo sát hoàn thành dưới 5 phút (NFR-05).
- Dữ liệu học sinh chỉ phụ huynh và gia sư được ghép mới xem được (NFR-02).

### Non-Goals
- Học sinh đăng nhập riêng; chế độ giao diện riêng cho học sinh.
- Đánh giá tâm lý hay chẩn đoán học tập.

### Dependencies
- US-01.

### Acceptance Criteria
- [ ] Tạo được ít nhất một hồ sơ học sinh đủ thông tin bắt buộc.
- [ ] Khảo sát không quá 15 câu và có thể hoàn thành dưới 5 phút.
- [ ] Không lưu dữ liệu nếu phụ huynh chưa đồng ý.
- [ ] Lưu được thông tin ai đã trả lời khảo sát.

---

## US-03: Gia sư tạo hồ sơ và khai báo phương pháp dạy

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Gia sư | Must | FR-03 / UC-03 |

### Feature
Hồ sơ gia sư và phương pháp dạy

### User Story
Là một gia sư,
tôi muốn tạo hồ sơ năng lực và khai báo cách tôi dạy,
để được hệ thống gợi ý cho những học sinh phù hợp và xây dựng uy tín của mình.

### Context
Hồ sơ gia sư là nguồn dữ liệu còn lại cho phần AI và là thứ phụ huynh dựa vào để tin tưởng. Gia sư cũng nộp minh chứng ở đây để được xác minh (US-04).

### Functional Requirements
- Hệ thống phải cho gia sư nhập môn dạy, cấp lớp, kinh nghiệm, hình thức dạy, khu vực, lịch rảnh, học phí.
- Hệ thống phải có bộ câu hỏi về cách dạy và ô mô tả tự do về phương pháp.
- Hệ thống phải cho gia sư tải minh chứng (thẻ sinh viên hoặc giấy tờ định danh, và tùy chọn bảng điểm, chứng chỉ, giải thưởng).
- Hệ thống phải đặt các minh chứng trong hồ sơ ở trạng thái "Chờ duyệt" sau khi gia sư nộp.
- Hệ thống phải cho gia sư xem trạng thái xác minh các minh chứng và lý do nếu bị từ chối.

### Business Rules
- Hồ sơ cần ít nhất một môn dạy, một cấp lớp và minh chứng cấp 1 trước khi nộp duyệt.
- Thay đổi thông tin xác minh phải được duyệt lại.
- Gia sư không tự gán huy hiệu xác minh.
- Môn, cấp lớp, hình thức, khu vực, lịch rảnh và học phí của gia sư cũng là điều kiện lọc hai chiều khi ghép (US-05).

### Scenarios
#### Scenario 1: Nộp hồ sơ
Given gia sư đã đăng nhập
When họ điền đủ thông tin bắt buộc và tải minh chứng
Then hồ sơ chuyển sang "Chờ duyệt"

#### Scenario 2: Thiếu thông tin bắt buộc
Given gia sư chưa tải minh chứng danh tính
When họ bấm nộp hồ sơ
Then hệ thống báo thiếu mục nào và không nộp

### Constraints
- Giấy tờ chỉ Admin xem được (NFR-09).

### Non-Goals
- Xác minh tự động bằng nhận diện giấy tờ.
- Hợp đồng và thanh toán cho gia sư.

### Dependencies
- US-01.

### Acceptance Criteria
- [ ] Gia sư nộp được hồ sơ có đủ môn, cấp lớp, phương pháp dạy và minh chứng.
- [ ] Hồ sơ mới ở trạng thái "Chờ duyệt", chưa hiển thị cho phụ huynh.
- [ ] Gia sư xem được kết quả và lý do từ chối.

---

## US-04A: Gia sư nộp minh chứng xác minh danh tính

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Gia sư | Must | FR-04 / UC-04 |

### Feature
Nộp minh chứng xác minh gia sư

### User Story
Là một gia sư,
tôi muốn cung cấp minh chứng xác minh danh tính của mình,
để Admin có thể kiểm tra và xác minh hồ sơ của tôi.

### Context
Gia sư cần được xác minh danh tính trước khi xuất hiện trong danh sách gợi ý cho phụ huynh. Gia sư cung cấp minh chứng theo quy trình xác minh thủ công của MVP. Hệ thống chỉ ghi nhận trạng thái và kết quả xác minh, không lưu ảnh giấy tờ định danh gốc (NFR-09).

### Functional Requirements
- Hệ thống phải cho gia sư gửi yêu cầu xác minh danh tính cấp 2.
- Hệ thống phải cho gia sư cung cấp minh chứng theo quy trình xác minh được quy định.
- Hệ thống phải kiểm tra hồ sơ gia sư có đủ thông tin bắt buộc trước khi gửi yêu cầu xác minh.
- Hệ thống phải đặt trạng thái yêu cầu xác minh thành "Chờ duyệt" sau khi gửi thành công.
- Hệ thống phải cho gia sư xem trạng thái xác minh hiện tại.
- Hệ thống phải cho gia sư xem lý do từ chối nếu yêu cầu xác minh không được chấp thuận.
- Hệ thống phải cho phép gia sư gửi lại yêu cầu xác minh sau khi bị từ chối.

### Business Rules
- Gia sư phải có ít nhất một môn dạy, một cấp lớp và thông tin xác minh cấp 1 trước khi yêu cầu xác minh cấp 2.
- Gia sư không được tự gán huy hiệu xác minh.
- Minh chứng định danh chỉ Admin được phép xem trong quá trình xác minh.
- Hệ thống không lưu ảnh giấy tờ định danh gốc (NFR-09).
- Thay đổi thông tin xác minh phải được duyệt lại.

### Scenarios
#### Scenario 1: Gửi yêu cầu xác minh thành công
Given gia sư đã đăng nhập và có hồ sơ hợp lệ
When gia sư gửi yêu cầu xác minh danh tính cấp 2
Then yêu cầu được lưu với trạng thái "Chờ duyệt"

#### Scenario 2: Thiếu thông tin bắt buộc
Given gia sư chưa cung cấp đủ thông tin hồ sơ hoặc minh chứng
When gia sư gửi yêu cầu xác minh
Then hệ thống thông báo các thông tin còn thiếu và không tiếp nhận yêu cầu

#### Scenario 3: Yêu cầu xác minh bị từ chối
Given yêu cầu xác minh của gia sư đã bị Admin từ chối
When gia sư xem trạng thái xác minh
Then hệ thống hiển thị kết quả và lý do từ chối để gia sư có thể bổ sung và gửi lại

### Constraints
- Chỉ Admin được xem thông tin minh chứng định danh (NFR-09).
- Giao diện tiếng Việt và sử dụng được trên thiết bị di động (NFR-03).

### Non-Goals
- Xác minh tự động bằng nhận diện giấy tờ.
- Lưu trữ ảnh CCCD/CMND hoặc giấy tờ định danh gốc.
- Kiểm tra lý lịch tư pháp.

### Dependencies
- US-01, US-03.

### Acceptance Criteria
- [ ] Gia sư gửi được yêu cầu xác minh danh tính cấp 2 khi hồ sơ đủ điều kiện.
- [ ] Yêu cầu xác minh chuyển sang trạng thái "Chờ duyệt".
- [ ] Gia sư xem được trạng thái và lý do từ chối.
- [ ] Gia sư có thể gửi lại yêu cầu sau khi bị từ chối.
- [ ] Gia sư không thể tự cấp huy hiệu xác minh.
- [ ] Hệ thống không lưu ảnh giấy tờ định danh gốc.

---

## US-04B: Admin duyệt minh chứng xác minh gia sư

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Admin | Must | FR-04 / UC-04 |

### Feature
Quản lý xác minh danh tính gia sư

### User Story
Là một Admin,
tôi muốn xem xét và duyệt hoặc từ chối minh chứng xác minh danh tính của gia sư,
để đảm bảo hồ sơ gia sư đáp ứng điều kiện xác minh của hệ thống.

### Context
Admin chịu trách nhiệm kiểm tra minh chứng và đưa ra quyết định xác minh thủ công. Chỉ gia sư được Admin duyệt cấp 2 mới đủ điều kiện hiển thị cho phụ huynh. Hệ thống ghi nhận kết quả xác minh và lịch sử quyết định, không lưu ảnh giấy tờ định danh gốc (NFR-09).

### Functional Requirements
- Hệ thống phải hiển thị danh sách hồ sơ gia sư đang ở trạng thái "Chờ duyệt".
- Hệ thống phải cho Admin xem hồ sơ và minh chứng xác minh của gia sư.
- Hệ thống phải cho Admin duyệt hoặc từ chối yêu cầu xác minh cấp 2.
- Hệ thống phải yêu cầu Admin nhập lý do khi từ chối.
- Hệ thống phải ghi nhận kết quả, thời gian và Admin thực hiện quyết định.
- Hệ thống phải cấp huy hiệu "Đã xác minh danh tính" khi Admin duyệt cấp 2.
- Hệ thống phải thông báo kết quả xác minh cho gia sư.
- Hệ thống phải lưu lịch sử các quyết định xác minh.

### Business Rules
- Admin phải kiểm tra minh chứng trước khi duyệt.
- Từ chối yêu cầu xác minh bắt buộc phải có lý do.
- Chỉ Admin có quyền thực hiện quyết định xác minh.
- Gia sư không được tự gán hoặc chỉnh sửa huy hiệu xác minh.
- Cấp 2 (danh tính) là điều kiện bắt buộc để gia sư được hiển thị cho phụ huynh.
- Cấp 3 (học lực, giải thưởng) là tùy chọn.
- Hệ thống không lưu ảnh giấy tờ định danh gốc (NFR-09).

### Scenarios
#### Scenario 1: Duyệt xác minh danh tính
Given có yêu cầu xác minh gia sư ở trạng thái "Chờ duyệt" với minh chứng hợp lệ
When Admin kiểm tra minh chứng và duyệt cấp 2
Then gia sư được cấp huy hiệu "Đã xác minh danh tính" và nhận thông báo kết quả

#### Scenario 2: Từ chối xác minh
Given minh chứng xác minh không rõ hoặc không hợp lệ
When Admin từ chối yêu cầu và nhập lý do
Then hệ thống lưu lý do từ chối và thông báo cho gia sư

#### Scenario 3: Xem lịch sử quyết định
Given Admin đã thực hiện một hoặc nhiều quyết định xác minh
When Admin mở lịch sử xác minh của gia sư
Then hệ thống hiển thị các quyết định xác minh đã được ghi nhận

### Constraints
- Chỉ Admin được xem thông tin minh chứng định danh (NFR-09).
- Giao diện tiếng Việt và sử dụng được trên thiết bị di động (NFR-03).

### Non-Goals
- Xác minh tự động bằng AI hoặc nhận diện giấy tờ.
- Kiểm tra lý lịch tư pháp.
- Tự động đánh giá học lực hoặc năng lực giảng dạy.

### Dependencies
- US-01, US-03, US-04A.

### Acceptance Criteria
- [ ] Admin xem được danh sách yêu cầu xác minh đang chờ duyệt.
- [ ] Admin xem được hồ sơ và minh chứng của gia sư.
- [ ] Admin duyệt được yêu cầu xác minh cấp 2.
- [ ] Admin từ chối được yêu cầu và bắt buộc nhập lý do.
- [ ] Kết quả xác minh được lưu và thông báo cho gia sư.
- [ ] Huy hiệu xác minh được cấp đúng cấp đã duyệt.
- [ ] Lịch sử quyết định xác minh được lưu.
- [ ] Chỉ Admin được xem thông tin minh chứng định danh.

---

## US-04C: Phụ huynh xem gia sư đã xác minh danh tính

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh | Must | FR-04 / UC-04 |

### Feature
Hiển thị gia sư đủ điều kiện xác minh

### User Story
Là một phụ huynh,
tôi muốn chỉ xem những gia sư đã được xác minh danh tính cấp 2 trở lên,
để tăng độ tin cậy khi lựa chọn gia sư cho con.

### Context
Phụ huynh cần lựa chọn gia sư có danh tính được xác minh để đảm bảo độ tin cậy. Hệ thống chỉ hiển thị gia sư đã được Admin duyệt xác minh cấp 2 trở lên, có hồ sơ hợp lệ và đang bật hiển thị. Huy hiệu xác minh giúp phụ huynh nhận biết mức độ xác minh của gia sư.

### Functional Requirements
- Hệ thống phải chỉ hiển thị gia sư đã được Admin duyệt xác minh cấp 2 trở lên trong danh sách gợi ý.
- Hệ thống phải chỉ hiển thị gia sư đã kích hoạt tài khoản và đang bật hiển thị.
- Hệ thống phải hiển thị huy hiệu xác minh tương ứng trên hồ sơ gia sư.
- Hệ thống phải loại bỏ gia sư chưa được duyệt, bị từ chối hoặc bị ẩn khỏi danh sách gợi ý.
- Hệ thống phải cập nhật danh sách gợi ý khi trạng thái xác minh của gia sư thay đổi.

### Business Rules
- Cấp 2 (danh tính) là điều kiện bắt buộc để gia sư được hiển thị.
- Cấp 3 (học lực, giải thưởng) là tùy chọn.
- Chỉ gia sư đã được Admin duyệt mới được cấp huy hiệu xác minh.
- Gia sư bị ẩn hoặc không còn đạt điều kiện xác minh không được xuất hiện trong danh sách phụ huynh.
- Huy hiệu xác minh không được dùng để khẳng định năng lực giảng dạy của gia sư.

### Scenarios
#### Scenario 1: Hiển thị gia sư đã xác minh
Given gia sư đã được Admin duyệt xác minh cấp 2 và đang bật hiển thị
When phụ huynh mở danh sách gợi ý gia sư
Then gia sư xuất hiện trong danh sách với huy hiệu "Đã xác minh danh tính"

#### Scenario 2: Gia sư chưa đạt cấp 2
Given gia sư chưa được Admin duyệt xác minh cấp 2
When phụ huynh mở danh sách gợi ý gia sư
Then gia sư không xuất hiện trong danh sách

#### Scenario 3: Gia sư bị ẩn
Given gia sư đã được xác minh nhưng đang tắt hiển thị
When phụ huynh mở danh sách gợi ý gia sư
Then gia sư không xuất hiện trong danh sách

#### Scenario 4: Thu hồi hoặc thay đổi trạng thái xác minh
Given gia sư không còn đáp ứng điều kiện xác minh
When hệ thống cập nhật trạng thái xác minh của gia sư
Then gia sư bị loại khỏi danh sách gợi ý của phụ huynh

### Constraints
- Giao diện tiếng Việt và sử dụng được trên thiết bị di động (NFR-03).
- Thông tin giấy tờ định danh gốc không được hiển thị cho phụ huynh (NFR-09).

### Non-Goals
- Phụ huynh trực tiếp xác minh danh tính gia sư.
- Hiển thị giấy tờ định danh gốc.
- Đánh giá hoặc xếp hạng năng lực giảng dạy dựa trên huy hiệu xác minh.

### Dependencies
- US-01, US-03, US-04A, US-04B.

### Acceptance Criteria
- [ ] Phụ huynh chỉ thấy gia sư đã được Admin duyệt xác minh cấp 2 trở lên.
- [ ] Gia sư chưa được duyệt hoặc bị ẩn không xuất hiện trong danh sách gợi ý.
- [ ] Huy hiệu xác minh hiển thị đúng với cấp đã duyệt.
- [ ] Danh sách gợi ý được cập nhật khi trạng thái xác minh thay đổi.
- [ ] Phụ huynh không thể xem giấy tờ định danh gốc của gia sư.

---

## US-05: Matching hai chiều dạng thẻ kèm mức phù hợp (lấy cảm hứng cách matching Tinder)

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh, Gia sư | Must | FR-05 / UC-05 |

### Feature
Matching bằng AI, hai bên cùng chọn

### User Story
Là một phụ huynh hoặc gia sư,
tôi muốn xem các thẻ gợi ý kèm mức phù hợp và chủ động chọn OK hoặc Bỏ qua,
để hai bên chỉ kết nối khi cùng quan tâm nhau, và gia sư cũng được chủ động chọn học sinh phù hợp với mình.

### Context
Hồ sơ học sinh đã hoàn thành khảo sát (US-02), hồ sơ gia sư đã được duyệt (US-03, US-04). Cách ghép lấy cảm hứng từ Tinder: mỗi bên có danh sách thẻ riêng, chỉ khi cả hai cùng OK mới là Match. Khác Tinder ở chỗ thẻ có lý do phù hợp và huy hiệu xác minh, không dựa vào ảnh. Thiết kế này để bảo vệ quyền lợi gia sư, không để gia sư chỉ chờ phụ huynh chọn. Tag, trạng thái và giá trị mặc định xem ở phần Quy ước chung.

### Functional Requirements
- Hệ thống phải tạo danh sách gợi ý riêng cho từng bên: phụ huynh có một danh sách cho mỗi hồ sơ học sinh và thấy thẻ gia sư; gia sư thấy thẻ hồ sơ học sinh.
- Thẻ hồ sơ học sinh hiển thị cho gia sư phải chủ yếu là thông tin học sinh (cấp lớp, môn, hình thức, khu vực, lịch, ngân sách, phong cách học, nhu cầu rút gọn); thông tin phụ huynh rất ít, không tên, không liên hệ.
- Hệ thống phải lọc hai chiều: một cặp chỉ xuất hiện khi thỏa điều kiện bắt buộc của cả hai (môn, cấp lớp, hình thức, khu vực, lịch, học phí và ngân sách).
- Hệ thống phải gửi hồ sơ ẩn danh của tối đa 20 ứng viên đã lọc cho AI để xếp hạng và giải thích mức phù hợp.
- Hệ thống phải hiển thị từng thẻ một, kèm nhãn mức phù hợp, điểm mạnh, điểm cần lưu ý và tag "Đề xuất" hoặc "Quan tâm bạn".
- Hệ thống phải cho chọn OK hoặc Bỏ qua bằng vuốt hoặc nút.
- Khi một bên chọn OK, hệ thống phải đưa cặp đó lên đầu danh sách bên kia với tag "Quan tâm bạn", chưa lộ thêm thông tin nào.
- Khi cả hai bên chọn OK, hệ thống phải tạo Match, thông báo cả hai và chuyển sang US-06.
- Khi một bên chọn Bỏ qua, hệ thống phải ẩn cặp đó khỏi danh sách của cả hai bên, không thông báo lý do.
- Hệ thống phải giới hạn số lượt OK vào thẻ "Quan tâm bạn" mỗi ngày (x) và tổng lượt OK mỗi ngày; khi đạt giới hạn thì khóa nút OK, vẫn cho Bỏ qua.
- Hệ thống phải tự đóng thẻ "Quan tâm bạn" nếu bên kia không phản hồi sau thời hạn quy định.
- Hệ thống phải tự chuyển sang chấm điểm theo luật khi AI lỗi hoặc quá 10 giây.

### Business Rules
- Chỉ gia sư đã xác minh tối thiểu cấp 2 và đang bật hiển thị mới tham gia.
- Cặp không thỏa điều kiện bắt buộc của một trong hai bên không bao giờ xuất hiện, dù điểm AI cao.
- Chỉ Match khi cả hai bên cùng OK. Trước Match, mỗi bên chỉ thấy thông tin sơ lược.
- Thông tin sơ lược của học sinh luôn ẩn danh: không tên, trường, địa chỉ, ảnh.
- Giới hạn lượt OK tính theo hồ sơ: mỗi học sinh có giới hạn riêng, mỗi gia sư có một giới hạn.
- Match tính theo từng cặp (hồ sơ học sinh, gia sư).
- Ảnh đại diện không được dùng làm tiêu chí xếp hạng.
- Mức phù hợp là một giá trị chung cho cả cặp, hiển thị giống nhau ở hai phía, mang tính tương đối, không phải cam kết kết quả học tập.

### Scenarios
#### Scenario 1: Gia sư OK trước, phụ huynh OK sau
Given gia sư thấy thẻ học sinh có tag "Đề xuất"
When gia sư chọn OK, rồi phụ huynh thấy thẻ gia sư đó ở đầu danh sách với tag "Quan tâm bạn" và chọn OK
Then hai bên thành Match và cùng được thông báo

#### Scenario 2: Một bên Bỏ qua
Given phụ huynh đã chọn OK một gia sư
When gia sư chọn Bỏ qua thẻ đó
Then cặp biến mất khỏi danh sách của cả hai và không có thông báo lý do

#### Scenario 3: Không thỏa điều kiện của gia sư
Given gia sư không nhận dạy cấp lớp của học sinh
When hệ thống tạo danh sách gợi ý
Then cặp đó không xuất hiện ở cả hai danh sách

#### Scenario 4: Đạt giới hạn lượt OK
Given một hồ sơ đã dùng hết số lượt OK vào thẻ "Quan tâm bạn" trong ngày
When người dùng mở thêm thẻ "Quan tâm bạn"
Then nút OK bị khóa kèm thông báo thử lại vào ngày mai, nút Bỏ qua vẫn dùng được và thẻ vẫn giữ đến khi hết hạn

#### Scenario 5: Thẻ "Quan tâm bạn" hết hạn
Given một bên đã chọn OK và bên kia chưa phản hồi
When quá thời hạn của thẻ
Then thẻ biến mất khỏi danh sách bên kia và cặp được đóng

#### Scenario 6: AI lỗi
Given hồ sơ hợp lệ
When dịch vụ AI không phản hồi trong 10 giây hoặc trả lỗi
Then hệ thống hiển thị thẻ theo chấm điểm theo luật, không hiện lỗi kỹ thuật

### Constraints
- Chỉ gửi dữ liệu ẩn danh cho nhà cung cấp AI (NFR-07).
- Danh sách lọc hiển thị trong 3 giây, kết quả AI trong tối đa 10 giây (NFR-01).
- Dữ liệu học sinh chỉ lộ theo từng cấp (NFR-02).

### Non-Goals
- Huấn luyện model riêng; chấm điểm riêng cho từng chiều.
- Vuốt không giới hạn; xếp hạng theo ảnh.
- Chat trước khi kết nối.

### Dependencies
- US-02, US-03, US-04; dịch vụ AI bên thứ ba (dev chọn ở STRUCTURAL/).
- Chuyển tiếp sang US-06 sau Match.

### Acceptance Criteria
- [ ] Phụ huynh (theo từng hồ sơ học sinh) và gia sư đều có danh sách thẻ riêng và chọn được OK hoặc Bỏ qua.
- [ ] Sau khi một bên chọn OK, bên kia thấy thẻ ở đầu danh sách với tag "Quan tâm bạn".
- [ ] Chỉ tạo Match khi cả hai cùng OK.
- [ ] Trước Match, gia sư không thấy thông tin định danh của học sinh hay liên hệ phụ huynh.
- [ ] Cặp bị Bỏ qua ẩn khỏi cả hai danh sách và không có thông báo lý do.
- [ ] Hết giới hạn lượt OK thì nút OK bị khóa, thẻ không bị xóa.
- [ ] Khi AI lỗi, vẫn có thẻ dự phòng (NFR-08).
- [ ] Không có gia sư chưa xác minh trong kết quả.

---

## US-06: Xem hồ sơ chi tiết và xác nhận kết nối

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh, Gia sư | Must | FR-06 / UC-06 |

### Feature
Hồ sơ chi tiết và xác nhận kết nối

### User Story
Là một phụ huynh hoặc gia sư đã Match,
tôi muốn xem hồ sơ chi tiết của bên kia và xác nhận có tiếp tục kết nối hay không,
để chỉ chia sẻ thông tin liên hệ khi cả hai thật sự ưng ý.

### Context
Sau Match (US-05), hai bên mới biết nhau qua thông tin sơ lược. Bước này mở hồ sơ chi tiết để hai bên cân nhắc, mỗi bên xác nhận một lần duy nhất. Chỉ khi cả hai xác nhận mới trao đổi thông tin liên hệ chéo. Đây là lớp bảo vệ cho dữ liệu trẻ em và quyền lợi gia sư.

### Functional Requirements
- Hệ thống phải mở hồ sơ chi tiết của bên kia cho cả hai bên sau Match.
- Phụ huynh phải thấy hồ sơ gia sư đầy đủ: môn dạy, cấp lớp, kinh nghiệm, phương pháp dạy, học phí, lịch rảnh, huy hiệu xác minh, đánh giá (nếu có), mức phù hợp và lý do.
- Gia sư phải thấy hồ sơ học sinh chi tiết (vẫn ẩn danh): nhu cầu, kết quả khảo sát, mô tả mục tiêu và điểm yếu, mức phù hợp và lý do.
- Mỗi bên phải chọn được "Xác nhận kết nối" hoặc "Từ chối", chỉ một lần duy nhất.
- Khi cả hai xác nhận, hệ thống phải chuyển cặp sang "Đã kết nối", hiển thị liên hệ của nhau và thông báo cả hai.
- Khi một bên từ chối, hệ thống phải đóng cặp và gửi thông báo trung tính cho bên kia, không nêu lý do.
- Hệ thống phải tự đóng cặp nếu một bên không xác nhận sau thời hạn quy định.

### Business Rules
- Hồ sơ chi tiết chỉ mở sau Match; thông tin liên hệ chỉ mở sau khi cả hai xác nhận.
- Liên hệ phía học sinh là liên hệ của phụ huynh, không chia sẻ liên hệ riêng của học sinh.
- Không hiển thị giấy tờ định danh hay bảng điểm gốc, chỉ hiển thị huy hiệu.
- Gia sư bị ẩn hoặc không còn được duyệt trong lúc chờ thì cặp tự đóng.
- Quyết định xác nhận hoặc từ chối là cuối cùng, không đổi lại trong cùng cặp.

### Scenarios
#### Scenario 1: Cả hai xác nhận
Given hai bên đã Match và đã xem hồ sơ chi tiết
When cả hai chọn "Xác nhận kết nối"
Then cặp chuyển sang "Đã kết nối", hai bên thấy liên hệ của nhau và được chuyển sang đặt lịch học thử

#### Scenario 2: Một bên từ chối
Given hai bên đã Match
When một bên chọn "Từ chối"
Then cặp đóng, bên kia nhận thông báo trung tính và không thấy liên hệ

#### Scenario 3: Hết thời hạn
Given một bên đã xác nhận, bên kia chưa phản hồi
When quá thời hạn xác nhận
Then cặp tự đóng và hai bên được thông báo

#### Scenario 4: Gia sư không còn khả dụng
Given hai bên đã Match
When gia sư bị ẩn hoặc không còn được duyệt
Then cặp tự đóng và phụ huynh được thông báo hồ sơ không còn khả dụng

### Constraints
- Giao diện tiếng Việt, hiển thị tốt trên di động (NFR-03).
- Dữ liệu học sinh ẩn danh với gia sư cho đến hết bước này (NFR-02).

### Non-Goals
- Chat trước khi kết nối; so sánh nhiều gia sư cạnh nhau.
- Xem giấy tờ gốc của gia sư.
- Bước xác nhận thứ hai sau khi đã xác nhận.

### Dependencies
- US-04, US-05. US-07 phụ thuộc US-06.

### Acceptance Criteria
- [ ] Hồ sơ chi tiết chỉ xem được sau Match.
- [ ] Mỗi bên xác nhận hoặc từ chối đúng một lần.
- [ ] Liên hệ chỉ hiển thị khi cả hai xác nhận, và phía học sinh chỉ có liên hệ phụ huynh.
- [ ] Không lộ giấy tờ gốc hay bảng điểm.
- [ ] Từ chối hoặc hết hạn làm cặp đóng và không lộ thông tin liên hệ.

---

## US-07: Đặt lịch học thử sau khi kết nối

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh, Gia sư | Must | FR-07 / UC-07 |

### Feature
Đặt lịch buổi học thử

### User Story
Là một phụ huynh hoặc gia sư đã kết nối,
tôi muốn thống nhất lịch buổi học thử với bên kia,
để con có buổi học đầu tiên trước khi quyết định học lâu dài.

### Context
Hai bên đã Match (US-05) và cùng xác nhận (US-06), đã thấy thông tin liên hệ của nhau. Đây là bước chuyển đổi cuối của MVP và là cơ sở đo KPI học thử. MVP chưa có chat và thanh toán, nên buổi học diễn ra ngoài nền tảng, hệ thống chỉ chốt lịch và ghi nhận kết quả. Bên nào cũng có thể đề xuất lịch trước.

### Functional Requirements
- Hệ thống phải cho một trong hai bên đề xuất từ 1 đến 3 khung giờ học thử và hình thức học (trực tuyến hoặc tại nhà).
- Hệ thống phải thông báo cho bên kia và cho họ xác nhận một khung giờ, hoặc đề xuất khung giờ khác.
- Hệ thống phải thông báo cho cả hai bên khi lịch được chốt, đổi hoặc hủy.
- Hệ thống phải hiển thị trạng thái lịch học thử: chờ xác nhận, đã chốt, đã hủy, hết hạn.
- Hệ thống phải cho hai bên hủy hoặc đổi lịch trước giờ học, kèm lý do.
- Hệ thống phải tự đóng lịch nếu một bên không phản hồi sau thời hạn quy định.
- Sau buổi học, hệ thống phải mở lời mời đánh giá ngắn cho phụ huynh (FR-09).

### Business Rules
- Chỉ đặt được lịch học thử với cặp ở trạng thái "Đã kết nối" (US-06).
- Mỗi cặp chỉ có một lịch học thử đang hoạt động tại một thời điểm.
- Thời hạn hủy trước giờ học do team chốt.
- Lịch học thử bị hủy hoặc hết hạn thì kết nối vẫn còn, hai bên có thể đặt lại.

### Scenarios
#### Scenario 1: Chốt lịch thành công
Given hai bên đã kết nối
When một bên đề xuất 2 khung giờ và bên kia xác nhận một trong hai
Then lịch học thử được chốt và cả hai bên nhận thông báo

#### Scenario 2: Đề xuất giờ khác
Given một bên đã đề xuất khung giờ
When bên kia đề xuất khung giờ khác và bên đầu đồng ý
Then lịch được chốt theo khung giờ mới

#### Scenario 3: Không ai phản hồi
Given lịch đang ở trạng thái chờ xác nhận
When quá thời hạn phản hồi mà một bên chưa trả lời
Then lịch tự chuyển sang hết hạn, hai bên được thông báo và có thể đặt lại

#### Scenario 4: Hủy lịch
Given lịch học thử đã được chốt
When một bên hủy trước giờ học và nhập lý do
Then lịch chuyển sang đã hủy và bên còn lại được thông báo

### Constraints
- Thông báo qua email hoặc trong ứng dụng; không dùng thông báo đẩy.
- Giao diện tiếng Việt, hiển thị tốt trên di động (NFR-03).

### Non-Goals
- Thanh toán, hợp đồng, chat realtime, video call tích hợp.
- Đồng bộ lịch với Google Calendar.
- Đặt nhiều buổi học dài hạn (chỉ buổi học thử).

### Dependencies
- US-01, US-06 (Đã kết nối là điều kiện bắt buộc).
- FR-09 (đánh giá sau học thử) nhận đầu vào từ lịch đã hoàn thành.

### Acceptance Criteria
- [ ] Không đặt được lịch với cặp chưa kết nối.
- [ ] Mỗi cặp chỉ có một lịch học thử đang hoạt động.
- [ ] Cả hai bên thấy trạng thái lịch và nhận thông báo khi trạng thái thay đổi.
- [ ] Lịch quá hạn phản hồi tự chuyển sang hết hạn.
- [ ] Hủy lịch bắt buộc có lý do.

## Giải thích & Quy ước chung
### Huy hiệu và xác minh theo cấp của gia sư
## 6. Xác minh gia sư

Xác minh theo cấp, hiển thị bằng huy hiệu cho phụ huynh.

| Cấp | Nội dung | Huy hiệu | MVP |
|---|---|---|---|
| 1 | Email, số điện thoại | (cơ bản) | Bắt buộc |
| 2 | Danh tính: Admin đối chiếu thẻ sinh viên hoặc giấy tờ (CCCD, CMND) qua cuộc gọi video ngắn, ảnh chụp, rồi đánh dấu "đã xác minh" | "Đã xác minh danh tính" | **Bắt buộc để được hiển thị** |
| 3 | Học lực, giải thưởng: gia sư tự khai | "Có thành tích" (tự khai - chưa xác minh) | Tùy chọn |
| 4 | Đánh giá từ buổi học thử | Điểm đánh giá | Có (FR-09) |


> **Các lưu ý!**
- Không bắt buộc chụp minh chứng giấy tờ (CCCD, CMND) khi tạo tài khoản, nhưng bắt buộc phải có để được hiển thị vào đề xuất
- **MVP không lưu ảnh giấy tờ.** Admin chỉ ghi nhận kết quả xác minh, nên giảm hẳn rủi ro lộ dữ liệu nhạy cảm (NFR-09).
- GPA chỉ là huy hiệu, **không** dùng để xếp hạng vì không phản ánh khả năng dạy và dễ khai khống.

### Quy ước chung cho US-05 đến US-07

**Cặp** là (hồ sơ học sinh, gia sư). Mỗi học sinh là một hồ sơ độc lập, nên danh sách gợi ý, Match và kết nối đều tính riêng cho từng học sinh.

**Tag trên thẻ** (hiển thị bằng chữ kèm icon cạnh thẻ):
- **Đề xuất:** hệ thống gợi ý, chưa bên nào chọn.
- **Quan tâm bạn:** bên kia đã chọn OK thẻ này.

**Trạng thái của một cặp:**
`Đề xuất → Quan tâm bạn (một bên OK) → Match (cả hai OK) → Đã kết nối (cả hai xác nhận) → Học thử`
Cặp bị đóng khi một bên Bỏ qua, Từ chối hoặc hết hạn.

**Thông tin được xem theo cấp:**

| Cấp | Khi nào | Gia sư thấy | Phụ huynh thấy |
|---|---|---|---|
| Sơ lược | Trên thẻ, trước Match | Chủ yếu thông tin học sinh (ẩn danh); thông tin phụ huynh rất ít, không tên, không liên hệ | Gia sư: môn dạy, huy hiệu xác minh, kinh nghiệm tóm tắt, học phí, khu vực |
| Chi tiết | Sau Match | Kết quả khảo sát, mô tả mục tiêu của học sinh (vẫn ẩn danh) | Hồ sơ đầy đủ của gia sư |
| Liên hệ | Sau khi cả hai xác nhận | Liên hệ của phụ huynh | Liên hệ của gia sư |

**Giá trị mặc định** (team có thể chỉnh khi Planning):

| Tham số | Mặc định |
|---|---|
| Lượt OK vào thẻ "Quan tâm bạn" mỗi ngày, mỗi hồ sơ (x) | 3 |
| Tổng lượt OK mỗi ngày, mỗi hồ sơ (gồm cả x) | 10 |
| Thời hạn của thẻ "Quan tâm bạn" | 7 ngày |
| Thời hạn xác nhận sau Match | 3 ngày |
| Thời hạn phản hồi đề xuất lịch học thử | 3 ngày |

---

