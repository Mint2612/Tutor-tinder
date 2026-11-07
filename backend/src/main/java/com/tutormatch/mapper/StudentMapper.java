package com.tutormatch.mapper;

import static com.tutormatch.mapper.JsonFields.*;

import com.tutormatch.dto.profile.StudentResponse;
import com.tutormatch.model.StudentProfile;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {
  public StudentProfile fromRow(Map<String, Object> row, List<String> subjects) {
    return new StudentProfile(
        text(row, "id"),
        text(row, "parent_id"),
        text(row, "nickname"),
        number(row, "grade"),
        subjects,
        text(row, "province"),
        text(row, "mode"),
        number(row, "max_hourly_rate"),
        strings(row.get("availability")),
        text(row, "needs"),
        survey(row.get("survey")),
        instant(row.get("consent_at")));
  }

  public StudentResponse response(StudentProfile s) {
    return new StudentResponse(
        s.id(),
        s.nickname(),
        s.grade(),
        s.subjects(),
        s.province(),
        s.mode(),
        s.budget(),
        s.availability(),
        s.needs(),
        s.survey(),
        s.consentAt());
  }
}
