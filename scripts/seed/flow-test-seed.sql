-- Seed kiểm thử luồng: Phụ huynh ↔ Gia sư ↔ Admin → Match → xác nhận kết nối → học thử.
-- Dữ liệu GIẢ, chỉ dùng cho môi trường phát triển/test. Chạy lại bất cứ lúc nào: script xóa
-- toàn bộ tài khoản `flow-*@example.test` rồi tạo lại (không đụng dữ liệu khác).
-- Mật khẩu chung: TutorMatch@Test2026   Hướng dẫn: docs/seed-flow-test.md
--
-- Cách ly dữ liệu: mọi hồ sơ học sinh/gia sư ở đây học TRỰC TIẾP tại "Huế" (không có dữ liệu cũ nào
-- ở Huế) và mỗi kịch bản dùng một lớp riêng (6–11), nên các kịch bản không lẫn vào nhau hoặc vào
-- dữ liệu seed khác. Gia sư seed chỉ dạy trực tiếp (không ONLINE) để không lọt vào hồ sơ học sinh online cũ.
SET NAMES utf8mb4;
START TRANSACTION;

-- ---------------------------------------------------------------- 0. Dọn dữ liệu cũ của seed
CREATE TEMPORARY TABLE old_users AS SELECT id FROM users WHERE email LIKE 'flow-%@example.test';
CREATE TEMPORARY TABLE old_tutors AS SELECT id FROM tutor_profiles WHERE user_id IN (SELECT id FROM old_users);
CREATE TEMPORARY TABLE old_students AS SELECT id FROM student_profiles WHERE parent_id IN (SELECT id FROM old_users);
DELETE FROM trial_reviews WHERE trial_id IN (SELECT id FROM trial_schedules
  WHERE student_id IN (SELECT id FROM old_students) OR tutor_id IN (SELECT id FROM old_tutors));
DELETE FROM trial_schedules WHERE student_id IN (SELECT id FROM old_students) OR tutor_id IN (SELECT id FROM old_tutors);
DELETE FROM pair_ai_scores WHERE student_id IN (SELECT id FROM old_students) OR tutor_id IN (SELECT id FROM old_tutors);
DELETE FROM lesson_requests WHERE student_id IN (SELECT id FROM old_students) OR tutor_id IN (SELECT id FROM old_tutors);
DELETE FROM verification_audit WHERE tutor_id IN (SELECT id FROM old_tutors) OR admin_id IN (SELECT id FROM old_users);
DELETE FROM document_reviews WHERE admin_id IN (SELECT id FROM old_users);
DELETE FROM matching_daily_usage WHERE profile_id IN (SELECT id FROM old_students UNION SELECT id FROM old_tutors);
DELETE FROM users WHERE id IN (SELECT id FROM old_users); -- cascade: hồ sơ, cặp, thông báo, phiên

