package com.tutormatch.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StudentResponse(
    String id,
    String nickname,
    int grade,
    List<String> subjects,
    String province,
    String mode,
    @JsonProperty("max_hourly_rate") int budget,
    List<String> availability,
    String needs,
    Map<String, String> survey,
    @JsonProperty("consent_at") Instant consentAt) {}
