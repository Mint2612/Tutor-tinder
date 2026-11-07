package com.tutormatch.repository;

import com.tutormatch.dto.verification.*;
import com.tutormatch.mapper.VerificationMapper;
import com.tutormatch.model.DocumentContent;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VerificationRepository {
  private final JdbcTemplate db;
  private final VerificationMapper mapper;

  public VerificationRepository(JdbcTemplate db, VerificationMapper mapper) {
    this.db = db;
    this.mapper = mapper;
  }

  public List<DocumentResponse> documents(String tutor) {
    return db
        .queryForList(
            "SELECT id,filename,content_type,revision,created_at FROM tutor_documents WHERE"
                + " tutor_id=? ORDER BY created_at DESC,id",
            tutor)
        .stream()
        .map(mapper::document)
        .toList();
  }

  public int documentCount(String tutor) {
    return db.queryForObject(
        "SELECT COUNT(*) FROM tutor_documents WHERE tutor_id=?", Integer.class, tutor);
  }

  public void upload(
      String id, String tutor, String filename, String type, String storageKey, int revision) {
    db.update(
        "INSERT INTO tutor_documents(id,tutor_id,filename,content_type,storage_key,revision)"
            + " VALUES(?,?,?,?,?,?)",
        id,
        tutor,
        filename,
        type,
        storageKey,
        revision);
  }

  /** Storage key of a document owned by this tutor; empty for legacy rows kept in the database. */
  public Optional<String> storageKey(String id, String tutor) {
    return db
        .queryForList(
            "SELECT storage_key FROM tutor_documents WHERE id=? AND tutor_id=? AND storage_key IS NOT NULL",
            String.class,
            id,
            tutor)
        .stream()
        .findFirst();
  }

  public int remove(String id, String tutor) {
    return db.update("DELETE FROM tutor_documents WHERE id=? AND tutor_id=?", id, tutor);
  }

  public void submit(String tutor, String submission) {
    db.update(
        "UPDATE tutor_profiles SET"
            + " workflow_state='PENDING_REVIEW',verification_status='PENDING',rejection_reason=NULL,submitted_at=CURRENT_TIMESTAMP,submission_id=?"
            + " WHERE id=?",
        submission,
        tutor);
  }

  /** Evidence changed after submission/approval: back to draft until it is submitted again. */
  public void reopen(String tutor) {
    db.update(
        "UPDATE tutor_profiles SET"
            + " workflow_state='DRAFT',verification_status='PENDING',verified_at=NULL,rejection_reason=NULL,submission_id=NULL,submitted_at=NULL"
            + " WHERE id=?",
        tutor);
  }

  public Optional<DocumentContent> content(String id) {
    return db.queryForList("SELECT * FROM tutor_documents WHERE id=?", id).stream()
        .findFirst()
        .map(mapper::content);
  }

  public void recordRead(String id, String admin, String tutor) {
    db.update(
        "INSERT INTO document_reviews(document_id,admin_id,submission_id) SELECT ?,?,submission_id"
            + " FROM tutor_profiles WHERE id=? ON DUPLICATE KEY UPDATE"
            + " reviewed_at=CURRENT_TIMESTAMP,submission_id=VALUES(submission_id)",
        id,
        admin,
        tutor);
  }

  public boolean reviewedCurrentSubmission(String tutor, String admin) {
    return db.queryForObject(
            "SELECT COUNT(*) FROM tutor_documents d JOIN document_reviews r ON r.document_id=d.id"
                + " JOIN tutor_profiles t ON t.id=d.tutor_id WHERE d.tutor_id=? AND r.admin_id=?"
                + " AND r.submission_id=t.submission_id",
            Integer.class,
            tutor,
            admin)
        > 0;
  }

  public void decide(String tutor, String admin, String status, String reason) {
    db.update(
        "UPDATE tutor_profiles SET"
            + " workflow_state=?,verification_status=?,rejection_reason=?,verified_at=IF(?='APPROVED',CURRENT_TIMESTAMP,NULL)"
            + " WHERE id=?",
        status,
        status,
        status.equals("APPROVED") ? null : reason,
        status,
        tutor);
    db.update(
        "INSERT INTO verification_audit(id,tutor_id,admin_id,decision,reason) VALUES(?,?,?,?,?)",
        UUID.randomUUID().toString(),
        tutor,
        admin,
        status,
        reason);
  }

  public int visibility(String tutor, boolean visible) {
    return db.update("UPDATE tutor_profiles SET visible=? WHERE id=?", visible, tutor);
  }

  public List<AuditResponse> audit(String tutor) {
    return db
        .queryForList(
            "SELECT id,decision,reason,created_at FROM verification_audit WHERE tutor_id=? ORDER BY"
                + " created_at DESC,id",
            tutor)
        .stream()
        .map(mapper::audit)
        .toList();
  }
}
