package com.tutormatch.model;

import java.time.Instant;
import java.util.List;

public record TutorProfile(
    String id,
    String userId,
    String displayName,
    String photoUrl,
    List<String> subjects,
    List<Integer> grades,
    List<String> modes,
    String province,
    int hourlyRate,
    String methodology,
    String style,
    String background,
    int experience,
    List<String> availability,
    String workflowState,
    String verificationStatus,
    boolean visible,
    String rejectionReason,
    int revision,
    String submissionId,
    Instant submittedAt,
    Instant verifiedAt,
    String achievements,
    /** Legal full name as on the identity documents; private to the owner and admins. */
    String legalName,
    boolean acceptingStudents) {
  public TutorProfile(
    String id,
    String userId,
    String displayName,
    String photoUrl,
    List<String> subjects,
    List<Integer> grades,
    List<String> modes,
    String province,
    int hourlyRate,
    String methodology,
    String style,
    String background,
    int experience,
    List<String> availability,
    String workflowState,
    String verificationStatus,
    boolean visible,
    String rejectionReason,
    int revision,
    String submissionId,
    Instant submittedAt,
    Instant verifiedAt,
    String achievements,
    /** Legal full name as on the identity documents; private to the owner and admins. */
    String legalName) {
    this(id, userId, displayName, photoUrl, subjects, grades, modes, province, hourlyRate, methodology, style, background, experience, availability, workflowState, verificationStatus, visible, rejectionReason, revision, submissionId, submittedAt, verifiedAt, achievements, legalName, true);
  }
}
