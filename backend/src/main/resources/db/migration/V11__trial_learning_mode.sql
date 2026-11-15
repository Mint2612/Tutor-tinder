ALTER TABLE trial_schedules ADD mode VARCHAR(9) NOT NULL DEFAULT 'ONLINE' CHECK(mode IN ('ONLINE','IN_PERSON'));
UPDATE trial_schedules q JOIN student_profiles s ON s.id=q.student_id SET q.mode=s.mode;
