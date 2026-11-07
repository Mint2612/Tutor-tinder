package com.tutormatch.dto.verification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record DocumentResponse(
    String id,
    String filename,
    @JsonProperty("content_type") String contentType,
    int revision,
    @JsonProperty("created_at") Instant createdAt) {}
