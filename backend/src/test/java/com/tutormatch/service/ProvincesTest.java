package com.tutormatch.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProvincesTest {
  @Test
  void theListHasThe34UnitsOfTheCurrentAdministrativeDivision() {
    assertEquals(34, Provinces.ALL.size());
    for (String name : Provinces.ALL) {
      assertEquals(name.trim(), name, "no stray spaces: " + name);
      assertFalse(name.isBlank());
      assertFalse(name.startsWith("Tỉnh ") || name.startsWith("Thành phố "), name);
    }
    // What profiles already store keeps working.
    assertTrue(Provinces.ALL.containsAll(java.util.List.of("Hà Nội", "Hồ Chí Minh", "Đà Nẵng")));
  }

  @Test
  void validationAcceptsTrimmedNamesAndRejectsEverythingElse() {
    ProfileValidation.province("Hà Nội");
    ProfileValidation.province("  Đồng Nai ");
    for (String bad : new String[] {null, "", "  ", "hà nội", "Ha Noi", "Bình Dương", "Tỉnh Đồng Nai"}) {
      var e =
          assertThrows(
              com.tutormatch.exception.BusinessException.class,
              () -> ProfileValidation.province(bad),
              String.valueOf(bad));
      assertEquals(400, e.status());
    }
  }
}
