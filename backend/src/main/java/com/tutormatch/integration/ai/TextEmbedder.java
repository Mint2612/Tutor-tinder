package com.tutormatch.integration.ai;

import java.util.List;

/** Ranh giới nhà cung cấp embedding: chỉ nhận văn bản đã ẩn danh. */
public interface TextEmbedder {
  /** Tên model kèm số chiều; lưu cùng vector để biết khi nào phải tính lại. */
  String model();

  /** query=true cho phía người tìm, false cho tài liệu (hồ sơ gia sư). */
  List<double[]> embed(List<String> texts, boolean query) throws Exception;
}
