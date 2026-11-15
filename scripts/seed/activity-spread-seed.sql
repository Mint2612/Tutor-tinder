-- Dàn đều (không quá đều) mốc thời gian của dữ liệu demo trong 7 ngày gần nhất để trang "Giám sát" của
-- admin có biểu đồ hoạt động hợp lý, thay vì dồn hết vào ngày nạp seed.
--
-- Chỉ động vào tài khoản demo/test (email @tutormatch.local hoặc @example.test, không phải admin) và các
-- cặp Match giữa họ. Giữ nguyên: tài khoản thật, cặp có buổi học thử đã chốt giờ (dùng để thử đánh giá)
-- và cặp với tài khoản thật; tài khoản của họ luôn được đặt trước các cặp đó.
-- KHÔNG đổi hạn xử lý (expires_at, confirm_expires_at) nên các kịch bản demo vẫn dùng như cũ.
-- Ngẫu nhiên nhưng xác định (băm theo id): chạy lại trong cùng một ngày cho kết quả y hệt.
--
-- Quy ước: thứ 2–6 đông hơn chủ nhật, giờ cao điểm buổi tối. Thứ tự thời gian luôn hợp lý:
-- tài khoản → (gia sư) gửi duyệt → admin duyệt → OK đầu tiên → Match → học thử.
SET NAMES utf8mb4;
START TRANSACTION;

SET @now = UTC_TIMESTAMP(6);
-- 00:00 giờ Việt Nam của hôm nay, tính theo UTC.
SET @day0 = TIMESTAMPADD(HOUR, -7, DATE(TIMESTAMPADD(HOUR, 7, @now)));

-- Lịch học thử do script này tạo ở lần chạy trước (xóa trước để chạy lại cho cùng kết quả).
DELETE FROM trial_schedules WHERE id LIKE 'ac7e5eed-%';

-- ---------------------------------------------------------------- 1. Ngày tạo tài khoản (ngẫu nhiên ban đầu)
-- Ngày (0 = hôm nay … 6 = 6 ngày trước) chọn theo trọng số; giờ trong ngày nghiêng về buổi tối.
CREATE TEMPORARY TABLE uu AS
SELECT id, role,
  CASE WHEN u1 < 0.09 THEN 6 WHEN u1 < 0.25 THEN 5 WHEN u1 < 0.42 THEN 4 WHEN u1 < 0.59 THEN 3
       WHEN u1 < 0.75 THEN 2 WHEN u1 < 0.89 THEN 1 ELSE 0 END AS d,
  u2
FROM (SELECT id, role,
        MOD(CRC32(CONCAT(id, 'a')), 100000) / 100000 AS u1,
        MOD(CRC32(CONCAT(id, 'b')), 100000) / 100000 AS u2
      FROM users
      WHERE deleted_at IS NULL AND role <> 'ADMIN'
        AND (email LIKE '%@tutormatch.local' OR email LIKE '%@example.test')) x;
ALTER TABLE uu ADD created_new DATETIME(6) NULL;
UPDATE uu SET created_new = LEAST(TIMESTAMPADD(MINUTE, -10, @now),
  TIMESTAMPADD(SECOND,
    FLOOR(POW(u2, 0.75) * TIMESTAMPDIFF(SECOND,
        TIMESTAMPADD(HOUR, 7, TIMESTAMPADD(DAY, -d, @day0)),
        IF(d = 0, GREATEST(TIMESTAMPADD(MINUTE, -10, @now), TIMESTAMPADD(HOUR, 8, @day0)),
           TIMESTAMPADD(HOUR, 22, TIMESTAMPADD(DAY, -d, @day0))))),
    TIMESTAMPADD(HOUR, 7, TIMESTAMPADD(DAY, -d, @day0))));

