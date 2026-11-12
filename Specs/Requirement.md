# TutorMatch: Yêu cầu hệ thống

> **Trạng thái:** Bản nháp v0.4 (MVP gọn). Tổng quan và lý do nằm ở [README.md](README.md).
> **Quy ước mã:** giữ nguyên mã FR, NFR, KPI của v0.3 để không vỡ liên kết với `BEHAVIOR/` (FR-01 ↔ UC-01 ↔ US-01). Mục nào hoãn thì ghi trạng thái **Hoãn**, không đánh số lại.
> **Ưu tiên (MoSCoW):** **Must** bắt buộc · **Should** nên có · **Could** có thì tốt · **Hoãn** để sau MVP.

---

## 1. Yêu cầu chức năng

| Mã | Yêu cầu | Ưu tiên | Chi tiết tại |
|---|---|---|---|
| FR-01 | Đăng ký, đăng nhập, phân quyền theo vai trò (Phụ huynh, Gia sư, Admin) | Must | UC-01, US-01 |
| FR-02 | Phụ huynh tạo hồ sơ học sinh (môn, cấp lớp, hình thức học, khu vực, lịch, học phí), làm khảo sát phong cách học (6-8 câu) và viết mô tả *"Con cần gia sư như thế nào?"*. MVP: 1 học sinh mỗi phụ huynh | Must | UC-02, US-02 |
| FR-03 | Gia sư tạo hồ sơ (môn dạy, trình độ, kinh nghiệm), trả lời bộ câu hỏi về cách dạy (6-8 câu) và viết mô tả *"Em dạy tốt nhất với học sinh như thế nào?"* | Must | UC-03, US-03 |
| FR-04 | Admin xác minh danh tính gia sư thủ công và đánh dấu kết quả; chỉ gia sư đạt cấp 2 mới được hiển thị; hồ sơ hiển thị huy hiệu xác minh. Thông tin học lực do gia sư tự khai, hiển thị là "Tự khai" | Must | UC-04, US-04 |
| FR-05 | Gợi ý gia sư: lọc điều kiện bắt buộc bằng luật, xếp hạng top 10 bằng embedding, hiển thị nhãn theo thứ hạng. Tự chuyển sang chấm điểm theo luật khi API lỗi hoặc quá chậm. *Nên có:* giải thích điểm mạnh, điểm cần lưu ý cho top 3 bằng LLM | Must (giải thích: Should) | UC-05, US-05 |
| FR-06 | Phụ huynh xem hồ sơ chi tiết của gia sư | Must | UC-06, US-06 |
| FR-07 | Phụ huynh gửi yêu cầu học thử; gia sư chấp nhận hoặc từ chối | Must | UC-07, US-07 |
| FR-08 | Gia sư quản lý lịch học thử trên nền tảng | **Hoãn** (liên hệ ngoài nền tảng sau khi chấp nhận) | UC-08, US-08 |
| FR-09 | Phụ huynh đánh giá ngắn sau buổi học thử | Should | UC-09, US-09 |
| FR-10 | Phụ huynh tìm và lọc gia sư thủ công theo môn, cấp lớp, khu vực | Could (dùng lại bộ lọc SQL của FR-05) | UC-10, US-10 |

## 2. Yêu cầu phi chức năng

