package com.tutormatch.integration.storage;

import com.tutormatch.exception.BusinessException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** S3 API adapter (MinIO locally). Buckets are created on first use and stay private. */
public class S3FileStorage implements FileStorage {
  private static final Logger LOG = LoggerFactory.getLogger(S3FileStorage.class);
  private final S3Client s3;
  private final Set<String> ready = ConcurrentHashMap.newKeySet();

  public S3FileStorage(String endpoint, String region, String accessKey, String secretKey) {
    s3 =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .forcePathStyle(true)
            // Default checksums of newer SDKs are rejected by some S3-compatible servers.
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .httpClient(
                UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(3))
                    .socketTimeout(Duration.ofSeconds(10))
                    .build())
            .build();
  }

  @Override
  public void put(String bucket, String key, byte[] content, String contentType) {
    ensureBucket(bucket);
    try {
      s3.putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
          RequestBody.fromBytes(content));
    } catch (SdkException e) {
      throw unavailable(e);
    }
  }

  @Override
  public Optional<StoredFile> get(String bucket, String key) {
    try {
      var object =
          s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
      return Optional.of(new StoredFile(object.asByteArray(), object.response().contentType()));
    } catch (NoSuchKeyException | NoSuchBucketException e) {
      return Optional.empty();
    } catch (SdkException e) {
      throw unavailable(e);
    }
  }

  @Override
  public void delete(String bucket, String key) {
    try {
      s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (SdkException e) {
      LOG.error("Could not delete stored object {}/{}: {}", bucket, key, e.getClass().getSimpleName());
    }
  }

  private void ensureBucket(String bucket) {
    if (ready.contains(bucket)) return;
    try {
      try {
        s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      } catch (NoSuchBucketException e) {
        create(bucket);
      } catch (S3Exception e) {
        if (e.statusCode() != 404) throw e;
        create(bucket);
      }
      ready.add(bucket);
    } catch (SdkException e) {
      throw unavailable(e);
    }
  }

  private void create(String bucket) {
    try {
      s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    } catch (BucketAlreadyOwnedByYouException ignored) {
      // Another request created it first.
    }
  }

  private BusinessException unavailable(SdkException e) {
    LOG.warn("Object storage unavailable: {}", e.getClass().getSimpleName());
    return new BusinessException(503, "Kho lưu trữ tệp tạm thời không khả dụng. Vui lòng thử lại sau.");
  }
}
