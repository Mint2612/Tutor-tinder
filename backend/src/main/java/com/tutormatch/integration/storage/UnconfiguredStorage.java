package com.tutormatch.integration.storage;

import com.tutormatch.exception.BusinessException;
import java.util.Optional;

/** Used when no object store is configured: uploads fail clearly instead of losing files. */
public class UnconfiguredStorage implements FileStorage {
  private static BusinessException missing() {
    return new BusinessException(503, "Kho lưu trữ tệp chưa được cấu hình (STORAGE_ENDPOINT).");
  }

  @Override
  public void put(String bucket, String key, byte[] content, String contentType) {
    throw missing();
  }

  @Override
  public Optional<StoredFile> get(String bucket, String key) {
    throw missing();
  }

  @Override
  public void delete(String bucket, String key) {
    // Nothing was ever stored.
  }
}
