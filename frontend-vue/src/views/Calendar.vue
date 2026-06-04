<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";

// ── Shared state from layout ──────────────────────────────────────────────
const classes = inject("classes", ref([]));

// ── Canvas feed ───────────────────────────────────────────────────────────
const icsUrl      = ref("");
const loading     = ref(false);
const syncing     = ref(false);
const error       = ref("");
const status      = ref("");
const canvasEvents = ref([]);

// ── Meeting events (personal + class-wide with specific dates) ────────────
const meetingEvents   = ref([]);
const meetingsLoading = ref(false);

// ── Office hours (recurring weekly schedules from enrolled classes) ────────
const officeHours = ref([]);   // [{ userId, firstName, lastName, role, schedule:[{dayOfWeek,startTime,endTime}] }]
const ohLoading   = ref(false);

// ── Calendar state ────────────────────────────────────────────────────────
const monthCursor = ref(startOfMonth(new Date()));
const selectedDay = ref(toYmd(new Date()));

const monthLabel = computed(() =>
  monthCursor.value.toLocaleString(undefined, { month: "long", year: "numeric" })
);

const days = computed(() => buildMonthGrid(monthCursor.value));

// Merge canvas + meetings into one dated-event list for the grid
const allDatedEvents = computed(() => {
  const canvas = canvasEvents.value.map(e => ({ ...e, eventType: "canvas" }));
  const meetings = meetingEvents.value.map(m => ({
    startAt:   `${m.meetingDate}T${m.startTime}`,
    summary:   m.recipientId ? `Meeting · ${m.classCode}` : `Class · ${m.classCode}`,
    location:  m.notes || "",
    isCancelled: false,
    allDay:    false,
    provider:  m.recipientId ? "meeting" : "class",
    eventType: m.recipientId ? "meeting" : "class",
    status:    m.status,
    meetingId: m.meetingId,
  }));
  return [...canvas, ...meetings];
});

const eventsByDay = computed(() => {
  const map = new Map();
  for (const e of allDatedEvents.value) {
    const key = toYmd(parseLocalDateTime(e.startAt));
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(e);
  }
  for (const [k, list] of map.entries()) {
    list.sort((a, b) => parseLocalDateTime(a.startAt) - parseLocalDateTime(b.startAt));
    map.set(k, list);
  }
  return map;
});

const selectedDatedEvents = computed(() =>
  eventsByDay.value.get(selectedDay.value) || []
);

// Recurring office hours matching the selected day-of-week
const DOW = ["SUN","MON","TUE","WED","THU","FRI","SAT"];
const selectedOH = computed(() => {
  if (!selectedDay.value) return [];
  const dow = DOW[new Date(selectedDay.value + "T00:00:00").getDay()];
  const out = [];
  for (const person of officeHours.value) {
    for (const block of person.schedule ?? []) {
      if (block.dayOfWeek === dow) out.push({ person, block });
    }
  }
  return out.sort((a, b) => (a.block.startTime ?? "").localeCompare(b.block.startTime ?? ""));
});

const selectedDayHasAnything = computed(() =>
  selectedDatedEvents.value.length > 0 || selectedOH.value.length > 0
);

const agendaHeader = computed(() => {
  const todayYmd = toYmd(new Date());
  if (selectedDay.value === todayYmd) return "Today's Schedule";

  const d = new Date(selectedDay.value + "T00:00:00");
  if (Number.isNaN(d.getTime())) return "Schedule";
  const pretty = d.toLocaleDateString(undefined, {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });
  return `Schedule for ${pretty}`;
});

function ohCountForDate(d) {
  const dow = DOW[d.getDay()];
  let count = 0;
  for (const person of officeHours.value) {
    for (const block of person.schedule ?? []) {
      if (block.dayOfWeek === dow) count++;
    }
  }
  return count;
}

// ── Data fetching ─────────────────────────────────────────────────────────
async function fetchCanvasEvents() {
  error.value = "";
  status.value = "";
  loading.value = true;
  try {
    const res = await fetch("/api/calendar/events", { credentials: "include" });
    if (!res.ok) throw new Error((await res.text()) || "Failed to load events");
    canvasEvents.value = await res.json();
    status.value = `Loaded ${canvasEvents.value.length} Canvas events.`;
  } catch (e) {
    error.value = e?.message || "Failed to load events";
  } finally {
    loading.value = false;
  }
}

async function fetchMeetings() {
  meetingsLoading.value = true;
  try {
    const res = await fetch("/api/meetings/my", { credentials: "include" });
    if (res.ok) meetingEvents.value = await res.json();
  } catch {} finally {
    meetingsLoading.value = false;
  }
}

