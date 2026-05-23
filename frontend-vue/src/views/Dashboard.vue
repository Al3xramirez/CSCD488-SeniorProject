<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";

import {
  endOfWeekSaturday,
  parseLocalDateTime,
  startOfWeekSunday,
  workloadLevelFromCount,
} from "../utils/workload";

const me = inject("me", null);
const router = useRouter();

const role = computed(() => {
  const r = me?.value?.role;
  return (r || "STUDENT").toString().trim().toUpperCase();
});

// ── Syllabus Overview ──────────────────────────────────────────────
const myClasses = inject('classes', ref([]));
const selectedJoinCode = ref(null);
const syllabus = ref(null);
const loadingSyllabus = ref(false);
const syllabusError = ref("");

// Set the initial selected class once classes are available
watch(
  myClasses,
  (val) => { if (val.length > 0 && !selectedJoinCode.value) selectedJoinCode.value = val[0].joinCode; },
  { immediate: true }
);

async function fetchSyllabus(joinCode) {
  if (!joinCode) return;
  loadingSyllabus.value = true;
  syllabusError.value = "";
  syllabus.value = null;
  try {
    const res = await fetch(`/api/courses/${joinCode}/syllabus`, { credentials: "include" });
    if (res.ok) {
      syllabus.value = await res.json();
    } else if (res.status === 404) {
      syllabusError.value = "No syllabus uploaded yet for this course.";
    } else {
      syllabusError.value = "Could not load syllabus.";
    }
  } catch {
    syllabusError.value = "Could not load syllabus.";
  } finally {
    loadingSyllabus.value = false;
  }
}

watch(selectedJoinCode, (val) => fetchSyllabus(val));

const selectedClass = computed(() =>
  myClasses.value.find(c => c.joinCode === selectedJoinCode.value) ?? null
);

const gradeBreakdown = computed(() => {
  const gb = syllabus.value?.gradeBreakdown;
  return Array.isArray(gb) ? gb : [];
});

const gradeScale = computed(() => {
  const gs = syllabus.value?.gradeScale;
  return Array.isArray(gs) ? gs : [];
});

const attendance = computed(() => syllabus.value?.attendance ?? null);

const passConditions = computed(() => {
  const pc = syllabus.value?.passConditions;
  return Array.isArray(pc) ? pc : [];
});

const meetingTimes = computed(() => syllabus.value?.classMeetingTimes ?? null);

const officeHours = computed(() => {
  const oh = syllabus.value?.officeHours;
  return Array.isArray(oh) ? oh : [];
});

function formatTime(t) {
  if (!t) return "";
  const [hh, mm] = t.split(":").map(Number);
  const ampm = hh < 12 ? "AM" : "PM";
  const h = hh % 12 || 12;
  return `${h}:${String(mm).padStart(2, "0")} ${ampm}`;
}

function formatDays(days) {
  if (!Array.isArray(days)) return "";
  const map = { MON: "Mon", TUE: "Tue", WED: "Wed", THU: "Thu", FRI: "Fri", SAT: "Sat", SUN: "Sun" };
  return days.map(d => map[d] || d).join(", ");
}

// ── Calendar events ────────────────────────────────────────────────
const loadingEvents = ref(false);
const events = ref([]);

const hasEvents = computed(() => events.value.length > 0);

const upcomingItems = computed(() => {
  const now = new Date();
  // End of current week Saturday 23:59:59 (Sunday–Saturday week)
  const endOfWeek = endOfWeekSaturday(now);

  return events.value
    .filter(e => {
      if (e.isCancelled) return false;
      const start = parseLocalDateTime(e.startAt);
      return start >= now && start <= endOfWeek;
    })
    .sort((a, b) => parseLocalDateTime(a.startAt) - parseLocalDateTime(b.startAt))
    .slice(0, 5)
    .map(e => {
      const start = parseLocalDateTime(e.startAt);
      const dayAbbr = start.toLocaleDateString(undefined, { weekday: "short" });
      const dateStr = start.toLocaleDateString(undefined, { month: "short", day: "numeric" });
      const timeStr = e.allDay
        ? "All day"
        : start.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
      return {
        label: dayAbbr,
        title: e.summary || "(No title)",
        sub: `${dateStr} · ${timeStr}`,
      };
    });
});

