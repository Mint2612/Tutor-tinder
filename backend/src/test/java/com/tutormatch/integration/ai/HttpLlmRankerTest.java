package com.tutormatch.integration.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.tutormatch.mapper.JsonFields;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpLlmRankerTest {
  @Test
  void localCompatibleEndpointWorksWithoutApiKey() throws Exception {
    var authorization = new AtomicReference<String>();
    var body = new AtomicReference<String>();
    var ranking =
        Map.of(
            "tutorId",
            "candidate-0",
            "level",
            "GOOD",
            "strengths",
            List.of("Thực hành"),
            "concerns",
            List.of("Cần học thử"),
            "explanation",
            "Phương pháp phù hợp để tìm hiểu.");
    String response =
        JsonFields.json(
            Map.of(
                "choices",
                List.of(
                    Map.of(
                        "message",
                        Map.of(
                            "content", JsonFields.json(Map.of("rankings", List.of(ranking))))))));
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v1/chat/completions",
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    try {
      var ranker =
          new HttpLlmRanker(
              "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions",
              "",
              "local-model");
      var result = ranker.rank(Map.of("grade", 6), List.of(Map.of("tutorId", "candidate-0")));
      assertEquals("candidate-0", result.getFirst().get("tutorId"));
      assertNull(authorization.get());
      assertTrue(body.get().contains("local-model"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void oversizedProviderBodyIsRejected() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          byte[] bytes = "x".repeat(70000).getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    try {
      var ranker =
          new HttpLlmRanker(
              "http://127.0.0.1:" + server.getAddress().getPort() + "/", "", "local-model");
      assertThrows(Exception.class, () -> ranker.rank(Map.of(), List.of()));
    } finally {
      server.stop(0);
    }
  }
}
