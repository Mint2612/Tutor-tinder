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

  /** Province / city: one of the fixed list, so in-person matching never hinges on spelling. */
  static void province(String value) {
    require(
        value != null && Provinces.ALL.contains(value.trim()),
        400,
        "Tỉnh / thành phố không hợp lệ. Hãy chọn một mục trong danh sách.");
  }

  /**
   * Free text that tutors can read must not carry a phone number or an email: contact details are
   * shared only after a tutor accepts a trial lesson.
   */
  static void noContactInfo(String text, String field) {
    ContactInfo.find(text)
        .ifPresent(
            kind -> {
              throw new com.tutormatch.exception.BusinessException(
                  400,
                  "Phần “"
                      + field
                      + "” không được ghi "
                      + (kind == ContactInfo.Kind.EMAIL ? "địa chỉ email" : "số điện thoại")
                      + ". Thông tin liên hệ chỉ được chia sẻ sau khi gia sư chấp nhận học thử.");
            });
  }

  /** Availability slots: no repeats, and valid, non-overlapping time ranges. */
  static void availability(List<String> slots) {
    distinct(slots);
    Schedule.validate(slots);
  }
}
