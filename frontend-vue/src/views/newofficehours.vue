<script setup>
import { ref, reactive, computed, watch, inject, onMounted } from "vue";
import TimePicker from "../components/TimePicker.vue";

// ── Shared state from layout ──────────────────────────────────────────────
const classes = inject("classes", ref([]));
const me      = inject("me", null);

// ── Data ──────────────────────────────────────────────────────────────────
// officeHours: [{ userId, firstName, lastName, role, schedule:[{dayOfWeek,startTime,endTime}] }]
const officeHours     = ref([]);
const ohLoading       = ref(false);

// classMeetings: all meeting-time entries across enrolled classes (specific dates)
const classMeetings   = ref([]);

// myMeetings: personal meetings (specific dates + recipientId set)
const myMeetings      = ref([]);

const currentUserEmail = ref("");

// ── Week navigation ───────────────────────────────────────────────────────
const weekStart = ref(getMonday(new Date()));

const weekDays = computed(() =>
  Array.from({ length: 5 }, (_, i) => {
    const d = new Date(weekStart.value);
    d.setDate(d.getDate() + i);
    return {
      dateStr: ymd(d),
      label:   d.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" }),
    };
  })
);

const weekLabel = computed(() => {
  const s = weekDays.value[0].dateStr;
  const e = weekDays.value[4].dateStr;
  const fmt = str => new Date(str + "T00:00:00").toLocaleDateString("en-US", { month: "short", day: "numeric" });
  return `${fmt(s)} – ${fmt(e)}, ${e.slice(0, 4)}`;
});

function prevWeek() { const d = new Date(weekStart.value); d.setDate(d.getDate() - 7); weekStart.value = d; }
function nextWeek() { const d = new Date(weekStart.value); d.setDate(d.getDate() + 7); weekStart.value = d; }
function goThisWeek() { weekStart.value = getMonday(new Date()); }

// ── Calendar geometry ─────────────────────────────────────────────────────
const CAL_START    = 7;   // 7 am
const PX_PER_HOUR  = 64;
const calHours     = Array.from({ length: 14 }, (_, i) => i + CAL_START); // 7 am – 8 pm

function timeToY(t) {
  if (!t) return 0;
  const [h, m] = t.split(":").map(Number);
  return (h + m / 60 - CAL_START) * PX_PER_HOUR;
}

function timeToDuration(s, e) {
  if (!s || !e) return PX_PER_HOUR;
  const [sh, sm] = s.split(":").map(Number);
  const [eh, em] = e.split(":").map(Number);
  return Math.max(((eh + em / 60) - (sh + sm / 60)) * PX_PER_HOUR, 24);
}

function calEventStyle(ev) {
  const totalLanes = ev.totalLanes || 1;
  const lane       = ev.lane       || 0;
  const pct        = 100 / totalLanes;
  return {
    top:    `${timeToY(ev.startTime)}px`,
    height: `${timeToDuration(ev.startTime, ev.endTime)}px`,
    left:   `calc(2px + ${lane * pct}%)`,
    width:  `calc(${pct}% - 4px)`,
    right:  "auto",
  };
}

// Assigns non-overlapping lanes to events so they sit side-by-side instead of stacking.
function layoutEvents(events) {
  if (!events.length) return [];
  const sorted = [...events].sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
  const laneEnds = []; // laneEnds[i] = endTime of last event placed in lane i

  const withLanes = sorted.map(ev => {
    const laneIdx = laneEnds.findIndex(end => !end || (ev.startTime ?? "") >= end);
    const lane = laneIdx === -1 ? laneEnds.push(null) - 1 : laneIdx;
    laneEnds[lane] = ev.endTime ?? "23:59";
    return { ...ev, lane };
  });

  // Second pass: each event's totalLanes = max lanes among its overlapping group
  return withLanes.map(ev => {
    const overlapping = withLanes.filter(o =>
      o !== ev &&
      (o.startTime ?? "") < (ev.endTime ?? "23:59") &&
      (o.endTime   ?? "23:59") > (ev.startTime ?? "")
    );
    const totalLanes = overlapping.length ? Math.max(ev.lane, ...overlapping.map(o => o.lane)) + 1 : 1;
    return { ...ev, totalLanes };
  });
}

