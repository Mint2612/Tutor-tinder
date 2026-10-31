# TutorMatch: Nền tảng kết nối gia sư và học sinh có tích hợp hỗ trợ AI

> **Trạng thái tài liệu:** Bản nháp v0.3 (MVP)
> **Người phụ trách:** Product Owner
> **Cách đọc:** Đọc mục 1-6 để hiểu "làm gì và vì sao". Chi tiết từng chức năng nằm ở các file trong mục 10.

---

## 1. Tổng quan

TutorMatch là nền tảng web giúp phụ huynh, học sinh (Tiểu học đến THPT) tìm được gia sư **phù hợp với cách học của học sinh**, thay vì chỉ ghép theo môn học và khu vực. Hệ thống thu thập thông tin về nhu cầu và phong cách học của học sinh, cùng hồ sơ và phương pháp dạy của gia sư, sau đó dùng AI (model có sẵn, không huấn luyện riêng) đánh giá mức độ phù hợp ("Matching") và gợi ý những gia sư tốt nhất kèm lý do dễ hiểu. Đồng thời, nền tảng giúp gia sư, đặc biệt gia sư mới, xây dựng hồ sơ năng lực minh bạch để tạo được niềm tin.

Phiên bản MVP tập trung kiểm chứng một giả thuyết cốt lõi: **ghép theo mức độ phù hợp nhiều yếu tố (phong cách học, mục tiêu học, tính cách, phương pháp dạy) bằng AI giúp người dùng chọn được gia sư ưng ý hơn so với cách tìm truyền thống.**

## 2. Vấn đề hiện tại

Nhu cầu tìm gia sư ở Việt Nam là có thật và phổ biến, nhưng cách tìm hiện nay (nhóm mạng xã hội, giới thiệu truyền miệng, trung tâm gia sư) còn nhiều bất cập:

- **Phụ huynh:** tốn thời gian tìm kiếm, khó kiểm chứng lý lịch và năng lực gia sư, khó biết gia sư có hợp với con hay không cho đến khi học thử nhiều lần.
- **Học sinh:** thường được ghép theo môn học và khu vực nên dễ gặp gia sư có phong cách dạy không hợp (khó hiểu, thiếu hiệu quả), dẫn đến học không tiến bộ.
- **Gia sư:** gia sư mới hoặc độc lập khó xây dựng hình ảnh và chứng minh năng lực; phụ thuộc vào trung tâm hoặc mạng xã hội để tìm học viên.

> **Giả định cần kiểm chứng:** quy mô thị trường và tỷ lệ phụ huynh có nhu cầu thuê gia sư chưa được xác minh bằng nguồn số liệu cụ thể. Nhóm sẽ bổ sung số liệu có dẫn nguồn (tên nguồn, năm) khi có, và kiểm chứng các nỗi đau ở trên bằng phỏng vấn 3-5 phụ huynh và gia sư.

## 3. Tầm nhìn sản phẩm (Product Vision)

**Dành cho** phụ huynh và học sinh (Tiểu học đến THPT) muốn tìm gia sư vừa uy tín vừa hợp với cách học, **và** gia sư muốn có học viên phù hợp cùng cơ hội chứng minh năng lực,
**TutorMatch là** nền tảng web kết nối gia sư với học sinh.
**Khác với** các nhóm mạng xã hội và trung tâm chỉ ghép theo môn học và khu vực,
**TutorMatch** dùng AI gợi ý gia sư dựa trên nhu cầu, phong cách học (nghe, nhìn, thực hành) và tính cách của học sinh, đối chiếu với phương pháp giảng dạy của gia sư, kèm mức phù hợp và lý do dễ hiểu, giúp nâng cao hiệu quả học tập.

## 4. Đối tượng người dùng

