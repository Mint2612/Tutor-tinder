package com.tutormatch.service;

import static com.tutormatch.exception.BusinessException.require;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Weekly availability. A slot is a string "{day}_{when}" with day 1 (Monday) to 7 (Sunday) and
 * {@code when} either an exact range "HH:MM-HH:MM" (what tutors enter) or one of the coarse
 * blocks AM/PM/EVENING (what parents still pick, and what older profiles contain). A coarse block
 * is simply a fixed range, so both kinds are compared as time ranges.
 */
final class Schedule {
  /** Two sides can meet when they share at least one lesson hour (or all of a shorter slot). */
  static final int MIN_OVERLAP_MINUTES = 60;

  static final int MIN_RANGE_MINUTES = 30;

  private static final Map<String, int[]> BLOCKS =
      Map.of("AM", new int[] {6 * 60, 12 * 60}, "PM", new int[] {12 * 60, 18 * 60}, "EVENING", new int[] {18 * 60, 22 * 60});
  private static final Pattern RANGE =
      Pattern.compile("([1-7])_([01]\\d|2[0-3]):([0-5]\\d)-([01]\\d|2[0-3]):([0-5]\\d)");

  record Range(int day, int start, int end) {
    int length() {
      return end - start;
    }
  }

  private Schedule() {}

  /** Parses a slot; null when malformed. An exact range may be empty or inverted here. */
  static Range parse(String slot) {
    if (slot == null || slot.length() < 3 || slot.charAt(1) != '_') return null;
    int day = slot.charAt(0) - '0';
    if (day < 1 || day > 7) return null;
    var block = BLOCKS.get(slot.substring(2));
    if (block != null) return new Range(day, block[0], block[1]);
    Matcher m = RANGE.matcher(slot);
    if (!m.matches()) return null;
    return new Range(
        day,
        Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3)),
        Integer.parseInt(m.group(4)) * 60 + Integer.parseInt(m.group(5)));
  }

  private static boolean isExact(String slot) {
    return !BLOCKS.containsKey(slot.substring(2));
  }

  /** Rejects malformed slots, empty/inverted or too short ranges, and overlapping exact ranges. */
  static void validate(List<String> slots) {
    var exact = new ArrayList<Range>();
    for (String slot : slots) {
      Range r = parse(slot);
      require(r != null, 400, "Khung giờ không hợp lệ");
      if (!isExact(slot)) continue;
      require(r.end() > r.start(), 400, "Giờ kết thúc phải sau giờ bắt đầu");
      require(r.length() >= MIN_RANGE_MINUTES, 400, "Mỗi khung giờ cần ít nhất 30 phút");
      exact.add(r);
    }
    exact.sort(Comparator.comparingInt(Range::day).thenComparingInt(Range::start));
    for (int i = 1; i < exact.size(); i++) {
      var previous = exact.get(i - 1);
      var current = exact.get(i);
      require(
          previous.day() != current.day() || current.start() >= previous.end(),
          400,
          "Các khung giờ trong cùng một ngày không được chồng nhau");
    }
  }

  /** The union of the slots as sorted, non-overlapping ranges (malformed slots are ignored). */
  static List<Range> merged(List<String> slots) {
    var ranges = new ArrayList<Range>();
    for (String slot : slots) {
      Range r = parse(slot);
      if (r != null && r.end() > r.start()) ranges.add(r);
    }
    ranges.sort(Comparator.comparingInt(Range::day).thenComparingInt(Range::start));
    var out = new ArrayList<Range>();
    for (Range r : ranges) {
      if (!out.isEmpty()) {
        var last = out.getLast();
        if (last.day() == r.day() && r.start() <= last.end()) {
          out.set(out.size() - 1, new Range(last.day(), last.start(), Math.max(last.end(), r.end())));
          continue;
        }
      }
      out.add(r);
    }
    return out;
  }

  private static int overlap(Range slot, List<Range> offered) {
    int minutes = 0;
    for (Range o : offered)
      if (o.day() == slot.day())
        minutes += Math.max(0, Math.min(slot.end(), o.end()) - Math.max(slot.start(), o.start()));
    return minutes;
  }

  /** How many of the wanted slots the offered availability can actually cover. */
  static int coveredSlots(List<String> wanted, List<String> offered) {
    var available = merged(offered);
    int covered = 0;
    for (String slot : wanted) {
      Range r = parse(slot);
      if (r == null || r.end() <= r.start()) continue;
      if (overlap(r, available) >= Math.min(MIN_OVERLAP_MINUTES, r.length())) covered++;
    }
    return covered;
  }
}
