/* This js file contains utility functions for parsing
and analyzing workload data, to help with vue pages like WorkloadProjections.vue.
This helps keep workload related logic in its own module, which can be used by
multiple components
 */

export function parseLocalDateTime(s) {
  if (!s) return new Date(NaN);
  const [datePart, timePart = "00:00:00"] = String(s).split("T");
  const [y, m, d] = datePart.split("-").map(Number);
  const [hh, mm, ssRaw] = timePart.split(":");
  const ss = (ssRaw || "0").split(".")[0];
  return new Date(y, (m || 1) - 1, d || 1, Number(hh || 0), Number(mm || 0), Number(ss || 0));
}

export function startOfWeekSunday(d = new Date()) {
  const out = new Date(d);
  out.setHours(0, 0, 0, 0);
  out.setDate(out.getDate() - out.getDay()); // Sunday=0
  return out;
}

export function endOfWeekSaturday(d = new Date()) {
  const start = startOfWeekSunday(d);
  const out = new Date(start);
  out.setDate(start.getDate() + 6);
  out.setHours(23, 59, 59, 999);
  return out;
}

export function addDays(d, days) {
  const out = new Date(d);
  out.setDate(out.getDate() + days);
  return out;
}

export function isLikelyWorkloadItem(summary) {
  const s = String(summary || "").toLowerCase();
  if (!s) return false;
  return (
    s.includes("due") ||
    s.includes("assignment") ||
    s.match(/\bhw\b/) ||
    s.includes("homework") ||
    s.includes("quiz") ||
    s.includes("exam") ||
    s.includes("midterm") ||
    s.includes("final") ||
    s.includes("project") ||
    s.includes("lab") ||
    s.includes("discussion")
  );
}

export function workloadLevelFromCount(count) {
  if (count >= 6) return "heavy";
  if (count >= 3) return "moderate";
  return "light";
}

export function extractCourseCodeFromSummary(summary) {
  const s = String(summary || "");
  // Common patterns: CSCD350, CSCD 350, CSCD-350
  const m = s.match(/\b([A-Za-z]{2,6})\s?-?\s?(\d{3,4})\b/);
  if (!m) return null;
  return `${m[1]}${m[2]}`.toUpperCase();
}

export function matchEventToClassCode(summary, myClasses) {
  const s = String(summary || "").toUpperCase();
  if (!s) return null;

  const candidates = (myClasses || [])
    .map((c) => String(c?.classCode || "").toUpperCase())
    .filter(Boolean)
    .sort((a, b) => b.length - a.length);

  for (const code of candidates) {
    if (s.includes(code)) return code;
    const spaced = code.replace(/^(\D+)(\d+)/, "$1 $2");
    if (spaced !== code && s.includes(spaced)) return code;
  }

  return extractCourseCodeFromSummary(summary);
}

export function parsePercent(weightStr) {
  const raw = String(weightStr || "").trim();
  if (!raw) return null;
  const m = raw.match(/(-?\d+(?:\.\d+)?)\s*%/);
  if (m) return Number(m[1]);
  const n = Number(raw);
  if (Number.isFinite(n)) {
    if (n > 0 && n <= 1) return n * 100;
    return n;
  }
  return null;
}

const TYPE_SYNONYMS = {
  assignment: ["assignment", "assignments", "hw", "homework"],
  quiz: ["quiz", "quizzes"],
  exam: ["exam", "midterm", "final", "test"],
  project: ["project", "milestone"],
  lab: ["lab", "labs"],
  discussion: ["discussion", "discussion post"],
  reading: ["reading", "read"],
};

function norm(s) {
  return String(s || "")
    .toLowerCase()
    .replace(/[^a-z0-9%\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function matchGradeBreakdownComponent(eventSummary, gradeBreakdown) {
  const s = norm(eventSummary);
  const list = Array.isArray(gradeBreakdown) ? gradeBreakdown : [];
  if (!s || !list.length) return null;

  // 1) direct component name match
  for (const item of list) {
    const comp = norm(item?.component);
    if (comp && s.includes(comp)) return item;
  }

  // 2) type-based match to a component
  let detectedType = null;
  for (const [t, words] of Object.entries(TYPE_SYNONYMS)) {
    for (const w of words) {
      if (w === "hw") {
        if (s.match(/\bhw\b/)) {
          detectedType = t;
          break;
        }
      } else if (s.includes(w)) {
        detectedType = t;
        break;
      }
    }
    if (detectedType) break;
  }

  if (detectedType) {
    const synonyms = TYPE_SYNONYMS[detectedType] || [];
    for (const item of list) {
      const comp = norm(item?.component);
      if (!comp) continue;
      if (synonyms.some((w) => (w === "hw" ? comp.match(/\bhw\b/) : comp.includes(w)))) {
        return item;
      }
    }
  }

  // 3) if exactly one component, assume it
  if (list.length === 1) return list[0];

  return null;
}