| Mã | Nhóm | Yêu cầu (đo được) | Ưu tiên |
|---|---|---|---|
| NFR-01 | Hiệu năng | Danh sách gợi ý (lọc và xếp hạng) hiển thị trong 3 giây với 500 hồ sơ gia sư trên 4G thông thường. Phần giải thích bằng LLM hiện sau, tối đa 10 giây, không chặn danh sách | Should |
| NFR-02 | Quyền riêng tư | Dữ liệu học sinh dưới 18 tuổi chỉ phụ huynh và gia sư đã được ghép mới xem được | Must |
| NFR-03 | Tương thích | Giao diện tiếng Việt, chạy tốt trên Chrome, Edge, Firefox, Safari bản mới; responsive từ 360px | Should |
| NFR-04 | Quyền riêng tư | Phụ huynh đồng ý rõ ràng trước khi hệ thống lưu thông tin và kết quả khảo sát của học sinh | Must |
| NFR-05 | Khả năng sử dụng | Khảo sát tối đa 8 câu cộng 1 ô mô tả tự do, hoàn thành dưới 5 phút | Should |
| NFR-06 | Bảo mật | Mật khẩu được mã hóa khi lưu; phiên đăng nhập hết hạn sau thời gian không hoạt động | Must |
| NFR-07 | Quyền riêng tư khi dùng AI | Chỉ gửi nhà cung cấp AI dữ liệu đã ẩn danh (không tên, trường, địa chỉ, số điện thoại, email). **Áp dụng cả cho embedding**, vì văn bản hồ sơ cũng được gửi lên API. Ô mô tả tự do có cảnh báo "đừng ghi thông tin định danh" và hệ thống lọc tự động số điện thoại, email trước khi gửi. Thông báo đồng ý nêu rõ việc dùng AI | Must |
| NFR-08 | Độ tin cậy | Khi API embedding lỗi hoặc quá 10 giây, tự chuyển sang chấm điểm theo luật; gợi ý thất bại hoàn toàn dưới 1%. Hồ sơ gia sư chưa tạo được vector thì chưa hiển thị, hệ thống tự thử lại | Must |
| NFR-09 | Dữ liệu xác minh | MVP **không lưu ảnh giấy tờ định danh hay bảng điểm**. Admin xem khi xác minh, chỉ lưu kết quả (đã xác minh hay chưa). Nếu sau này cần lưu thì chỉ Admin xem được và xóa sau khi duyệt | Must |
| NFR-10 | Chất lượng AI | Kiểm thử tay (checklist), không cần tự động hóa: (a) đổi tên, giới tính, trường trong hồ sơ mẫu thì top 3 không đổi; (b) chạy cùng đầu vào 3 lần ra cùng thứ tự | Should |
NFR-11 | Khả năng sử dụng | Mọi màn hình chính có trạng thái tải, trống và lỗi; thông báo bằng tiếng Việt dễ hiểu; chặn gửi lặp khi đang xử lý.| Must |

## 3. Chỉ số đo lường

| Mã | Chỉ số | Chỉ tiêu đề xuất | Trạng thái |
|---|---|---|---|
| KPI-01 | Phụ huynh gửi yêu cầu học thử cho ít nhất 1 gia sư trong top 3 | ≥ 50% | Giữ |
| KPI-02 | Yêu cầu học thử được gia sư chấp nhận | ≥ 60% | Hoãn |
| KPI-03 | Điểm hài lòng trung bình sau học thử (thang 5) | ≥ 4,0 | Giữ |
| KPI-04 | Thời gian hoàn thành khảo sát | ≤ 5 phút | Giữ |
| KPI-05 | Gia sư hoàn thiện hồ sơ sau khi đăng ký | ≥ 80% | Hoãn |
| KPI-06 | Điểm tin cậy phụ huynh đánh giá về hồ sơ gia sư | ≥ 4,0 | Hoãn |
| KPI-07 | Lượt gợi ý dùng được AI, không rơi xuống dự phòng | ≥ 95% | Giữ |

## 4. Ràng buộc và giả định

**Ràng buộc**
- Thời gian theo `Schedule.md`.
- Nhân sự là đội sinh viên theo mô hình Scrum, ưu tiên công nghệ đã quen và học dần trong lúc làm.
- Chưa có dữ liệu thật; cần tạo hồ sơ mẫu để kiểm thử gợi ý.
- Chi phí API AI trong ngân sách giới hạn. Vector gia sư tính một lần và lưu lại; mỗi lượt gợi ý chỉ tốn một lần gọi embedding cho phía học sinh (cộng một lần gọi LLM nếu bật giải thích).

