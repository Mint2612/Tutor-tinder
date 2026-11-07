package com.tutormatch.service;

import java.text.Normalizer;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spots a phone number or an email address written into free text, so contact details stay hidden
 * until a tutor accepts a trial lesson. It also catches the usual ways of dodging a plain check:
 * spaces or dots inside a number, digits spelled out ("không chín không ba ..."), full-width
 * characters, "abc (at) gmail (dot) com" and "abc a còng gmail chấm com".
 *
 * <p>It leans towards false positives on purpose; the message tells the user how to rephrase. The
 * browser has a mirror of these rules (frontend/src/lib/contactInfo.js) that only warns early: this
 * class is the authority.
 */
final class ContactInfo {
  enum Kind {
    EMAIL,
    PHONE
  }

  private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

  // --- email ---------------------------------------------------------------------------------
  private static final String LOCAL = "[\\p{L}\\p{N}._%+-]+";
  private static final String LABEL = "[\\p{L}\\p{N}-]+";
  private static final String BRACKET_DOT = "\\s*[(\\[{<]\\s*(?:dot|chấm)\\s*[)\\]}>]\\s*";
  private static final String WORD_DOT = "\\s+(?:dot|chấm)\\s+";
  private static final String ANY_DOT = "(?:\\s*\\.\\s*|" + BRACKET_DOT + "|" + WORD_DOT + ")";
  private static final String SPOKEN_DOT = "(?:" + BRACKET_DOT + "|" + WORD_DOT + ")";
  // "name@host.tld" and "name(at)host.tld" accept a plain dot; a bare word "at" is only trusted
  // together with a spoken "dot"/"chấm", otherwise "looking at math. Good" would look like mail.
  // The label repetition is bounded: Java's regex engine recurses once per iteration of a group,
  // so an unbounded "+" overflows the stack on input like "a@b.b.b.b. ..." (3000 characters).
  private static final Pattern EMAIL =
      Pattern.compile(
          "(?:"
              + LOCAL
              + "\\s*(?:@|[(\\[{<]\\s*at\\s*[)\\]}>])\\s*"
              + LABEL
              + "(?:"
              + ANY_DOT
              + LABEL
              + "){1,8}"
              + "|(?<![\\p{L}\\p{N}])"
              + LOCAL
              + "\\s+(?:at|a\\s+còng|a\\s+cờ\\s+còng|a\\s+móc)\\s+"
              + LABEL
              + "(?:"
              + SPOKEN_DOT
              + LABEL
              + "){1,8})",
          FLAGS);

  // --- phone ---------------------------------------------------------------------------------
  private static final Map<String, Character> DIGIT_WORDS =
      Map.ofEntries(
          Map.entry("không", '0'),
          Map.entry("zero", '0'),
          Map.entry("một", '1'),
          Map.entry("mốt", '1'),
          Map.entry("one", '1'),
          Map.entry("hai", '2'),
          Map.entry("two", '2'),
          Map.entry("ba", '3'),
          Map.entry("three", '3'),
          Map.entry("bốn", '4'),
          Map.entry("tư", '4'),
          Map.entry("four", '4'),
          Map.entry("năm", '5'),
          Map.entry("five", '5'),
          Map.entry("sáu", '6'),
          Map.entry("six", '6'),
          Map.entry("bảy", '7'),
          Map.entry("seven", '7'),
          Map.entry("tám", '8'),
          Map.entry("eight", '8'),
          Map.entry("chín", '9'),
          Map.entry("nine", '9'));
  private static final String WORD =
      "(?<![\\p{L}\\p{N}])(?:"
          + String.join("|", DIGIT_WORDS.keySet())
          + ")(?![\\p{L}\\p{N}])";
  private static final Pattern TOKEN = Pattern.compile("[0-9]|" + WORD, FLAGS);
  // A run is nine or more digits (or spelled digits) with at most two spaces/dots/dashes/brackets
  // between neighbours. It is walked token by token: a regex like (token sep?){8,} would recurse
  // once per token and overflow the stack on a long "1 1 1 1 ..." text.
  private static final int MIN_RUN = 9;
  // A Vietnamese number: 0 or 84, then a mobile/landline prefix, then the rest. Searching inside
  // the run also catches "lớp 7 0903123456", where the grade runs into the number.
  private static final Pattern VIETNAMESE = Pattern.compile("(?:84|0)[235789][0-9]{7,9}");

  private ContactInfo() {}

  static Optional<Kind> find(String text) {
    if (text == null || text.isBlank()) return Optional.empty();
    String s =
        Normalizer.normalize(text, Normalizer.Form.NFKC)
            .replaceAll("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD]", "");
    if (EMAIL.matcher(s).find()) return Optional.of(Kind.EMAIL);
    return phone(s) ? Optional.of(Kind.PHONE) : Optional.empty();
  }

  private static boolean phone(String s) {
    Matcher token = TOKEN.matcher(s);
    StringBuilder digits = new StringBuilder();
    int tokens = 0;
    int previousEnd = -1;
    while (token.find()) {
      boolean joined = previousEnd >= 0 && separator(s, previousEnd, token.start());
      if (!joined) {
        if (isPhone(tokens, digits)) return true;
        digits.setLength(0);
        tokens = 0;
      }
      String t = token.group();
      digits.append(
          t.length() == 1 ? t.charAt(0) : DIGIT_WORDS.get(t.toLowerCase(java.util.Locale.ROOT)));
      tokens++;
      previousEnd = token.end();
    }
    return isPhone(tokens, digits);
  }

  private static boolean isPhone(int tokens, CharSequence digits) {
    return tokens >= MIN_RUN && VIETNAMESE.matcher(digits).find();
  }

  /** Up to two spaces, dots, dashes or brackets between two digits still count as one number. */
  private static boolean separator(String s, int from, int to) {
    if (to - from > 2) return false;
    for (int i = from; i < to; i++) {
      char c = s.charAt(i);
      if (!(Character.isWhitespace(c) || c == '.' || c == '-' || c == '(' || c == ')'))
        return false;
    }
    return true;
  }
}
