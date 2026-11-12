# TutorMatch: Nền tảng kết nối gia sư và học sinh có hỗ trợ AI

> **Trạng thái:** Bản nháp v0.5 (MVP gọn, ghép hai chiều) · **Người phụ trách:** Product Owner
> **Cách đọc:** README này đủ để hiểu "làm gì và vì sao". Yêu cầu chi tiết nằm ở [Requirement.md](Requirement.md), câu chuyện người dùng ở [USER_STORIES.md](BEHAVIOR%20%28%20NON%20TECH%20%29/USER_STORIES.md).

---

## 1. Tổng quan

TutorMatch là web giúp phụ huynh và gia sư tìm được nhau, với gia sư **hợp với cách học của con** (Tiểu học đến THPT), thay vì chỉ ghép theo môn học và khu vực.

Hệ thống gợi ý cho **cả hai phía**: phụ huynh thấy thẻ gia sư, gia sư thấy thẻ hồ sơ học sinh (ẩn danh). Mỗi bên chọn OK hoặc Bỏ qua; chỉ khi cả hai cùng OK mới là **Match**. Sau đó hai bên xem hồ sơ chi tiết, xác nhận một lần rồi mới trao đổi liên hệ và đặt lịch học thử. Cách ghép lấy cảm hứng từ Tinder, để gia sư chủ động và được bảo vệ quyền lợi, không chỉ chờ phụ huynh chọn.

Gợi ý được tạo bằng cách lọc điều kiện bắt buộc, rồi xếp hạng bằng AI (embedding, model có sẵn qua API, không tự huấn luyện) và giải thích ngắn gọn vì sao phù hợp.

**Giả thuyết MVP cần kiểm chứng:** gợi ý của AI dựa trên nhiều yếu tố (mục tiêu học, phong cách học, tính cách, phương pháp dạy), kết hợp việc hai bên cùng chọn, giúp phụ huynh và gia sư tìm được nhau ưng ý hơn cách tìm truyền thống.

## 2. Vấn đề

- **Phụ huynh:** tốn thời gian tìm, khó kiểm chứng gia sư, chỉ biết có hợp con hay không sau nhiều buổi học thử.
- **Học sinh:** được ghép theo môn và khu vực nên dễ gặp cách dạy không hợp.
- **Gia sư mới:** khó chứng minh năng lực, phụ thuộc trung tâm hoặc mạng xã hội, và thường ở thế bị động chờ phụ huynh chọn.

> **Cần kiểm chứng:** quy mô thị trường chưa có số liệu có nguồn. Nhóm sẽ phỏng vấn 3-5 phụ huynh và gia sư để xác nhận các nỗi đau trên.

## 3. Người dùng và vai trò

| Vai trò | Ai | Cần gì |
|---|---|---|
| **Phụ huynh** | 32-50 tuổi, có con Tiểu học đến THPT _(giả định)_ | Gia sư uy tín, kiên nhẫn, hợp với con |
| **Gia sư** | 18-24 tuổi, sinh viên hoặc mới tốt nghiệp _(giả định)_ | Có học viên phù hợp, được chủ động chọn, xây được uy tín |
| **Admin** | Thành viên vận hành | Đảm bảo hồ sơ gia sư hiển thị là đáng tin |

**Quy ước:** hệ thống có 3 role đăng nhập (Phụ huynh, Gia sư, Admin). **Học sinh không có tài khoản**, chỉ có *hồ sơ học sinh* thuộc tài khoản phụ huynh. Mỗi học sinh là một hồ sơ độc lập; MVP cho mỗi phụ huynh tạo 1 hồ sơ. Phụ huynh đồng ý việc lưu dữ liệu của con (NFR-04). Học sinh THPT có thể tự làm khảo sát trên tài khoản của phụ huynh.

## 4. Phạm vi MVP

MVP đi trọn một luồng: **hồ sơ → xác minh gia sư → gợi ý hai chiều → Match → xác nhận kết nối → học thử**.