async function fetchOHForClasses(courseList) {
  if (!courseList.length) { officeHours.value = []; return; }
  ohLoading.value = true;
  const seen = new Set();
  const result = [];
  for (const c of courseList) {
    try {
      const res = await fetch(`/api/office-hours/class/${c.classCode}`, { credentials: "include" });
      if (!res.ok) continue;
      for (const person of await res.json()) {
        if (!seen.has(person.userId)) { seen.add(person.userId); result.push(person); }
      }
    } catch {}
  }
  officeHours.value = result;
  ohLoading.value = false;
}

// Re-fetch office hours whenever the shared class list changes
watch(classes, val => { if (val.length) fetchOHForClasses(val); }, { immediate: true });

async function subscribeCanvas() {
  error.value = "";
  status.value = "";
  const url = icsUrl.value.trim();
  if (!url) { error.value = "Paste your Canvas iCal (.ics) URL."; return; }
  icsUrl.value = "";
  syncing.value = true;
  try {
    const res = await fetch("/api/calendar/canvas/subscribe", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ icsUrl: url }),
    });
    if (!res.ok) throw new Error((await res.text()) || "Subscribe failed");
    status.value = "Canvas feed connected. Syncing events...";
    await fetchCanvasEvents();
  } catch (e) {
    error.value = e?.message || "Subscribe failed";
  } finally {
    syncing.value = false;
  }
}

// ── Navigation ────────────────────────────────────────────────────────────
const prevMonth = () => {
  monthCursor.value = startOfMonth(new Date(monthCursor.value.getFullYear(), monthCursor.value.getMonth() - 1, 1));
};
const nextMonth = () => {
  monthCursor.value = startOfMonth(new Date(monthCursor.value.getFullYear(), monthCursor.value.getMonth() + 1, 1));
};
const goToday = () => {
  const now = new Date();
  monthCursor.value = startOfMonth(now);
  selectedDay.value = toYmd(now);
};

onMounted(async () => {
  localStorage.removeItem("syllabussync_canvas_ics");
  await fetchCanvasEvents();
  fetchMeetings();
});

