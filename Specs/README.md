# TutorMatch: Nền tảng kết nối gia sư và học sinh có hỗ trợ AI

> **Trạng thái:** Bản nháp v0.4 (MVP gọn) · **Người phụ trách:** Product Owner
> **Cách đọc:** README này đủ để hiểu "làm gì và vì sao". Yêu cầu chi tiết nằm ở [REQUIREMENTS.md](REQUIREMENTS.md).

---

## 1. Tổng quan

TutorMatch là web giúp phụ huynh tìm gia sư (Tiểu học đến THPT) **hợp với cách học của con**, thay vì chỉ ghép theo môn học và khu vực. Hệ thống lọc gia sư theo điều kiện bắt buộc, dùng AI (model có sẵn qua API, không tự huấn luyện) để xếp hạng theo mức phù hợp, rồi giải thích ngắn gọn vì sao. Gia sư mới có chỗ xây hồ sơ minh bạch để tạo niềm tin.

**Giả thuyết MVP cần kiểm chứng:** gợi ý của AI dựa trên nhiều yếu tố (mục tiêu học, phong cách học, tính cách, phương pháp dạy) giúp phụ huynh chọn được gia sư ưng ý hơn cách tìm truyền thống.

## 2. Vấn đề

- **Phụ huynh:** tốn thời gian tìm, khó kiểm chứng gia sư, chỉ biết có hợp con hay không sau nhiều buổi học thử.
- **Học sinh:** được ghép theo môn và khu vực nên dễ gặp cách dạy không hợp.
- **Gia sư mới:** khó chứng minh năng lực, phụ thuộc trung tâm hoặc mạng xã hội.

> **Cần kiểm chứng:** quy mô thị trường chưa có số liệu có nguồn. Nhóm sẽ phỏng vấn 3-5 phụ huynh và gia sư để xác nhận các nỗi đau trên.

## 3. Người dùng và vai trò

| Vai trò | Ai | Cần gì |
|---|---|---|
| **Phụ huynh** | 32-50 tuổi, có con Tiểu học đến THPT _(giả định)_ | Gia sư uy tín, kiên nhẫn, hợp với con |
| **Gia sư** | 18-24 tuổi, sinh viên hoặc mới tốt nghiệp _(giả định)_ | Có học viên phù hợp, xây được uy tín |
| **Admin** | Thành viên vận hành | Đảm bảo hồ sơ gia sư hiển thị là đáng tin |

**Quy ước:** hệ thống có 3 role đăng nhập (Phụ huynh, Gia sư, Admin). **Học sinh không có tài khoản**, chỉ có *hồ sơ học sinh* thuộc tài khoản phụ huynh. Phụ huynh đồng ý việc lưu dữ liệu của con (NFR-04). Học sinh THPT có thể tự làm khảo sát trên tài khoản của phụ huynh.

## 4. Phạm vi MVP

MVP đi trọn một luồng: **hồ sơ → gợi ý → học thử**.

**Trong phạm vi**
1. Đăng ký, đăng nhập, phân quyền theo vai trò.
2. Phụ huynh tạo hồ sơ học sinh (**1 học sinh mỗi phụ huynh** trong MVP), làm khảo sát ngắn và viết mô tả nhu cầu.
3. Gia sư tạo hồ sơ, trả lời bộ câu hỏi ngắn về cách dạy và viết mô tả.
4. Admin xác minh gia sư thủ công trước khi hồ sơ hiển thị.
5. Gợi ý gia sư: lọc bằng luật, xếp hạng bằng AI, có phương án dự phòng.
6. Xem hồ sơ chi tiết gia sư.
7. Gửi yêu cầu học thử, gia sư chấp nhận hoặc từ chối.
8. Đánh giá ngắn sau buổi học thử.

