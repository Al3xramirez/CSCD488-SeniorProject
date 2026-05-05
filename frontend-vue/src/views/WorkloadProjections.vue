<script setup>
import { computed, onMounted, reactive, ref } from "vue";

import {
  addDays,
  endOfWeekSaturday,
  matchEventToClassCode,
  matchGradeBreakdownComponent,
  parseLocalDateTime,
  startOfWeekSunday,
  workloadLevelFromCount,
} from "../utils/workload";

const loading = ref(false);
const error = ref("");

const myClasses = ref([]);
const events = ref([]);

const weekIndex = ref(0); // 0 = this week

// toggle state per joinCode
const open = reactive({});

const syllabusCache = ref(new Map()); // joinCode -> syllabusObject|null

async function fetchMyClasses() {
  const res = await fetch("/api/classes/mine", { credentials: "include" });
  if (!res.ok) throw new Error("Failed to load classes");
  myClasses.value = await res.json();
}

async function fetchEvents() {
  const res = await fetch("/api/calendar/events", { credentials: "include" });
  if (!res.ok) throw new Error("Failed to load calendar events");
  events.value = await res.json();
}

async function fetchSyllabusIfNeeded(joinCode) {
  if (!joinCode) return;
  if (syllabusCache.value.has(joinCode)) return;

  const res = await fetch(`/api/courses/${joinCode}/syllabus`, { credentials: "include" });
  if (res.ok) {
    const data = await res.json();
    syllabusCache.value = new Map(syllabusCache.value).set(joinCode, data);
  } else {
    syllabusCache.value = new Map(syllabusCache.value).set(joinCode, null);
  }
}

function weekStartForIndex(i) {
  return addDays(startOfWeekSunday(new Date()), i * 7);
}

function weekEndForIndex(i) {
  return endOfWeekSaturday(weekStartForIndex(i));
}

const selectedWeekStart = computed(() => weekStartForIndex(weekIndex.value));
const selectedWeekEnd = computed(() => weekEndForIndex(weekIndex.value));

function fmtRange(start, end) {
  const a = start.toLocaleDateString(undefined, { month: "short", day: "numeric" });
  const b = end.toLocaleDateString(undefined, { month: "short", day: "numeric" });
  return `${a} – ${b}`;
}

const weekOptions = computed(() => {
  const out = [];
  for (let i = 0; i < 8; i++) {
    out.push({
      value: i,
      label: `${i === 0 ? "This week" : `Week +${i}`} · ${fmtRange(weekStartForIndex(i), weekEndForIndex(i))}`,
    });
  }
  return out;
});

function inSelectedWeek(dt) {
  return dt >= selectedWeekStart.value && dt <= selectedWeekEnd.value;
}

// All iCal events returned by the backend are treated as workload items.
// (We only exclude cancelled events.)
const workloadEvents = computed(() => events.value.filter((e) => !e.isCancelled));

const overallCount = computed(() =>
  workloadEvents.value.filter((e) => inSelectedWeek(parseLocalDateTime(e.startAt))).length
);

const overallLevel = computed(() => workloadLevelFromCount(overallCount.value));

const overallMeterWidth = computed(() => Math.min(100, (overallCount.value / 8) * 100));

function classCodeForJoinCode(joinCode) {
  const c = myClasses.value.find((x) => x.joinCode === joinCode);
  return String(c?.classCode || "").toUpperCase() || null;
}

function eventsForClassThisWeek(joinCode) {
  const code = classCodeForJoinCode(joinCode);
  if (!code) return [];

  return workloadEvents.value
    .filter((e) => {
      const dt = parseLocalDateTime(e.startAt);
      if (!inSelectedWeek(dt)) return false;
      const matched = matchEventToClassCode(e.summary, myClasses.value);
      return matched === code;
    })
    .sort((a, b) => parseLocalDateTime(a.startAt) - parseLocalDateTime(b.startAt));
}

function gradeBreakdownForJoinCode(joinCode) {
  const syllabus = syllabusCache.value.get(joinCode);
  const gb = syllabus?.gradeBreakdown;
  return Array.isArray(gb) ? gb : [];
}