-- ---------------------------------------------------------------- 1. Tài khoản
-- Mật khẩu TutorMatch@Test2026 (BCrypt cost 12). Tài khoản đã kích hoạt email.
SET @pw = '$2a$12$AjBAPsW/qN7EhOO3CNlEm.VSXuLnN3s1UgqGq.qcifqcYsfgvva3i';
CREATE TEMPORARY TABLE people (
  k VARCHAR(12) PRIMARY KEY, n INT NOT NULL, role VARCHAR(8) NOT NULL, email VARCHAR(255), name VARCHAR(120), phone VARCHAR(24)
);
INSERT INTO people VALUES
 ('admin',  1, 'ADMIN',  'flow-admin@example.test',          'Quản trị viên Test',                 '0910000001'),
 -- Bước 1: phụ huynh chưa có hồ sơ; Bước 2: gia sư chưa có hồ sơ
 ('ph-moi', 2, 'PARENT', 'flow-phuhuynh-moi@example.test',   'Phụ huynh Mới (chưa có hồ sơ)',      '0910000002'),
 ('gs-moi', 3, 'TUTOR',  'flow-giasu-moi@example.test',      'Gia sư Mới (chưa có hồ sơ)',         '0910000003'),
 -- Bước 2→3: gia sư đã có hồ sơ + giấy tờ
 ('gs-nhap',4, 'TUTOR',  'flow-giasu-nhap@example.test',     'Gia sư Nháp (chưa gửi duyệt)',       '0910000004'),
 ('gs-cho1',5, 'TUTOR',  'flow-giasu-choduyet1@example.test','Gia sư Chờ duyệt 1',                 '0910000005'),
 ('gs-cho2',6, 'TUTOR',  'flow-giasu-choduyet2@example.test','Gia sư Chờ duyệt 2',                 '0910000006'),
 -- Bước 4: phụ huynh bấm OK trên thẻ gia sư (lớp 6)
 ('ph-1',   7, 'PARENT', 'flow-phuhuynh-1@example.test',     'Phụ huynh Bước 4',                   '0910000007'),
 ('gs-ok1', 8, 'TUTOR',  'flow-giasu-ok1@example.test',      'Cô Hoa Test',                        '0910000008'),
 ('gs-ok2', 9, 'TUTOR',  'flow-giasu-ok2@example.test',      'Thầy Bình Test',                     '0910000009'),
 ('gs-ok3',10, 'TUTOR',  'flow-giasu-ok3@example.test',      'Cô Lan Test',                        '0910000010'),
 -- Bước 5: gia sư thấy nhãn "Quan tâm bạn" (lớp 7)
 ('ph-2',  11, 'PARENT', 'flow-phuhuynh-2@example.test',     'Phụ huynh Bước 5',                   '0910000011'),
 ('gs-qt', 12, 'TUTOR',  'flow-giasu-quantam@example.test',  'Thầy Quang Test',                    '0910000012'),
 -- Bước 6–7: đã Match, chưa ai xác nhận (lớp 8)
 ('ph-3',  13, 'PARENT', 'flow-phuhuynh-3@example.test',     'Phụ huynh Bước 6',                   '0910000013'),
 ('gs-3',  14, 'TUTOR',  'flow-giasu-match@example.test',    'Cô Mai Test',                        '0910000014'),
 -- Bước 7: phụ huynh đã xác nhận, chờ gia sư (lớp 9)
 ('ph-4',  15, 'PARENT', 'flow-phuhuynh-4@example.test',     'Phụ huynh Bước 7a',                  '0910000015'),
 ('gs-4',  16, 'TUTOR',  'flow-giasu-choxacnhan@example.test','Thầy Đức Test',                     '0910000016'),
 -- Bước 7: đã kết nối, đã có đề xuất học thử đang chờ gia sư (lớp 10)
 ('ph-5',  17, 'PARENT', 'flow-phuhuynh-5@example.test',     'Phụ huynh Bước 7b',                  '0910000017'),
 ('gs-5',  18, 'TUTOR',  'flow-giasu-hocthu@example.test',   'Cô Thảo Test',                       '0910000018'),
 -- Bước 7: đã kết nối, chưa đề xuất lịch học thử (lớp 11)
 ('ph-6',  19, 'PARENT', 'flow-phuhuynh-6@example.test',     'Phụ huynh Bước 7c',                  '0910000019'),
 ('gs-6',  20, 'TUTOR',  'flow-giasu-ketnoi@example.test',   'Thầy Long Test',                     '0910000020');
CREATE TEMPORARY TABLE ids AS
  SELECT k, CONCAT('f10e0000-0000-4000-8000-', LPAD(n, 12, '0')) AS uid FROM people;

INSERT INTO users (id, email, full_name, role, password_hash, phone, email_verified, phone_verified)
  SELECT i.uid, p.email, p.name, p.role, @pw, p.phone, TRUE, FALSE FROM people p JOIN ids i USING (k);

-- ---------------------------------------------------------------- 2. Hồ sơ gia sư
-- g = lớp dạy (cách ly kịch bản); state: DRAFT | PENDING_REVIEW | APPROVED
CREATE TEMPORARY TABLE tutors_def (
  k VARCHAR(12) PRIMARY KEY, n INT, grade INT, state VARCHAR(16), subjects VARCHAR(100), rate INT, years INT,
  style VARCHAR(200), background VARCHAR(300)
);
INSERT INTO tutors_def VALUES
 ('gs-nhap', 1, 6, 'DRAFT',          'Toán',             140000, 2, 'Kiên nhẫn, hướng dẫn từng bước', 'Sinh viên Sư phạm Toán năm cuối.'),
 ('gs-cho1', 2, 6, 'PENDING_REVIEW', 'Toán',             150000, 3, 'Trực quan, nhiều ví dụ',         'Cử nhân Toán ứng dụng.'),
 ('gs-cho2', 3, 6, 'PENDING_REVIEW', 'Toán',             160000, 5, 'Thực hành, tương tác',           'Giáo viên THCS.'),
 ('gs-ok1',  4, 6, 'APPROVED',       'Toán',             150000, 4, 'Kiên nhẫn, hướng dẫn từng bước', 'Cử nhân Sư phạm Toán.'),
 ('gs-ok2',  5, 6, 'APPROVED',       'Toán,Vật lý',      170000, 6, 'Trực quan, nhiều ví dụ',         'Thạc sĩ Vật lý.'),
 ('gs-ok3',  6, 6, 'APPROVED',       'Toán,Tiếng Anh',   180000, 8, 'Thực hành, tương tác',           'Giáo viên song ngữ.'),
 ('gs-qt',   7, 7, 'APPROVED',       'Toán',             150000, 5, 'Kiên nhẫn, hướng dẫn từng bước', 'Cử nhân Toán.'),
 ('gs-3',    8, 8, 'APPROVED',       'Toán',             150000, 4, 'Trực quan, nhiều ví dụ',         'Giáo viên THCS.'),
 ('gs-4',    9, 9, 'APPROVED',       'Toán',             160000, 6, 'Thực hành, tương tác',           'Cử nhân Toán tin.'),
 ('gs-5',   10,10, 'APPROVED',       'Toán',             170000, 7, 'Kiên nhẫn, hướng dẫn từng bước', 'Giáo viên THPT.'),
 ('gs-6',   11,11, 'APPROVED',       'Toán',             180000, 9, 'Trực quan, nhiều ví dụ',         'Thạc sĩ Toán.');

