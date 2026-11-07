package com.tutormatch.dto.lesson;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record LessonResponse(
    String id,
    @JsonProperty("student_id") String studentId,
    @JsonProperty("tutor_id") String tutorId,
    String status,
    @JsonProperty("proposed_at") Instant proposedAt,
    @JsonProperty("expires_at") Instant expiresAt,
    String message,
    @JsonProperty("created_at") Instant createdAt,
    String nickname,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("parent_email") String parentEmail,
    @JsonProperty("parent_phone") String parentPhone,
    @JsonProperty("tutor_email") String tutorEmail,
    @JsonProperty("tutor_phone") String tutorPhone) {}