function weightBadgeForEvent(joinCode, summary) {
  const gb = gradeBreakdownForJoinCode(joinCode);
  const matched = matchGradeBreakdownComponent(summary, gb);
  if (!matched) return null;
  const comp = matched.component || "";
  const w = matched.weight || "";
  if (!comp && !w) return null;
  return `${comp}${comp && w ? " · " : ""}${w}`;
}

function eventKey(e) {
  return `${e.icalUid || ""}|${e.startAt || ""}|${e.recurrenceId || ""}`;
}

async function toggleClass(joinCode) {
  open[joinCode] = !open[joinCode];
  if (open[joinCode]) {
    await fetchSyllabusIfNeeded(joinCode);
  }
}

onMounted(async () => {
  loading.value = true;
  error.value = "";
  try {
    await Promise.all([fetchMyClasses(), fetchEvents()]);
  } catch (e) {
    error.value = e?.message || "Failed to load workload projections";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="wrap">
    <div class="topbar">
      <div>
        <h1>Workload Projections</h1>
        <p class="muted">Counts are based on Canvas iCal due items (Sunday–Saturday).</p>
      </div>

      <select class="select" v-model.number="weekIndex">
        <option v-for="o in weekOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
      </select>
    </div>

    <div v-if="error" class="note err">{{ error }}</div>

    <div class="grid">
      <!-- Overall -->
      <section class="card">
        <div class="card-header">
          <div>
            <h2>Overall Workload</h2>
            <p class="muted">{{ overallCount }} assignment(s) due · {{ fmtRange(selectedWeekStart, selectedWeekEnd) }}</p>
          </div>
          <span class="pill" :class="overallLevel">{{ overallLevel }}</span>
        </div>

        <div class="workload-scale">
          <span class="pill heavy">Heavy</span>
          <span class="pill moderate">Moderate</span>
          <span class="pill light">Light</span>
        </div>

        <div class="workload-meter" aria-hidden="true">
          <div class="workload-meter-fill" :class="overallLevel" :style="{ width: overallMeterWidth + '%' }" />
        </div>

        <div class="empty-note">
          {{ overallCount ? `Workload level: ${overallLevel}.` : 'No due assignments detected for this week.' }}
        </div>
      </section>

      <!-- Per-class -->
      <section class="card">
        <div class="card-header">
          <div>
            <h2>By Class</h2>
            <p class="muted">Toggle a class to see due items and their grade weight (from the syllabus breakdown).</p>
          </div>
        </div>

        <div v-if="loading" class="placeholder">Loading…</div>
        <div v-else-if="!myClasses.length" class="placeholder">
          <p class="muted" style="margin:0">No classes found.</p>
        </div>

        <div v-else class="class-list">
          <div class="class-block" v-for="c in myClasses" :key="c.joinCode">
            <button class="toggle" @click="toggleClass(c.joinCode)">
              <div>
                <div class="class-code">{{ c.classCode }}</div>
                <div class="class-sub">{{ c.quarter }} {{ c.year }}</div>
              </div>
              <div class="toggle-right">
                <span class="count">{{ eventsForClassThisWeek(c.joinCode).length }}</span>
                <span class="chev">{{ open[c.joinCode] ? '−' : '+' }}</span>
              </div>
            </button>

            <div v-if="open[c.joinCode]" class="panel">
              <div v-if="syllabusCache.has(c.joinCode) && syllabusCache.get(c.joinCode) === null" class="placeholder">
                <p class="muted" style="margin:0">No syllabus uploaded yet for this course.</p>
              </div>

              <div v-else>
                <div class="subcard" v-if="gradeBreakdownForJoinCode(c.joinCode).length">
                  <div class="subhead">Grade breakdown</div>
                  <div class="breakdown-row" v-for="(item, i) in gradeBreakdownForJoinCode(c.joinCode)" :key="i">
                    <span class="breakdown-name">{{ item.component }}</span>
                    <span class="breakdown-weight">{{ item.weight }}</span>
                  </div>
                </div>

                <div class="subcard">
                  <div class="subhead">Due this week · {{ fmtRange(selectedWeekStart, selectedWeekEnd) }}</div>

                  <div v-if="eventsForClassThisWeek(c.joinCode).length" class="items">
                    <div class="item" v-for="e in eventsForClassThisWeek(c.joinCode)" :key="eventKey(e)">
                      <div class="item-title">{{ e.summary || '(No title)' }}</div>
                      <div class="item-sub">
                        <span class="chip">Due {{ parseLocalDateTime(e.startAt).toLocaleString(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }) }}</span>
                        <span class="chip" v-if="weightBadgeForEvent(c.joinCode, e.summary)">
                          {{ weightBadgeForEvent(c.joinCode, e.summary) }}
                        </span>
                        <span class="chip muted" v-else>Weight: —</span>
                      </div>
                    </div>
                  </div>

                  <div v-else class="placeholder">
                    <p class="muted" style="margin:0">No due assignments detected for this class this week.</p>
                  </div>
                </div>

                <div class="note small">
                  If your syllabus only provides category weights (e.g., “Assignments 60%”), we show that category weight for each matched item. Exact per-assignment % typically requires point totals or explicit per-assignment grading.
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.wrap { color: #e5e7eb; }

.topbar {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

h1 { margin: 0; font-size: 22px; }
h2 { margin: 0; font-size: 18px; }

.muted { margin: 6px 0 0; color: #9ca3af; font-size: 13px; }

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
}

.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
}

.card-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.select {
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(255,255,255,0.04);
  color: #e5e7eb;
  padding: 10px 12px;
  border-radius: 14px;
}

.workload-scale {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.2px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(255,255,255,0.04);
  color: #e5e7eb;
  text-transform: capitalize;
}

.pill.heavy {
  background: rgba(239, 68, 68, 0.18);
  border-color: rgba(239, 68, 68, 0.35);
  color: rgba(254, 202, 202, 0.95);
}

.pill.moderate {
  background: rgba(245, 158, 11, 0.16);
  border-color: rgba(245, 158, 11, 0.32);
  color: rgba(253, 230, 138, 0.95);
}

.pill.light {
  background: rgba(34, 197, 94, 0.16);
  border-color: rgba(34, 197, 94, 0.30);
  color: rgba(187, 247, 208, 0.95);
}

.workload-meter {
  height: 12px;
  border-radius: 999px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  overflow: hidden;
  margin-bottom: 12px;
}

.workload-meter-fill { height: 100%; border-radius: 999px; background: rgba(255,255,255,0.10); }

.workload-meter-fill.heavy {
  background: linear-gradient(90deg, rgba(239, 68, 68, 0.65), rgba(239, 68, 68, 0.25));
}
.workload-meter-fill.moderate {
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.65), rgba(245, 158, 11, 0.25));
}
.workload-meter-fill.light {
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.65), rgba(34, 197, 94, 0.25));
}