async function fetchEvents() {
  loadingEvents.value = true;
  try {
    const res = await fetch("/api/calendar/events", { credentials: "include" });
    if (res.ok) events.value = await res.json();
  } finally {
    loadingEvents.value = false;
  }
}

// ── Workload This Week (overall Sun–Sat) ──────────────────────────
const weekStart = computed(() => startOfWeekSunday(new Date()));
const weekEnd = computed(() => endOfWeekSaturday(new Date()));

const assignmentsThisWeek = computed(() => {
  const start = weekStart.value;
  const end = weekEnd.value;
  return events.value.filter((e) => {
    if (e.isCancelled) return false;
    const dt = parseLocalDateTime(e.startAt);
    return dt >= start && dt <= end;
  });
});

const assignmentCountThisWeek = computed(() => assignmentsThisWeek.value.length);
const workloadLevelThisWeek = computed(() => workloadLevelFromCount(assignmentCountThisWeek.value));
const workloadMeterWidth = computed(() => Math.min(100, (assignmentCountThisWeek.value / 8) * 100));

onMounted(() => {
  fetchEvents();
});
</script>
<template>
  <div class="grid">
    <!-- Left: Syllabus Overview -->
    <section class="card big">
      <div class="card-header">
        <div>
          <h2>Syllabus Overview</h2>
          <p class="muted">Content organized from professor-uploaded syllabus PDF.</p>
        </div>
        <!-- Class selector when enrolled in multiple courses -->
        <select
          v-if="myClasses.length > 1"
          class="class-select"
          v-model="selectedJoinCode"
        >
          <option v-for="c in myClasses" :key="c.joinCode" :value="c.joinCode">
            {{ c.classCode }} · {{ c.quarter }} {{ c.year }}
          </option>
        </select>
      </div>

      <!-- No classes enrolled -->
      <div v-if="myClasses.length === 0" class="placeholder">
        <p class="muted" style="margin:0">You are not enrolled in any classes yet.</p>
      </div>

      <!-- Loading -->
      <div v-else-if="loadingSyllabus" class="placeholder">
        <p class="muted" style="margin:0">Loading syllabus…</p>
      </div>

      <!-- Error / not uploaded yet -->
      <div v-else-if="syllabusError" class="placeholder">
        <p class="muted" style="margin:0">{{ syllabusError }}</p>
      </div>

      <!-- Real syllabus data -->
      <div v-else-if="syllabus" class="content">
        <div class="mini-grid">
          <div class="mini">
            <div class="mini-title">Course</div>
            <div class="mini-value">{{ selectedClass?.classCode ?? '—' }}</div>
          </div>
          <div class="mini">
            <div class="mini-title">Quarter / Year</div>
            <div class="mini-value">{{ selectedClass ? `${selectedClass.quarter} ${selectedClass.year}` : '—' }}</div>
          </div>
          <div class="mini">
            <div class="mini-title">Attendance</div>
            <div class="mini-value">{{ attendance ? (attendance.tracked === 'yes' ? 'Tracked' : attendance.tracked === 'partial' ? 'Partial' : 'Not tracked') : '—' }}</div>
          </div>
        </div>

        <!-- Office Hours -->
        <div v-if="officeHours.length" class="syllabus-section">
          <div class="syllabus-section__title">Office Hours</div>
          <div v-for="(block, i) in officeHours" :key="i">
            <hr v-if="i > 0" class="oh-divider" />
            <div class="meeting-times">
              <div v-if="block.days?.length" class="meeting-row">
                <span class="meeting-label">Days</span>
                <span class="meeting-value">{{ formatDays(block.days) }}</span>
              </div>
              <div v-if="block.startTime && block.endTime" class="meeting-row">
                <span class="meeting-label">Time</span>
                <span class="meeting-value">{{ formatTime(block.startTime) }} – {{ formatTime(block.endTime) }}</span>
              </div>
              <div v-if="block.location" class="meeting-row">
                <span class="meeting-label">Location</span>
                <span class="meeting-value">{{ block.location }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Meeting Times -->
        <div v-if="meetingTimes" class="syllabus-section">
          <div class="syllabus-section__title">Meeting Times</div>
          <div class="meeting-times">
            <div v-if="meetingTimes.days?.length" class="meeting-row">
              <span class="meeting-label">Days</span>
              <span class="meeting-value">{{ formatDays(meetingTimes.days) }}</span>
            </div>
            <div v-if="meetingTimes.startTime && meetingTimes.endTime" class="meeting-row">
              <span class="meeting-label">Time</span>
              <span class="meeting-value">{{ formatTime(meetingTimes.startTime) }} – {{ formatTime(meetingTimes.endTime) }}</span>
            </div>
            <div v-if="meetingTimes.location" class="meeting-row">
              <span class="meeting-label">Location</span>
              <span class="meeting-value">{{ meetingTimes.location }}</span>
            </div>
          </div>
        </div>

        <!-- Grade Scale -->
        <div v-if="gradeScale.length" class="syllabus-section">
          <div class="syllabus-section__title">Grade Scale</div>
          <div class="grade-scale-list">
            <div class="grade-scale-row" v-for="(item, i) in gradeScale" :key="i">
              <span class="grade-range">{{ item.range }}</span>
              <span class="grade-letter">{{ item.letter }}</span>
            </div>
          </div>
        </div>

        <!-- Grade Breakdown -->
        <div v-if="gradeBreakdown.length" class="syllabus-section">
          <div class="syllabus-section__title">Grade Breakdown</div>
          <div class="breakdown-row" v-for="(item, i) in gradeBreakdown" :key="i">
            <span class="breakdown-name">{{ item.component }}</span>
            <span class="breakdown-weight">{{ item.weight }}</span>
          </div>
        </div>

        <!-- Pass / Fail Conditions -->
        <div v-if="passConditions.length" class="syllabus-section">
          <div class="syllabus-section__title">Pass / Fail Conditions</div>
          <ul class="pass-conditions-list">
            <li v-for="(cond, i) in passConditions" :key="i">{{ cond }}</li>
          </ul>
        </div>

        <!-- AI Policy -->
        <div v-if="syllabus.aiPolicy" class="syllabus-section">
          <div class="syllabus-section__title">AI Policy</div>
          <p class="syllabus-text">{{ syllabus.aiPolicy }}</p>
        </div>

        <!-- Late Work Policy -->
        <div v-if="syllabus.lateWorkPolicy" class="syllabus-section">
          <div class="syllabus-section__title">Late Work Policy</div>
          <p class="syllabus-text">{{ syllabus.lateWorkPolicy }}</p>
        </div>
      </div>
    </section>

    <div class="right-col">
      <!-- Right: Upcoming This Week -->
      <section class="card side">
        <div class="card-header">
          <div>
            <h2>Upcoming This Week</h2>
          </div>
          <button class="btn" @click="router.push('/app/calendar')">Import Canvas .ics</button>
        </div>

        <div class="list" v-if="loadingEvents">
          <div class="empty-note">Loading…</div>
        </div>

        <div class="list" v-else-if="upcomingItems.length">
          <div class="item" v-for="(row, i) in upcomingItems" :key="i">
            <div class="badge">{{ row.label }}</div>
            <div class="info">
              <div class="title">{{ row.title }}</div>
              <div class="sub">{{ row.sub }}</div>
            </div>
          </div>
        </div>

        <div class="list" v-else>
          <div class="empty-note">
            {{ hasEvents ? "Nothing due this week." : "No Canvas feed connected yet." }}
          </div>
        </div>
      </section>

      <!-- Workload This Week  -->
      <section class="card">
        <div class="card-header">
          <div>
            <h2>Workload This Week</h2>
            <p class="muted" style="margin:6px 0 0">{{ assignmentCountThisWeek }} assignment(s) due (Sun–Sat).</p>
          </div>
          <button class="btn" @click="router.push('/app/workload-projections')">Details</button>
        </div>

        <div class="workload-scale">
          <span class="pill heavy">Heavy</span>
          <span class="pill moderate">Moderate</span>
          <span class="pill light">Light</span>
        </div>

        <div class="workload-meter" aria-hidden="true">
          <div class="workload-meter-fill" :class="workloadLevelThisWeek" :style="{ width: workloadMeterWidth + '%' }"></div>
        </div>

        <div v-if="!assignmentCountThisWeek" class="empty-note">
          {{ hasEvents ? 'No due assignments detected this week.' : 'No Canvas feed connected yet.' }}
        </div>
      </section>
    </div>

  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
}