**Trong phạm vi**
1. Đăng ký, đăng nhập, phân quyền theo vai trò.
2. Phụ huynh tạo hồ sơ học sinh (**1 học sinh mỗi phụ huynh** trong MVP), làm khảo sát ngắn (6-8 câu) và viết mô tả nhu cầu.
3. Gia sư tạo hồ sơ, trả lời bộ câu hỏi ngắn về cách dạy (6-8 câu) và viết mô tả.
4. Admin xác minh danh tính gia sư thủ công qua cuộc gọi video trước khi hồ sơ hiển thị.
5. Gợi ý hai chiều dạng thẻ: phụ huynh và gia sư cùng chọn OK hoặc Bỏ qua; cả hai OK mới là Match.
6. Sau Match, xem hồ sơ chi tiết và xác nhận kết nối một lần; khi cả hai xác nhận thì trao đổi liên hệ.
7. Chốt lịch một buổi học thử.
8. Đánh giá ngắn sau buổi học thử.

**Ngoài phạm vi (hoãn sau MVP)**
- Thanh toán, hợp đồng điện tử, giải quyết tranh chấp.
- Theo dõi tiến độ học tập, báo cáo cho phụ huynh.
- Chat realtime, video call tích hợp (sau khi kết nối, hai bên liên hệ và học ngoài nền tảng).
- Ứng dụng mobile riêng, thông báo đẩy, đa ngôn ngữ.
- Huấn luyện model AI riêng, xác minh tự động (eKYC).
- Quản lý lịch dạy tổng thể của gia sư trên nền tảng (FR-08), huy hiệu và xếp hạng nâng cao.
- Nhiều hồ sơ học sinh cho một phụ huynh. *Lưu ý cho dev: thiết kế sẵn quan hệ 1-nhiều trong database, MVP chỉ giới hạn ở giao diện, để mở rộng sau không phải làm lại.*

## 5. Cách matching trong MVP

### 5.1. Luồng ghép hai chiều

Một **cặp** là (hồ sơ học sinh, gia sư). Cặp đi qua các trạng thái:

`Đề xuất → Quan tâm bạn (một bên OK) → Match (cả hai OK) → Đã kết nối (cả hai xác nhận) → Học thử`

Cặp bị đóng khi một bên Bỏ qua, Từ chối hoặc hết hạn.

- **Hai tag trên thẻ:** *Đề xuất* (hệ thống gợi ý, chưa bên nào chọn) và *Quan tâm bạn* (bên kia đã chọn OK, thẻ được đưa lên đầu danh sách).
- **Bỏ qua** ẩn cặp khỏi danh sách của cả hai bên và không báo lý do cho bên kia.
- **Thông tin hiển thị theo cấp:** sơ lược trên thẻ (trước Match, học sinh luôn ẩn danh, thông tin phụ huynh rất ít) → hồ sơ chi tiết (sau Match) → liên hệ (sau khi cả hai xác nhận; phía học sinh chỉ có liên hệ của phụ huynh).
- **Giới hạn chống OK hàng loạt** (mặc định, team chỉnh được): mỗi hồ sơ tối đa 3 lượt OK vào thẻ "Quan tâm bạn" và 10 lượt OK mỗi ngày; thẻ "Quan tâm bạn" hết hạn sau 7 ngày; xác nhận sau Match trong 3 ngày.

### 5.2. Cách xếp hạng

Mỗi bên có danh sách riêng, được tạo theo 4 bước:

1. **Lọc điều kiện bắt buộc của cả hai bên bằng luật (SQL, không dùng AI):** môn, cấp lớp, hình thức dạy, khu vực, lịch, học phí; chỉ gia sư đã xác minh và đang bật hiển thị; loại các cặp đã Bỏ qua hoặc đã đóng.
2. **Xếp hạng bằng embedding:** hồ sơ học sinh và hồ sơ gia sư (khảo sát và mô tả tự do, đã ẩn danh) được gom thành một đoạn văn rồi chuyển thành vector qua API embedding có sẵn. Vector gia sư tính **một lần** khi hồ sơ được duyệt hoặc sửa; vector học sinh tính khi tạo hoặc sửa hồ sơ. Độ tương đồng là **một giá trị chung cho cả cặp**. Mỗi danh sách lấy **top 10**; thẻ "Quan tâm bạn" luôn có mặt trong danh sách.
3. **Giải thích (nên có):** với top 3 của mỗi danh sách, gọi LLM sinh điểm mạnh, điểm cần lưu ý và lý do ngắn, chỉ dựa trên nội dung hồ sơ. Phần này hiện sau, không chặn danh sách.
4. **Dự phòng:** nếu API lỗi hoặc quá chậm, hệ thống xếp hạng theo luật (số tiêu chí khớp). Người dùng vẫn thấy danh sách, chỉ thiếu phần giải thích.

