package com.tutormatch.mapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Serialization of JSON database columns; no business rules or database access. */
public final class JsonFields {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private JsonFields() {}

  public static String json(Object value) {
    return JSON.writeValueAsString(value);
  }

  public static List<String> strings(Object value) {
    return JSON.readValue(value.toString(), new TypeReference<List<String>>() {});
  }

  public static Map<String, String> survey(Object value) {
    return JSON.readValue(value.toString(), new TypeReference<Map<String, String>>() {});
  }

  public static Instant instant(Object value) {
    return value == null ? null : ((Timestamp) value).toInstant();
  }

  public static String text(Map<String, Object> row, String key) {
    return row.get(key) == null ? null : row.get(key).toString();
  }

  public static int number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).intValue();
  }

  public static boolean bool(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value instanceof Boolean b ? b : ((Number) value).intValue() != 0;
  }
}