-- ---------------------------------------------------------------- 2. Lần OK đầu tiên của từng cặp (chọn trước)
CREATE TEMPORARY TABLE uu2 AS SELECT id FROM uu;
CREATE TEMPORARY TABLE pp AS
SELECT p.student_id, p.tutor_id, s.parent_id, t.user_id AS tutor_user,
  MOD(CRC32(CONCAT(p.student_id, p.tutor_id, 'a')), 100000) / 100000 AS u1,
  MOD(CRC32(CONCAT(p.student_id, p.tutor_id, 'b')), 100000) / 100000 AS u2,
  MOD(CRC32(CONCAT(p.student_id, p.tutor_id, 'd')), 100000) / 100000 AS u4
FROM matching_pairs p
  JOIN student_profiles s ON s.id = p.student_id
  JOIN tutor_profiles t ON t.id = p.tutor_id
WHERE p.first_liked_at IS NOT NULL
  AND s.parent_id IN (SELECT id FROM uu)
  AND t.user_id IN (SELECT id FROM uu2)
  AND NOT EXISTS (SELECT 1 FROM trial_schedules q
                  WHERE q.student_id = p.student_id AND q.tutor_id = p.tutor_id AND q.chosen_at IS NOT NULL);
ALTER TABLE pp ADD d INT NULL, ADD first_new DATETIME(6) NULL, ADD matched_new DATETIME(6) NULL;
UPDATE pp SET d = CASE WHEN u1 < 0.08 THEN 6 WHEN u1 < 0.23 THEN 5 WHEN u1 < 0.40 THEN 4 WHEN u1 < 0.57 THEN 3
                       WHEN u1 < 0.73 THEN 2 WHEN u1 < 0.88 THEN 1 ELSE 0 END;
UPDATE pp SET first_new = LEAST(TIMESTAMPADD(MINUTE, -20, @now),
    TIMESTAMPADD(SECOND,
      FLOOR(POW(u2, 0.75) * TIMESTAMPDIFF(SECOND,
          TIMESTAMPADD(HOUR, 7, TIMESTAMPADD(DAY, -d, @day0)),
          IF(d = 0, GREATEST(TIMESTAMPADD(MINUTE, -30, @now), TIMESTAMPADD(HOUR, 8, @day0)),
             TIMESTAMPADD(HOUR, 22, TIMESTAMPADD(DAY, -d, @day0))))),
      TIMESTAMPADD(HOUR, 7, TIMESTAMPADD(DAY, -d, @day0))));
-- Match sau lần OK đầu 15 phút – 40 giờ, nhưng không vượt quá hiện tại (không dồn về "bây giờ").
UPDATE pp SET matched_new = TIMESTAMPADD(MINUTE,
    15 + FLOOR(u4 * LEAST(2385, GREATEST(0, TIMESTAMPDIFF(MINUTE, first_new, @now) - 17))), first_new);

-- ---------------------------------------------------------------- 3. Tài khoản phải có trước lần OK đầu tiên
-- Phụ huynh: tạo tài khoản + đồng ý (≤ 40 phút) trước lần OK sớm nhất.
-- Gia sư: gửi duyệt (≤ 90 phút) + admin duyệt (≤ 6 giờ) trước lần OK sớm nhất.
CREATE TEMPORARY TABLE pm AS SELECT parent_id AS uid, MIN(first_new) AS m FROM pp GROUP BY parent_id;
CREATE TEMPORARY TABLE tm AS SELECT tutor_user AS uid, MIN(first_new) AS m FROM pp GROUP BY tutor_user;
UPDATE uu JOIN pm ON pm.uid = uu.id
SET uu.created_new = LEAST(uu.created_new, TIMESTAMPADD(MINUTE, -(60 + MOD(CRC32(CONCAT(uu.id, 'l')), 120)), pm.m));
UPDATE uu JOIN tm ON tm.uid = uu.id
SET uu.created_new = LEAST(uu.created_new, TIMESTAMPADD(MINUTE, -(540 + MOD(CRC32(CONCAT(uu.id, 'l')), 180)), tm.m));
-- Các cặp được giữ nguyên (đã chốt học thử, hoặc có tài khoản thật) cũng cần tài khoản có trước.
CREATE TEMPORARY TABLE pq AS
SELECT s.parent_id, t.user_id AS tutor_user, p.first_liked_at AS f
FROM matching_pairs p
  JOIN student_profiles s ON s.id = p.student_id
  JOIN tutor_profiles t ON t.id = p.tutor_id
  LEFT JOIN pp ON pp.student_id = p.student_id AND pp.tutor_id = p.tutor_id