INSERT INTO tutor_profiles
  (id, user_id, province, hourly_rate, teaching_description, verification_status, availability, survey,
   display_name, photo_url, background, experience_years, teaching_style, workflow_state, visible,
   revision, submitted_at, submission_id, verified_at, legal_name, accepting_students, achievements)
SELECT CONCAT('f10e0000-0000-4000-9000-', LPAD(d.n, 12, '0')), i.uid, 'Huế', d.rate,
  'Chia nhỏ kiến thức, luyện từng dạng bài và nhận xét sau mỗi buổi học.',
  IF(d.state = 'APPROVED', 'APPROVED', 'PENDING'),
  '["2_EVENING","4_EVENING","6_PM"]', '{}',
  p.name, '', d.background, d.years, d.style, d.state, TRUE,
  1, IF(d.state = 'DRAFT', NULL, NOW(6)),
  IF(d.state = 'DRAFT', NULL, CONCAT('f10e0000-0000-4000-b000-', LPAD(d.n, 12, '0'))),
  IF(d.state = 'APPROVED', NOW(6), NULL),
  CONCAT('Nguyễn Văn Test ', d.n), TRUE,
  IF(d.k = 'gs-ok3', 'Giải nhì Toán cấp tỉnh (tự khai)', NULL)
FROM tutors_def d JOIN ids i USING (k) JOIN people p USING (k);

CREATE TEMPORARY TABLE tid AS
  SELECT d.k, CONCAT('f10e0000-0000-4000-9000-', LPAD(d.n, 12, '0')) AS tutor_id FROM tutors_def d;

INSERT INTO tutor_subjects (tutor_id, subject)
  SELECT t.tutor_id, TRIM(s.subject) FROM tid t JOIN tutors_def d USING (k)
  JOIN JSON_TABLE(CONCAT('["', REPLACE(d.subjects, ',', '","'), '"]'), '$[*]' COLUMNS (subject VARCHAR(80) PATH '$')) s;
INSERT INTO tutor_grades (tutor_id, grade) SELECT t.tutor_id, d.grade FROM tid t JOIN tutors_def d USING (k);
INSERT INTO tutor_modes (tutor_id, mode) SELECT t.tutor_id, 'IN_PERSON' FROM tid t;

-- Giấy tờ định danh giả (PDF hợp lệ, nội dung nằm trong DB) để admin mở và kiểm tra.
INSERT INTO tutor_documents (id, tutor_id, filename, content_type, content, revision)
  SELECT CONCAT('f10e0000-0000-4000-c000-', LPAD(d.n, 12, '0')), t.tutor_id, 'GIAY-TO-GIA-TEST.pdf', 'application/pdf',
         CAST(CONCAT('%PDF-1.4\n% Giay to gia dung cho kiem thu - ', d.k, '\n%%EOF\n') AS BINARY), 1
  FROM tid t JOIN tutors_def d USING (k);

-- Gia sư đã được duyệt có lịch sử quyết định của admin.
INSERT INTO verification_audit (id, tutor_id, admin_id, decision, reason, submission_id, revision)
  SELECT CONCAT('f10e0000-0000-4000-d000-', LPAD(d.n, 12, '0')), t.tutor_id, a.uid, 'APPROVED', NULL,
         CONCAT('f10e0000-0000-4000-b000-', LPAD(d.n, 12, '0')), 1
  FROM tid t JOIN tutors_def d USING (k) JOIN ids a ON a.k = 'admin' WHERE d.state = 'APPROVED';