// ── Events per day ────────────────────────────────────────────────────────
const DOW_TO_3 = { MONDAY:"MON", TUESDAY:"TUE", WEDNESDAY:"WED", THURSDAY:"THU", FRIDAY:"FRI" };

function eventsForDay(dateStr) {
  const dow = new Date(dateStr + "T00:00:00").toLocaleDateString("en-US", { weekday: "long" }).toUpperCase();
  const key = DOW_TO_3[dow];
  const out = [];

  // Class meeting times (specific date)
  for (const m of classMeetings.value) {
    if (m.meetingDate === dateStr)
      out.push({ type: "class", startTime: m.startTime, endTime: m.endTime, label: m.classCode || "Class" });
  }

  // Office hours blocks (recurring, matched by day of week)
  for (const person of officeHours.value) {
    const dayExceptions = (person.exceptions ?? []).filter(e => e.exceptionDate === dateStr);
    for (const b of person.schedule ?? []) {
      if (b.dayOfWeek !== key) continue;
      const base = {
        type:       "oh",
        personName: `${person.firstName} ${person.lastName}`,
        userId:     person.userId,
        role:       person.role,
        date:       dateStr,
      };
      const unavailEx = dayExceptions.find(e =>
        e.unavailable && (!e.startTime || (b.startTime < e.endTime && b.endTime > e.startTime))
      );
      const modifiedEx = !unavailEx && dayExceptions.find(e =>
        !e.unavailable && e.startTime && (b.startTime < e.endTime && b.endTime > e.startTime)
      );
      if (unavailEx) {
        out.push({ ...base, startTime: b.startTime, endTime: b.endTime, unavailable: true, unavailableNote: unavailEx.note });
      } else if (modifiedEx) {
        out.push({ ...base, startTime: modifiedEx.startTime, endTime: modifiedEx.endTime });
      } else {
        out.push({ ...base, startTime: b.startTime, endTime: b.endTime });
      }
    }
  }

  // My personal meetings (specific date)
  for (const m of myMeetings.value) {
    if (m.meetingDate === dateStr && m.recipientId)
      out.push({ type: "personal", startTime: m.startTime, endTime: m.endTime, status: m.status, label: "My Meeting" });
  }

  return out.sort((a, b) => (a.startTime ?? "").localeCompare(b.startTime ?? ""));
}

// ── Data fetching ─────────────────────────────────────────────────────────
async function fetchOHAndMeetings(courseList) {
  if (!courseList.length) { officeHours.value = []; classMeetings.value = []; return; }
  ohLoading.value = true;

  const seen = new Set();
  const allOH = [];
  const allMtg = [];

  await Promise.allSettled(courseList.map(async c => {
    try {
      const [ohRes, mtgRes] = await Promise.all([
        fetch(`/api/office-hours/class/${c.classCode}`,             { credentials: "include" }),
        fetch(`/api/classes/${c.classCode}/class-meetings`,          { credentials: "include" }),
      ]);
      if (ohRes.ok) {
        for (const p of await ohRes.json()) {
          // Store classCode/quarter/year so we know which class each person belongs to
          if (!seen.has(p.userId)) {
            seen.add(p.userId);
            allOH.push({ ...p, classCode: c.classCode, quarter: c.quarter, year: c.year });
          }
        }
      }
      if (mtgRes.ok) allMtg.push(...await mtgRes.json());
    } catch {}
  }));

  officeHours.value   = allOH;
  classMeetings.value = allMtg;
  ohLoading.value     = false;
}

async function fetchMyMeetings() {
  try {
    const res = await fetch("/api/meetings/my", { credentials: "include" });
    if (res.ok) myMeetings.value = await res.json();
  } catch {}
}

// Re-fetch when shared classes list is ready
watch(classes, val => { if (val.length) fetchOHAndMeetings(val); }, { immediate: true });

