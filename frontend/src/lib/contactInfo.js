// Early warning for a phone number or email typed into free text that tutors can read. Contact
// details are shared only after a tutor accepts a trial lesson. The server enforces the same
// rule (backend ContactInfo.java, which is the authority); keep the two in step.
const DIGIT_WORDS = {
  không: "0",
  zero: "0",
  một: "1",
  mốt: "1",
  one: "1",
  hai: "2",
  two: "2",
  ba: "3",
  three: "3",
  bốn: "4",
  tư: "4",
  four: "4",
  năm: "5",
  five: "5",
  sáu: "6",
  six: "6",
  bảy: "7",
  seven: "7",
  tám: "8",
  eight: "8",
  chín: "9",
  nine: "9",
};

const LOCAL = "[\\p{L}\\p{N}._%+-]+";
const LABEL = "[\\p{L}\\p{N}-]+";
const BRACKET_DOT = "\\s*[(\\[{<]\\s*(?:dot|chấm)\\s*[)\\]}>]\\s*";
const WORD_DOT = "\\s+(?:dot|chấm)\\s+";
const ANY_DOT = `(?:\\s*\\.\\s*|${BRACKET_DOT}|${WORD_DOT})`;
const SPOKEN_DOT = `(?:${BRACKET_DOT}|${WORD_DOT})`;
// A bare word "at" is only trusted together with a spoken "dot"/"chấm".
// The label repetition is bounded: an unbounded "+" can overflow the regex stack on long input.
const EMAIL = new RegExp(
  `(?:${LOCAL}\\s*(?:@|[(\\[{<]\\s*at\\s*[)\\]}>])\\s*${LABEL}(?:${ANY_DOT}${LABEL}){1,8}` +
    `|(?<![\\p{L}\\p{N}])${LOCAL}\\s+(?:at|a\\s+còng|a\\s+cờ\\s+còng|a\\s+móc)\\s+${LABEL}(?:${SPOKEN_DOT}${LABEL}){1,8})`,
  "iu",
);

const WORD = `(?<![\\p{L}\\p{N}])(?:${Object.keys(DIGIT_WORDS).join("|")})(?![\\p{L}\\p{N}])`;
const TOKEN = new RegExp(`[0-9]|${WORD}`, "giu");
// 0 or 84, a mobile/landline prefix, then the rest; also found inside "lớp 7 0903123456".
const VIETNAMESE = /(?:84|0)[235789][0-9]{7,9}/;
const MIN_RUN = 9;

// Up to two spaces, dots, dashes or brackets between two digits still count as one number.
const isSeparator = (gap) => gap.length <= 2 && /^[\s.\-()]*$/.test(gap);

// A run is nine or more digits (or spelled digits) joined that way. Walked token by token rather
// than with one repeated-group regex, which recurses once per token on a long "1 1 1 1 ..." text.
function hasPhone(text) {
  let digits = "";
  let tokens = 0;
  let previousEnd = -1;
  const endsRun = () => tokens >= MIN_RUN && VIETNAMESE.test(digits);
  for (const m of text.matchAll(TOKEN)) {
    if (previousEnd < 0 || !isSeparator(text.slice(previousEnd, m.index))) {
      if (endsRun()) return true;
      digits = "";
      tokens = 0;
    }
    digits += m[0].length === 1 ? m[0] : DIGIT_WORDS[m[0].toLowerCase()];
    tokens++;
    previousEnd = m.index + m[0].length;
  }
  return endsRun();
}

/** "EMAIL", "PHONE" or null. */
export function findContact(text) {
  if (!text || !text.trim()) return null;
  const s = text.normalize("NFKC").replace(/[​-‍⁠﻿­]/g, "");
  if (EMAIL.test(s)) return "EMAIL";
  return hasPhone(s) ? "PHONE" : null;
}

export function contactMessage(kind) {
  if (!kind) return "";
  return `Phần này không được ghi ${kind === "EMAIL" ? "địa chỉ email" : "số điện thoại"}. Thông tin liên hệ chỉ được chia sẻ sau khi gia sư chấp nhận học thử.`;
}