| Vai trò                   | Đặc điểm                                                                                                                                                | Nhu cầu chính                                                                               | Nỗi đau hiện tại                                                                              |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| **Học sinh**              | 5-18 tuổi, từ Tiểu học đến THPT                                                                                                                         | Gia sư giảng dễ hiểu, hợp với phong cách học; nhu cầu học từng môn khác nhau                | Gia sư có phương pháp dạy khó hiểu hoặc thiếu hiệu quả                                        |
| **Phụ huynh**             | Khoảng 32-50 tuổi, có con từ Tiểu học đến THPT, phần lớn là nhân viên văn phòng hoặc kinh doanh, thu nhập trung bình khá trở lên _(giả định)_           | Gia sư kiên nhẫn, uy tín, cải thiện được việc học của con                                   | Thiếu thời gian tìm gia sư, lo ngại lý lịch gia sư không rõ ràng, khó nắm tiến độ học của con |
| **Gia sư**                | Khoảng 18-24 tuổi, chủ yếu sinh viên năm hai đến năm cuối hoặc cử nhân mới tốt nghiệp, thành tích học tập tốt, 0-2 năm kinh nghiệm dạy kèm _(giả định)_ | Hợp đồng và thù lao minh bạch, được bảo vệ quyền lợi khi có mâu thuẫn, xây dựng được uy tín | Bị trung tâm cắt phí hoặc lừa đảo, chịu áp lực từ phụ huynh, thiếu công cụ quản lý lịch dạy   |
| **Quản trị viên (Admin)** | Thành viên vận hành của TutorMatch                                                                                                                      | Đảm bảo hồ sơ gia sư hiển thị là đáng tin                                                   | Không có công cụ kiểm duyệt hồ sơ                                                             |

> Các mục đánh dấu _(giả định)_ là suy đoán ban đầu, cần kiểm chứng qua phỏng vấn người dùng.

**Quy ước về tài khoản và vai trò trong MVP:**

- Hệ thống có 3 role đăng nhập: **Phụ huynh**, **Gia sư**, **Admin**.
- Học sinh không có tài khoản riêng. Mỗi học sinh là một **hồ sơ học sinh** thuộc
  tài khoản của phụ huynh. Một phụ huynh có thể tạo nhiều hồ sơ học sinh
  (nhiều con), mỗi hồ sơ có khảo sát và gợi ý gia sư riêng.
- Phụ huynh thao tác chính và đồng ý việc lưu dữ liệu của học sinh (NFR-04).
  Học sinh THPT có thể làm khảo sát phong cách học ngay trên tài khoản phụ huynh;
  hệ thống ghi nhận khảo sát do phụ huynh hay học sinh trả lời.
- Học sinh vẫn là đối tượng người dùng quan trọng (nhu cầu, phong cách học),
  nhưng không phải là một role phân quyền.

## 5. Phạm vi MVP

### 5.1. Trong phạm vi (In-scope)

MVP phải đi trọn một luồng giá trị: **hồ sơ → gợi ý → học thử**.

1. Đăng ký, đăng nhập và phân quyền theo vai trò (Phụ huynh, Gia sư, Admin).
2. Phụ huynh tạo (các) hồ sơ học cho học sinh và làm khảo sát phong cách học.
3. Gia sư tạo hồ sơ, khai báo phương pháp dạy và làm bộ câu hỏi về cách dạy.
4. Xác minh gia sư theo cấp (xem mục 5.4); Admin duyệt thủ công trước khi hồ sơ hiển thị.
5. Hệ thống lọc gia sư bằng luật, dùng AI (model có sẵn qua API) xếp hạng và giải thích mức phù hợp, có phương án dự phòng khi AI lỗi.
6. Xem hồ sơ chi tiết của gia sư.
7. Gửi yêu cầu học thử, gia sư chấp nhận hoặc từ chối.
8. Đánh giá ngắn sau buổi học thử.

### 5.2. Ngoài phạm vi (Out-of-scope)