**Ngoài phạm vi (hoãn sau MVP)**
- Thanh toán, hợp đồng điện tử, giải quyết tranh chấp.
- Theo dõi tiến độ học tập, báo cáo cho phụ huynh.
- Chat realtime, video call (người dùng liên hệ ngoài nền tảng sau khi gia sư chấp nhận).
- Ứng dụng mobile riêng, thông báo đẩy, đa ngôn ngữ.
- Huấn luyện model AI riêng, xác minh tự động (eKYC).
- Quản lý lịch học thử trên nền tảng (FR-08), huy hiệu và xếp hạng nâng cao.
- Nhiều hồ sơ học sinh cho một phụ huynh. *Lưu ý cho dev: thiết kế sẵn quan hệ 1-nhiều trong database, MVP chỉ giới hạn ở giao diện, để mở rộng sau không phải làm lại.*

## 5. Cách matching trong MVP

Hệ thống ghép theo 4 bước:

1. **Lọc điều kiện bắt buộc bằng luật (SQL, không dùng AI):** môn, cấp lớp, hình thức dạy, khu vực, lịch, học phí; chỉ gia sư đã xác minh.
2. **Xếp hạng bằng embedding:** hồ sơ học sinh và hồ sơ gia sư (khảo sát và mô tả tự do, đã ẩn danh) được gom thành một đoạn văn rồi chuyển thành vector qua API embedding có sẵn. Vector gia sư tính **một lần** khi hồ sơ được duyệt hoặc sửa; vector học sinh tính khi tạo hoặc sửa hồ sơ. Gợi ý là **top 10** gia sư có độ tương đồng cao nhất.
3. **Giải thích (nên có):** với top 3, gọi LLM sinh điểm mạnh, điểm cần lưu ý và lý do ngắn, chỉ dựa trên nội dung hồ sơ. Phần này hiện sau, không chặn danh sách.
4. **Dự phòng:** nếu API lỗi hoặc quá chậm, hệ thống chấm điểm theo luật (số tiêu chí khớp). Người dùng vẫn thấy danh sách, chỉ thiếu phần giải thích.

**Cách hiển thị:** vì chưa có dữ liệu thật để hiệu chuẩn, "Matching" là **mức phù hợp tương đối**, hiển thị theo **thứ hạng** (top 1-3: *Rất phù hợp*, 4-6: *Phù hợp*, 7-10: *Tạm được*) kèm lý do. Không đặt ngưỡng theo điểm vì điểm tương đồng của embedding thường dồn trong một khoảng hẹp.

**Điểm cần lưu ý:** embedding tìm văn bản *giống nhau*, trong khi ghép gia sư đôi khi cần *bổ sung nhau* (học sinh hay mất tập trung, gia sư kiên nhẫn dạy qua trò chơi). Vì vậy hai đoạn mô tả được viết theo cùng một chủ đề:
- Phụ huynh: *"Con cần gia sư như thế nào?"*
- Gia sư: *"Em dạy tốt nhất với học sinh như thế nào?"*

Nếu cần chính xác hơn, bước sau MVP là cho LLM xếp lại top 10.

Chi tiết kỹ thuật (chọn model, định dạng dữ liệu, lưu trữ vector, điểm dự phòng) do đội phát triển đề xuất tại `STRUCTURAL/`. Với khoảng 500 gia sư, **chưa cần Vector Database**: lưu vector cùng bảng gia sư và tính độ tương đồng trực tiếp trên server.

## 6. Xác minh gia sư

Xác minh theo cấp, hiển thị bằng huy hiệu cho phụ huynh.

| Cấp | Nội dung | Huy hiệu | MVP |
|---|---|---|---|
| 1 | Email, số điện thoại | (cơ bản) | Bắt buộc |
| 2 | Danh tính: Admin đối chiếu thẻ sinh viên hoặc giấy tờ qua cuộc gọi video ngắn, rồi đánh dấu "đã xác minh" | "Đã xác minh danh tính" | **Bắt buộc để được hiển thị** (an toàn cho trẻ em) |
| 3 | Học lực, giải thưởng: gia sư tự khai | "Tự khai" (chưa xác minh) | Tùy chọn |
| 4 | Đánh giá từ buổi học thử | Điểm đánh giá | Có (FR-09) |

