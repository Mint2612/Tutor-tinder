package com.tutormatch.config;

import com.tutormatch.integration.storage.FileStorage;
import com.tutormatch.integration.storage.S3FileStorage;
import com.tutormatch.integration.storage.UnconfiguredStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StorageConfig {
  @Bean
  FileStorage fileStorage(
      @Value("${STORAGE_ENDPOINT:}") String endpoint,
      @Value("${STORAGE_ACCESS_KEY:}") String accessKey,
      @Value("${STORAGE_SECRET_KEY:}") String secretKey,
      @Value("${STORAGE_REGION:us-east-1}") String region) {
    return endpoint.isBlank()
        ? new UnconfiguredStorage()
        : new S3FileStorage(endpoint, region, accessKey, secretKey);
  }
}
