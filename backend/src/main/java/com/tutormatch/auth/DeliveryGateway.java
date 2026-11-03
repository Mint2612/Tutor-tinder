package com.tutormatch.auth;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Trusted backend webhook adapter. Provider must send email/SMS, never echo codes to clients. */
@Component
public class DeliveryGateway {
  private final String url, key;

  public DeliveryGateway(
      @Value("${DELIVERY_URL:}") String url, @Value("${DELIVERY_KEY:}") String key) {
    this.url = url;
    this.key = key;
  }

  public void send(String channel, String destination, String code, String challengeId) {
    if (url.isBlank() || key.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ gửi mã xác minh chưa được cấu hình");
    try {
      var request =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(Duration.ofSeconds(5))
              .header("Authorization", "Bearer " + key)
              .header("Content-Type", "application/json")
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      JsonMapper.builder()
                          .build()
                          .writeValueAsString(
                              Map.of(
                                  "channel",
                                  channel,
                                  "destination",
                                  destination,
                                  "code",
                                  code,
                                  "challengeId",
                                  challengeId,
                                  "expiresInSeconds",
                                  600))))
              .build();
      var response =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(2))
              .build()
              .send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() / 100 != 2) throw new IllegalStateException();
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Không thể gửi mã xác minh. Vui lòng thử lại sau.");
    }
  }
}