.right-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

 /* Pill component for workload levels */

.pill {
  display: inline-flex;
  align-items: center;
  padding: 10px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.2px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(255,255,255,0.04);
  color: #e5e7eb;
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

/* css for workload meter */

.workload-scale {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.workload-meter {
  height: 14px;
  border-radius: 999px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  overflow: hidden;
  margin-bottom: 12px;
}

.workload-meter-fill {
  height: 100%;
  border-radius: 999px;
  background: rgba(255,255,255,0.10);
}

.workload-meter-fill.heavy {
  background: linear-gradient(
    90deg,
    rgba(239, 68, 68, 0.65),
    rgba(239, 68, 68, 0.25)
  );
}

.workload-meter-fill.moderate {
  background: linear-gradient(
    90deg,
    rgba(245, 158, 11, 0.65),
    rgba(245, 158, 11, 0.25)
  );
}

.workload-meter-fill.light {
  background: linear-gradient(
    90deg,
    rgba(34, 197, 94, 0.65),
    rgba(34, 197, 94, 0.25)
  );
}

/* Class schedule grid */

.prof {
  display: flex;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
  margin-top: 8px;
}

.class-box {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
}

.class-code {
  font-weight: 900;
  color: #e5e7eb;
}

.class-title {
  margin-top: 6px;
  color: #9ca3af;
  font-size: 13px;
}

.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 16px;
  color: #e5e7eb;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
}

.big {
  min-height: 360px;
  background: rgba(255, 255, 255, 0);
  border-color: rgba(255, 255, 255, 0.13);
}

.side {
  min-height: 360px;
}

.small {
  grid-column: span 1;
}

.card-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

h2 {
  margin: 0;
  font-size: 18px;
}
h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.muted {
  margin: 6px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

.btn {
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 800;
  padding: 10px 12px;
  border-radius: 14px;
  cursor: pointer;
}

.btn:hover {
  background: #1d4ed8;
}

.btn.ghost {
  background: hsla(0, 0%, 100%, 0.06);
  border: 1px solid rgba(255,255,255,0.10);
}

.btn.ghost:hover {
  background: rgba(255,255,255,0.09);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.placeholder {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.10);
}

.line {
  height: 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.08);
  margin: 10px 0;
}

.w55 { width: 55%; }
.w60 { width: 60%; }
.w70 { width: 70%; }
.w85 { width: 85%; }
.w90 { width: 90%; }

.mini-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.mini {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.mini-title {
  font-size: 12px;
  color: #9ca3af;
}

.mini-value {
  margin-top: 6px;
  font-weight: 900;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.badge {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: rgba(37,99,235,0.25);
  border: 1px solid rgba(37,99,235,0.35);
  color: #bfdbfe;
  font-weight: 900;
}

.title {
  font-weight: 900;
}

.sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
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

.row {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.input {
  flex: 1;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(15,23,42,0.6);
  color: #e5e7eb;
  outline: none;
}

.input:focus {
  box-shadow: 0 0 0 2px rgba(37,99,235,0.5);
  border-color: rgba(37,99,235,0.6);
}

.class-select {
  font-size: 13px;
  padding: 10px 12px;
  border-radius: 14px;
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 900;
  outline: none;
  cursor: pointer;
  flex-shrink: 0;
}

.class-select:hover {
  background: #1d4ed8;
}

.class-select:focus {
  box-shadow: 0 0 0 2px rgba(37,99,235,0.5);
}

.syllabus-section {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.syllabus-section__title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
  margin-bottom: 8px;
}

.breakdown-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.breakdown-row:last-child {
  border-bottom: none;
}

.breakdown-name {
  color: #e5e7eb;
}

.breakdown-weight {
  color: #9ca3af;
  font-weight: 600;
}

.grade-scale-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.grade-scale-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.grade-scale-row:last-child {
  border-bottom: none;
}

.grade-letter {
  font-weight: 700;
  color: #e5e7eb;
}

.grade-range {
  color: #9ca3af;
}

.pass-conditions-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pass-conditions-list li {
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.5;
}

.syllabus-text {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.oh-divider {
  border: none;
  border-top: 1px solid rgba(255,255,255,0.07);
  margin: 6px 0;
}

.meeting-times {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meeting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.meeting-row:last-child {
  border-bottom: none;
}

.meeting-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #6b7280;
}

.meeting-value {
  color: #e5e7eb;
  font-weight: 600;
}

@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .mini-grid {
    grid-template-columns: 1fr;
  }
}
</style>
