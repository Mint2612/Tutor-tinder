package com.tutormatch.integration.ai;

import java.util.*;

/** Provider boundary: inputs contain only pseudonymous IDs and allowlisted learning attributes. */
public interface LlmRanker {
  List<Map<String, Object>> rank(Map<String, Object> student, List<Map<String, Object>> tutors)
      throws Exception;
}
