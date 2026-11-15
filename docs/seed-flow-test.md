# Seed kiểm thử luồng Phụ huynh – Gia sư – Admin

Dữ liệu **giả**, chỉ dùng cho môi trường phát triển. Mỗi kịch bản ở sẵn một trạng thái của luồng để
thử bằng tay trên http://localhost:3000.

```sh
sh scripts/seed/run-flow-seed.sh            # hoặc: powershell scripts/seed/run-flow-seed.ps1
node scripts/seed/verify-flow-seed.mjs      # kiểm tra seed qua API thật (chỉ đọc)
```

Chạy lại bất cứ lúc nào để **đặt lại** về trạng thái ban đầu: script chỉ xóa/tạo lại tài khoản
`flow-*@example.test`, không đụng dữ liệu khác. Mật khẩu chung: `TutorMatch@Test2026`.
Hạn Match/xác nhận/học thử tính từ lúc nạp (OK 7 ngày, xác nhận 3 ngày, lịch học thử 3 ngày): nếu
đã quá hạn, nạp lại seed. Đăng nhập bị giới hạn 20 lần/IP/15 phút; nếu bị chặn khi test lặp nhiều,
chạy `DELETE FROM auth_throttles` trong DB dev.

Tất cả học sinh/gia sư seed học **trực tiếp tại Huế** và mỗi kịch bản dùng một lớp riêng (6–11) nên
không lẫn vào nhau hay vào dữ liệu seed cũ. Email có dạng `flow-<tên>@example.test`.

| Bước | Tài khoản (mở bằng) | Trạng thái sẵn có | Việc cần thử |
| --- | --- | --- | --- |
| 1. Phụ huynh tạo hồ sơ học sinh + khảo sát | `flow-phuhuynh-moi` | Chưa có hồ sơ | **Hồ sơ của con → Thêm hồ sơ**, điền khảo sát 6 câu, tích đồng ý. (Ví dụ đã hoàn chỉnh: `flow-phuhuynh-1`) |
| 2. Gia sư tạo hồ sơ và yêu cầu xác minh | `flow-giasu-moi` | Chưa có hồ sơ | Điền hồ sơ, tải giấy tờ, **Gửi hồ sơ để duyệt** |
| | `flow-giasu-nhap` | Hồ sơ + giấy tờ, chưa gửi | Bấm **Gửi hồ sơ để duyệt** |
| 3. Admin xác minh gia sư | `flow-admin` | Hàng chờ có 2 hồ sơ ở đầu: `Gia sư Chờ duyệt 1/2` (tài khoản `flow-giasu-choduyet1/2`) | Mở giấy tờ → **Phê duyệt**. Sau duyệt, họ xuất hiện trong danh sách của `flow-phuhuynh-1` |
| 4. Phụ huynh bấm OK trên thẻ gia sư | `flow-phuhuynh-1` (Bé Minh, lớp 6) | 3 thẻ: Cô Hoa, Thầy Bình, Cô Lan (+ gia sư vừa được admin duyệt) | **Khám phá gia sư** → OK/vuốt phải |
| 5. Gia sư thấy nhãn “Quan tâm bạn” và bấm OK | `flow-giasu-quantam` | Phụ huynh `flow-phuhuynh-2` (Bé Lan, lớp 7) đã OK | **Tìm học sinh** → thẻ có nhãn ♡ Quan tâm bạn → OK |
| 6. Match được tạo | `flow-phuhuynh-3` / `flow-giasu-match` (lớp 8) | Đã Match, có thông báo, chưa ai xác nhận | **Đã OK & Match** → Xem hồ sơ & xác nhận kết nối |
| 7a. Xác nhận hai chiều | `flow-phuhuynh-4` / `flow-giasu-choxacnhan` (lớp 9) | Phụ huynh đã xác nhận, chờ gia sư | Gia sư xác nhận → hai bên thấy liên hệ |
| 7b. Liên hệ + học thử (chờ chốt) | `flow-phuhuynh-5` / `flow-giasu-hocthu` (lớp 10) | Đã kết nối, phụ huynh đề xuất 3 giờ học thử | Gia sư chốt một giờ hoặc đề xuất giờ khác |
| 7c. Liên hệ + học thử (chưa đề xuất) | `flow-phuhuynh-6` / `flow-giasu-ketnoi` (lớp 11) | Đã kết nối, chưa có lịch | Tự đề xuất 1–3 giờ học thử |

Trước khi hai bên xác nhận, email/điện thoại của nhau bị ẩn; sau khi cả hai xác nhận mới hiện
(7b, 7c đã ở trạng thái này). Các tài khoản có số điện thoại `0910000001`–`0910000020`.

## Chuỗi đầy đủ không cần dữ liệu có sẵn

