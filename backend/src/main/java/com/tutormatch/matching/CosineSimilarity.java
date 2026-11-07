package com.tutormatch.matching;

public final class CosineSimilarity {
  private CosineSimilarity() {}

  public static double calculate(double[] a, double[] b) {
    if (a == null || b == null || a.length == 0 || a.length != b.length) {
      throw new IllegalArgumentException("Vectors must have equal, non-zero dimensions");
    }
    double dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < a.length; i++) {
      if (!Double.isFinite(a[i]) || !Double.isFinite(b[i])) {
        throw new IllegalArgumentException("Vectors must contain finite numbers");
      }
      dot += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }
    return normA == 0 || normB == 0 ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
