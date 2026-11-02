# Đặc tả hệ thống kết nối Gia sư – Học viên

> Phiên bản: 0.1 (bản nháp) · Ngày: 02/11/2026

## 1. Tổng quan

Nền tảng kết nối **gia sư (tutor)** và **học viên (tutee)** trong cùng một tỉnh/thành. Có 3 loại người dùng:

| Vai trò | Mô tả | Quyền chính |
|---|---|---|
| **Admin** | Quản trị hệ thống | Xem danh sách tutor/tutee, duyệt hồ sơ tutor |
| **Tutor** | Gia sư (phải > 18 tuổi, cần admin duyệt) | Hoàn thiện hồ sơ, tìm tutee, dùng AI gợi ý |
| **Tutee** | Học viên | Hoàn thiện hồ sơ, tìm tutor, dùng AI gợi ý |

**Quy tắc cốt lõi:** Tutor và Tutee chỉ tìm thấy nhau khi **cùng tỉnh/thành** (mỗi tỉnh là một "room" riêng).
Ví dụ: TP.HCM = Room 1, tutor và tutee ở TP.HCM chỉ thấy nhau trong Room 1.

---

## 2. Mô hình dữ liệu (Database)

### 2.1 Tài khoản (chung cho Tutor & Tutee)

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `username` | string, unique | Bắt buộc |
| `password` | string (hash) | Bắt buộc, lưu bằng bcrypt/argon2, không lưu plain text |

### 2.2 Hồ sơ dùng chung

Cột **Bắt buộc** = phải điền ở bước đăng ký (form), "Sau" = bổ sung sau khi đăng nhập.

| # | Trường | Kiểu | Bắt buộc | Ghi chú |
|---|---|---|---|---|
| 1 | `full_name` | char/string | ✅ Đăng ký | Họ tên đầy đủ |
| 2 | `role` | bool | ✅ Đăng ký | `true` = tutor, `false` = tutee |
| 3 | `gender` | bool | ✅ Đăng ký | `true` = nam, `false` = nữ |
| 4 | `age` | number | ✅ Đăng ký | Tutor: **> 18**. Tutee: theo quy định hệ thống (đề xuất ≥ 6) |
| 5 | `location` | `{province: enum, town: enum}` | ✅ Đăng ký | Ví dụ: TP.HCM (1) → Dĩ An (2). Town phụ thuộc Province |
| 6 | `self_description` | text | Sau | Giới thiệu bản thân, dùng cho AI gợi ý |
| 7 | `contact_info` | text | Sau | Gợi ý nhập: số điện thoại, địa chỉ, gmail |


### 2.3 Thông tin học vấn — dùng cho Tutee only

```
is_univer: bool
├── true  → list major: enum + grade: enum (1–4)
└── false → list subject: enum + grade: enum (1–12)
```

Ví dụ:
- `yes, { cs(1) }, 1` → học viên là sinh viên ngành CS, năm 1
- `no, {math(1),english(2)} ,4` → học viên lớp 4 cần học môn toán và tiếng anh

### 2.4 Chỉ riêng Tutor

| # | Trường | Kiểu | Ghi chú |
|---|---|---|---|
| 8 | `self_face` | url | Ảnh chân dung |
| 9 | `over18_evidence` | url | Giấy tờ chứng minh trên 18 tuổi (**private**, chỉ admin xem) |
| 10 | `study_evidence` | url[] (ảnh) | Bằng cấp / bảng điểm / thẻ sinh viên |
| – | `approval_status` | enum | `pending` / `approved` / `rejected` |
| – | `reject_reason` | text | Lý do từ chối (nếu có) |

### 2.5 Public profile (thông tin hiển thị công khai)

| Loại | Trường hiển thị |
|---|---|
| **Tutor public profile** | 1 → 10, **trừ 9** (không lộ giấy tờ tuổi) |
| **Tutee public profile** | 1 → 6 + 11 |

> Lưu ý: `contact_info` (7) nằm trong public profile theo yêu cầu gốc. Nên cân nhắc chỉ hiện sau khi hai bên đồng ý kết nối (xem mục 8).

### 2.6 Sơ đồ bảng đề xuất

```
users            (id, username, password_hash, role, created_at)
profiles         (user_id FK, full_name, gender, age, province_id, town_id,
                  self_description, contact_info)
education        (user_id FK, is_univer, major_id, subject_id, grade)
tutor_documents  (user_id FK, self_face_url, over18_evidence_url,
                  approval_status, reject_reason, reviewed_by, reviewed_at)
study_evidences  (id, user_id FK, image_url)
provinces        (id, name)
towns            (id, province_id FK, name)
majors           (id, name)
subjects         (id, name)
```

---

## 3. Quy tắc phòng theo tỉnh (Room)

- Mỗi `province` tương ứng một room tìm kiếm riêng.
- Mọi truy vấn tìm kiếm **bắt buộc** lọc theo `province_id` của người đang đăng nhập.
- Không cho phép tutor/tutee truy cập hồ sơ ngoài tỉnh của mình (kiểm tra ở backend, không chỉ ở UI).
- Đề xuất index: `(province_id, role, is_univer, major_id/subject_id)`.

---

## 4. Luồng người dùng — Tutee (học viên)