`flow-admin` duyệt `flow-giasu-choduyet1` → `flow-phuhuynh-1` OK gia sư đó → đăng nhập
`flow-giasu-choduyet1`, thấy nhãn **Quan tâm bạn** và OK → Match → cả hai xác nhận → xem liên hệ →
đề xuất học thử. Chuỗi này đã chạy thành công trên API thật khi tạo seed.

## Demo với 2 tài khoản có sẵn: Phụ huynh Demo 004 và Phạm Đức Huy (Demo 004)

```sh
sh scripts/seed/run-flow-seed.sh demo004        # hoặc: powershell scripts/seed/run-flow-seed.ps1 demo004
```

Tạo thêm **6 hồ sơ học sinh** cho `seed.parent004@tutormatch.local`, mỗi hồ sơ ghép với gia sư
`seed.tutor004@tutormatch.local` ở một bước của luồng. Hồ sơ mới có tên bắt đầu bằng `01`–`06` nên
đứng đầu ô chọn học sinh. Chạy lại để đặt lại đúng 6 hồ sơ này (hồ sơ, Match và lịch học cũ của hai
tài khoản được giữ nguyên). Không đổi mật khẩu; dùng mật khẩu hiện có của hai tài khoản.

| Chọn hồ sơ (phụ huynh) | Trạng thái | Việc cần demo |
| --- | --- | --- |
| `01 Demo - Bước 4: chưa OK gia sư` | Chưa có quyết định | **Khám phá gia sư** → thẻ Phạm Đức Huy (Demo 004) → OK |
| `02 Demo - Bước 5: gia sư thấy nhãn Quan tâm bạn` | Phụ huynh đã OK | Đăng nhập gia sư → **Tìm học sinh**, thẻ ♡ Quan tâm bạn đứng đầu → OK → Match |
| `03 Demo - Bước 6: đã Match, chưa xác nhận` | Đã Match | **Đã OK & Match** → xem hồ sơ → hai bên xác nhận → thấy liên hệ |
| `04 Demo - Bước 7a: phụ huynh đã xác nhận` | Chờ gia sư xác nhận | Gia sư xác nhận → liên hệ hiện ở cả hai bên |
| `05 Demo - Bước 7b: đã kết nối, học thử chờ chốt` | Đã kết nối + 3 giờ học thử | Gia sư chốt một giờ (hoặc đề xuất giờ khác) |
| `06 Demo - Bước 7c: đã kết nối, chưa đặt lịch` | Đã kết nối | Tự đề xuất giờ học thử |

Hồ sơ mới học trực tiếp tại Hải Phòng, Hóa học + Vật lý, ngân sách 200.000đ/giờ: chỉ gia sư này thỏa
(đã kiểm tra), nên chỉ có đúng một thẻ cho mỗi hồ sơ chưa OK. Phía gia sư, các thẻ “Quan tâm bạn” luôn
đứng đầu danh sách, trước các học sinh demo cũ.

**Bước 1 (phụ huynh tạo hồ sơ + khảo sát)**: tạo hồ sơ mới ngay trong giao diện. **Bước 2–3 (gia sư
yêu cầu xác minh, admin duyệt)**: gia sư này đã được duyệt sẵn và chưa có giấy tờ. Để demo, gia sư sửa
một trường hồ sơ → tải một giấy tờ → **Gửi hồ sơ để duyệt**; admin `flow-admin@example.test`
(mật khẩu `TutorMatch@Test2026`, tạo bởi seed flow) mở giấy tờ rồi phê duyệt. Trong lúc chờ duyệt gia
sư không hiện trong danh sách của phụ huynh, nên làm bước này **trước** bước 4.

## Thêm 80 gia sư demo đã duyệt

```sh
sh scripts/seed/run-flow-seed.sh tutors         # hoặc: powershell scripts/seed/run-flow-seed.ps1 tutors
```

Tạo `seed.tutor101` … `seed.tutor180@tutormatch.local`, mật khẩu `TutorDemo!2026#` (giống các tài
khoản demo hiện có). Chạy lại an toàn: cập nhật hồ sơ nhưng giữ OK/Match/lịch học đã phát sinh.
Danh sách tài khoản: `.task-data/demo_accounts_more_tutors.csv`.

- Môn: Toán nhiều nhất (29 gia sư mới), rồi Tiếng Anh, Ngữ văn, Vật lý, Hóa học, Sinh học, Tin học;
  một nửa dạy thêm môn liên quan (Toán+Vật lý, Hóa+Sinh, Anh+Văn, …).
- Lớp: 5 nhóm (1–5, 4–8, 6–9, 10–12, 6–12). **Lớp 6 có nhiều gia sư nhất**: 18 gia sư Toán mới,
  tổng cộng 60 gia sư Toán lớp 6 đang hiển thị.
- 1/3 chỉ dạy online, còn lại cả hai hình thức; 5 tỉnh; học phí 100.000–350.000đ/giờ; kinh nghiệm 1–12 năm.