| Hạng mục                                                       | Lý do hoãn                                                                     |
| -------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Thanh toán trực tuyến, hợp đồng điện tử, giải quyết tranh chấp | Phức tạp về pháp lý và kỹ thuật; chưa cần để kiểm chứng giả thuyết matching    |
| Theo dõi tiến độ học tập, báo cáo cho phụ huynh                | Cần có buổi học thật diễn ra lâu dài; đưa vào giai đoạn sau MVP                |
| Chat realtime, video call tích hợp                             | Người dùng tạm thời liên hệ ngoài nền tảng sau khi gia sư chấp nhận học thử    |
| Ứng dụng mobile riêng (iOS/Android)                            | MVP là web responsive (xem NFR-03)                                             |
| Huấn luyện model AI/Machine Learning riêng                     | MVP dùng model có sẵn qua API; cân nhắc khi có đủ dữ liệu phản hồi sau học thử |
| Xác minh tự động (nhận diện giấy tờ, eKYC)                     | MVP để Admin duyệt thủ công                                                    |
| Hệ thống xếp hạng, huy hiệu, đánh giá nâng cao                 | Chỉ giữ đánh giá ngắn sau học thử                                              |
| Thông báo đẩy (push), đa ngôn ngữ                              | MVP chỉ hỗ trợ tiếng Việt, thông báo qua email hoặc trong ứng dụng             |

### 5.3. Cách tiếp cận matching trong MVP

Hệ thống ghép theo ba bước:

1. **Lọc điều kiện bắt buộc bằng luật (không dùng AI):** môn học, cấp lớp, hình thức dạy (trực tuyến/tại nhà), khu vực, lịch, học phí. Chỉ gia sư đã được xác minh mới tham gia.
2. **AI xếp hạng và giải thích:** hệ thống gửi hồ sơ (đã ẩn thông tin định danh) của Phụ huynh và khoảng 10-20 gia sư còn lại cho model AI có sẵn qua API. AI trả về kết quả có cấu trúc gồm mức phù hợp, điểm mạnh, điểm cần lưu ý và lý do ngắn. AI xét nhiều yếu tố: mục tiêu học, trình độ hiện tại, phong cách học, tính cách, cách giao tiếp, phương pháp dạy.
3. **Dự phòng:** nếu AI lỗi hoặc quá chậm, hệ thống tự chuyển sang chấm điểm theo luật đơn giản để người dùng vẫn nhận được gợi ý.

**Cách hiển thị:** vì chưa có dữ liệu thật để hiệu chuẩn, "Matching" được hiểu là **mức độ phù hợp tương đối**, hiển thị theo nhãn (Rất phù hợp / Phù hợp / Tạm được) kèm điểm và lý do. Độ chính xác sẽ được kiểm chứng bằng phản hồi sau học thử (KPI-02, KPI-03).

Chi tiết kỹ thuật (chọn model, câu lệnh, định dạng đầu ra, lưu tạm kết quả, điểm dự phòng) do đội phát triển đề xuất tại `STRUCTURAL/`.

### 5.4. Xác minh gia sư theo cấp

Thay vì bắt buộc mọi giấy tờ, hồ sơ gia sư được xác minh theo cấp và hiển thị bằng huy hiệu dễ hiểu cho phụ huynh.

| Cấp | Nội dung xác minh                                                          | Huy hiệu hiển thị                             | MVP                                            |
| --- | -------------------------------------------------------------------------- | --------------------------------------------- | ---------------------------------------------- |
| 1   | Email, số điện thoại                                                       | (cơ bản)                                      | Bắt buộc                                       |
| 2   | Danh tính: ảnh thẻ sinh viên hoặc giấy tờ định danh, Admin duyệt           | "Đã xác minh danh tính"                       | Bắt buộc để được hiển thị (an toàn cho trẻ em) |
| 3   | Học lực: bảng điểm/GPA, bằng cấp, chứng chỉ, giải thưởng liên quan môn dạy | "Đã xác minh học lực", "Có giải thưởng môn X" | Tùy chọn, Admin duyệt thủ công                 |
| 4   | Đánh giá từ các buổi học thử                                               | Điểm đánh giá                                 | Có (FR-09)                                     |

Nguyên tắc:

- GPA chỉ hiển thị như một huy hiệu, **không** dùng làm điểm xếp hạng chính vì không phản ánh khả năng dạy và dễ khai khống.
- Giải thưởng, chứng chỉ gắn với môn dạy có giá trị hơn GPA chung.
- Giấy tờ định danh và bảng điểm là dữ liệu nhạy cảm: chỉ Admin được xem, chỉ lưu khi cần, che hoặc xóa sau khi duyệt xong (NFR-09).

