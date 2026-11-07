package com.tutormatch.service;

import static org.junit.jupiter.api.Assertions.*;

import com.tutormatch.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleTest {
  static void rejected(String message, String... slots) {
    var e = assertThrows(BusinessException.class, () -> Schedule.validate(List.of(slots)), message);
    assertEquals(400, e.status());
  }

  @Test
  void coarseBlocksAreFixedRangesAndExactRangesAreParsed() {
    assertEquals(new Schedule.Range(1, 18 * 60, 22 * 60), Schedule.parse("1_EVENING"));
    assertEquals(new Schedule.Range(2, 6 * 60, 12 * 60), Schedule.parse("2_AM"));
    assertEquals(new Schedule.Range(3, 12 * 60, 18 * 60), Schedule.parse("3_PM"));
    assertEquals(new Schedule.Range(7, 19 * 60 + 30, 21 * 60), Schedule.parse("7_19:30-21:00"));
    for (String bad : new String[] {null, "", "1", "1_", "0_AM", "8_AM", "1_NOON", "1_9:00-10:00", "1_25:00-26:00", "1_18:00", "x_AM"})
      assertNull(Schedule.parse(bad), String.valueOf(bad));
  }

  @Test
  void validRangesPass() {
    Schedule.validate(List.of("1_18:00-20:30", "1_20:30-21:00", "2_AM", "3_09:00-09:30"));
    Schedule.validate(List.of("4_EVENING", "4_19:00-20:00")); // a block never conflicts with a range
    Schedule.validate(List.of());
  }

  @Test
  void brokenRangesAreRejected() {
    rejected("inverted", "1_20:00-18:00");
    rejected("empty", "1_18:00-18:00");
    rejected("shorter than 30 minutes", "1_18:00-18:29");
    rejected("overlap", "1_18:00-20:00", "1_19:00-21:00");
    rejected("contained", "1_18:00-21:00", "1_19:00-20:00");
    rejected("malformed", "1_18:00-20:00", "tối thứ hai");
  }

  @Test
  void overlappingRangesOnDifferentDaysAreFine() {
    Schedule.validate(List.of("1_18:00-20:00", "2_18:00-20:00"));
  }

  @Test
  void aTutorCoversAWantedSlotOnlyWithAtLeastOneSharedHour() {
    var evening = List.of("1_EVENING"); // 18:00-22:00
    assertEquals(1, Schedule.coveredSlots(evening, List.of("1_19:00-21:00")));
    assertEquals(1, Schedule.coveredSlots(evening, List.of("1_17:00-19:00"))); // exactly one hour inside
    assertEquals(0, Schedule.coveredSlots(evening, List.of("1_17:00-18:59"))); // 59 minutes
    assertEquals(0, Schedule.coveredSlots(evening, List.of("1_21:30-23:00"))); // 30 minutes
    assertEquals(0, Schedule.coveredSlots(evening, List.of("2_19:00-21:00"))); // another day
    assertEquals(0, Schedule.coveredSlots(evening, List.of("1_AM")));
  }

  @Test
  void adjacentOrOverlappingTutorRangesAreAddedTogetherBeforeComparing() {
    var evening = List.of("1_EVENING");
    assertEquals(1, Schedule.coveredSlots(evening, List.of("1_18:00-18:30", "1_18:30-19:30")));
    // A coarse block plus an exact range that overlaps it must not be counted twice.
    assertEquals(0, Schedule.coveredSlots(List.of("1_21:30-22:00"), List.of("1_PM", "1_17:00-18:30")));
    assertEquals(1, Schedule.coveredSlots(List.of("1_20:00-21:00"), List.of("1_EVENING", "1_19:00-22:00")));
  }

  @Test
  void identicalCoarseBlocksKeepTheirOldMeaning() {
    var wanted = List.of("1_EVENING", "2_AM", "3_PM");
    assertEquals(3, Schedule.coveredSlots(wanted, List.of("1_EVENING", "2_AM", "3_PM", "5_AM")));
    assertEquals(1, Schedule.coveredSlots(wanted, List.of("2_AM", "3_AM")));
    assertEquals(0, Schedule.coveredSlots(wanted, List.of()));
  }

  @Test
  void aShortWantedSlotIsCoveredByCoveringAllOfIt() {
    assertEquals(1, Schedule.coveredSlots(List.of("1_20:00-20:30"), List.of("1_19:00-21:00")));
    assertEquals(0, Schedule.coveredSlots(List.of("1_20:00-20:30"), List.of("1_19:00-20:15")));
  }
}
