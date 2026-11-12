package com.tutormatch.dto.verification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record AuditResponse(
    String id, String decision, String reason, @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("submission_id") String submissionId, Integer revision) {}