WHERE p.first_liked_at IS NOT NULL AND pp.student_id IS NULL;
CREATE TEMPORARY TABLE pq2 AS SELECT parent_id, tutor_user, f FROM pq;
CREATE TEMPORARY TABLE pm2 AS SELECT parent_id AS uid, MIN(f) AS m FROM pq GROUP BY parent_id;
CREATE TEMPORARY TABLE tm2 AS SELECT tutor_user AS uid, MIN(f) AS m FROM pq2 GROUP BY tutor_user;
UPDATE uu JOIN pm2 ON pm2.uid = uu.id
SET uu.created_new = LEAST(uu.created_new, TIMESTAMPADD(MINUTE, -(60 + MOD(CRC32(CONCAT(uu.id, 'k')), 120)), pm2.m));
UPDATE uu JOIN tm2 ON tm2.uid = uu.id
SET uu.created_new = LEAST(uu.created_new, TIMESTAMPADD(MINUTE, -(540 + MOD(CRC32(CONCAT(uu.id, 'k')), 180)), tm2.m));
-- Gia sư đang chờ duyệt / bản nháp giữ nguyên giấy tờ và giờ gửi: tài khoản phải có trước đó.
UPDATE uu
  JOIN tutor_profiles t ON t.user_id = uu.id
  LEFT JOIN (SELECT tutor_id, MIN(created_at) AS first_doc FROM tutor_documents GROUP BY tutor_id) d ON d.tutor_id = t.id
SET uu.created_new = LEAST(uu.created_new,
  TIMESTAMPADD(MINUTE, -45, LEAST(COALESCE(t.submitted_at, '9999-01-01'), COALESCE(d.first_doc, '9999-01-01'))))
WHERE t.workflow_state IN ('PENDING_REVIEW', 'DRAFT');
UPDATE users u JOIN uu ON uu.id = u.id SET u.created_at = uu.created_new;

-- ---------------------------------------------------------------- 4. Gia sư đã duyệt: gửi duyệt → admin duyệt
UPDATE tutor_profiles t JOIN users u ON u.id = t.user_id
SET t.submitted_at = LEAST(TIMESTAMPADD(MINUTE, 20 + MOD(CRC32(CONCAT(t.id, 's')), 70), u.created_at),
                           TIMESTAMPADD(MINUTE, -6, @now))
WHERE t.workflow_state = 'APPROVED' AND t.user_id IN (SELECT id FROM uu);
-- Admin duyệt sau 1–6 giờ (không quá hiện tại).
UPDATE tutor_profiles t
SET t.verified_at = GREATEST(TIMESTAMPADD(MINUTE, 1, t.submitted_at),
      LEAST(TIMESTAMPADD(MINUTE, 60 + MOD(CRC32(CONCAT(t.id, 'v')), 300), t.submitted_at),
            TIMESTAMPADD(MINUTE, -5, @now)))
WHERE t.workflow_state = 'APPROVED' AND t.user_id IN (SELECT id FROM uu);
UPDATE tutor_documents d JOIN tutor_profiles t ON t.id = d.tutor_id
SET d.created_at = TIMESTAMPADD(MINUTE, -(5 + MOD(CRC32(d.id), 15)), t.submitted_at)
WHERE t.workflow_state = 'APPROVED' AND t.user_id IN (SELECT id FROM uu);
UPDATE verification_audit a JOIN tutor_profiles t ON t.id = a.tutor_id
SET a.created_at = t.verified_at
WHERE a.decision = 'APPROVED' AND t.workflow_state = 'APPROVED' AND t.user_id IN (SELECT id FROM uu);
UPDATE document_reviews r
  JOIN tutor_documents d ON d.id = r.document_id
  JOIN tutor_profiles t ON t.id = d.tutor_id
SET r.reviewed_at = TIMESTAMPADD(MINUTE, -10, t.verified_at)
WHERE t.workflow_state = 'APPROVED' AND t.user_id IN (SELECT id FROM uu);

