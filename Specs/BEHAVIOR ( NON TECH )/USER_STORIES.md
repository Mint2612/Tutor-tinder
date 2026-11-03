# User Stories: TutorMatch

> Các câu chuyện người dùng của MVP, dùng theo form AI-Ready User Story của team. Yêu cầu tổng quan xem [README](../README.md).
> Tài liệu này hỗ trợ Scrum nhưng không phải thành phần bắt buộc của Scrum, nên giữ ngắn gọn và cập nhật khi team làm rõ thêm.
> Các story mức Should/Could (US-08 đến US-10) sẽ viết sau khi nhóm Must ổn định.

## Mục lục

| ID                                                           | Tên story                                    | Vai trò                  | Ưu tiên | FR    |
| ------------------------------------------------------------ | -------------------------------------------- | ------------------------ | ------- | ----- |
| [US-01](#us-01-đăng-ký-đăng-nhập-và-phân-quyền)              | Đăng ký, đăng nhập và phân quyền             | Phụ huynh, Gia sư, Admin | Must    | FR-01 |
| [US-02](#us-02-tạo-hồ-sơ-học-sinh-và-làm-khảo-sát)           | Tạo hồ sơ học sinh và làm khảo sát           | Phụ huynh                | Must    | FR-02 |
| [US-03](#us-03-gia-sư-tạo-hồ-sơ-và-khai-báo-phương-pháp-dạy) | Gia sư tạo hồ sơ và khai báo phương pháp dạy | Gia sư                   | Must    | FR-03 |
| [US-04](#us-04-xác-minh-hồ-sơ-gia-sư)                        | Xác minh hồ sơ gia sư                        | Admin                    | Must    | FR-04 |
| [US-05](#us-05-gợi-ý-gia-sư-kèm-mức-phù-hợp)                 | Gợi ý gia sư kèm mức phù hợp (lấy cảm hứng từ tinder                 | Phụ huynh                | Must    | FR-05 |
| [US-06](#us-06-xem-hồ-sơ-chi-tiết-gia-sư)                    | Xem hồ sơ chi tiết gia sư                    | Phụ huynh                | Must    | FR-06 |
| [US-07](#us-07-gửi-yêu-cầu-học-thử)                          | Gửi yêu cầu học thử                          | Phụ huynh, Gia sư        | Must    | FR-07 |

---

## US-01: Đăng ký, đăng nhập và phân quyền

| Vai trò                  | Ưu tiên | FR / UC liên quan |
| ------------------------ | ------- | ----------------- |
| Phụ huynh, Gia sư, Admin | Must    | FR-01 / UC-01     |

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

| Vai trò   | Ưu tiên | FR / UC liên quan |
| --------- | ------- | ----------------- |
| Phụ huynh | Must    | FR-02 / UC-02     |

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
| ------- | ------- | ----------------- |
| Gia sư  | Must    | FR-03 / UC-03     |

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
- Hệ thống phải đặt hồ sơ ở trạng thái "Chờ duyệt" sau khi gia sư nộp.
- Hệ thống phải cho gia sư xem trạng thái xác minh và lý do nếu bị từ chối.

### Business Rules

- Hồ sơ cần ít nhất một môn dạy, một cấp lớp và minh chứng cấp 2 trước khi nộp duyệt.
- Thay đổi thông tin xác minh phải được duyệt lại.
- Gia sư không tự gán huy hiệu xác minh.

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

## US-04: Xác minh hồ sơ gia sư

| Vai trò | Ưu tiên | FR / UC liên quan |
| ------- | ------- | ----------------- |
| Admin   | Must    | FR-04 / UC-04     |

### Feature

Xác minh gia sư theo cấp

### User Story

Là một Admin,
tôi muốn duyệt hoặc từ chối hồ sơ và minh chứng của gia sư,
để chỉ những gia sư đáng tin mới được hiển thị cho phụ huynh.

### Context

Gia sư tiếp xúc với trẻ em nên danh tính phải được kiểm tra. Việc xác minh theo cấp được mô tả ở README mục 5.4; MVP duyệt thủ công.

### Functional Requirements

- Hệ thống phải hiển thị cho Admin danh sách hồ sơ "Chờ duyệt".
- Hệ thống phải cho Admin xem hồ sơ và minh chứng của từng gia sư.
- Hệ thống phải cho Admin duyệt hoặc từ chối từng cấp xác minh, từ chối phải kèm lý do.
- Hệ thống phải gắn huy hiệu tương ứng cấp đã duyệt lên hồ sơ gia sư.
- Hệ thống phải chỉ hiển thị gia sư đạt tối thiểu cấp 2 cho phụ huynh.

### Business Rules

- Cấp 2 (danh tính) bắt buộc để hiển thị; cấp 3 (học lực) tùy chọn.
- GPA chỉ hiển thị như huy hiệu, không dùng để xếp hạng.
- Giấy tờ được che hoặc xóa sau khi duyệt xong (NFR-09).

### Scenarios

#### Scenario 1: Duyệt hồ sơ

Given có hồ sơ gia sư ở trạng thái "Chờ duyệt" với minh chứng hợp lệ
When Admin duyệt cấp 2
Then gia sư được hiển thị cho phụ huynh với huy hiệu "Đã xác minh danh tính"

#### Scenario 2: Từ chối hồ sơ

Given minh chứng không rõ hoặc không hợp lệ
When Admin từ chối và nhập lý do
Then gia sư nhận được lý do và có thể nộp lại

### Constraints

- Chỉ Admin xem được giấy tờ định danh và bảng điểm (NFR-09).

### Non-Goals

- Xác minh tự động; kiểm tra lý lịch tư pháp.

### Dependencies

- US-01, US-03.

### Acceptance Criteria

- [ ] Gia sư chưa đạt cấp 2 không xuất hiện với phụ huynh.
- [ ] Từ chối bắt buộc có lý do.
- [ ] Huy hiệu hiển thị đúng với cấp đã duyệt.
- [ ] Giấy tờ chỉ Admin xem được.

---

## US-05: Gợi ý gia sư kèm mức phù hợp

| Vai trò   | Ưu tiên | FR / UC liên quan |
| --------- | ------- | ----------------- |
| Phụ huynh | Must    | FR-05 / UC-05     |

### Feature

Matching bằng AI

### User Story

Là một phụ huynh,
tôi muốn nhận danh sách gia sư được gợi ý kèm mức phù hợp và lý do,
để tôi chọn được gia sư hợp với con mà không phải tự so sánh từng hồ sơ.

### Context

Hồ sơ học sinh đã hoàn thành khảo sát (US-02) và đã có gia sư được duyệt (US-04). Hệ thống lọc bằng luật rồi dùng model AI có sẵn để xếp hạng và giải thích (README mục 5.3).

### Functional Requirements

- Hệ thống phải lọc gia sư theo môn, cấp lớp, hình thức dạy, khu vực, lịch, học phí.
- Hệ thống phải gửi hồ sơ ẩn danh của học sinh và tối đa 20 gia sư cho AI để xếp hạng.
- Hệ thống phải hiển thị tối đa 5 gia sư kèm nhãn mức phù hợp, điểm mạnh, điểm cần lưu ý.
- Hệ thống phải tự chuyển sang chấm điểm theo luật khi AI lỗi hoặc quá 10 giây.

### Business Rules

- Chỉ gia sư đã xác minh tối thiểu cấp 2 mới được đưa vào danh sách.
- Gia sư không đáp ứng điều kiện bắt buộc (môn, cấp lớp) không xuất hiện dù điểm AI cao.
- Mức phù hợp mang tính tương đối, không phải cam kết kết quả học tập.

### Scenarios

#### Scenario 1: Gợi ý thành công

Given hồ sơ học sinh hoàn chỉnh và có ít nhất 3 gia sư thỏa điều kiện lọc
When phụ huynh mở trang gợi ý
Then hệ thống hiển thị 3-5 gia sư theo mức phù hợp giảm dần, mỗi gia sư có nhãn mức phù hợp và lý do

#### Scenario 2: AI lỗi

Given hồ sơ học sinh hợp lệ
When dịch vụ AI không phản hồi trong 10 giây hoặc trả lỗi
Then hệ thống hiển thị gợi ý bằng chấm điểm theo luật, không hiện lỗi kỹ thuật cho phụ huynh

#### Scenario 3: Không có gia sư phù hợp

Given không gia sư nào đã duyệt thỏa điều kiện lọc
When phụ huynh mở trang gợi ý
Then hệ thống thông báo không có kết quả và gợi ý nới điều kiện (khu vực, học phí)

### Constraints

- Chỉ gửi dữ liệu ẩn danh cho nhà cung cấp AI (NFR-07).
- Danh sách lọc hiển thị trong 3 giây, kết quả AI trong tối đa 10 giây (NFR-01).

### Non-Goals

- Huấn luyện model riêng.
- Cho phụ huynh chỉnh trọng số matching.

### Dependencies

- US-02, US-03, US-04; dịch vụ AI bên thứ ba (dev chọn ở STRUCTURAL/).

### Acceptance Criteria

- [ ] Mỗi gợi ý có nhãn mức phù hợp, ít nhất 1 điểm mạnh và 1 điểm cần lưu ý.
- [ ] Khi AI lỗi, phụ huynh vẫn nhận được gợi ý dự phòng (NFR-08).
- [ ] Yêu cầu gửi AI không chứa dữ liệu định danh (NFR-07).
- [ ] Không gia sư chưa xác minh nào xuất hiện trong kết quả.

---

## US-06: Xem hồ sơ chi tiết gia sư

| Vai trò   | Ưu tiên | FR / UC liên quan |
| --------- | ------- | ----------------- |
| Phụ huynh | Must    | FR-06 / UC-06     |

### Feature

Hồ sơ chi tiết gia sư

### User Story

Là một phụ huynh,
tôi muốn xem hồ sơ chi tiết của gia sư được gợi ý,
để tôi yên tâm về năng lực và độ tin cậy trước khi đặt học thử.

### Context

Từ danh sách gợi ý (US-05), phụ huynh cần thêm thông tin để quyết định. Đây là nơi huy hiệu xác minh phát huy giá trị tin cậy (H2).

### Functional Requirements

- Hệ thống phải hiển thị thông tin gia sư: môn dạy, cấp lớp, kinh nghiệm, phương pháp dạy, học phí, lịch rảnh.
- Hệ thống phải hiển thị huy hiệu xác minh và đánh giá từ các buổi học thử (nếu có).
- Hệ thống phải hiển thị mức phù hợp và lý do của gia sư này với hồ sơ học sinh đang chọn.
- Hệ thống phải có nút "Đặt học thử" dẫn tới US-07.

### Business Rules

- Chỉ hiển thị gia sư đã được duyệt.
- Không hiển thị số điện thoại, email riêng của gia sư trước khi gia sư chấp nhận học thử.
- Không hiển thị giấy tờ định danh hay bảng điểm gốc, chỉ hiển thị huy hiệu.

### Scenarios

#### Scenario 1: Xem hồ sơ

Given phụ huynh đang xem danh sách gợi ý
When họ chọn một gia sư
Then hệ thống hiển thị hồ sơ chi tiết kèm huy hiệu xác minh và nút "Đặt học thử"

#### Scenario 2: Gia sư không còn khả dụng

Given gia sư bị tạm ẩn hoặc không còn được duyệt
When phụ huynh mở lại đường dẫn hồ sơ
Then hệ thống thông báo hồ sơ không còn khả dụng

### Constraints

- Giao diện tiếng Việt, hiển thị tốt trên di động (NFR-03).

### Non-Goals

- Chat trực tiếp với gia sư; so sánh nhiều gia sư cạnh nhau.

### Dependencies

- US-04, US-05.

### Acceptance Criteria

- [ ] Hồ sơ hiển thị đủ thông tin và huy hiệu xác minh.
- [ ] Không lộ thông tin liên hệ riêng hay giấy tờ gốc.
- [ ] Có đường dẫn sang đặt học thử.

---

## US-07: Gửi yêu cầu học thử

| Vai trò           | Ưu tiên | FR / UC liên quan |
| ----------------- | ------- | ----------------- |
| Phụ huynh, Gia sư | Must    | FR-07 / UC-07     |

### Feature

Yêu cầu học thử

### User Story

Là một phụ huynh,
tôi muốn gửi yêu cầu học thử cho gia sư tôi chọn và biết gia sư chấp nhận hay từ chối,
để con có buổi học đầu tiên trước khi quyết định học lâu dài.

### Context

Đây là bước chuyển đổi chính của MVP và là cơ sở đo KPI-01, KPI-02. Sau khi gia sư chấp nhận, hai bên liên hệ với nhau ngoài nền tảng vì MVP chưa có chat.

### Functional Requirements

- Hệ thống phải cho phụ huynh gửi yêu cầu học thử kèm hồ sơ học sinh, khung giờ đề xuất và lời nhắn.
- Hệ thống phải thông báo cho gia sư và cho gia sư chấp nhận hoặc từ chối (từ chối có thể kèm lý do).
- Hệ thống phải thông báo kết quả cho phụ huynh.
- Hệ thống phải hiển thị thông tin liên hệ cho hai bên sau khi gia sư chấp nhận.
- Hệ thống phải tự đóng yêu cầu nếu gia sư không phản hồi sau thời hạn quy định.

### Business Rules

- Mỗi hồ sơ học sinh chỉ có một yêu cầu đang chờ với cùng một gia sư.
- Thời hạn phản hồi của gia sư do team chốt (ví dụ vài ngày).
- Thông tin liên hệ chỉ chia sẻ sau khi gia sư chấp nhận.

### Scenarios

#### Scenario 1: Gia sư chấp nhận

Given phụ huynh đã gửi yêu cầu học thử
When gia sư chấp nhận
Then phụ huynh được thông báo và hai bên thấy thông tin liên hệ

#### Scenario 2: Gia sư từ chối

Given phụ huynh đã gửi yêu cầu học thử
When gia sư từ chối
Then phụ huynh được thông báo và có thể chọn gia sư khác trong danh sách gợi ý

#### Scenario 3: Gửi trùng yêu cầu

Given đang có yêu cầu chờ phản hồi giữa hồ sơ học sinh này và gia sư này
When phụ huynh gửi thêm yêu cầu
Then hệ thống từ chối và hiển thị yêu cầu hiện có

### Constraints

- Thông báo qua email hoặc trong ứng dụng; không dùng thông báo đẩy.

### Non-Goals

- Thanh toán, hợp đồng, chat realtime, video call.

### Dependencies

- US-01, US-05, US-06.

### Acceptance Criteria

- [ ] Gửi được yêu cầu, gia sư nhận thông báo và phản hồi được.
- [ ] Phụ huynh thấy trạng thái yêu cầu (chờ, đã chấp nhận, bị từ chối, hết hạn).
- [ ] Thông tin liên hệ chỉ hiện sau khi gia sư chấp nhận.
- [ ] Không gửi trùng yêu cầu đang chờ.
