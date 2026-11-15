-- Phạm Đức Huy (Demo 004) = seed.tutor004@tutormatch.local trở thành gia sư "nổi bật":
--  * luôn đứng ĐẦU danh sách Khám phá và Gợi ý AI của mọi phụ huynh (cột featured, migration V13);
--  * hồ sơ được mở rộng để phù hợp với hầu hết học sinh: đủ 7 môn, lớp 1–12, online và trực tiếp,
--    học phí 80.000đ/giờ (thấp nhất hệ thống), rảnh cả tuần từ 00:00 đến 23:59, kinh nghiệm 15 năm.
-- Nổi bật chỉ đổi THỨ TỰ: gia sư vẫn phải đạt điều kiện bắt buộc. Không đạt khi: học sinh học trực tiếp ngoài
-- Hải Phòng (gia sư ở Hải Phòng) hoặc ngân sách dưới 80.000đ/giờ.
-- Chỉ một gia sư được nổi bật tại một thời điểm. Giữ nguyên Match, lịch học và thông báo cũ.
-- Chạy sau khi backend đã khởi động với migration V13.
SET NAMES utf8mb4;
START TRANSACTION;

SET @T = (SELECT t.id FROM tutor_profiles t JOIN users u ON u.id = t.user_id WHERE u.email = 'seed.tutor004@tutormatch.local');
CREATE TEMPORARY TABLE guard (v VARCHAR(36) NOT NULL);
INSERT INTO guard VALUES (@T);

UPDATE tutor_profiles SET featured = FALSE WHERE featured = TRUE AND id <> @T;
UPDATE tutor_profiles SET
  featured = TRUE, visible = TRUE, accepting_students = TRUE, deleted_at = NULL,
  workflow_state = 'APPROVED', verification_status = 'APPROVED', verified_at = COALESCE(verified_at, NOW(6)),
  hourly_rate = 80000, experience_years = 15,
  availability = '["1_00:00-23:59","2_00:00-23:59","3_00:00-23:59","4_00:00-23:59","5_00:00-23:59","6_00:00-23:59","7_00:00-23:59"]',
  teaching_style = 'Kiên nhẫn, hướng dẫn từng bước',
  teaching_description = 'Dạy mọi môn phổ thông từ lớp 1 đến lớp 12: chia nhỏ kiến thức, luyện từng dạng bài và nhận xét sau mỗi buổi học. Lịch linh hoạt cả tuần.',
  background = 'Giáo viên nhiều năm kinh nghiệm dạy kèm học sinh mọi cấp, từng hướng dẫn nhiều nhóm học sinh yếu lẫn học sinh giỏi.',
  embedding = NULL, embedding_model = NULL
WHERE id = @T;

DELETE FROM tutor_subjects WHERE tutor_id = @T;
INSERT INTO tutor_subjects (tutor_id, subject) VALUES
  (@T, 'Toán'), (@T, 'Ngữ văn'), (@T, 'Tiếng Anh'), (@T, 'Vật lý'), (@T, 'Hóa học'), (@T, 'Sinh học'), (@T, 'Tin học');
DELETE FROM tutor_grades WHERE tutor_id = @T;
INSERT INTO tutor_grades (tutor_id, grade) VALUES
  (@T, 1), (@T, 2), (@T, 3), (@T, 4), (@T, 5), (@T, 6), (@T, 7), (@T, 8), (@T, 9), (@T, 10), (@T, 11), (@T, 12);
DELETE FROM tutor_modes WHERE tutor_id = @T;
INSERT INTO tutor_modes (tutor_id, mode) VALUES (@T, 'ONLINE'), (@T, 'IN_PERSON');

-- Điểm AI cũ của gia sư này tính theo hồ sơ trước khi mở rộng.
DELETE FROM pair_ai_scores WHERE tutor_id = @T;

COMMIT;