## 6. Mục tiêu và chỉ số thành công

### 6.1. Giả thuyết cần kiểm chứng

| Mã  | Giả thuyết                                                                                                                           |
| --- | ------------------------------------------------------------------------------------------------------------------------------------ |
| H1  | Người dùng thấy gia sư do AI gợi ý (theo nhiều yếu tố, trong đó có phong cách học) là "phù hợp" nhiều hơn so với cách chọn thủ công. |
| H2  | Hồ sơ gia sư minh bạch và được duyệt làm phụ huynh yên tâm hơn khi chọn gia sư.                                                      |

### 6.2. Chỉ số đo lường (chỉ tiêu đề xuất, sẽ điều chỉnh sau khi thử nghiệm)

| Mã     | Chỉ số                                                                     | Chỉ tiêu đề xuất | Liên quan         |
| ------ | -------------------------------------------------------------------------- | ---------------- | ----------------- |
| KPI-01 | Tỷ lệ Phụ huynh gửi yêu cầu học thử cho ít nhất 1 gia sư trong top 3 gợi ý | ≥ 50%            | H1                |
| KPI-02 | Tỷ lệ yêu cầu học thử được gia sư chấp nhận                                | ≥ 60%            | H1                |
| KPI-03 | Điểm hài lòng trung bình sau học thử (thang 5)                             | ≥ 4,0            | H1                |
| KPI-04 | Thời gian hoàn thành khảo sát phong cách học                               | ≤ 5 phút         | Khả năng sử dụng  |
| KPI-05 | Tỷ lệ gia sư hoàn thiện hồ sơ sau khi đăng ký                              | ≥ 80%            | H2                |
| KPI-06 | Điểm tin cậy phụ huynh đánh giá về hồ sơ gia sư (thang 5, khảo sát nhanh)  | ≥ 4,0            | H2                |
| KPI-07 | Tỷ lệ lượt gợi ý trả về kết quả AI hợp lệ (không phải dự phòng)            | ≥ 95%            | Độ ổn định của AI |

> Các chỉ tiêu trên là đề xuất ban đầu. Khi thử nghiệm với nhóm nhỏ (ví dụ 10-20 người dùng thử), nhóm sẽ xem lại các con số này cho thực tế.

## 7. Yêu cầu của hệ thống

> Mã UC và US được đánh số theo mã FR tương ứng (FR-01 ↔ UC-01 ↔ US-01) để dễ truy vết. Ưu tiên theo thang MoSCoW: **Must** (bắt buộc), **Should** (nên có), **Could** (có thì tốt).

### 7.1. Yêu cầu chức năng (mức tổng quan)

| Mã    | Yêu cầu                                                                                                                                                                                     | Ưu tiên | Chi tiết tại |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- | ------------ |
| FR-01 | Người dùng đăng ký, đăng nhập và được phân quyền theo vai trò (Phụ huynh, Gia sư, Admin)                                                                                                    | Must    | UC-01, US-01 |
| FR-02 | Phụ huynh tạo hồ sơ học của Học sinh (môn, cấp lớp, hình thức học, khu vực, lịch, học phí), làm khảo sát phong cách học và mô tả nhu cầu bằng văn bản tự do (mục tiêu, điểm yếu, tính cách) | Must    | UC-02, US-02 |
| FR-03 | Gia sư tạo hồ sơ (môn dạy, trình độ, kinh nghiệm), khai báo phương pháp dạy bằng bộ câu hỏi và mô tả tự do, và tải minh chứng xác minh (xem mục 5.4)                                        | Must    | UC-03, US-03 |
| FR-04 | Admin xác minh gia sư theo cấp, duyệt hoặc từ chối minh chứng; chỉ gia sư đạt tối thiểu cấp 2 mới được hiển thị; hồ sơ hiển thị huy hiệu xác minh                                           | Must    | UC-04, US-04 |
| FR-05 | Hệ thống lọc gia sư theo điều kiện bắt buộc, dùng AI xếp hạng và hiển thị mức phù hợp kèm lý do, điểm mạnh, điểm cần lưu ý; tự chuyển sang chấm điểm theo luật khi AI lỗi hoặc quá chậm     | Must    | UC-05, US-05 |
| FR-06 | Phụ huynh xem hồ sơ chi tiết của gia sư                                                                                                                                                     | Must    | UC-06, US-06 |
| FR-07 | Phụ huynh gửi yêu cầu học thử; gia sư chấp nhận hoặc từ chối                                                                                                                                | Must    | UC-07, US-07 |
| FR-08 | Gia sư quản lý lịch học thử (xem, xác nhận khung giờ)                                                                                                                                       | Should  | UC-08, US-08 |
| FR-09 | Phụ huynh đánh giá ngắn sau buổi học thử                                                                                                                                                    | Should  | UC-09, US-09 |
| FR-10 | Phụ huynh tìm và lọc gia sư thủ công theo môn, cấp lớp, khu vực                                                                                                                             | Could   | UC-10, US-10 |