## Gợi ý 10 hồ sơ bằng AI (thẻ tóm gọn, cuộn vòng)

Thẻ trong danh sách “Gợi ý 10 hồ sơ bằng AI” (cuộn ngang vô tận: hết hồ sơ thứ 10 lại quay về hồ sơ đầu) chỉ còn: tên, huy hiệu xác minh, môn · lớp (gộp dải, ví
dụ “Lớp 6–9” thay vì “6, 7, 8, 9”), học phí · số năm kinh nghiệm, tỉnh · hình thức. Đã bỏ khối “Rất
đáng cân nhắc”, lời giải thích, mục “Điểm mạnh & điều cần cân nhắc” và lịch rảnh. Phía gia sư, thẻ học
sinh còn: lớp, môn · hình thức · tỉnh, ngân sách và tối đa 3 nhãn học tập. Thẻ đơn ở “Khám phá gia
sư” vẫn hiển thị đầy đủ.

## Dàn đều hoạt động 7 ngày cho trang Giám sát

```sh
sh scripts/seed/run-flow-seed.sh activity       # hoặc: powershell scripts/seed/run-flow-seed.ps1 activity
```

Chạy sau khi nạp các seed khác. Script chỉ đổi **mốc thời gian** của tài khoản demo (`@tutormatch.local`,
`@example.test`, không phải admin) và các Match/lịch học giữa họ, để biểu đồ 7 ngày có số liệu hợp lý thay
vì dồn vào ngày nạp seed. Không đổi trạng thái, hạn xử lý (`expires_at`, `confirm_expires_at`) hay các kịch
bản demo ở trên. Tài khoản thật, cặp đã chốt học thử (dùng thử đánh giá) và cặp với tài khoản thật giữ nguyên.

- Thứ 2–6 đông hơn chủ nhật, giờ cao điểm buổi tối; số liệu mỗi ngày chênh lệch vừa phải (không bằng phẳng).
- Thứ tự luôn hợp lý: tài khoản → gia sư gửi duyệt → admin duyệt (1–6 giờ) → OK đầu tiên → Match (15 phút – 40 giờ
  sau) → học thử; không mốc nào ở tương lai.
- Thêm khoảng 6 lịch học thử (chốt / hủy / hết hạn) cho các cặp đã kết nối nhưng chưa có lịch, trừ các cặp
  dành cho demo.
- Chạy lại trong cùng một ngày cho kết quả y hệt (ngẫu nhiên theo id); sang ngày khác, các mốc dời theo.

## Gia sư nổi bật: Phạm Đức Huy (Demo 004)

```sh
sh scripts/seed/run-flow-seed.sh featured       # hoặc: powershell scripts/seed/run-flow-seed.ps1 featured
```

`seed.tutor004@tutormatch.local` được đánh dấu **nổi bật** (cột `featured`, migration V13): luôn đứng đầu
**Khám phá** và **Gợi ý AI** của mọi phụ huynh. Hồ sơ được mở rộng để hợp với hầu hết học sinh: đủ 7 môn, lớp
1–12, online + trực tiếp, 80.000đ/giờ (thấp nhất hệ thống), rảnh cả tuần 00:00–23:59, 15 năm kinh nghiệm.
Chỉ một gia sư nổi bật tại một thời điểm; chạy lại seed là đặt lại hồ sơ này, giữ nguyên Match/lịch học cũ.

- **Nổi bật chỉ đổi thứ tự**, không bỏ qua điều kiện bắt buộc. Gia sư **không** hiện với: học sinh học trực tiếp ngoài
  Hải Phòng (gia sư ở Hải Phòng), ngân sách dưới 80.000đ/giờ, và cặp đã được quyết định trước đó (đã OK, Match, bỏ qua).
- Phụ huynh mới (học online, hoặc trực tiếp tại Hải Phòng) luôn thấy Phạm Đức Huy ở thẻ đầu tiên; hãy đăng nhập
  `seed.tutor004@tutormatch.local` (mật khẩu `TutorDemo!2026#`) để OK lại và thấy nhãn “Quan tâm bạn”.
- Vì vậy thẻ đầu của các phụ huynh `demo.flow.parent0x` giờ là Phạm Đức Huy chứ không còn là `Demo Flow Tutor 0x`:
  muốn thử đúng cặp trong bảng kịch bản ở trên, bỏ qua thẻ đầu hoặc tìm thẻ `Demo Flow Tutor` ở vị trí kế tiếp.
- Đã kiểm tra bằng đăng nhập thật: 73 hồ sơ học sinh của 12 phụ huynh, Phạm Đức Huy đứng đầu ở **42/42** hồ sơ đủ điều kiện và
  chưa quyết định; Gợi ý AI đứng đầu ở **12/12** hồ sơ thử.