// ── Request meeting modal ─────────────────────────────────────────────────
// Using reactive (not ref) so property mutations always trigger cleanly
const modal = reactive({ show: false, mode: "oh", personName: "", userId: "", classCode: "", quarter: "", year: "", date: "", startTime: "", endTime: "", notes: "" });
const modalLoading = ref(false);
const modalError   = ref("");
const emailCache   = {};

// Open from an office-hours block click (pre-filled)
function openModal(ev) {
  const person = officeHours.value.find(p => p.userId === ev.userId);
  modalError.value = "";
  Object.assign(modal, {
    show: true, mode: "oh",
    personName: ev.personName, userId: ev.userId,
    classCode: person?.classCode || classes.value[0]?.classCode || "",
    quarter:   person?.quarter   || classes.value[0]?.quarter   || "",
    year:      person?.year      || classes.value[0]?.year      || "",
    date: ev.date, startTime: ev.startTime, endTime: ev.endTime, notes: "",
  });
}

// Open free-form (header button)
function openCustomModal() {
  modalError.value = "";
  Object.assign(modal, { show: true, mode: "custom", personName: "", userId: "", classCode: "", quarter: "", year: "", date: "", startTime: "12:00", endTime: "13:00", notes: "" });
}

// Called when the person dropdown changes in custom mode
function onSelectPerson(userId) {
  const person = officeHours.value.find(p => p.userId === userId);
  if (!person) return;
  modal.userId     = person.userId;
  modal.personName = `${person.firstName} ${person.lastName}`;
  modal.classCode  = person.classCode || "";
  modal.quarter    = person.quarter   || "";
  modal.year       = person.year      || "";
}

// Returns true if the given time window overlaps any class meeting on that date
function hasInstructionConflict(date, startTime, endTime) {
  return classMeetings.value.some(m =>
    m.meetingDate === date &&
    startTime < m.endTime &&
    endTime   > m.startTime
  );
}

async function submitRequest() {
  modalLoading.value = true;
  modalError.value   = "";
  try {
    // ── Instruction-time guard ───────────────────────────────────────────
    if (hasInstructionConflict(modal.date, modal.startTime, modal.endTime)) {
      modalError.value = "The professor is in instruction time during this slot. Please choose a different time.";
      return;
    }

    const requesterId = me?.value?.email || currentUserEmail.value;

    // Resolve the class for this meeting
    const cls = classes.value.find(c => c.classCode === modal.classCode) || classes.value[0];
    if (!cls) throw new Error("No class found");

    let recipientEmail = emailCache[modal.userId];
    if (!recipientEmail) {
      const r = await fetch(`/api/users/${modal.userId}`, { credentials: "include" });
      if (!r.ok) throw new Error("Could not find recipient");
      recipientEmail = (await r.json()).email;
      emailCache[modal.userId] = recipientEmail;
    }

    const res = await fetch("/api/meetings/create-meeting", {
      method:  "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        classCode:   cls.classCode,
        quarter:     cls.quarter,
        year:        cls.year,
        requesterId: requesterId,
        recipientId: recipientEmail,
        meetingDate: modal.date,
        startTime:   modal.startTime,
        endTime:     modal.endTime,
        status:      "PENDING",
        notes:       modal.notes,
      }),
    });
    if (!res.ok) throw new Error((await res.text()) || "Failed to create meeting");
    modal.show = false;
    fetchMyMeetings();
  } catch (e) {
    modalError.value = e.message || "Failed to send request";
  } finally {
    modalLoading.value = false;
  }
}

onMounted(async () => {
  try {
    const r = await fetch("/api/auth/current-user", { credentials: "include" });
    if (r.ok) currentUserEmail.value = (await r.json()).currentUser || "";
  } catch {}
  fetchMyMeetings();
});

// ── Utilities ─────────────────────────────────────────────────────────────
function getMonday(d) {
  const c = new Date(d); c.setHours(0, 0, 0, 0);
  const diff = c.getDay() === 0 ? -6 : 1 - c.getDay();
  c.setDate(c.getDate() + diff);
  return c;
}