**Cách hiển thị:** vì chưa có dữ liệu thật để hiệu chuẩn, mức phù hợp là **tương đối**, hiển thị theo **thứ hạng trong danh sách của người xem** (1-3: *Rất phù hợp*, 4-6: *Phù hợp*, từ 7: *Tạm được*) kèm lý do nếu có. Vì xếp hạng tính trong từng danh sách, hai bên có thể thấy nhãn khác nhau cho cùng một cặp. Không đặt ngưỡng theo điểm vì điểm tương đồng của embedding thường dồn trong một khoảng hẹp.

**Điểm cần lưu ý:** embedding tìm văn bản *giống nhau*, trong khi ghép gia sư đôi khi cần *bổ sung nhau* (học sinh hay mất tập trung, gia sư kiên nhẫn dạy qua trò chơi). Vì vậy hai đoạn mô tả được viết theo cùng một chủ đề:
- Phụ huynh: *"Con cần gia sư như thế nào?"*
- Gia sư: *"Em dạy tốt nhất với học sinh như thế nào?"*

Nếu cần chính xác hơn, bước sau MVP là cho LLM xếp lại top 10.

**Thứ tự triển khai:** theo `sprint-tracking.md`, Sprint 4 chạy trọn luồng ghép hai chiều với xếp hạng theo luật (cũng là phương án dự phòng); embedding và giải thích bằng LLM là story AI bonus của Sprint 6. Xem rủi ro tương ứng ở mục 8.

Chi tiết kỹ thuật (chọn model, định dạng dữ liệu, lưu trữ vector, điểm dự phòng) do đội phát triển đề xuất tại `STRUCTURAL/`. Với khoảng 500 gia sư, **chưa cần Vector Database**: lưu vector cùng bảng gia sư và học sinh, tính độ tương đồng trực tiếp trên server.

## 6. Xác minh gia sư

Xác minh theo cấp, hiển thị bằng huy hiệu cho phụ huynh. Gia sư gửi yêu cầu xác minh kèm khung giờ rảnh; Admin gọi video để đối chiếu.

| Cấp | Nội dung | Huy hiệu | MVP |
|---|---|---|---|
| 1 | Email, số điện thoại | (cơ bản) | Bắt buộc |
| 2 | Danh tính: Admin đối chiếu thẻ sinh viên hoặc giấy tờ (CCCD, CMND) qua cuộc gọi video ngắn, rồi đánh dấu "đã xác minh" | "Đã xác minh danh tính" | **Bắt buộc để được hiển thị** (an toàn cho trẻ em) |
| 3 | Học lực, giải thưởng: gia sư tự khai | "Tự khai" (chưa xác minh) | Tùy chọn |
| 4 | Đánh giá từ buổi học thử | Điểm đánh giá | Có (FR-09) |

- **MVP không lưu ảnh giấy tờ.** Admin chỉ ghi nhận kết quả xác minh, nên giảm hẳn rủi ro lộ dữ liệu nhạy cảm (NFR-09).
- GPA chỉ là huy hiệu, **không** dùng để xếp hạng vì không phản ánh khả năng dạy và dễ khai khống.

## 7. Chỉ số thành công (MVP)

| Mã | Chỉ số | Chỉ tiêu đề xuất |
|---|---|---|
| KPI-01 | Phụ huynh chọn OK ít nhất 1 gia sư trong top 3 | ≥ 50% |
| KPI-03 | Điểm hài lòng trung bình sau học thử (thang 5) | ≥ 4,0 |
| KPI-04 | Thời gian hoàn thành khảo sát | ≤ 5 phút |
| KPI-07 | Lượt gợi ý dùng được embedding (không rơi xuống dự phòng) | ≥ 95% |

