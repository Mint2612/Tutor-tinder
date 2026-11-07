package com.tutormatch.repository;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("demo")
public class DemoDataRepository {
  private final JdbcTemplate jdbc;

  public DemoDataRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void seed() {
    for (int i = 1; i <= 20; i++) {
      String userId = id("user-" + i);
      String tutorId = id("tutor-" + i);
      jdbc.update(
          """
          INSERT INTO users (id, email, full_name, role)
          VALUES (?, ?, ?, 'TUTOR') ON DUPLICATE KEY UPDATE id = id
          """,
          userId,
          "tutor" + i + "@example.test",
          "Gia sư mẫu " + i);
      jdbc.update(
          """
          INSERT INTO tutor_profiles
          (id, user_id, province, hourly_rate, teaching_description, availability, survey)
          VALUES (?, ?, ?, ?, ?, '[]', '{}') ON DUPLICATE KEY UPDATE id = id
          """,
          tutorId,
          userId,
          "Hồ Chí Minh",
          100000 + i * 5000,
          "Kiên nhẫn, giải thích từng bước và hướng dẫn thực hành.");
      jdbc.update(
          "INSERT INTO tutor_subjects VALUES (?, 'math') ON DUPLICATE KEY UPDATE subject = subject",
          tutorId);
      jdbc.update(
          "INSERT INTO tutor_grades VALUES (?, 6) ON DUPLICATE KEY UPDATE grade = grade", tutorId);
      jdbc.update(
          "INSERT INTO tutor_modes VALUES (?, 'ONLINE') ON DUPLICATE KEY UPDATE mode = mode",
          tutorId);
    }
  }

  private static String id(String value) {
    return UUID.nameUUIDFromBytes(("tutormatch-demo-" + value).getBytes(StandardCharsets.UTF_8))
        .toString();
  }
}