### 7.2. Yêu cầu phi chức năng

| Mã     | Nhóm                       | Yêu cầu (đo được)                                                                                                                                                                                                |
| ------ | -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-01 | Hiệu năng                  | Danh sách gia sư sau bước lọc hiển thị trong 3 giây với 500 hồ sơ gia sư, trên kết nối 4G thông thường; mức phù hợp và lý do do AI hiển thị trong tối đa 10 giây (có thể hiển thị dần hoặc dùng kết quả lưu tạm) |
| NFR-02 | Bảo mật, quyền riêng tư    | Dữ liệu học sinh dưới 18 tuổi chỉ phụ huynh và gia sư đã được ghép mới xem được                                                                                                                                  |
| NFR-03 | Tương thích                | Giao diện tiếng Việt, chạy tốt trên Chrome, Edge, Firefox, Safari bản mới; hiển thị đúng từ màn hình rộng 360px trở lên (responsive web)                                                                         |
| NFR-04 | Quyền riêng tư             | Phụ huynh phải đồng ý rõ ràng trước khi hệ thống lưu thông tin và kết quả khảo sát của học sinh                                                                                                                  |
| NFR-05 | Khả năng sử dụng           | Khảo sát phong cách học có tối đa 15 câu, hoàn thành trong dưới 5 phút                                                                                                                                           |
| NFR-06 | Bảo mật                    | Mật khẩu được mã hóa khi lưu; phiên đăng nhập hết hạn sau thời gian không hoạt động                                                                                                                              |
| NFR-07 | Quyền riêng tư khi dùng AI | Chỉ gửi cho nhà cung cấp AI dữ liệu đã ẩn danh (không gửi tên, trường, địa chỉ, giấy tờ); thông báo đồng ý của phụ huynh nêu rõ việc dùng AI                                                                     |
| NFR-08 | Độ tin cậy                 | Khi AI lỗi hoặc quá thời gian chờ (tối đa 10 giây), hệ thống tự chuyển sang chấm điểm theo luật; tỷ lệ gợi ý thất bại hoàn toàn dưới 1%                                                                          |
| NFR-09 | Dữ liệu xác minh           | Giấy tờ định danh và bảng điểm chỉ Admin xem được, được che hoặc xóa sau khi duyệt xong                                                                                                                          |
| NFR-10 | Chất lượng AI              | Kiểm tra mẫu để kết quả gợi ý không bị lệch theo yếu tố không liên quan (giới tính, tên, trường); cùng một đầu vào cho kết quả tương đối ổn định                                                                 |

## 8. Ràng buộc và giả định

**Ràng buộc**

- Thời gian thực hiện theo lịch trong `Schedule.md`.
- Nhân sự là đội sinh viên theo mô hình Scrum; ưu tiên công nghệ đội đã quen thuộc.
- Chưa có dữ liệu người dùng thật; cần tạo dữ liệu mẫu để kiểm thử gợi ý.
- Chi phí gọi API AI nằm trong ngân sách giới hạn; cần ước lượng số lần gọi mỗi lượt gợi ý.