Các chỉ số còn lại (KPI-02: tỷ lệ OK của một bên được bên kia OK lại thành Match; KPI-05, 06) để sau khi có người dùng thật. Chỉ tiêu sẽ điều chỉnh sau khi thử với 10-20 người. KPI-07 chỉ đo được sau khi bật embedding.

## 8. Rủi ro chính

| Rủi ro | Hướng giảm thiểu |
|---|---|
| Mức phù hợp chưa được hiệu chuẩn | Hiển thị theo thứ hạng kèm lý do; thu phản hồi sau học thử để chỉnh |
| Dữ liệu trẻ em nhạy cảm, lại đi qua nhà cung cấp AI | Ẩn danh, cảnh báo ở ô mô tả tự do, xin đồng ý rõ ràng (NFR-02, 04, 07) |
| Ít gia sư và học sinh nên danh sách hai chiều thưa, khó có Match | Chuẩn bị hồ sơ mẫu cho cả hai phía, mời nhóm thử nghiệm trước |
| OK hàng loạt làm Match quá dày | Giới hạn lượt OK theo hồ sơ và thời hạn thẻ (mục 5.1) |
| AI chậm, lỗi, thiên lệch hoặc tốn phí | Vector tính một lần và lưu lại; dự phòng bằng luật; kiểm thử tay (NFR-08, 10) |
| Embedding là story bonus Sprint 6, nếu không kịp thì giả thuyết về AI chưa được kiểm chứng | Chạy thử embedding sớm trên 15-20 hồ sơ mẫu ngoài sprint (mục 9); nếu bonus không kịp, ghi rõ trong Review rằng demo đang dùng xếp hạng theo luật |

## 9. Lộ trình

Lịch chi tiết, story theo từng sprint và điều kiện Done nằm trong `sprint-tracking.md`; phân công nằm trong `Schedule.md`. Tóm tắt 6 sprint:

| Sprint | Mục tiêu |
|---|---|
| 1 | Đăng ký, xác thực, đăng nhập, khôi phục tài khoản đúng vai trò |
| 2 | Hồ sơ học sinh, khảo sát, hồ sơ gia sư |
| 3 | Admin xác minh danh tính gia sư; chỉ gia sư đã xác minh mới hiển thị |
| 4 | Gợi ý hai chiều dạng thẻ, OK/Bỏ qua, Match |
| 5 | Xem chi tiết, xác nhận kết nối, trao đổi liên hệ, chốt lịch học thử |
| 6 | Hoàn thiện trên máy tính và điện thoại; bonus: embedding và giải thích bằng LLM |

**Việc nên làm trước Sprint 4 (ngoài sprint, 3-5 ngày):** chạy thử embedding bằng 15-20 gia sư mẫu và vài học sinh mẫu, xem top 3 có hợp lý không. Đây là cách kiểm chứng giả thuyết về mặt kỹ thuật sớm nhất, và giúp quyết định có nên đưa embedding lên trước Sprint 6 hay không.

## 10. Mục lục tài liệu

- [Requirement.md](Requirement.md): yêu cầu chức năng, phi chức năng, ràng buộc, thuật ngữ
- [BEHAVIOR/USECASE.md](BEHAVIOR%20%28%20NON%20TECH%20%29/USECASE.md): các use case
- [BEHAVIOR/SEQUENCE.md](BEHAVIOR%20%28%20NON%20TECH%20%29/SEQUENCE.md): luồng tương tác theo thời gian
- [BEHAVIOR/USER_STORIES.md](BEHAVIOR%20%28%20NON%20TECH%20%29/USER_STORIES.md): user story và tiêu chí chấp nhận
- [STRUCTURAL/](STRUCTURAL%20%28%20TECH%20ADVISE%20%29/): tư vấn kỹ thuật từ đội phát triển
- [USER_VIEW/](USER_VIEW%20%28%20FOR%20UI%20%29/): mô tả giao diện cho UI
- [Schedule.md](Schedule.md): lịch trình và phân công
- `sprint-tracking.md`: theo dõi 6 sprint, story và Retrospective