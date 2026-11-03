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
| [US-05](#us-05-gợi-ý-gia-sư-dạng-thẻ-kèm-mức-phù-hợp-lấy-cảm-hứng-cách-matching-tinder)                 | Gợi ý gia sư kèm mức phù hợp (lấy cảm hứng từ cách ma tinder)                 | Phụ huynh                | Must    | FR-05 |
| [US-06](#us-06-xem-hồ-sơ-chi-tiết-gia-sư)                    | Xem hồ sơ chi tiết gia sư                    | Phụ huynh                | Must    | FR-06 |
| [US-07](#us-07-đặt-lịch-học-thử-sau-khi-match)                          | Gửi yêu cầu học thử                          | Phụ huynh, Gia sư        | Must    | FR-07 |

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

## US-05: Gợi ý gia sư dạng thẻ kèm mức phù hợp (lấy cảm hứng cách matching Tinder)

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh, Gia sư | Must | FR-05 / UC-05 |

### Feature
Matching bằng AI, chọn gia sư dạng thẻ

### User Story
Là một phụ huynh,
tôi muốn xem từng gia sư được gợi ý dưới dạng thẻ kèm mức phù hợp và lý do, rồi chọn "Quan tâm" hoặc "Bỏ qua",
để tôi chọn được gia sư hợp với con mà không phải tự so sánh từng hồ sơ.

### Context
Hồ sơ học sinh đã hoàn thành khảo sát (US-02) và đã có gia sư được duyệt (US-04). Hệ thống lọc bằng luật rồi dùng model AI có sẵn để xếp hạng và giải thích (README mục 5.3).
Cách ghép lấy cảm hứng từ Tinder: hiển thị từng thẻ, chọn nhanh, và chỉ "Match" khi cả hai bên đồng ý. Khác Tinder ở chỗ thẻ gia sư phải có lý do phù hợp và huy hiệu xác minh, không dựa vào ảnh hay ngoại hình, vì đây là quyết định liên quan đến trẻ em.

### Functional Requirements
- Hệ thống phải lọc gia sư theo môn, cấp lớp, hình thức dạy, khu vực, lịch, học phí.
- Hệ thống phải gửi hồ sơ ẩn danh của học sinh và tối đa 20 gia sư cho AI để xếp hạng.
- Hệ thống phải hiển thị gia sư dưới dạng thẻ, mỗi lần một thẻ, theo mức phù hợp giảm dần, mỗi đợt tối đa 5 thẻ.
- Thẻ phải có: môn dạy, huy hiệu xác minh, nhãn mức phù hợp, điểm mạnh, điểm cần lưu ý, và nút "Xem chi tiết" (US-06).
- Hệ thống phải cho phụ huynh chọn "Quan tâm" hoặc "Bỏ qua" cho từng thẻ (bằng nút hoặc vuốt).
- Khi phụ huynh chọn "Quan tâm", hệ thống phải gửi thông báo cho gia sư kèm hồ sơ học sinh ẩn danh, mức phù hợp và lý do.
- Hệ thống phải cho gia sư chấp nhận hoặc từ chối. Khi gia sư chấp nhận, hai bên thành **Match** và chuyển sang đặt học thử (US-07).
- Hệ thống phải lưu danh sách gia sư đã "Quan tâm" để phụ huynh xem lại, và không hiển thị lại gia sư đã "Bỏ qua" cho cùng hồ sơ học sinh.
- Hệ thống phải tự chuyển sang chấm điểm theo luật khi AI lỗi hoặc quá 10 giây.

### Business Rules
- Chỉ gia sư đã xác minh tối thiểu cấp 2 mới được đưa vào danh sách.
- Gia sư không đáp ứng điều kiện bắt buộc (môn, cấp lớp) không xuất hiện dù điểm AI cao.
- Chỉ Match khi cả hai bên đồng ý; chưa Match thì gia sư chỉ thấy hồ sơ học sinh ẩn danh (không tên, trường, địa chỉ), và hai bên không thấy thông tin liên hệ của nhau.
- Thứ tự hiển thị không chỉ theo mức phù hợp: gia sư mới (chưa có đánh giá) phải có cơ hội xuất hiện (cách chia cụ thể do team chốt).
- Ảnh đại diện và ngoại hình không được dùng làm tiêu chí xếp hạng.
- Mức phù hợp mang tính tương đối, không phải cam kết kết quả học tập.

### Scenarios
#### Scenario 1: Match thành công
Given phụ huynh đang xem thẻ của một gia sư
When phụ huynh chọn "Quan tâm" và gia sư chấp nhận
Then hai bên thành Match, thấy thông tin liên hệ của nhau và được chuyển sang đặt học thử

#### Scenario 2: Bỏ qua gia sư
Given phụ huynh đang xem thẻ của một gia sư
When phụ huynh chọn "Bỏ qua"
Then hệ thống chuyển sang thẻ tiếp theo và không hiển thị lại gia sư này cho hồ sơ học sinh đó

#### Scenario 3: Gia sư từ chối
Given phụ huynh đã chọn "Quan tâm" một gia sư
When gia sư từ chối
Then phụ huynh được thông báo và gia sư không còn nằm trong danh sách quan tâm

#### Scenario 4: AI lỗi
Given hồ sơ học sinh hợp lệ
When dịch vụ AI không phản hồi trong 10 giây hoặc trả lỗi
Then hệ thống hiển thị thẻ theo chấm điểm theo luật, không hiện lỗi kỹ thuật cho phụ huynh

#### Scenario 5: Hết thẻ hoặc không có gia sư phù hợp
Given đã xem hết thẻ trong đợt hoặc không gia sư nào thỏa điều kiện lọc
When phụ huynh tiếp tục
Then hệ thống cho xem thêm đợt mới nếu còn, hoặc thông báo không có kết quả và gợi ý nới điều kiện (khu vực, học phí)

### Constraints
- Chỉ gửi dữ liệu ẩn danh cho nhà cung cấp AI (NFR-07).
- Danh sách lọc hiển thị trong 3 giây, kết quả AI trong tối đa 10 giây (NFR-01).
- Hồ sơ học sinh chỉ lộ thông tin định danh sau khi Match (NFR-02).

### Non-Goals
- Huấn luyện model riêng.
- Cho phụ huynh chỉnh trọng số matching.
- Vuốt không giới hạn; chấm điểm hay xếp hạng theo ảnh.
- Chat trước khi Match.

### Dependencies
- US-02, US-03, US-04; dịch vụ AI bên thứ ba (dev chọn ở STRUCTURAL/).
- Chuyển tiếp sang US-06 (xem chi tiết) và US-07 (đặt học thử sau Match).

### Acceptance Criteria
- [ ] Mỗi thẻ có nhãn mức phù hợp, ít nhất 1 điểm mạnh và 1 điểm cần lưu ý.
- [ ] Chỉ Match khi cả phụ huynh và gia sư đồng ý.
- [ ] Trước Match, gia sư không thấy thông tin định danh của học sinh.
- [ ] Gia sư đã "Bỏ qua" không hiện lại cho cùng hồ sơ học sinh.
- [ ] Khi AI lỗi, phụ huynh vẫn nhận được thẻ dự phòng (NFR-08).
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

## US-07: Đặt lịch học thử sau khi Match

| Vai trò | Ưu tiên | FR / UC liên quan |
|---|---|---|
| Phụ huynh, Gia sư | Must | FR-07 / UC-07 |

### Feature
Đặt lịch buổi học thử

### User Story
Là một phụ huynh,
tôi muốn thống nhất lịch buổi học thử với gia sư đã Match,
để con có buổi học đầu tiên trước khi quyết định học lâu dài.

### Context
Hai bên đã Match ở US-05 (phụ huynh chọn "Quan tâm", gia sư chấp nhận) và đã thấy thông tin liên hệ của nhau. Đây là bước chuyển đổi cuối của MVP và là cơ sở đo KPI học thử. MVP chưa có chat và thanh toán, nên buổi học diễn ra ngoài nền tảng, hệ thống chỉ chốt lịch và ghi nhận kết quả.

### Functional Requirements
- Hệ thống phải cho phụ huynh đề xuất từ 1 đến 3 khung giờ học thử và hình thức học (trực tuyến hoặc tại nhà) cho một Match.
- Hệ thống phải thông báo cho gia sư và cho gia sư xác nhận một khung giờ, hoặc đề xuất khung giờ khác.
- Hệ thống phải cho phụ huynh xác nhận hoặc từ chối khung giờ gia sư đề xuất lại.
- Hệ thống phải thông báo cho cả hai bên khi lịch được chốt, đổi hoặc hủy.
- Hệ thống phải hiển thị trạng thái lịch học thử: chờ xác nhận, đã chốt, đã hủy, hết hạn.
- Hệ thống phải cho hai bên hủy hoặc đổi lịch trước giờ học, kèm lý do.
- Hệ thống phải tự đóng lịch nếu một bên không phản hồi sau thời hạn quy định.
- Sau buổi học, hệ thống phải mở lời mời đánh giá ngắn cho phụ huynh (FR-09).

### Business Rules
- Chỉ đặt được lịch học thử với gia sư đã Match (US-05).
- Mỗi Match chỉ có một lịch học thử đang hoạt động tại một thời điểm.
- Thời hạn phản hồi và thời hạn được hủy trước giờ học do team chốt (ví dụ vài ngày, tối thiểu vài giờ).
- Lịch học thử bị hủy hoặc hết hạn thì Match vẫn còn, hai bên có thể đặt lại.

### Scenarios
#### Scenario 1: Chốt lịch thành công
Given phụ huynh và gia sư đã Match
When phụ huynh đề xuất 2 khung giờ và gia sư xác nhận một trong hai
Then lịch học thử được chốt và cả hai bên nhận thông báo

#### Scenario 2: Gia sư đề xuất giờ khác
Given phụ huynh đã đề xuất khung giờ
When gia sư đề xuất khung giờ khác và phụ huynh đồng ý
Then lịch được chốt theo khung giờ mới

#### Scenario 3: Không ai phản hồi
Given lịch đang ở trạng thái chờ xác nhận
When quá thời hạn phản hồi mà một bên chưa trả lời
Then lịch tự chuyển sang hết hạn và hai bên được thông báo, có thể đặt lại

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
- US-01, US-05 (Match là điều kiện bắt buộc), US-06.
- FR-09 (đánh giá sau học thử) nhận đầu vào từ lịch đã hoàn thành.

### Acceptance Criteria
- [ ] Không đặt được lịch với gia sư chưa Match.
- [ ] Mỗi Match chỉ có một lịch học thử đang hoạt động.
- [ ] Cả hai bên đều thấy trạng thái lịch và nhận thông báo khi trạng thái thay đổi.
- [ ] Lịch quá hạn phản hồi tự chuyển sang hết hạn.
- [ ] Hủy lịch bắt buộc có lý do.
