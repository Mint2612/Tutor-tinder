-- Thêm 80 gia sư demo ĐÃ DUYỆT: seed.tutor101 … seed.tutor180 @tutormatch.local
-- Mật khẩu giống các tài khoản demo hiện có (TutorDemo!2026#): lấy trực tiếp từ seed.tutor001.
-- Chạy lại an toàn: cập nhật hồ sơ của 80 gia sư này, giữ nguyên các OK/Match/lịch học đã phát sinh.
--
-- Độ phủ (để thử mượt với mọi lớp/môn):
--  * Môn: Toán chiếm nhiều nhất, còn lại Tiếng Anh, Ngữ văn, Vật lý, Hóa học, Sinh học, Tin học;
--    một nửa dạy thêm một môn liên quan (Toán+Vật lý, Hóa+Sinh, Anh+Văn, ...).
--  * Lớp: 5 nhóm (1–5, 4–8, 6–9, 10–12, 6–12) nên lớp 6 có nhiều gia sư nhất.
--  * Hình thức: 1/3 chỉ dạy online, còn lại cả online và trực tiếp; 5 tỉnh (Hà Nội, Hồ Chí Minh,
--    Đà Nẵng, Hải Phòng, Cần Thơ). Học phí 100.000–350.000đ/giờ, kinh nghiệm 1–12 năm.
--  * Lịch rảnh: 4 kiểu (tối các ngày trong tuần, tối T3-T5-CN, cuối tuần, 17:30–20:30 T2–T6).
SET NAMES utf8mb4;
START TRANSACTION;

SET @pw = (SELECT password_hash FROM users WHERE email = 'seed.tutor001@tutormatch.local');
CREATE TEMPORARY TABLE guard (v VARCHAR(100) NOT NULL);
INSERT INTO guard VALUES (@pw);

CREATE TEMPORARY TABLE nt AS
WITH RECURSIVE seq(j) AS (SELECT 101 UNION ALL SELECT j + 1 FROM seq WHERE j < 180)
SELECT j,
  CONCAT('seed.tutor', j, '@tutormatch.local') AS email,
  CONCAT(
    ELT(1 + MOD(j, 12), 'Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Vũ', 'Đặng', 'Bùi', 'Đỗ', 'Phan', 'Võ', 'Ngô'), ' ',
    ELT(1 + MOD(j * 7 + 3, 24), 'Minh Châu', 'Thanh Hà', 'Bảo Ngọc', 'Hoài An', 'Quang Huy', 'Đức Anh', 'Thu Trang', 'Hải Yến',
        'Văn Khoa', 'Ngọc Lan', 'Tuấn Kiệt', 'Phương Thảo', 'Gia Bảo', 'Khánh Linh', 'Anh Tuấn', 'Mai Phương', 'Hữu Nghĩa',
        'Thùy Dương', 'Công Minh', 'Kim Ngân', 'Trọng Nghĩa', 'Lan Anh', 'Xuân Hòa', 'Đình Phúc'),
    ' (Demo ', j, ')') AS name,
  CONCAT('0930', LPAD(j, 6, '0')) AS phone,
  CONCAT('d0d0e000-0000-4000-8000-', LPAD(j, 12, '0')) AS uid,
  CONCAT('d0d0e000-0000-4000-9000-', LPAD(j, 12, '0')) AS tid,
  ELT(1 + MOD(j, 11), 'Toán', 'Toán', 'Toán', 'Tiếng Anh', 'Tiếng Anh', 'Ngữ văn', 'Ngữ văn', 'Vật lý', 'Hóa học', 'Sinh học', 'Tin học') AS s1,
  ELT(1 + MOD(j, 5), 1, 4, 6, 10, 6) AS g_from,
  ELT(1 + MOD(j, 5), 5, 8, 9, 12, 12) AS g_to,
  MOD(j, 3) = 0 AS online_only,
  ELT(1 + MOD(j, 5), 'Hà Nội', 'Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Cần Thơ') AS province,
  ELT(1 + MOD(j, 4),
      '["1_18:00-21:00","3_18:00-21:00","5_18:00-21:00"]',
      '["2_19:00-21:00","4_19:00-21:00","7_08:00-11:00"]',
      '["6_08:00-12:00","7_08:00-12:00","7_14:00-17:00"]',
      '["1_17:30-20:30","2_17:30-20:30","3_17:30-20:30","4_17:30-20:30","5_17:30-20:30"]') AS availability,
  100000 + MOD(j * 37, 11) * 25000 AS rate,
  1 + MOD(j * 5, 12) AS years,
  ELT(1 + MOD(j, 5), 'Kiên nhẫn, hướng dẫn từng bước', 'Trực quan, nhiều ví dụ', 'Thực hành, tương tác',
      'Hệ thống hóa kiến thức, luyện đề', 'Vui vẻ, tạo động lực học') AS style,
  ELT(1 + MOD(j, 6),
      'Chia nhỏ kiến thức, luyện từng dạng bài và nhận xét sau mỗi buổi học.',
      'Bắt đầu từ chỗ con còn yếu, giao bài vừa sức và theo dõi tiến bộ hằng tuần.',
      'Dùng ví dụ đời thường để con hiểu bản chất trước khi làm bài.',
      'Kết hợp giảng ngắn và thực hành để con chủ động hơn.',
      'Lập kế hoạch ôn tập theo mục tiêu điểm số của từng giai đoạn.',
      'Luyện đề có phân tích lỗi sai để con không lặp lại.') AS method,
  ELT(1 + MOD(j, 6),
      'Cử nhân Sư phạm, có kinh nghiệm dạy kèm nhóm nhỏ.',
      'Giáo viên trung học, dạy kèm ngoài giờ.',
      'Sinh viên giỏi trường đại học sư phạm.',
      'Thạc sĩ chuyên ngành, từng tham gia bồi dưỡng học sinh.',
      'Cử nhân chuyên ngành, đã dạy học sinh nhiều trình độ.',
      'Giáo viên dạy giỏi cấp trường.') AS background
FROM seq;

-- Môn thứ hai (một nửa số gia sư).
ALTER TABLE nt ADD COLUMN s2 VARCHAR(40) NULL;
UPDATE nt SET s2 = CASE s1
  WHEN 'Toán' THEN 'Vật lý' WHEN 'Vật lý' THEN 'Toán' WHEN 'Hóa học' THEN 'Sinh học' WHEN 'Sinh học' THEN 'Hóa học'
  WHEN 'Tiếng Anh' THEN 'Ngữ văn' WHEN 'Ngữ văn' THEN 'Tiếng Anh' WHEN 'Tin học' THEN 'Toán' END
WHERE MOD(j, 2) = 0;

-- ---------------------------------------------------------------- tài khoản + hồ sơ
INSERT INTO users (id, email, full_name, role, password_hash, phone, email_verified, phone_verified)
  SELECT uid, email, name, 'TUTOR', @pw, phone, TRUE, FALSE FROM nt
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), phone = VALUES(phone), password_hash = VALUES(password_hash);

