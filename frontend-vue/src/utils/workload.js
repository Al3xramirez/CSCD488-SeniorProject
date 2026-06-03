/* This js file contains utility functions for parsing
and analyzing workload data, to help with vue pages like WorkloadProjections.vue.
This helps keep workload related logic in its own module, which can be used by
multiple components
 */


/* Parses a local date-time string (e.g. "2024-09-15T14:30") into a Date object.
* This is used to handle date-time strings for workload Items
*/
export function parseLocalDateTime(s) {
  if (!s) return new Date(NaN);
  const [datePart, timePart = "00:00:00"] = String(s).split("T");
  const [y, m, d] = datePart.split("-").map(Number);
  const [hh, mm, ssRaw] = timePart.split(":");
  const ss = (ssRaw || "0").split(".")[0];
  return new Date(y, (m || 1) - 1, d || 1, Number(hh || 0), Number(mm || 0), Number(ss || 0));
}

/*
* Returns the start of the week (Sunday) for a given date.
* If no date is provided, uses the current date.
*/
export function startOfWeekSunday(d = new Date()) {
  const out = new Date(d);
  out.setHours(0, 0, 0, 0); 
  out.setDate(out.getDate() - out.getDay()); // Sunday=0
  return out;
}

/**
 * Returns the end of the week (Saturday) for a given date.
 * If no date is provided, uses the current date.
 */
export function endOfWeekSaturday(d = new Date()) {
  const start = startOfWeekSunday(d);
  const out = new Date(start);
  out.setDate(start.getDate() + 6);
  out.setHours(23, 59, 59, 999);
  return out;
}

/* Adds a specified number of days to a date and returns the new date.
* This is used to calculate week ranges for workload projections.
*/
export function addDays(d, days) {
  const out = new Date(d);
  out.setDate(out.getDate() + days);
  return out;
}

/* Checks if the count of due items falls into light, moderate, or heavy workload levels.*/
export function workloadLevelFromCount(count) {
  if (count >= 6) return "heavy";
  if (count >= 3) return "moderate";
  return "light";
}

/* Extracts a course code (e.g. "CSCD350") from a calendar event summary string.
* This is used to link calendar events to specific classes for workload analysis.
*/
export function extractCourseCodeFromSummary(summary) {
  const s = String(summary || "");
  // Common patterns: CSCD350, CSCD 350, CSCD-350
  const m = s.match(/\b([A-Za-z]{2,6})\s?-?\s?(\d{3,4})\b/);
  if (!m) return null;
  return `${m[1]}${m[2]}`.toUpperCase();
}

/* Attempts to match a calendar event summary to one of the user's class codes.
* This is used to link calendar events to specific classes for workload analysis.
* THIS IS A VERY IMPORTANT FUNCTION
* It will first look for direct matches (which is really unlikely to work), then looks for type-based matches
*/
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

/* This const defines synonyms for different types of coursework (e.g. "assignment", "quiz", "exam") to help
* match calendar events to grade breakdown components, even if the wording is different.
* For example, an event with "HW1" in the summary could be matched to a
* grade breakdown component labeled "Homework 1" because "hw" is a recognized synonym for "homework".
* This improves the accuracy of linking calendar events to their corresponding grade components for workload analysis. 
*/
const TYPE_SYNONYMS = {
  assignment: ["assignment", "assignments", "hw", "homework"],
  quiz: ["quiz", "quizzes"],
  exam: ["exam", "midterm", "final", "test"],
  project: ["project", "milestone"],
  presentation: ["presentation", "presentations", "powerpoint", "ppt", "slides"],
  lab: ["lab", "labs"],
  discussion: ["discussion", "discussion post"],
  reading: ["reading", "read"],
};

/* Normalizes a string by converting it to lowercase, removing special characters, and collapsing whitespace.
* This is used to improve the robustness of matching calendar event summaries to grade breakdown components,
* by reducing variations in formatting and wording.
*/
function norm(s) {
  return String(s || "")
    .toLowerCase()
    .replace(/[^a-z0-9%\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/* This is the big boy function that tries to match a calendar event summary to a grade breakdown component, using multiple strategies:
1) Direct component name match: checks if the normalized event summary includes the normalized component name.
2) Type-based match: detects if the event summary contains keywords associated with a coursework type (e.g. "hw" for homework),
 and then checks if any component names include synonyms for that type.
3) If there is exactly one component in the grade breakdown, it assumes that is the match.
This function is crucial for linking calendar events to their corresponding grade components
*/
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
