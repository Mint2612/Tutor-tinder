package com.tutormatch.mapper;

import static com.tutormatch.mapper.JsonFields.*;

import com.tutormatch.dto.lesson.LessonResponse;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {
  public LessonResponse fromRow(Map<String, Object> r) {
    return new LessonResponse(
        text(r, "id"),
        text(r, "student_id"),
        text(r, "tutor_id"),
        text(r, "status"),
        instant(r.get("proposed_at")),
        instant(r.get("expires_at")),
        text(r, "message"),
        instant(r.get("created_at")),
        text(r, "nickname"),
        text(r, "display_name"),
        text(r, "parent_email"),
        text(r, "parent_phone"),
        text(r, "tutor_email"),
        text(r, "tutor_phone"));
  }
}