-- ---------------------------------------------------------------- 5. Phụ huynh đồng ý + tạo hồ sơ học sinh
UPDATE student_profiles s JOIN users pu ON pu.id = s.parent_id
SET s.consent_at = LEAST(TIMESTAMPADD(MINUTE, 5 + MOD(CRC32(s.id), 40), pu.created_at), TIMESTAMPADD(MINUTE, -8, @now))
WHERE s.parent_id IN (SELECT id FROM uu);

-- ---------------------------------------------------------------- 6. OK đầu tiên và Match
UPDATE matching_pairs p JOIN pp ON pp.student_id = p.student_id AND pp.tutor_id = p.tutor_id
SET p.first_liked_at = pp.first_new,
    p.created_at = TIMESTAMPADD(MINUTE, -1, pp.first_new),
    p.matched_at = IF(p.matched_at IS NULL, NULL, pp.matched_new);

-- Thông báo Match / kết nối khớp thời điểm tương ứng (kết nối sau Match 1–25 giờ, không vượt quá hiện tại).
UPDATE matching_notifications n
  JOIN matching_pairs p ON p.student_id = n.student_id AND p.tutor_id = n.tutor_id
  JOIN pp ON pp.student_id = p.student_id AND pp.tutor_id = p.tutor_id
SET n.created_at = CASE
  WHEN n.event_key = 'MATCH' THEN p.matched_at
  ELSE TIMESTAMPADD(MINUTE, 60 + FLOOR(MOD(CRC32(n.id), 1500) / 1500 *
         LEAST(1500, GREATEST(0, TIMESTAMPDIFF(MINUTE, p.matched_at, @now) - 62))), p.matched_at) END
WHERE n.event_key IN ('MATCH', 'CONNECTED') AND p.matched_at IS NOT NULL;
UPDATE matching_notifications n
  JOIN pp ON pp.student_id = n.student_id AND pp.tutor_id = n.tutor_id
SET n.read_at = LEAST(@now, TIMESTAMPADD(MINUTE, 2 + MOD(CRC32(n.id), 90), n.created_at))
WHERE n.event_key IN ('MATCH', 'CONNECTED') AND n.read_at IS NOT NULL;

-- ---------------------------------------------------------------- 7. Học thử
-- Đề xuất đang chờ phản hồi: gửi 1–36 giờ sau khi kết nối (không vượt quá hiện tại).
UPDATE trial_schedules q
  JOIN matching_pairs p ON p.student_id = q.student_id AND p.tutor_id = q.tutor_id
  JOIN pp ON pp.student_id = p.student_id AND pp.tutor_id = p.tutor_id
SET q.created_at = TIMESTAMPADD(MINUTE, 60 + FLOOR(MOD(CRC32(q.id), 2100) / 2100 *
        LEAST(2100, GREATEST(0, TIMESTAMPDIFF(MINUTE, p.matched_at, @now) - 62))), p.matched_at),
    q.updated_at = TIMESTAMPADD(MINUTE, 60 + FLOOR(MOD(CRC32(q.id), 2100) / 2100 *
        LEAST(2100, GREATEST(0, TIMESTAMPDIFF(MINUTE, p.matched_at, @now) - 62))), p.matched_at)
WHERE q.status = 'PENDING' AND p.matched_at IS NOT NULL;

-- Thêm một số lịch học thử cho các cặp đã kết nối nhưng chưa có lịch (trừ các cặp dành riêng cho demo).
-- Khoảng 45% cặp được chọn; ~45% đã chốt, ~30% hủy, ~25% hết hạn (chỉ khi đã đủ cũ).
CREATE TEMPORARY TABLE nt AS
SELECT p.student_id, p.tutor_id, s.mode, p.matched_at,
  MOD(CRC32(CONCAT(p.student_id, p.tutor_id, 't')), 100000) / 100000 AS h1,
  MOD(CRC32(CONCAT(p.student_id, p.tutor_id, 'u')), 100000) / 100000 AS h2,
  MOD(CRC32(CONCAT(p.student_id, p.tutor_id, 'w')), 100000) / 100000 AS h3,
  MD5(CONCAT(p.student_id, p.tutor_id)) AS hash