**Giả định**
- Model có sẵn qua API là đủ để kiểm chứng giả thuyết, không cần dữ liệu huấn luyện.
- Người dùng chấp nhận liên hệ ngoài nền tảng sau khi gia sư đồng ý học thử.
- Phụ huynh sẵn sàng làm khảo sát khoảng 5 phút thay cho học sinh nhỏ tuổi.
- Gia sư sẵn sàng xác minh danh tính qua cuộc gọi video ngắn.

**Lưu ý về dữ liệu và nhà cung cấp AI:** gói miễn phí và gói trả phí của một số nhà cung cấp có điều khoản dữ liệu khác nhau. Đọc điều khoản hiện hành trước khi dùng. MVP chỉ dùng dữ liệu mẫu thì ổn; trước khi nhận người dùng thật, nên dùng gói trả phí và tra quy định bảo vệ dữ liệu cá nhân hiện hành của Việt Nam, vì dữ liệu trẻ em thường được bảo vệ chặt hơn.

## 5. Thuật ngữ

| Thuật ngữ | Giải thích |
|---|---|
| Hồ sơ học sinh | Thông tin học tập, khảo sát và nhu cầu của một học sinh, thuộc tài khoản phụ huynh |
| Matching | Mức độ phù hợp tương đối giữa học sinh và gia sư, hiển thị theo thứ hạng |
| Phong cách học | Cách học sinh tiếp thu tốt nhất: nghe, nhìn hoặc thực hành |
| Phương pháp dạy | Cách gia sư truyền đạt kiến thức, khai báo trong hồ sơ |
| Buổi học thử | Buổi đầu để học sinh và gia sư làm quen trước khi quyết định học lâu dài |
| Embedding | Dãy số do AI tạo ra để biểu diễn ý nghĩa của một đoạn văn; hai đoạn văn gần nghĩa có dãy số gần nhau |
| Model AI có sẵn (LLM) | Mô hình ngôn ngữ lớn do bên thứ ba cung cấp qua API, dùng trực tiếp không cần huấn luyện lại |
| Cấp xác minh | Mức độ kiểm chứng thông tin gia sư (1 đến 4), hiển thị bằng huy hiệu |
| Dự phòng (fallback) | Cách xử lý thay thế tự động khi AI lỗi hoặc quá chậm |
| MVP | Phiên bản tối thiểu, đủ để kiểm chứng giả thuyết cốt lõi |

> Thống nhất dùng "Học sinh" và "hồ sơ học sinh". Bản v0.3 có chỗ dùng "Người học", nên rà lại các file trong `BEHAVIOR/` và `USER_VIEW/`.

## 6. Thay đổi so với v0.3

| Hạng mục | Thay đổi | Lý do |
|---|---|---|
| Matching (README mục 5) | LLM xếp hạng 10-20 hồ sơ → lọc SQL, embedding xếp hạng top 10, LLM chỉ giải thích top 3 | Nhanh hơn, rẻ hơn, kết quả ổn định hơn (NFR-10) |
| Nhãn hiển thị | Theo thứ hạng thay vì ngưỡng điểm | Điểm embedding dồn trong khoảng hẹp, ngưỡng dễ lệch |
| Nhiều hồ sơ học sinh | Hoãn, MVP 1 học sinh mỗi phụ huynh | Giảm khối lượng; database vẫn thiết kế sẵn quan hệ 1-nhiều |
| Xác minh | Admin xác minh qua video, không lưu giấy tờ; cấp 3 gia sư tự khai | Bỏ phần lưu trữ nhạy cảm, giảm việc cho Admin |
| FR-08 | Hoãn | Liên hệ ngoài nền tảng |
| Khảo sát (NFR-05) | 15 câu → 6-8 câu cộng ô tự do | Ô tự do là phần embedding dùng được nhiều nhất |
| KPI | 7 → 4 | Chỉ đo những gì kiểm chứng được giả thuyết |
| NFR-07 | Thêm: embedding cũng phải ẩn danh, lọc tự động số điện thoại và email | Văn bản hồ sơ cũng được gửi lên API |
| NFR-09, NFR-10 | Không lưu giấy tờ; chất lượng AI kiểm thử tay | Phù hợp nhóm newbie, không cần hạ tầng riêng |