package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import java.util.*;

final class ProfileValidation {
  static final Set<String> SUBJECTS =
      Set.of("Toán", "Ngữ văn", "Tiếng Anh", "Vật lý", "Hóa học", "Sinh học", "Tin học");

  private ProfileValidation() {}

  static void distinct(Collection<?> values) {
    require(new HashSet<>(values).size() == values.size(), 400, "Không được lặp lại lựa chọn");
  }
}
