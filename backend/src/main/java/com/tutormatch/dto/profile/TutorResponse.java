package com.tutormatch.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tutormatch.dto.verification.AuditResponse;
import com.tutormatch.dto.verification.DocumentResponse;
import java.time.Instant;
import java.util.List;

/**
 * Explicit response allowlist. Private review fields are populated only by the owner/admin mapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TutorResponse(
    String id,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("photo_url") String photoUrl,
    List<String> subjects,
    List<Integer> grades,
    List<String> modes,
    String province,
    @JsonProperty("hourly_rate") int hourlyRate,
    @JsonProperty("teaching_description") String methodology,
    @JsonProperty("teaching_style") String style,
    String background,
    @JsonProperty("experience_years") int experience,
    List<String> availability,
    /** Self-declared (verification level 3); shown to parents labelled "Tự khai", never ranked. */
    String achievements,
    /** Legal name for identity checks: owner and admin only, never in a public response. */
    @JsonProperty("full_name") String fullName,
    @JsonProperty("workflow_state") String workflowState,
    @JsonProperty("verification_status") String verificationStatus,
    @JsonProperty("verified_at") Instant verifiedAt,
    @JsonProperty("user_id") String userId,
    Boolean visible,
    @JsonProperty("rejection_reason") String rejectionReason,
    Integer revision,
    @JsonProperty("submitted_at") Instant submittedAt,
    List<DocumentResponse> documents,
    List<AuditResponse> audit) {}