.placeholder {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.10);
}

.empty-note {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.10);
  font-size: 13px;
  color: #9ca3af;
  font-weight: 800;
}

.note {
  padding: 10px 12px;
  border-radius: 14px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
}

.note.small {
  margin-top: 12px;
  font-weight: 700;
  color: #9ca3af;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.note.err {
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.25);
  color: rgba(254, 202, 202, 0.95);
}

.class-list { display: flex; flex-direction: column; gap: 12px; }

.class-block {
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  overflow: hidden;
}

.toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 12px;
  background: transparent;
  border: none;
  color: inherit;
  cursor: pointer;
}

.toggle:hover { background: rgba(255,255,255,0.04); }

.toggle-right { display: flex; align-items: center; gap: 10px; }

.count {
  font-size: 12px;
  font-weight: 1000;
  padding: 6px 9px;
  border-radius: 999px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
}

.chev { font-weight: 1000; font-size: 16px; width: 16px; text-align: center; }

.class-code { font-weight: 900; color: #e5e7eb; }
.class-sub { margin-top: 4px; color: #9ca3af; font-size: 12px; font-weight: 800; }

.panel { padding: 12px; border-top: 1px solid rgba(255,255,255,0.06); }

.subcard {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  margin-bottom: 12px;
}

.subhead {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
  margin-bottom: 10px;
}

.breakdown-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.breakdown-row:last-child { border-bottom: none; }

.breakdown-name { color: #e5e7eb; font-weight: 800; }

.breakdown-weight { color: #9ca3af; font-weight: 900; }

.items { display: flex; flex-direction: column; gap: 10px; }

.item {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.item-title { font-weight: 900; }

.item-sub { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 8px; }

.chip {
  font-size: 12px;
  font-weight: 900;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  color: #e5e7eb;
}

.chip.muted { color: #9ca3af; }
</style>