**Giả định**

- Dùng model AI có sẵn qua API (kèm bước lọc bằng luật) là đủ để kiểm chứng giả thuyết H1, không cần dữ liệu huấn luyện.
- Người dùng chấp nhận liên hệ ngoài nền tảng sau khi gia sư đồng ý học thử.
- Phụ huynh sẵn sàng làm khảo sát khoảng 5 phút thay cho học sinh nhỏ tuổi.
- Gia sư sẵn sàng cung cấp ảnh thẻ sinh viên hoặc giấy tờ định danh để được duyệt hồ sơ.

**Rủi ro chính**
| Rủi ro | Hướng giảm thiểu |
|---|---|
| Mức phù hợp do AI đưa ra chưa được hiệu chuẩn, phong cách học chỉ là một trong nhiều yếu tố | Hiển thị theo nhãn kèm lý do; xét nhiều yếu tố ngoài phong cách học; thu phản hồi sau học thử để kiểm chứng và chỉnh câu lệnh |
| Dữ liệu trẻ em nhạy cảm, lại đi qua nhà cung cấp AI | Áp dụng NFR-02, NFR-04, NFR-07 ngay từ đầu |
| Ít gia sư trong hệ thống nên gợi ý nghèo nàn | Chuẩn bị hồ sơ mẫu, mời nhóm gia sư thử nghiệm trước khi mở cho Người học |
| AI trả kết quả không ổn định, thiên lệch, chậm hoặc tốn phí | Kết quả có cấu trúc, lưu tạm, giới hạn số gia sư gửi cho AI, dự phòng bằng luật (NFR-08, NFR-10) |
| Giấy tờ xác minh bị lộ hoặc bị khai khống | Chỉ Admin xem, xóa sau duyệt (NFR-09); GPA chỉ là huy hiệu, không quyết định xếp hạng |

## 9. Thuật ngữ

| Thuật ngữ             | Giải thích                                                                                      |
| --------------------- | ----------------------------------------------------------------------------------------------- |
| Hồ sơ học sinh        | Thông tin học tập, khảo sát và nhu cầu của một học sinh, thuộc tài khoản phụ huynh              |
| Matching              | Mức độ phù hợp tương đối giữa người học và gia sư, do AI đánh giá                               |
| Phong cách học        | Cách học sinh tiếp thu tốt nhất: nghe, nhìn hoặc thực hành                                      |
| Phương pháp dạy       | Cách gia sư truyền đạt kiến thức, khai báo trong hồ sơ                                          |
| Buổi học thử          | Buổi học đầu tiên để Người học và gia sư làm quen trước khi quyết định học lâu dài              |
| MVP                   | Phiên bản sản phẩm tối thiểu, đủ để kiểm chứng giả thuyết cốt lõi                               |
| Model AI có sẵn (LLM) | Mô hình ngôn ngữ lớn do bên thứ ba cung cấp qua API, dùng trực tiếp mà không cần huấn luyện lại |
| Cấp xác minh          | Mức độ kiểm chứng thông tin của gia sư (1 đến 4), hiển thị bằng huy hiệu                        |
| Dự phòng (fallback)   | Cách xử lý thay thế tự động khi AI lỗi hoặc quá chậm                                            |

## 10. Mục lục tài liệu

- [BEHAVIOR/USECASE.md](BEHAVIOR/USECASE.md): các use case
- [BEHAVIOR/SEQUENCE.md](BEHAVIOR/SEQUENCE.md): luồng tương tác theo thời gian
- [BEHAVIOR/USER_STORIES.md](BEHAVIOR/USER_STORIES.md): user story và tiêu chí chấp nhận
- [STRUCTURAL/](STRUCTURAL/): tư vấn kỹ thuật từ đội phát triển
- [USER_VIEW/](USER_VIEW/): mô tả giao diện cho UI
- [Schedule.md](Schedule.md): lịch trình và phân công