- **MVP không lưu ảnh giấy tờ.** Admin chỉ ghi nhận kết quả xác minh, nên giảm hẳn rủi ro lộ dữ liệu nhạy cảm (NFR-09).
- GPA chỉ là huy hiệu, **không** dùng để xếp hạng vì không phản ánh khả năng dạy và dễ khai khống.

## 7. Chỉ số thành công (MVP)

| Mã | Chỉ số | Chỉ tiêu đề xuất |
|---|---|---|
| KPI-01 | Phụ huynh gửi yêu cầu học thử cho ít nhất 1 gia sư trong top 3 | ≥ 50% |
| KPI-03 | Điểm hài lòng trung bình sau học thử (thang 5) | ≥ 4,0 |
| KPI-04 | Thời gian hoàn thành khảo sát | ≤ 5 phút |
| KPI-07 | Lượt gợi ý dùng được AI (không rơi xuống dự phòng) | ≥ 95% |

Các chỉ số còn lại (KPI-02, 05, 06) để sau khi có người dùng thật. Chỉ tiêu sẽ điều chỉnh sau khi thử với 10-20 người.

## 8. Rủi ro chính

| Rủi ro | Hướng giảm thiểu |
|---|---|
| Mức phù hợp chưa được hiệu chuẩn | Hiển thị theo thứ hạng kèm lý do; thu phản hồi sau học thử để chỉnh |
| Dữ liệu trẻ em nhạy cảm, lại đi qua nhà cung cấp AI | Ẩn danh, cảnh báo ở ô mô tả tự do, xin đồng ý rõ ràng (NFR-02, 04, 07) |
| Ít gia sư nên gợi ý nghèo nàn | Chuẩn bị hồ sơ mẫu, mời nhóm gia sư thử nghiệm trước |
| AI chậm, lỗi, thiên lệch hoặc tốn phí | Vector tính một lần và lưu lại; dự phòng bằng luật; kiểm thử tay (NFR-08, 10) |

## 9. Lộ trình gợi ý (nhóm vừa học vừa làm)

Lịch chi tiết và phân công nằm trong `Schedule.md`. Thứ tự đề xuất:

- **Sprint 0 (3-5 ngày):** chạy thử matching trước khi dựng web. Dùng 15-20 gia sư mẫu và vài học sinh mẫu, xem top 3 có hợp lý không. Đây là cách kiểm chứng giả thuyết về mặt kỹ thuật sớm nhất.
- **Sprint 1:** đăng ký, đăng nhập, phân quyền, hồ sơ phụ huynh và gia sư.
- **Sprint 2:** gợi ý gia sư, xem hồ sơ chi tiết, gửi yêu cầu học thử.
- **Sprint 3:** Admin duyệt gia sư, đánh giá sau học thử, giải thích bằng LLM.

## 10. Mục lục tài liệu

- [REQUIREMENTS.md](REQUIREMENTS.md): yêu cầu chức năng, phi chức năng, ràng buộc, thuật ngữ
- [BEHAVIOR/USECASE.md](BEHAVIOR/USECASE.md): các use case
- [BEHAVIOR/SEQUENCE.md](BEHAVIOR/SEQUENCE.md): luồng tương tác theo thời gian
- [BEHAVIOR/USER_STORIES.md](BEHAVIOR/USER_STORIES.md): user story và tiêu chí chấp nhận
- [STRUCTURAL/](STRUCTURAL/): tư vấn kỹ thuật từ đội phát triển
- [USER_VIEW/](USER_VIEW/): mô tả giao diện cho UI
- [Schedule.md](Schedule.md): lịch trình và phân công