FROM matching_pairs p
  JOIN student_profiles s ON s.id = p.student_id
  JOIN users pu ON pu.id = s.parent_id
  JOIN tutor_profiles t ON t.id = p.tutor_id
  JOIN users tu ON tu.id = t.user_id
WHERE p.status = 'MATCHED' AND p.parent_confirm = 'CONFIRM' AND p.tutor_confirm = 'CONFIRM' AND p.matched_at IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM trial_schedules q WHERE q.student_id = p.student_id AND q.tutor_id = p.tutor_id)
  AND (pu.email LIKE '%@tutormatch.local' OR pu.email LIKE '%@example.test')
  AND (tu.email LIKE '%@tutormatch.local' OR tu.email LIKE '%@example.test')
  AND pu.email NOT LIKE 'demo.flow.%' AND pu.email NOT LIKE 'flow-%' AND s.id NOT LIKE 'd004e000-%'
  ;
DELETE FROM nt WHERE h1 >= 0.45;
ALTER TABLE nt ADD created DATETIME(6) NULL, ADD st VARCHAR(10) NULL, ADD chosen DATETIME(6) NULL;
UPDATE nt SET created = TIMESTAMPADD(MINUTE, 120 + FLOOR(h2 *
    LEAST(2280, GREATEST(0, TIMESTAMPDIFF(MINUTE, matched_at, @now) - 125))), matched_at);
-- Cặp vừa Match chưa đủ thời gian để có lịch học thử thì bỏ qua.
DELETE FROM nt WHERE created > TIMESTAMPADD(MINUTE, -5, @now);
UPDATE nt SET st = CASE WHEN created <= TIMESTAMPADD(DAY, -3, @now) AND h3 < 0.25 THEN 'EXPIRED'
                        WHEN h3 < 0.70 THEN 'CONFIRMED' ELSE 'CANCELLED' END,
              chosen = TIMESTAMPADD(HOUR, 24 + FLOOR(h2 * 48), created);
INSERT INTO trial_schedules
  (id, student_id, tutor_id, proposer_role, slots, status, version, chosen_at, expires_at, cancellation_reason,
   created_at, updated_at, mode)
SELECT CONCAT('ac7e5eed-', SUBSTR(hash, 1, 4), '-4000-8000-', SUBSTR(hash, 5, 12)),
  student_id, tutor_id, IF(h1 < 0.15, 'TUTOR', 'PARENT'),
  IF(st = 'EXPIRED',
     JSON_ARRAY(DATE_FORMAT(TIMESTAMPADD(DAY, 2, created), '%Y-%m-%dT%H:00:00Z'), DATE_FORMAT(TIMESTAMPADD(DAY, 3, created), '%Y-%m-%dT%H:00:00Z')),
     JSON_ARRAY(DATE_FORMAT(chosen, '%Y-%m-%dT%H:00:00Z'), DATE_FORMAT(TIMESTAMPADD(DAY, 1, chosen), '%Y-%m-%dT%H:00:00Z'))),
  st,
  CASE st WHEN 'CONFIRMED' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 1 END,
  IF(st = 'EXPIRED', NULL, chosen),
  IF(st = 'EXPIRED', TIMESTAMPADD(DAY, 2, created), LEAST(TIMESTAMPADD(DAY, 3, created), chosen)),
  IF(st = 'CANCELLED', 'Gia đình có lịch đột xuất, hẹn lại sau.', NULL),
  created,
  CASE st WHEN 'CONFIRMED' THEN LEAST(TIMESTAMPADD(MINUTE, -1, @now), TIMESTAMPADD(MINUTE, 60 + FLOOR(h3 * 1200), created))
          WHEN 'CANCELLED' THEN LEAST(TIMESTAMPADD(MINUTE, -1, @now), TIMESTAMPADD(HOUR, -3, chosen))
          ELSE TIMESTAMPADD(DAY, 2, created) END,
  mode
FROM nt;

COMMIT;