INSERT INTO document_reviews (document_id, admin_id, submission_id)
  SELECT CONCAT('f10e0000-0000-4000-c000-', LPAD(d.n, 12, '0')), a.uid,
         CONCAT('f10e0000-0000-4000-b000-', LPAD(d.n, 12, '0'))
  FROM tutors_def d JOIN ids a ON a.k = 'admin' WHERE d.state = 'APPROVED';

-- ---------------------------------------------------------------- 3. Hồ sơ học sinh + khảo sát
CREATE TEMPORARY TABLE students_def (
  k VARCHAR(12) PRIMARY KEY, n INT, nick VARCHAR(60), grade INT, survey VARCHAR(400), needs VARCHAR(300)
);
INSERT INTO students_def VALUES
 ('ph-1', 1, 'Bé Minh', 6,  '{"goal":"foundation","learning":"practice","difficulty":"understanding","pace":"step_by_step","support":"feedback","lesson":"examples"}',
   'Củng cố nền tảng Toán, cần giải thích từng bước.'),
 ('ph-2', 2, 'Bé Lan',  7,  '{"goal":"confidence","learning":"visual","difficulty":"motivation","pace":"balanced","support":"support","lesson":"review"}',
   'Con ngại phát biểu, cần gia sư kiên nhẫn để tự tin hơn.'),
 ('ph-3', 3, 'Bé Nam',  8,  '{"goal":"exam","learning":"discussion","difficulty":"attention","pace":"challenge","support":"independent","lesson":"examples"}',
   'Chuẩn bị thi học kỳ, cần luyện đề.'),
 ('ph-4', 4, 'Bé Hà',   9,  '{"goal":"exam","learning":"practice","difficulty":"understanding","pace":"challenge","support":"feedback","lesson":"review"}',
   'Ôn thi vào lớp 10, cần hệ thống kiến thức.'),
 ('ph-5', 5, 'Bé Khoa', 10, '{"goal":"foundation","learning":"visual","difficulty":"attention","pace":"step_by_step","support":"support","lesson":"examples"}',
   'Mất căn bản hình học, cần học lại từ đầu.'),
 ('ph-6', 6, 'Bé Vy',   11, '{"goal":"confidence","learning":"discussion","difficulty":"motivation","pace":"balanced","support":"independent","lesson":"review"}',
   'Muốn tăng điểm Toán cuối năm.');

INSERT INTO student_profiles
  (id, parent_id, nickname, grade, province, mode, max_hourly_rate, availability, survey, needs, consent_at, visible)
SELECT CONCAT('f10e0000-0000-4000-a000-', LPAD(d.n, 12, '0')), i.uid, d.nick, d.grade, 'Huế', 'IN_PERSON', 250000,
       '["2_EVENING","4_EVENING"]', d.survey, d.needs, NOW(6), TRUE
FROM students_def d JOIN ids i USING (k);
INSERT INTO student_subjects (student_id, subject)
  SELECT CONCAT('f10e0000-0000-4000-a000-', LPAD(d.n, 12, '0')), 'Toán' FROM students_def d;

-- ---------------------------------------------------------------- 4. Quyết định OK / Match / xác nhận
-- Cặp: (học sinh n, gia sư). Bước 5: phụ huynh đã OK, gia sư chưa (OPEN, hạn 7 ngày).
-- Bước 6–7: hai bên đã OK (MATCHED), hạn xác nhận kết nối 3 ngày.
CREATE TEMPORARY TABLE pairs_def (
  sn INT, tk VARCHAR(12), pchoice VARCHAR(4), tchoice VARCHAR(4), status VARCHAR(8),
  pconfirm VARCHAR(7), tconfirm VARCHAR(7), PRIMARY KEY (sn, tk)
);
INSERT INTO pairs_def VALUES
 (2, 'gs-qt', 'LIKE', NULL,   'OPEN',    NULL,      NULL),
 (3, 'gs-3',  'LIKE', 'LIKE', 'MATCHED', NULL,      NULL),
 (4, 'gs-4',  'LIKE', 'LIKE', 'MATCHED', 'CONFIRM', NULL),
 (5, 'gs-5',  'LIKE', 'LIKE', 'MATCHED', 'CONFIRM', 'CONFIRM'),
 (6, 'gs-6',  'LIKE', 'LIKE', 'MATCHED', 'CONFIRM', 'CONFIRM');

