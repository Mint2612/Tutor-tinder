package com.tutormatch.integration.storage;

import java.util.Optional;

/**
 * Object storage for uploaded files. Callers never see storage URLs: avatars are served through
 * the API and identity evidence is readable only by an admin through the API, so the buckets stay
 * private and the backing server can be swapped for any S3-compatible one.
 */
public interface FileStorage {
  /** Tutor profile photos, normalised JPEGs served via {@code /api/media/avatars/{name}}. */
  String AVATARS = "avatars";

  /** Private identity evidence; never exposed by URL. */
  String EVIDENCE = "evidence";

  void put(String bucket, String key, byte[] content, String contentType);

  Optional<StoredFile> get(String bucket, String key);

  /** Best effort: a failed delete is logged, never thrown, so it cannot undo a committed change. */
  void delete(String bucket, String key);

  record StoredFile(byte[] content, String contentType) {}
}
