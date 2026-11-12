package com.tutormatch.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StudentProfile(
    String id,
    String parentId,
    String nickname,
    int grade,
    List<String> subjects,
    String province,
    String mode,
    int budget,
    List<String> availability,
    String needs,
    Map<String, String> survey,
    Instant consentAt,
    boolean visible) {
  public StudentProfile(
    String id,
    String parentId,
    String nickname,
    int grade,
    List<String> subjects,
    String province,
    String mode,
    int budget,
    List<String> availability,
    String needs,
    Map<String, String> survey,
    Instant consentAt) {
    this(id, parentId, nickname, grade, subjects, province, mode, budget, availability, needs, survey, consentAt, true);
  }
}
