package com.tutormatch.platform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {
  final RecommendationService service =
      new RecommendationService(mock(ProfileService.class), mock(LlmRanker.class));

  Map<String, Object> student() {
    return new HashMap<>(
        Map.of(
            "grade",
            6,
            "subjects",
            List.of("Toán"),
            "mode",
            "IN_PERSON",
            "province",
            "Hà Nội",
            "max_hourly_rate",
            200000,
            "availability",
            List.of("1_EVENING")));
  }

  Map<String, Object> tutor() {
    var t = new HashMap<String, Object>();
    t.put("workflow_state", "APPROVED");
    t.put("verification_status", "APPROVED");
    t.put("grades", List.of(6));
    t.put("subjects", List.of("Toán"));
    t.put("modes", List.of("IN_PERSON"));
    t.put("province", "Hà Nội");
    t.put("hourly_rate", 150000);
    t.put("availability", List.of("1_EVENING"));
    return t;
  }

  @Test
  void everyMandatoryConstraintBlocksAnIneligibleTutor() {
    assertTrue(service.eligible(student(), tutor()));
    for (var change :
        Map.of(
                "workflow_state",
                "DRAFT",
                "verification_status",
                "PENDING",
                "grades",
                List.of(7),
                "subjects",
                List.of("Văn"),
                "modes",
                List.of("ONLINE"),
                "province",
                "Huế",
                "hourly_rate",
                250000,
                "availability",
                List.of("2_AM"))
            .entrySet()) {
      var t = tutor();
      t.put(change.getKey(), change.getValue());
      assertFalse(service.eligible(student(), t), change.getKey());
    }
  }

  @Test
  void anonymizationDropsUnknownFreeText() {
    assertEquals(
        List.of("practice"),
        service.safePreferences(
            Map.of("learning", "practice", "details", "Child name, email@example.test")));
  }

  @Test
  void invalidOrInventedLlmOutputFailsClosed() {
    var candidate = Map.of("candidate-0", tutor());
    var valid =
        Map.<String, Object>of(
            "tutorId",
            "candidate-0",
            "level",
            "GOOD",
            "strengths",
            List.of("Cách dạy từng bước"),
            "concerns",
            List.of("Cần học thử"),
            "explanation",
            "Hãy tìm hiểu thêm trong buổi học thử");
    assertEquals(1, service.validate(List.of(valid), candidate).size());
    var invalid = new HashMap<>(valid);
    invalid.put("tutorId", "invented");
    assertThrows(
        IllegalArgumentException.class, () -> service.validate(List.of(invalid), candidate));
    assertThrows(
        IllegalArgumentException.class, () -> service.validate(List.of(valid, valid), candidate));
    invalid.put("tutorId", "candidate-0");
    invalid.put("explanation", "Phù hợp 99%");
    assertThrows(
        IllegalArgumentException.class, () -> service.validate(List.of(invalid), candidate));
  }
}