1. **Đăng ký:** nhập `username` + `password` → điền form các trường **1–5** → có thể sửa lại sau khi đăng nhập.
2. **Trang chính:** form bổ sung thông tin **6–7** (và field 11 nếu chưa có).
3. **Tìm kiếm gia sư:**
   - **Gửi:** `{ is_univer, major/subject, tutee_profile }`
   - **Nhận:** danh sách `tutor-public-profile` cùng major/subject, cùng tỉnh, đã `approved`.
   - Ghi chú: có thể thêm **Redis cache** cho kết quả tìm kiếm ở giai đoạn sau.
4. **AI gợi ý:** dùng `self_description` của hai bên để gợi ý người phù hợp.

## 5. Luồng người dùng — Tutor (gia sư)

1. **Đăng ký:** điền form **1–5** → **admin kiểm tra** → trạng thái `pending` đến khi được duyệt.
2. **Trang chính:** header có mục thêm thông tin (điền **6–10**).
3. **Tìm học viên:**
   - **Gửi:** `{ is_univer, major/subject, profile }`
   - **Nhận:** danh sách `tutee-public-profile` cùng major/subject, cùng tỉnh.
4. **AI gợi ý:** ghép dựa trên `self_description`.

> Tutor chưa được duyệt: có thể đăng nhập và cập nhật hồ sơ nhưng **không hiển thị công khai** và **không dùng được tìm kiếm** (đề xuất).

## 6. Luồng — Admin

1. **Xem danh sách** tutor và tutee (lọc theo tỉnh, trạng thái, tìm theo tên).
2. **Duyệt hồ sơ tutor:**
   - Xem đầy đủ thông tin 1–10 (bao gồm `over18_evidence` và `study_evidence`).
   - Đối chiếu: tuổi khai báo > 18, ảnh khuôn mặt khớp giấy tờ, bằng cấp hợp lệ.
   - Hành động: **Approve** hoặc **Reject** (kèm lý do).

### Mẫu form duyệt hồ sơ tutor

```markdown
## Duyệt hồ sơ gia sư #<id>

**Họ tên:** ...            **Giới tính:** ...        **Tuổi:** ...
**Khu vực:** <Tỉnh> – <Quận/Huyện>
**Học vấn:** [Đại học – Ngành: ... – Năm: ...] / [Phổ thông – Môn: ... – Lớp: ...]
**Giới thiệu:** ...
**Liên hệ:** ...

### Tài liệu
- Ảnh khuôn mặt: <url>
- Giấy tờ chứng minh > 18 tuổi: <url>
- Minh chứng học tập: <url>, <url>

### Checklist
- [ ] Ảnh khuôn mặt rõ, khớp giấy tờ
- [ ] Tuổi trong giấy tờ > 18 và khớp với tuổi khai báo
- [ ] Minh chứng học tập hợp lệ, đúng ngành/môn khai báo
- [ ] Thông tin liên hệ hợp lệ

### Quyết định
( ) Approve    ( ) Reject
**Lý do (nếu reject):** ...
```

---

## 7. API đề xuất (tóm tắt)

| Method | Endpoint | Mô tả | Quyền |
|---|---|---|---|
| POST | `/auth/register` | Đăng ký (username, password, trường 1–5) | Public |
| POST | `/auth/login` | Đăng nhập | Public |
| GET/PUT | `/me/profile` | Xem/sửa hồ sơ | Tutor, Tutee |
| PUT | `/me/tutor-documents` | Upload trường 8–10 | Tutor |
| GET | `/search/tutors` | Tìm tutor (`is_univer`, `major/subject`) | Tutee |
| GET | `/search/tutees` | Tìm tutee | Tutor (approved) |
| GET | `/recommend` | AI gợi ý dựa trên self-description | Tutor, Tutee |
| GET | `/admin/users?role=&status=` | Danh sách người dùng | Admin |
| GET | `/admin/tutors/{id}` | Chi tiết hồ sơ duyệt | Admin |
| POST | `/admin/tutors/{id}/approve` | Duyệt | Admin |
| POST | `/admin/tutors/{id}/reject` | Từ chối (kèm lý do) | Admin |

## 8. Điểm cần làm rõ / đề xuất bảo mật

1. **Tutor có field 11 không?** Bản gốc chỉ ghi field 11 cho tutee, nhưng tutor cũng cần major/subject để ghép. Spec này giả định tutor cũng có (tức "ngành/môn có thể dạy").
2. **Ghép theo cấp độ:** nên so khớp thêm `grade` (tutor dạy được lớp ≥ lớp của tutee), không chỉ major/subject.
3. **`contact_info` công khai:** SĐT/địa chỉ hiện công khai dễ bị lạm dụng, nên chỉ hiện sau khi hai bên kết nối, hoặc cho người dùng chọn ẩn/hiện.
4. **Tutee là trẻ em:** cần quy định tuổi tối thiểu và có thể cần phụ huynh đồng ý.
5. **Bảo mật tài liệu:** `over18_evidence` và `study_evidence` lưu ở storage private, chỉ admin truy cập qua signed URL.
6. **Đồng bộ tuổi:** `age` lưu số dễ lỗi thời, nên cân nhắc lưu `date_of_birth`.
7. **Thông báo:** gửi thông báo cho tutor khi hồ sơ được duyệt/từ chối.
8. **Đổi tỉnh:** nếu người dùng chuyển tỉnh, cần quy trình cập nhật `location` (tutor có thể cần duyệt lại).
9. **Thống nhất thuật ngữ:** tài liệu gốc dùng lẫn mentee/tutee, spec này thống nhất dùng **tutee**.
