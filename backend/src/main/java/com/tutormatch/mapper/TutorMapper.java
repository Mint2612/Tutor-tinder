package com.tutormatch.mapper;

import static com.tutormatch.mapper.JsonFields.*;

import com.tutormatch.dto.profile.TutorResponse;
import com.tutormatch.dto.verification.*;
import com.tutormatch.model.TutorProfile;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class TutorMapper {
  public TutorProfile fromRow(
      Map<String, Object> row, List<String> subjects, List<Integer> grades, List<String> modes) {
    return new TutorProfile(
        text(row, "id"),
        text(row, "user_id"),
        text(row, "display_name"),
        text(row, "photo_url"),
        subjects,
        grades,
        modes,
        text(row, "province"),
        number(row, "hourly_rate"),
        text(row, "teaching_description"),
        text(row, "teaching_style"),
        text(row, "background"),
        number(row, "experience_years"),
        strings(row.get("availability")),
        text(row, "workflow_state"),
        text(row, "verification_status"),
        bool(row, "visible"),
        text(row, "rejection_reason"),
        number(row, "revision"),
        text(row, "submission_id"),
        instant(row.get("submitted_at")),
        instant(row.get("verified_at")),
        text(row, "achievements"),
        text(row, "legal_name"), bool(row, "accepting_students"));
  }

  public TutorResponse publicResponse(TutorProfile t) {
    return response(t, false, null, null);
  }

  public TutorResponse ownerResponse(TutorProfile t) {
    return response(t, true, null, null);
  }

  public TutorResponse adminResponse(
      TutorProfile t, List<DocumentResponse> documents, List<AuditResponse> audit) {
    return response(t, true, documents, audit);
  }

  private TutorResponse response(
      TutorProfile t,
      boolean privateFields,
      List<DocumentResponse> documents,
      List<AuditResponse> audit) {
    return new TutorResponse(
        t.id(),
        t.displayName(),
        t.photoUrl(),
        t.subjects(),
        t.grades(),
        t.modes(),
        t.province(),
        t.hourlyRate(),
        t.methodology(),
        t.style(),
        t.background(),
        t.experience(),
        t.availability(),
        t.achievements(),
        privateFields ? t.legalName() : null,
        t.workflowState(),
        t.verificationStatus(),
        t.verifiedAt(),
        privateFields ? t.userId() : null,
        privateFields ? t.visible() : null,
        privateFields ? t.rejectionReason() : null,
        privateFields ? t.revision() : null,
        privateFields ? t.submittedAt() : null,
        documents,
        audit, privateFields ? t.acceptingStudents() : null);
  }
}
