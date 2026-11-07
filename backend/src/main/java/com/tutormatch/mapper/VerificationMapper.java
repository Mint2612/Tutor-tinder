package com.tutormatch.mapper;

import static com.tutormatch.mapper.JsonFields.*;

import com.tutormatch.dto.verification.*;
import com.tutormatch.model.DocumentContent;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class VerificationMapper {
  public DocumentResponse document(Map<String, Object> r) {
    return new DocumentResponse(
        text(r, "id"),
        text(r, "filename"),
        text(r, "content_type"),
        number(r, "revision"),
        instant(r.get("created_at")));
  }

  public AuditResponse audit(Map<String, Object> r) {
    return new AuditResponse(
        text(r, "id"), text(r, "decision"), text(r, "reason"), instant(r.get("created_at")));
  }

  public DocumentContent content(Map<String, Object> r) {
    return new DocumentContent(
        text(r, "id"),
        text(r, "tutor_id"),
        text(r, "filename"),
        text(r, "content_type"),
        (byte[]) r.get("content"),
        text(r, "storage_key"));
  }
}