INSERT INTO tutor_profiles
  (id, user_id, province, hourly_rate, teaching_description, verification_status, availability, survey,
   display_name, photo_url, background, experience_years, teaching_style, workflow_state, visible,
   revision, verified_at, legal_name, accepting_students, achievements)
SELECT tid, uid, province, rate, method, 'APPROVED', availability, '{}',
       name, '', background, years, style, 'APPROVED', TRUE,
       1, NOW(6), name, TRUE,
       IF(MOD(j, 4) = 0, 'Dữ liệu demo, không phải thành tích đã xác thực.', NULL)
FROM nt
ON DUPLICATE KEY UPDATE province = VALUES(province), hourly_rate = VALUES(hourly_rate),
  teaching_description = VALUES(teaching_description), availability = VALUES(availability),
  display_name = VALUES(display_name), background = VALUES(background), experience_years = VALUES(experience_years),
  teaching_style = VALUES(teaching_style), achievements = VALUES(achievements), embedding = NULL, embedding_model = NULL;

DELETE FROM tutor_subjects WHERE tutor_id IN (SELECT tid FROM nt);
DELETE FROM tutor_grades WHERE tutor_id IN (SELECT tid FROM nt);
DELETE FROM tutor_modes WHERE tutor_id IN (SELECT tid FROM nt);
INSERT INTO tutor_subjects (tutor_id, subject) SELECT tid, s1 FROM nt;
INSERT INTO tutor_subjects (tutor_id, subject) SELECT tid, s2 FROM nt WHERE s2 IS NOT NULL;
INSERT INTO tutor_grades (tutor_id, grade)
  SELECT nt.tid, g.grade FROM nt JOIN (SELECT 1 AS grade UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
    UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) g
  ON g.grade BETWEEN nt.g_from AND nt.g_to;
INSERT INTO tutor_modes (tutor_id, mode) SELECT tid, 'ONLINE' FROM nt;
INSERT INTO tutor_modes (tutor_id, mode) SELECT tid, 'IN_PERSON' FROM nt WHERE NOT online_only;

COMMIT;
