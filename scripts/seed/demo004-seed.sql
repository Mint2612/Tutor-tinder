-- Seed demo cho 2 tài khoản có sẵn:
--   Phụ huynh Demo 004   seed.parent004@tutormatch.local
--   Phạm Đức Huy (Demo 004)  seed.tutor004@tutormatch.local   (gia sư Hóa học + Vật lý, Hải Phòng, 175.000đ/giờ)
-- Tạo 6 hồ sơ học sinh mới cho phụ huynh, mỗi hồ sơ ở một bước của luồng với đúng gia sư này.
-- Chạy lại để ĐẶT LẠI 6 hồ sơ này; không đụng hồ sơ/Match/lịch học cũ của hai tài khoản.
--
-- Cách ly: hồ sơ học trực tiếp tại Hải Phòng, môn Hóa học + Vật lý, ngân sách 200.000đ/giờ. Chỉ gia sư
-- này thỏa cả bốn điều kiện (các gia sư khác ở Hải Phòng dạy đủ hai môn đều có học phí >= 225.000đ),
-- nên thẻ gia sư xuất hiện đúng một lần cho mỗi hồ sơ chưa OK.
SET NAMES utf8mb4;
START TRANSACTION;

SET @P = (SELECT id FROM users WHERE email = 'seed.parent004@tutormatch.local' AND role = 'PARENT');
SET @TU = (SELECT id FROM users WHERE email = 'seed.tutor004@tutormatch.local' AND role = 'TUTOR');
SET @T = (SELECT id FROM tutor_profiles WHERE user_id = @TU);
-- Thiếu tài khoản → dừng ngay (cột NOT NULL).
CREATE TEMPORARY TABLE guard (v VARCHAR(36) NOT NULL);
INSERT INTO guard VALUES (@P), (@T);

-- ---------------------------------------------------------------- Dọn bản demo cũ
CREATE TEMPORARY TABLE old_students AS
  SELECT id FROM student_profiles WHERE parent_id = @P AND id LIKE 'd004e000-%';
DELETE FROM trial_reviews WHERE trial_id IN (SELECT id FROM trial_schedules WHERE student_id IN (SELECT id FROM old_students));
DELETE FROM trial_schedules WHERE student_id IN (SELECT id FROM old_students);
DELETE FROM pair_ai_scores WHERE student_id IN (SELECT id FROM old_students);
DELETE FROM lesson_requests WHERE student_id IN (SELECT id FROM old_students);
DELETE FROM matching_daily_usage WHERE profile_id IN (SELECT id FROM old_students);
DELETE FROM student_profiles WHERE id IN (SELECT id FROM old_students); -- cascade: môn, cặp, thông báo, OK cũ

-- ---------------------------------------------------------------- 6 hồ sơ học sinh + khảo sát
CREATE TEMPORARY TABLE sd (
  n INT PRIMARY KEY, nick VARCHAR(80), grade INT, survey VARCHAR(400), needs VARCHAR(300)
);
INSERT INTO sd VALUES
 (1, '01 Demo - Bước 4: chưa OK gia sư',               9,  '{"goal":"exam","learning":"practice","difficulty":"understanding","pace":"step_by_step","support":"feedback","lesson":"examples"}',
     'Ôn thi vào lớp 10, cần củng cố Hóa học và Vật lý.'),
 (2, '02 Demo - Bước 5: gia sư thấy nhãn Quan tâm bạn', 10, '{"goal":"foundation","learning":"visual","difficulty":"attention","pace":"balanced","support":"support","lesson":"review"}',
     'Mất căn bản, cần học lại từ đầu với nhiều ví dụ trực quan.'),
 (3, '03 Demo - Bước 6: đã Match, chưa xác nhận',        10, '{"goal":"confidence","learning":"discussion","difficulty":"motivation","pace":"step_by_step","support":"feedback","lesson":"examples"}',
     'Con thiếu tự tin, cần gia sư trao đổi nhiều.'),
 (4, '04 Demo - Bước 7a: phụ huynh đã xác nhận',         11, '{"goal":"exam","learning":"practice","difficulty":"understanding","pace":"challenge","support":"independent","lesson":"review"}',
     'Luyện đề thi học kỳ môn Hóa học và Vật lý.'),
 (5, '05 Demo - Bước 7b: đã kết nối, học thử chờ chốt',  11, '{"goal":"exam","learning":"visual","difficulty":"attention","pace":"balanced","support":"feedback","lesson":"examples"}',
     'Cần gia sư kèm nhóm bài tập khó về điện và phản ứng hóa học.'),
 (6, '06 Demo - Bước 7c: đã kết nối, chưa đặt lịch',     12, '{"goal":"confidence","learning":"practice","difficulty":"motivation","pace":"challenge","support":"independent","lesson":"review"}',
     'Ôn thi tốt nghiệp, cần kế hoạch học rõ ràng.');

INSERT INTO student_profiles
  (id, parent_id, nickname, grade, province, mode, max_hourly_rate, availability, survey, needs, consent_at, visible)
