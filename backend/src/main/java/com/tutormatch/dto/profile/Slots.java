package com.tutormatch.dto.profile;

/** Shape of one availability slot: a weekday plus a coarse block or an exact time range. */
final class Slots {
  /** "1_EVENING" (older/parent form) or "1_18:00-20:30" (exact range). */
  static final String PATTERN =
      "[1-7]_(AM|PM|EVENING|([01][0-9]|2[0-3]):[0-5][0-9]-([01][0-9]|2[0-3]):[0-5][0-9])";

  private Slots() {}
}
