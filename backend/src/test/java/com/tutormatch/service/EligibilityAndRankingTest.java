package com.tutormatch.service;

import static org.junit.jupiter.api.Assertions.*;

import com.tutormatch.mapper.*;
import com.tutormatch.model.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class EligibilityAndRankingTest {
  final EligibilityPolicy policy = new EligibilityPolicy();
  final RecommendationMapper mapper = new RecommendationMapper(new TutorMapper());

  StudentProfile student() {
    return new StudentProfile(
        "student",
        "parent",
        "Con",
        6,
        List.of("Toán"),
        "Hà Nội",
        "IN_PERSON",
        200000,
        List.of("1_EVENING"),
        "Private child narrative",
        Map.of("learning", "practice"),
        Instant.now());
  }

  TutorProfile tutor(
      String workflow,
      String verification,
      boolean visible,
      int rate,
      List<Integer> grades,
      List<String> subjects,
      List<String> modes,
      String province,
      List<String> availability) {
    return new TutorProfile(
        "tutor",
        "private-user-id",
        "Gia sư",
        "",
        subjects,
        grades,
        modes,
        province,
        rate,
        "Kiên nhẫn, thực hành từng bước",
        "Kiên nhẫn",
        "Đại học",
        4,
        availability,
        workflow,
        verification,
        visible,
        "Private rejection reason",
        1,
        "private-submission-id",
        null,
        null,
        null,
        "Họ và tên đầy đủ");
  }

  TutorProfile tutor() {
    return tutor(
        "APPROVED",
        "APPROVED",
        true,
        150000,
        List.of(6),
        List.of("Toán"),
        List.of("IN_PERSON"),
        "Hà Nội",
        List.of("1_EVENING"));
  }

  @Test
  void everyMandatoryConstraintBlocksAnIneligibleTutor() {
    assertTrue(policy.eligible(student(), tutor()));
    var cases =
        List.of(
            tutor(
                "DRAFT",
                "APPROVED",
                true,
                150000,
                List.of(6),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Hà Nội",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "PENDING",
                true,
                150000,
                List.of(6),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Hà Nội",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "APPROVED",
                false,
                150000,
                List.of(6),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Hà Nội",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "APPROVED",
                true,
                250000,
                List.of(6),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Hà Nội",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "APPROVED",
                true,
                150000,
                List.of(7),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Hà Nội",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "APPROVED",
                true,
                150000,
                List.of(6),
                List.of("Ngữ văn"),
                List.of("IN_PERSON"),
                "Hà Nội",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "APPROVED",
                true,
                150000,
                List.of(6),
                List.of("Toán"),
                List.of("ONLINE"),
                "Hà Nội",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "APPROVED",
                true,
                150000,
                List.of(6),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Huế",
                List.of("1_EVENING")),
            tutor(
                "APPROVED",
                "APPROVED",
                true,
                150000,
                List.of(6),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Hà Nội",
                List.of("2_AM")));
    for (var t : cases) assertFalse(policy.eligible(student(), t));
  }

  @Test
  void exactTutorRangesDecideEligibilityAgainstTheFamilysCoarseSlots() {
    java.util.function.Function<List<String>, TutorProfile> withHours =
        hours ->
            tutor(
                "APPROVED",
                "APPROVED",
                true,
                150000,
                List.of(6),
                List.of("Toán"),
                List.of("IN_PERSON"),
                "Hà Nội",
                hours);
    assertTrue(policy.eligible(student(), withHours.apply(List.of("1_19:00-20:30"))));
    assertTrue(policy.eligible(student(), withHours.apply(List.of("1_17:00-19:00", "3_AM"))));
    assertFalse(policy.eligible(student(), withHours.apply(List.of("1_09:00-12:00"))));
    assertFalse(policy.eligible(student(), withHours.apply(List.of("1_21:30-23:00")))); // 30 minutes
    assertFalse(policy.eligible(student(), withHours.apply(List.of("2_19:00-21:00"))));
  }

  @Test
  void scoreCountsCoveredSlotsExactlyAsTheOldTokenOverlapDid() {
    // 4 years of experience + 1 shared slot * 3 + cheaper than 80% of the budget (3).
    assertEquals(10, policy.score(student(), tutor()));
    var exact =
        tutor(
            "APPROVED",
            "APPROVED",
            true,
            150000,
            List.of(6),
            List.of("Toán"),
            List.of("IN_PERSON"),
            "Hà Nội",
            List.of("1_19:00-21:00"));
    assertEquals(10, policy.score(student(), exact));
  }

  @Test
  void anonymizationDropsUnknownFreeTextAndPrivateProfileFields() {
    assertEquals(
        List.of("practice"),
        mapper.safePreferences(
            Map.of("learning", "practice", "details", "Child name, email@example.test")));
    String payload =
        JsonFields.json(
            Map.of(
                "student",
                mapper.anonymousStudent(student()),
                "tutor",
                mapper.anonymousTutor("candidate-0", tutor())));
    for (String secret :
        List.of(
            "Private child narrative",
            "private-user-id",
            "private-submission-id",
            "Hà Nội",
            "Gia sư")) assertFalse(payload.contains(secret));
    String response = JsonFields.json(new TutorMapper().publicResponse(tutor()));
    for (String secret :
        List.of("user_id", "submission_id", "rejection_reason", "documents", "audit", "revision"))
      assertFalse(response.contains(secret));
  }

  @Test
  void invalidOrInventedLlmOutputFailsClosed() {
    var candidates = Map.of("candidate-0", tutor());
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
    assertEquals(1, mapper.validate(List.of(valid), candidates).size());
    var invalid = new HashMap<>(valid);
    invalid.put("tutorId", "invented");
    assertThrows(
        IllegalArgumentException.class, () -> mapper.validate(List.of(invalid), candidates));
    assertThrows(
        IllegalArgumentException.class, () -> mapper.validate(List.of(valid, valid), candidates));
    invalid.put("tutorId", "candidate-0");
    invalid.put("explanation", "Phù hợp 99%");
    assertThrows(
        IllegalArgumentException.class, () -> mapper.validate(List.of(invalid), candidates));
  }
}
