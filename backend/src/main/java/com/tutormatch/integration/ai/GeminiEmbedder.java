package com.tutormatch.integration.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeminiEmbedder implements TextEmbedder {
  private static final int DIMENSIONS = 768;
  private static final int BATCH = 100;
  private final String key, name;
  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private final JsonMapper json = JsonMapper.builder().build();

  public GeminiEmbedder(
      @Value("${GEMINI_API_KEY:}") String key,
      @Value("${GEMINI_EMBED_MODEL:gemini-embedding-001}") String name) {
    this.key = key;
    this.name = name;
  }

  @Override
  public String model() {
    return name + "@" + DIMENSIONS;
  }

  @Override
  public List<double[]> embed(List<String> texts, boolean query) throws Exception {
    if (key.isBlank()) throw new IllegalStateException("Embedding disabled");
    var out = new ArrayList<double[]>();
    for (int from = 0; from < texts.size(); from += BATCH)
      out.addAll(batch(texts.subList(from, Math.min(texts.size(), from + BATCH)), query));
    return out;
  }

  private List<double[]> batch(List<String> texts, boolean query) throws Exception {
    var requests = new ArrayList<Map<String, Object>>();
    for (String text : texts)
      requests.add(
          Map.of(
              "model", "models/" + name,
              "content", Map.of("parts", List.of(Map.of("text", text))),
              "taskType", query ? "RETRIEVAL_QUERY" : "RETRIEVAL_DOCUMENT",
              "outputDimensionality", DIMENSIONS));
    var request =
        HttpRequest.newBuilder(
                URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                        + name
                        + ":batchEmbedContents"))
            .timeout(Duration.ofSeconds(8))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", key)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    json.writeValueAsString(Map.of("requests", requests))))
            .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() / 100 != 2)
      throw new IllegalStateException("Embedding provider status " + response.statusCode());
    var embeddings = json.readTree(response.body()).path("embeddings");
    if (embeddings.size() != texts.size()) throw new IllegalStateException("Bad embedding count");
    var result = new ArrayList<double[]>();
    for (var item : embeddings) {
      var values = item.path("values");
      if (values.size() != DIMENSIONS) throw new IllegalStateException("Bad embedding size");
      double[] vector = new double[DIMENSIONS];
      for (int i = 0; i < DIMENSIONS; i++) vector[i] = values.get(i).asDouble();
      result.add(vector);
    }
    return result;
  }
}