// ── Utilities ─────────────────────────────────────────────────────────────
function startOfMonth(d) {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function toYmd(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function parseLocalDateTime(s) {
  if (!s) return new Date(NaN);
  const parts = String(s).split("T");
  const [y, m, d] = (parts[0] || "").split("-").map(Number);
  const [hh, mm, ssRaw] = (parts[1] || "00:00:00").split(":");
  const ss = (ssRaw || "0").split(".")[0];
  return new Date(y, (m || 1) - 1, d || 1, Number(hh || 0), Number(mm || 0), Number(ss || 0));
}

function buildMonthGrid(monthStart) {
  const year = monthStart.getFullYear();
  const month = monthStart.getMonth();
  const firstDow = (new Date(year, month, 1).getDay() + 6) % 7; // Mon=0
  const gridStart = new Date(year, month, 1 - firstDow);
  return Array.from({ length: 42 }, (_, i) => {
    const d = new Date(gridStart);
    d.setDate(gridStart.getDate() + i);
    return d;
  });
}

// Format a full ISO datetime string to "h:mm AM/PM"
function fmtDateTime(s) {
  const d = parseLocalDateTime(s);
  if (Number.isNaN(d.getTime())) return "";
  return d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}

// Format a plain "HH:MM:SS" time string to "h:mm AM/PM"
function fmtTimeStr(t) {
  if (!t) return "";
  const [hh, mm] = t.split(":").map(Number);
  const ampm = hh < 12 ? "AM" : "PM";
  return `${hh % 12 || 12}:${String(mm).padStart(2, "0")} ${ampm}`;
}

const OH_DAY_LABEL = { MON:"Monday", TUE:"Tuesday", WED:"Wednesday", THU:"Thursday", FRI:"Friday", SAT:"Saturday", SUN:"Sunday" };
</script>

<template>
  <div class="wrap">

    <!-- Top bar -->
    <div class="topbar">
      <div>
        <h1>Calendar</h1>
        <p class="muted">Canvas assignments, class meetings, personal meetings, and office hours — all in one place.</p>
      </div>
      <div class="top-actions">
        <button class="btn" @click="goToday">Today</button>
        <button class="btn" @click="prevMonth">‹</button>
        <button class="btn" @click="nextMonth">›</button>
      </div>
    </div>

    <!-- Canvas subscription -->
    <div class="panel">
      <div class="subscribe">
        <div class="row">
          <div class="field grow">
            <label>Canvas iCal URL</label>
            <input v-model="icsUrl" placeholder="https://.../calendar.ics" />
          </div>
          <button class="btn" :disabled="syncing" @click="subscribeCanvas">
            {{ syncing ? "Syncing…" : "Connect Canvas" }}
          </button>
          <button class="btn ghost" :disabled="loading" @click="fetchCanvasEvents">
            {{ loading ? "Loading…" : "Refresh" }}
          </button>
        </div>
        <div v-if="status" class="note ok">{{ status }}</div>
        <div v-if="error"  class="note err">{{ error }}</div>
      </div>

      <!-- Calendar: grid + agenda -->
      <div class="cal">

        <!-- Month grid (left) -->
        <div class="left">
          <div class="month-head">
            <div class="month">{{ monthLabel }}</div>
            <div class="dow">
              <div v-for="d in ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']" :key="d" class="dowcell">{{ d }}</div>
            </div>
          </div>

          <div class="grid">
            <button
              v-for="d in days"
              :key="d.toISOString()"
              class="cell"
              :class="{
                other:    d.getMonth() !== monthCursor.getMonth(),
                selected: toYmd(d) === selectedDay,
                today:    toYmd(d) === toYmd(new Date()),
              }"
              @click="selectedDay = toYmd(d)"
            >
              <div class="num">{{ d.getDate() }}</div>
              <div class="dots" v-if="eventsByDay.get(toYmd(d)) || ohCountForDate(d)">
                <span
                  v-for="(e, i) in (eventsByDay.get(toYmd(d)) || []).slice(0,3)"
                  :key="i"
                  class="dot"
                  :class="`dot-${e.eventType}`"
                />
                <span v-if="ohCountForDate(d)" class="dot dot-oh" title="Office hours" />
                <span v-if="(eventsByDay.get(toYmd(d)) || []).length > 3" class="dot-more">
                  +{{ (eventsByDay.get(toYmd(d)) || []).length - 3 }}
                </span>
              </div>
            </button>
          </div>

          <!-- Legend -->
          <div class="legend">
            <span class="leg leg-canvas">Canvas</span>
            <span class="leg leg-class">Class</span>
            <span class="leg leg-meeting">Meeting</span>
            <span class="leg leg-oh">Office Hours</span>
          </div>
        </div>

        <!-- Day agenda (right) -->
        <div class="right">
          <div class="agenda-head">
            <div class="agenda-title">{{ agendaHeader }}</div>
            <div class="agenda-sub muted">
              {{ selectedDatedEvents.length }} event{{ selectedDatedEvents.length === 1 ? '' : 's' }}
              <template v-if="selectedOH.length"> · {{ selectedOH.length }} office hour{{ selectedOH.length === 1 ? '' : 's' }}</template>
            </div>
          </div>

          <div class="agenda">
            <div v-if="!selectedDayHasAnything" class="empty">
              No events for this day.
            </div>

            <!-- Dated events (Canvas + meetings) -->
            <div
              v-for="e in selectedDatedEvents"
              :key="`${e.eventType}-${e.startAt}`"
              class="evt"
              :class="{ cancelled: e.isCancelled }"
            >
              <div class="time">
                <div class="t">{{ e.allDay ? "All day" : fmtDateTime(e.startAt) }}</div>
                <div class="chip" :class="e.eventType">{{ e.eventType }}</div>
              </div>
              <div class="meta">
                <div class="title">{{ e.summary || "(no title)" }}</div>
                <div class="sub muted" v-if="e.location">{{ e.location }}</div>
                <div class="sub muted" v-if="e.isCancelled">Cancelled</div>
                <div class="sub muted" v-if="e.status && e.eventType === 'meeting'">{{ e.status }}</div>
              </div>
            </div>

            <!-- Recurring office hours for this day of week -->
            <template v-if="selectedOH.length">
              <div class="oh-divider">
                <span class="oh-divider-label">Office Hours (recurring)</span>
              </div>
              <div
                v-for="({ person, block }, i) in selectedOH"
                :key="`oh-${person.userId}-${i}`"
                class="evt"
              >
                <div class="time">
                  <div class="t">{{ fmtTimeStr(block.startTime) }}</div>
                  <div class="chip oh">OH</div>
                </div>
                <div class="meta">
                  <div class="title">{{ person.firstName }} {{ person.lastName }}</div>
                  <div class="sub muted">
                    {{ person.role === 'PROFESSOR' ? 'Professor' : 'TA' }} ·
                    {{ fmtTimeStr(block.startTime) }} – {{ fmtTimeStr(block.endTime) }}
                  </div>
                </div>
              </div>
            </template>

          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap { color: #e5e7eb; }

.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

h1 { margin: 0; font-size: 22px; }

.muted {
  margin: 6px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

.top-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.panel {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
  overflow: hidden;
}

/* ── Subscribe strip ── */
.subscribe {
  padding: 14px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  background: rgba(15,23,42,0.55);
}

.row { display: flex; gap: 12px; align-items: end; flex-wrap: wrap; }

.field { display: flex; flex-direction: column; gap: 6px; min-width: 200px; }
.field.grow { flex: 1; min-width: 280px; }

label { font-size: 12px; font-weight: 900; color: #cbd5e1; }

input {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 14px;
  padding: 10px 12px;
  color: #e5e7eb;
  outline: none;
}
input:focus { border-color: rgba(96,165,250,0.7); box-shadow: 0 0 0 3px rgba(37,99,235,0.15); }

.btn { border: none; background: #2563eb; color: white; font-weight: 900; padding: 10px 12px; border-radius: 14px; cursor: pointer; }
.btn:hover { background: #1d4ed8; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn.ghost { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.10); }
.btn.ghost:hover { background: rgba(255,255,255,0.09); }

.note { margin-top: 10px; font-size: 13px; font-weight: 800; }
.note.ok { color: #34d399; }
.note.err { color: #fb7185; }

/* ── Calendar layout ── */
.cal { display: grid; grid-template-columns: 1.4fr 1fr; min-height: 520px; }

.left { padding: 14px; border-right: 1px solid rgba(255,255,255,0.06); }

.month-head { display: flex; flex-direction: column; gap: 10px; margin-bottom: 10px; }
.month { font-weight: 1000; letter-spacing: 0.2px; }

.dow { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.dowcell { font-size: 12px; color: #9ca3af; font-weight: 900; text-transform: uppercase; letter-spacing: 0.6px; }

.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }

.cell {
  position: relative;
  min-height: 66px;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
  color: #e5e7eb;
  padding: 8px;
  text-align: left;
  cursor: pointer;
}
.cell:hover { background: rgba(255,255,255,0.05); transform: translateY(-1px); }
.cell.other  { opacity: 0.55; }
.cell.selected { background: rgba(37,99,235,0.22); border-color: rgba(37,99,235,0.45); }
.cell.today  { box-shadow: inset 0 0 0 2px rgba(96,165,250,0.45); }

.num { font-weight: 1000; }

.dots {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  margin-top: 4px;
  align-items: center;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-canvas  { background: #34d399; }
.dot-class   { background: #60a5fa; }
.dot-meeting { background: #a78bfa; }
.dot-oh      { background: #2dd4bf; }

.dot-more {
  font-size: 10px;
  font-weight: 700;
  color: #6b7280;
}

/* ── Legend ── */
.legend {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px solid rgba(255,255,255,0.06);
  margin-top: 12px;
}

.leg {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #9ca3af;
}
.leg::before {
  content: '';
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.leg-canvas::before  { background: #34d399; }
.leg-class::before   { background: #60a5fa; }
.leg-meeting::before { background: #a78bfa; }
.leg-oh::before      { background: #2dd4bf; }

/* ── Agenda ── */
.right { padding: 14px; overflow-y: auto; }

.agenda-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.agenda-title { font-weight: 1000; }

.agenda { display: flex; flex-direction: column; gap: 10px; }

.empty {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.10);
  color: #9ca3af;
  font-weight: 800;
}

.evt {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}
.evt.cancelled { opacity: 0.6; }

.time .t { font-weight: 1000; font-size: 13px; }

/* Event type chips */
.chip {
  display: inline-flex;
  align-items: center;
  height: 20px;
  margin-top: 6px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.chip.canvas  { background: rgba(52,211,153,0.14); border: 1px solid rgba(52,211,153,0.3); color: #6ee7b7; }
.chip.class   { background: rgba(96,165,250,0.14); border: 1px solid rgba(96,165,250,0.3); color: #93c5fd; }
.chip.meeting { background: rgba(167,139,250,0.14); border: 1px solid rgba(167,139,250,0.3); color: #c4b5fd; }
.chip.oh      { background: rgba(45,212,191,0.14); border: 1px solid rgba(45,212,191,0.3); color: #5eead4; }

.title { font-weight: 1000; font-size: 13px; }
.sub { margin-top: 3px; font-size: 12px; }

/* ── OH divider ── */
.oh-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}
.oh-divider::before,
.oh-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(255,255,255,0.06);
}
.oh-divider-label {
  font-size: 11px;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .cal { grid-template-columns: 1fr; }
  .left { border-right: none; border-bottom: 1px solid rgba(255,255,255,0.06); }
}
</style>