SELECT CONCAT('d004e000-0000-4000-a000-', LPAD(n, 12, '0')), @P, nick, grade, 'Hải Phòng', 'IN_PERSON', 200000,
       '["1_19:00-20:30","3_19:00-20:30"]', survey, needs, NOW(6), TRUE
FROM sd;
INSERT INTO student_subjects (student_id, subject)
  SELECT CONCAT('d004e000-0000-4000-a000-', LPAD(n, 12, '0')), 'Hóa học' FROM sd;
INSERT INTO student_subjects (student_id, subject)
  SELECT CONCAT('d004e000-0000-4000-a000-', LPAD(n, 12, '0')), 'Vật lý' FROM sd;

-- ---------------------------------------------------------------- Quyết định OK / Match / xác nhận
-- Hồ sơ 1: không có hàng nào → thẻ gia sư hiện ra để phụ huynh bấm OK.
CREATE TEMPORARY TABLE pd (
  n INT PRIMARY KEY, pchoice VARCHAR(4), tchoice VARCHAR(4), status VARCHAR(8), pconfirm VARCHAR(7), tconfirm VARCHAR(7)
);
INSERT INTO pd VALUES
 (2, 'LIKE', NULL,   'OPEN',    NULL,      NULL),
 (3, 'LIKE', 'LIKE', 'MATCHED', NULL,      NULL),
 (4, 'LIKE', 'LIKE', 'MATCHED', 'CONFIRM', NULL),
 (5, 'LIKE', 'LIKE', 'MATCHED', 'CONFIRM', 'CONFIRM'),
 (6, 'LIKE', 'LIKE', 'MATCHED', 'CONFIRM', 'CONFIRM');
INSERT INTO matching_pairs
  (student_id, tutor_id, parent_choice, tutor_choice, status, first_liked_at, expires_at, matched_at,
   parent_confirm, tutor_confirm, confirm_expires_at)
SELECT CONCAT('d004e000-0000-4000-a000-', LPAD(n, 12, '0')), @T, pchoice, tchoice, status,
       TIMESTAMPADD(HOUR, -2, NOW(6)), TIMESTAMPADD(DAY, 7, NOW(6)),
       IF(status = 'MATCHED', TIMESTAMPADD(HOUR, -1, NOW(6)), NULL), pconfirm, tconfirm,
       IF(status = 'MATCHED', TIMESTAMPADD(DAY, 3, NOW(6)), NULL)
FROM pd;
INSERT INTO swipe_interactions (student_id, tutor_id, interested)
  SELECT student_id, tutor_id, TRUE FROM matching_pairs
  WHERE tutor_id = @T AND student_id LIKE 'd004e000-%' AND parent_choice = 'LIKE';

-- Thông báo chưa đọc cho cả hai bên.
INSERT INTO matching_notifications (id, user_id, student_id, tutor_id, event_key, message)
  SELECT UUID(), u, student_id, @T, 'MATCH', 'Hai bên đã cùng OK. Bạn có một Match mới!'
  FROM matching_pairs JOIN (SELECT @P AS u UNION SELECT @TU) users_
  WHERE tutor_id = @T AND student_id LIKE 'd004e000-%' AND status = 'MATCHED';
INSERT INTO matching_notifications (id, user_id, student_id, tutor_id, event_key, message)
  SELECT UUID(), u, student_id, @T, 'CONNECTED', 'Hai bên đã xác nhận kết nối. Có thể đề xuất lịch học thử.'
  FROM matching_pairs JOIN (SELECT @P AS u UNION SELECT @TU) users_
  WHERE tutor_id = @T AND student_id LIKE 'd004e000-%' AND parent_confirm = 'CONFIRM' AND tutor_confirm = 'CONFIRM';

-- ---------------------------------------------------------------- Hồ sơ 5: phụ huynh đã đề xuất 3 giờ học thử (19:00 giờ Việt Nam)
SET @STU5 = 'd004e000-0000-4000-a000-000000000005';
SET @TRIAL5 = 'd004e000-0000-4000-e000-000000000005';
INSERT INTO trial_schedules (id, student_id, tutor_id, proposer_role, slots, status, version, expires_at, mode)
VALUES (@TRIAL5, @STU5, @T, 'PARENT',
  JSON_ARRAY(DATE_FORMAT(TIMESTAMPADD(DAY, 4, UTC_TIMESTAMP()), '%Y-%m-%dT12:00:00Z'),
             DATE_FORMAT(TIMESTAMPADD(DAY, 5, UTC_TIMESTAMP()), '%Y-%m-%dT12:00:00Z'),
             DATE_FORMAT(TIMESTAMPADD(DAY, 6, UTC_TIMESTAMP()), '%Y-%m-%dT12:00:00Z')),
  'PENDING', 1, TIMESTAMPADD(DAY, 3, NOW(6)), 'IN_PERSON');
INSERT INTO matching_notifications (id, user_id, student_id, tutor_id, event_key, message)
  SELECT UUID(), u, @STU5, @T, CONCAT('TRIAL_', @TRIAL5, '_1_PROPOSED'),
         'Có đề xuất giờ học thử mới. Mở lịch học thử để phản hồi.'
  FROM (SELECT @P AS u UNION SELECT @TU) users_;

COMMIT;
