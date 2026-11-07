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
    Instant consentAt) {}
