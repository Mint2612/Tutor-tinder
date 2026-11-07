package com.tutormatch.service;

import static org.junit.jupiter.api.Assertions.*;

import com.tutormatch.service.ContactInfo.Kind;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContactInfoTest {
  static void assertFound(Kind expected, String... samples) {
    for (String s : samples)
      assertEquals(Optional.of(expected), ContactInfo.find(s), "should be flagged: " + s);
  }

  static void assertClean(String... samples) {
    for (String s : samples) assertEquals(Optional.empty(), ContactInfo.find(s), "should pass: " + s);
  }

  @Test
  void phoneNumbersAreFoundHoweverTheyAreWritten() {
    assertFound(
        Kind.PHONE,
        "0903123456",
        "Gọi cô theo số 0903123456 nhé",
        "090 312 3456",
        "0903.123.456",
        "0903-123-456",
        "(090) 312 3456",
        "+84 903 123 456",
        "+84903123456",
        "84 903 123 456",
        "0 9 0 3 1 2 3 4 5 6",
        "09 03 12 34 56",
        "028 3822 1234", // landline
        "SĐT0903123456", // glued to a word
        "lớp 7 0903123456", // the grade runs into the number
        "０９０３１２３４５６", // full-width digits
        "không chín không ba một hai ba bốn năm sáu", // spelled out
        "0903 một hai ba bốn năm sáu", // half digits, half words
        "KHÔNG CHÍN KHÔNG BA MỘT HAI BA BỐN NĂM SÁU",
        "zero nine zero three one two three four five six",
        "so dien thoai: 0987 654 321\nzalo cung so nay");
  }

  @Test
  void emailAddressesAreFoundHoweverTheyAreWritten() {
    assertFound(
        Kind.EMAIL,
        "abc@gmail.com",
        "Liên hệ: phuhuynh@example.com nhé",
        "nguyen.van+test@mail.hcmut.edu.vn",
        "abc @ gmail . com",
        "abc(at)gmail.com",
        "abc [at] gmail [dot] com",
        "abc at gmail dot com",
        "abc a còng gmail chấm com",
        "abc A CÒNG gmail CHẤM com",
        "ＡＢＣ＠ｇｍａｉｌ．ｃｏｍ", // full-width
        "ab​c@gm​ail.com"); // zero-width characters
  }

  @Test
  void ordinaryTextAboutLessonsPasses() {
    assertClean(
        "",
        "Con hay mất tập trung, cần ôn Toán lớp 6 và lấy lại căn bản.",
        "Con học lớp 9, mục tiêu thi vào lớp 10, điểm Toán 8.5, Lý 9.0",
        "Học phí 200.000 đồng mỗi giờ, ngân sách 150.000 - 250.000",
        "Ngân sách 1000000-2000000 mỗi tháng",
        "Học phí 1.500.000 hoặc 2.000.000 một tháng",
        "Ngày 12/05/2026 lúc 19:30, học 3 buổi mỗi tuần, mỗi buổi 90 phút",
        "Con thi vào năm 2026 và cần tăng điểm trước năm 2027",
        "Điểm trung bình 8.25 năm lớp 11, mục tiêu 9.0",
        "Cần một hai buổi mỗi tuần, không quá ba bốn tiếng",
        "Looking at math. Good results are the goal",
        "Con ở at nhà. Sau giờ học con rất mệt",
        "Nhắn cho cô ở trường @ lớp 9 sau",
        "Email của con ở trường hay quên, cần nhắc",
        "Số điện thoại sẽ cho sau khi học thử",
        "Phụ huynh dùng zalo sau khi gia sư chấp nhận");
  }

  @Test
  void nullAndBlankTextAreFine() {
    assertClean(null, "   ", "\n");
  }

  @Test
  void aVeryLongTextWithoutAnyContactIsNotSlow() {
    // 3000 characters is the field limit. Java's regex engine recurses once per iteration of a
    // repeated group, so these used to be a StackOverflowError (HTTP 500) waiting to happen.
    long start = System.nanoTime();
    assertClean(
        "học ".repeat(750),
        "a.".repeat(1500),
        "1 ".repeat(1500),
        "0 ".repeat(1500),
        "một ".repeat(750),
        "(".repeat(3000),
        "1-".repeat(1500),
        "a@" + "b ".repeat(1500));
    assertFound(Kind.EMAIL, "a@" + "b.".repeat(1500)); // still found, and without a stack overflow
    assertFound(Kind.PHONE, "1 ".repeat(1400) + "0903123456");
    assertTrue((System.nanoTime() - start) / 1_000_000 < 3000, "scan took too long");
  }

  @Test
  void validationExplainsWhatToChange() {
    var phone =
        assertThrows(
            com.tutormatch.exception.BusinessException.class,
            () -> ProfileValidation.noContactInfo("gọi 0903123456", "Mục tiêu"));
    assertEquals(400, phone.status());
    assertTrue(phone.getMessage().contains("số điện thoại"));
    var mail =
        assertThrows(
            com.tutormatch.exception.BusinessException.class,
            () -> ProfileValidation.noContactInfo("a@b.com", "Mục tiêu"));
    assertTrue(mail.getMessage().contains("địa chỉ email"));
    ProfileValidation.noContactInfo("Cần ôn Toán", "Mục tiêu");
  }
}