function ymd(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function fmtTime(t) {
  if (!t) return "";
  const [hh, mm] = t.split(":").map(Number);
  const ampm = hh < 12 ? "AM" : "PM";
  return `${hh % 12 || 12}:${String(mm).padStart(2, "0")} ${ampm}`;
}
</script>

<template>
  <div class="wrap">

    <!-- Header + week nav -->
    <div class="page-header">
      <div>
        <h1>Office Hours</h1>
        <p class="muted">Click a green block to request during office hours, or use the button to request at any time.</p>
      </div>
      <div class="header-right">
        <button class="btn" @click="openCustomModal">+ Request Meeting</button>
        <div class="week-nav">
          <button class="btn-ghost" @click="prevWeek">‹</button>
          <button class="btn-ghost today-btn" @click="goThisWeek">This week</button>
          <span class="week-label">{{ weekLabel }}</span>
          <button class="btn-ghost" @click="nextWeek">›</button>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!classes.length" class="empty-card">
      You are not enrolled in any classes yet.
    </div>

    <!-- Loading -->
    <div v-else-if="ohLoading" class="empty-card">Loading schedule…</div>

    <!-- No office hours -->
    <div v-else-if="!officeHours.length" class="empty-card">
      No office hours have been posted for your classes yet.
    </div>

    <!-- Week calendar -->
    <template v-else>
      <div class="card">
        <div class="cal-wrap">

          <!-- Time labels -->
          <div class="cal-time-col">
            <div class="cal-hdr-spacer"></div>
            <div v-for="h in calHours" :key="h" class="cal-hour-label">
              {{ h === 12 ? "12 pm" : h < 12 ? `${h} am` : `${h - 12} pm` }}
            </div>
          </div>

          <!-- Day columns -->
          <div class="cal-days">
            <div
              v-for="day in weekDays"
              :key="day.dateStr"
              class="cal-day-col"
              :class="{ today: day.dateStr === ymd(new Date()) }"
            >
              <div class="cal-day-hdr">{{ day.label }}</div>
              <div class="cal-day-body">
                <div v-for="h in calHours" :key="h" class="cal-hour-line"></div>
                <template v-for="ev in layoutEvents(eventsForDay(day.dateStr))" :key="`${day.dateStr}-${ev.type}-${ev.startTime}-${ev.lane}`">
                  <!-- Office hours: unavailable -->
                  <div
                    v-if="ev.type === 'oh' && ev.unavailable"
                    class="cal-event cal-event-oh-unavailable"
                    :style="calEventStyle(ev)"
                    :title="ev.unavailableNote || 'Unavailable'"
                  >
                    <span class="cal-ev-time">{{ fmtTime(ev.startTime) }}</span>
                    <span class="cal-ev-name">{{ ev.personName }}</span>
                    <span class="cal-ev-unavail-label">Unavailable</span>
                  </div>

                  <!-- Office hours: clickable -->
                  <button
                    v-else-if="ev.type === 'oh'"
                    class="cal-event cal-event-oh"
                    :style="calEventStyle(ev)"
                    @click="openModal(ev)"
                  >
                    <span class="cal-ev-time">{{ fmtTime(ev.startTime) }}</span>
                    <span class="cal-ev-name">{{ ev.personName }}</span>
                    <span class="cal-ev-role">{{ ev.role === 'PROFESSOR' ? 'Prof' : 'TA' }}</span>
                  </button>

                  <!-- Class meeting time -->
                  <div v-else-if="ev.type === 'class'" class="cal-event cal-event-class" :style="calEventStyle(ev)">
                    <span class="cal-ev-time">{{ fmtTime(ev.startTime) }}</span>
                    <span class="cal-ev-name">{{ ev.label }}</span>
                  </div>

                  <!-- Personal meeting -->
                  <div v-else-if="ev.type === 'personal'" class="cal-event cal-event-personal" :style="calEventStyle(ev)">
                    <span class="cal-ev-time">{{ fmtTime(ev.startTime) }}</span>
                    <span class="cal-ev-name">My Meeting</span>
                    <span class="cal-ev-status" :class="`status-${(ev.status || '').toLowerCase()}`">{{ ev.status }}</span>
                  </div>
                </template>
              </div>
            </div>
          </div>

        </div>

        <!-- Legend -->
        <div class="legend">
          <span class="leg leg-oh">Office Hours — click to request</span>
          <span class="leg leg-oh-unavailable">Unavailable</span>
          <span class="leg leg-class">Class</span>
          <span class="leg leg-personal">My Meetings</span>
        </div>
      </div>
    </template>
  </div>

  <!-- Request meeting modal -->
  <teleport to="body">
    <div v-if="modal.show" class="overlay" @click.self="() => { modal.show = false; }">
      <div class="modal">
        <div class="modal-hdr">
          <h3>Request a Meeting</h3>
          <button class="close-btn" @click="() => { modal.show = false; }">✕</button>
        </div>
        <div class="modal-body">
          <div class="mfield">
            <label>With</label>
            <!-- Custom mode: pick any instructor or TA from enrolled classes -->
            <select
              v-if="modal.mode === 'custom'"
              class="input"
              :value="modal.userId"
              @change="onSelectPerson($event.target.value)"
            >
              <option value="">Select a person…</option>
              <option v-for="p in officeHours" :key="p.userId" :value="p.userId">
                {{ p.firstName }} {{ p.lastName }} ({{ p.role === 'PROFESSOR' ? 'Prof' : 'TA' }})
              </option>
            </select>
            <!-- OH block mode: person already known -->
            <span v-else class="mval">{{ modal.personName }}</span>
          </div>
          <div class="mfield">
            <label>Date</label>
            <input v-model="modal.date" type="date" class="input" />
          </div>
          <div class="mfield mfield-col">
            <label>Start</label>
            <TimePicker v-model="modal.startTime" />
          </div>
          <div class="mfield mfield-col">
            <label>End</label>
            <TimePicker v-model="modal.endTime" />
          </div>
          <div class="mfield mfield-col">
            <label>Notes</label>
            <textarea v-model="modal.notes" class="input" rows="3" placeholder="Optional agenda or questions" />
          </div>
          <div v-if="modalError" class="req-error">{{ modalError }}</div>
          <div class="modal-actions">
            <button class="btn-ghost" @click="() => { modal.show = false; }">Cancel</button>
            <button
              class="btn"
              :disabled="modalLoading || !modal.date || !modal.startTime || !modal.endTime || (modal.mode === 'custom' && !modal.userId)"
              @click="submitRequest"
            >{{ modalLoading ? "Sending…" : "Send Request" }}</button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.wrap {
  display: flex;
  flex-direction: column;
  gap: 18px;
  color: #e5e7eb;
}

/* ── Header ── */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

h1 { margin: 0; font-size: 20px; color: #f3f4f6; }

.muted { margin: 4px 0 0; font-size: 13px; color: #9ca3af; }

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.week-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.week-label {
  font-size: 13px;
  font-weight: 600;
  color: #e5e7eb;
  min-width: 160px;
  text-align: center;
}

.btn-ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  color: #e5e7eb;
  border-radius: 8px;
  padding: 5px 12px;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
}
.btn-ghost:hover { background: rgba(255,255,255,0.12); }

.today-btn { font-size: 12px; font-weight: 700; }

/* ── Card ── */
.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ── Empty / loading ── */
.empty-card {
  padding: 24px;
  border-radius: 18px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.08);
  color: #6b7280;
  font-size: 13px;
  text-align: center;
}

/* ── Week calendar ── */
.cal-wrap {
  display: flex;
  overflow-x: auto;
}

.cal-time-col {
  flex-shrink: 0;
  width: 48px;
  display: flex;
  flex-direction: column;
}

.cal-hdr-spacer { height: 36px; flex-shrink: 0; }

.cal-hour-label {
  height: 64px;
  font-size: 11px;
  color: #6b7280;
  text-align: right;
  padding-right: 8px;
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  transform: translateY(-7px);
}

.cal-days {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  min-width: 420px;
}

.cal-day-col {
  border-left: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
}

.cal-day-col.today .cal-day-hdr {
  color: #60a5fa;
  font-weight: 800;
}

.cal-day-hdr {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}

.cal-day-body { position: relative; }

.cal-hour-line {
  height: 64px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

/* ── Events ── */
.cal-event {
  position: absolute;
  left: 2px;
  right: 2px;
  border-radius: 6px;
  padding: 4px 6px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
  z-index: 1;
  border: none;
  text-align: left;
}

.cal-event-oh {
  background: rgba(74,222,128,0.18);
  border: 1px solid rgba(74,222,128,0.45) !important;
  color: #4ade80;
  cursor: pointer;
}
.cal-event-oh:hover { background: rgba(74,222,128,0.30); }

.cal-event-oh-unavailable {
  background: rgba(239,68,68,0.12);
  border: 1px solid rgba(239,68,68,0.35) !important;
  color: #f87171;
  cursor: default;
  opacity: 0.75;
}

.cal-ev-unavail-label {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  opacity: 0.85;
}

.cal-event-class {
  background: rgba(37,99,235,0.22);
  border: 1px solid rgba(37,99,235,0.45) !important;
  color: #93c5fd;
  cursor: default;
}

.cal-event-personal {
  background: rgba(139,92,246,0.18);
  border: 1px solid rgba(139,92,246,0.4) !important;
  color: #c4b5fd;
  cursor: default;
}

.cal-ev-time  { font-size: 10px; opacity: 0.75; }
.cal-ev-name  { font-size: 11px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cal-ev-role  { font-size: 10px; opacity: 0.65; }

.cal-ev-status {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: 3px;
  align-self: flex-start;
}
.status-confirmed { background: rgba(74,222,128,0.2); color: #4ade80; }
.status-pending   { background: rgba(251,191,36,0.2);  color: #fbbf24; }
.status-cancelled { background: rgba(248,113,113,0.2); color: #f87171; }

/* ── Legend ── */
.legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  padding-top: 6px;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.leg {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
}
.leg::before {
  content: '';
  width: 12px;
  height: 12px;
  border-radius: 3px;
  flex-shrink: 0;
}
.leg-oh::before             { background: rgba(74,222,128,0.35); border: 1px solid rgba(74,222,128,0.6); }
.leg-oh-unavailable::before { background: rgba(239,68,68,0.2);   border: 1px solid rgba(239,68,68,0.5);  }
.leg-class::before          { background: rgba(37,99,235,0.35);  border: 1px solid rgba(37,99,235,0.6);  }
.leg-personal::before       { background: rgba(139,92,246,0.3);  border: 1px solid rgba(139,92,246,0.5); }

/* ── Request modal ── */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #1a1d2e;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 18px;
  width: min(440px, 92vw);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-hdr {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
}
.modal-hdr h3 { margin: 0; font-size: 15px; color: #f3f4f6; }

.close-btn {
  background: none;
  border: none;
  color: #6b7280;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
}
.close-btn:hover { color: #e5e7eb; }

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mfield { display: flex; align-items: center; gap: 12px; }
.mfield label { width: 48px; font-size: 12px; font-weight: 600; color: #9ca3af; flex-shrink: 0; }

.mfield-col { flex-direction: column; align-items: flex-start; }
.mfield-col label { width: auto; }

.mval { font-size: 13px; font-weight: 600; color: #e5e7eb; }

.input {
  flex: 1;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  color: #e5e7eb;
  font-size: 13px;
  padding: 7px 10px;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}
.input:focus { border-color: rgba(37,99,235,0.5); }

.req-error {
  font-size: 12px;
  color: #f87171;
  background: rgba(248,113,113,0.08);
  border: 1px solid rgba(248,113,113,0.2);
  border-radius: 8px;
  padding: 8px 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.btn {
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
}
.btn:hover { background: #1d4ed8; }
.btn:disabled { background: #6b7280; cursor: not-allowed; opacity: 0.6; }
</style>