INSERT INTO matching_pairs
  (student_id, tutor_id, parent_choice, tutor_choice, status, first_liked_at, expires_at, matched_at,
   parent_confirm, tutor_confirm, confirm_expires_at)
SELECT CONCAT('f10e0000-0000-4000-a000-', LPAD(p.sn, 12, '0')), t.tutor_id, p.pchoice, p.tchoice, p.status,
       TIMESTAMPADD(HOUR, -2, NOW(6)), TIMESTAMPADD(DAY, 7, NOW(6)),
       IF(p.status = 'MATCHED', TIMESTAMPADD(HOUR, -1, NOW(6)), NULL),
       p.pconfirm, p.tconfirm, IF(p.status = 'MATCHED', TIMESTAMPADD(DAY, 3, NOW(6)), NULL)
FROM pairs_def p JOIN tid t ON t.k = p.tk;

-- Lịch sử OK cũ của phụ huynh (dùng cho danh sách "đã quan tâm").
INSERT INTO swipe_interactions (student_id, tutor_id, interested)
  SELECT student_id, tutor_id, TRUE FROM matching_pairs WHERE student_id IN (SELECT id FROM student_profiles WHERE parent_id IN
    (SELECT uid FROM ids)) AND parent_choice = 'LIKE';

-- Thông báo chưa đọc: Match mới cho cả hai bên; "đã kết nối" cho hai cặp bước 7b/7c.
INSERT INTO matching_notifications (id, user_id, student_id, tutor_id, event_key, message)
SELECT UUID(), u.id, p.student_id, p.tutor_id, 'MATCH', 'Hai bên đã cùng OK. Bạn có một Match mới!'
FROM matching_pairs p
JOIN student_profiles s ON s.id = p.student_id
JOIN tutor_profiles t ON t.id = p.tutor_id
JOIN users u ON u.id IN (s.parent_id, t.user_id)
WHERE p.status = 'MATCHED' AND s.parent_id IN (SELECT uid FROM ids);
INSERT INTO matching_notifications (id, user_id, student_id, tutor_id, event_key, message)
SELECT UUID(), u.id, p.student_id, p.tutor_id, 'CONNECTED', 'Hai bên đã xác nhận kết nối. Có thể đề xuất lịch học thử.'
FROM matching_pairs p
JOIN student_profiles s ON s.id = p.student_id
JOIN tutor_profiles t ON t.id = p.tutor_id
JOIN users u ON u.id IN (s.parent_id, t.user_id)
WHERE p.parent_confirm = 'CONFIRM' AND p.tutor_confirm = 'CONFIRM' AND s.parent_id IN (SELECT uid FROM ids);

-- ---------------------------------------------------------------- 5. Học thử
-- Cặp bước 7b: phụ huynh đã đề xuất 3 khung giờ (4–6 ngày tới); gia sư cần chốt hoặc đề xuất giờ khác.
INSERT INTO trial_schedules (id, student_id, tutor_id, proposer_role, slots, status, version, expires_at, mode)
SELECT 'f10e0000-0000-4000-e000-000000000001', CONCAT('f10e0000-0000-4000-a000-', LPAD(5, 12, '0')), t.tutor_id, 'PARENT',
  JSON_ARRAY(DATE_FORMAT(TIMESTAMPADD(DAY, 4, UTC_TIMESTAMP()), '%Y-%m-%dT12:00:00Z'),
             DATE_FORMAT(TIMESTAMPADD(DAY, 5, UTC_TIMESTAMP()), '%Y-%m-%dT12:00:00Z'),
             DATE_FORMAT(TIMESTAMPADD(DAY, 6, UTC_TIMESTAMP()), '%Y-%m-%dT13:00:00Z')),
  'PENDING', 1, TIMESTAMPADD(DAY, 3, NOW(6)), 'IN_PERSON'
FROM tid t WHERE t.k = 'gs-5';
INSERT INTO matching_notifications (id, user_id, student_id, tutor_id, event_key, message)
SELECT UUID(), u.id, s.id, t.tutor_id, 'TRIAL_f10e0000-0000-4000-e000-000000000001_1_PROPOSED',
       'Có đề xuất giờ học thử mới. Mở lịch học thử để phản hồi.'
FROM tid t JOIN tutor_profiles tp ON tp.id = t.tutor_id
JOIN student_profiles s ON s.id = CONCAT('f10e0000-0000-4000-a000-', LPAD(5, 12, '0'))
JOIN users u ON u.id IN (s.parent_id, tp.user_id)
WHERE t.k = 'gs-5';

COMMIT;
