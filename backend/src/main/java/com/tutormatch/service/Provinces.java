package com.tutormatch.service;

import java.util.Set;

/**
 * The 34 provincial-level units of Vietnam (6 centrally governed cities and 28 provinces) after the
 * 2025 reorganisation, written without a "Tỉnh"/"Thành phố" prefix. A free-text province made
 * in-person matching depend on spelling, so profiles must use one of these. The browser has the
 * same list (frontend/src/lib/provinces.js) for its combobox: keep the two in step.
 */
final class Provinces {
  static final Set<String> ALL =
      Set.of(
          "An Giang",
          "Bắc Ninh",
          "Cà Mau",
          "Cần Thơ",
          "Cao Bằng",
          "Đà Nẵng",
          "Đắk Lắk",
          "Điện Biên",
          "Đồng Nai",
          "Đồng Tháp",
          "Gia Lai",
          "Hà Nội",
          "Hà Tĩnh",
          "Hải Phòng",
          "Hồ Chí Minh",
          "Huế",
          "Hưng Yên",
          "Khánh Hòa",
          "Lai Châu",
          "Lâm Đồng",
          "Lạng Sơn",
          "Lào Cai",
          "Nghệ An",
          "Ninh Bình",
          "Phú Thọ",
          "Quảng Ngãi",
          "Quảng Ninh",
          "Quảng Trị",
          "Sơn La",
          "Tây Ninh",
          "Thái Nguyên",
          "Thanh Hóa",
          "Tuyên Quang",
          "Vĩnh Long");

  private Provinces() {}
}
