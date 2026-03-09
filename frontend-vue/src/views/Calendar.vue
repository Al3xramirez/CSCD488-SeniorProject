<script setup>
import { computed, onMounted, ref } from "vue";

const userId = ref(localStorage.getItem("syllabussync_user_id") || "");
const icsUrl = ref(localStorage.getItem("syllabussync_canvas_ics") || "");

const loading = ref(false);
const syncing = ref(false);
const error = ref("");
const status = ref("");

const events = ref([]);

const monthCursor = ref(startOfMonth(new Date()));
const selectedDay = ref(toYmd(new Date()));

const monthLabel = computed(() => {
  const d = monthCursor.value;
  return d.toLocaleString(undefined, { month: "long", year: "numeric" });
});

const days = computed(() => buildMonthGrid(monthCursor.value));

const eventsByDay = computed(() => {
  const map = new Map();
  for (const e of events.value) {
    const start = parseLocalDateTime(e.startAt);
    const key = toYmd(start);
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(e);
  }
  for (const [k, list] of map.entries()) {
    list.sort((a, b) => parseLocalDateTime(a.startAt) - parseLocalDateTime(b.startAt));
    map.set(k, list);
  }
  return map;
});

const selectedEvents = computed(() => eventsByDay.value.get(selectedDay.value) || []);

const validateUserId = () => {
  const v = userId.value.trim();
  if (!v) {
    throw new Error("Enter your userId first.");
  }
  localStorage.setItem("syllabussync_user_id", v);
  userId.value = v;
};

const fetchEvents = async () => {
  error.value = "";
  status.value = "";

  validateUserId();

  loading.value = true;
  try {
    const res = await fetch(`/api/calendar/events?userId=${encodeURIComponent(userId.value)}`, {
      credentials: "include",
    });

    if (!res.ok) {
      const msg = await res.text();
      throw new Error(msg || "Failed to load events");
    }

    events.value = await res.json();
    status.value = `Loaded ${events.value.length} events.`;
  } catch (e) {
    error.value = e?.message || "Failed to load events";
  } finally {
    loading.value = false;
  }
};

const subscribeCanvas = async () => {
  error.value = "";
  status.value = "";

  validateUserId();

  const url = icsUrl.value.trim();
  if (!url) {
    error.value = "Paste your Canvas iCal (.ics) URL.";
    return;
  }
  localStorage.setItem("syllabussync_canvas_ics", url);
  icsUrl.value = url;

  syncing.value = true;
  try {
    const res = await fetch("/api/calendar/canvas/subscribe", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: userId.value, icsUrl: url }),
    });

    if (!res.ok) {
      const msg = await res.text();
      throw new Error(msg || "Subscribe failed");
    }

    status.value = "Canvas feed connected. Syncing events...";
    await fetchEvents();
  } catch (e) {
    error.value = e?.message || "Subscribe failed";
  } finally {
    syncing.value = false;
  }
};

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
  if (userId.value.trim()) {
    await fetchEvents();
  }
});

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
  const datePart = parts[0] || "";
  const timePart = parts[1] || "00:00:00";
  const [y, m, d] = datePart.split("-").map((v) => Number(v));
  const [hh, mm, ssRaw] = timePart.split(":");
  const ss = (ssRaw || "0").split(".")[0];
  return new Date(y, (m || 1) - 1, d || 1, Number(hh || 0), Number(mm || 0), Number(ss || 0));
}

function buildMonthGrid(monthStart) {
  const year = monthStart.getFullYear();
  const month = monthStart.getMonth();
  const first = new Date(year, month, 1);
  const firstDow = (first.getDay() + 6) % 7; // Monday=0
  const gridStart = new Date(year, month, 1 - firstDow);

  const out = [];
  for (let i = 0; i < 42; i++) {
    const d = new Date(gridStart);
    d.setDate(gridStart.getDate() + i);
    out.push(d);
  }
  return out;
}

function fmtTime(s) {
  const d = parseLocalDateTime(s);
  if (Number.isNaN(d.getTime())) return "";
  return d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}
</script>

<template>
  <div class="wrap">
    <div class="topbar">
      <div>
        <h1>Calendar</h1>
        <p class="muted">Canvas events cached from your .ics feed.</p>
      </div>

      <div class="top-actions">
        <button class="btn ghost" @click="goToday">Today</button>
        <button class="btn ghost" @click="prevMonth">Prev</button>
        <button class="btn ghost" @click="nextMonth">Next</button>
      </div>
    </div>

    <div class="panel">
      <div class="subscribe">
        <div class="row">
          <div class="field">
            <label>User ID</label>
            <input v-model="userId" placeholder="UserID (20 chars)" />
          </div>
          <div class="field grow">
            <label>Canvas iCal URL</label>
            <input v-model="icsUrl" placeholder="https://.../calendar.ics" />
          </div>
          <button class="btn" :disabled="syncing" @click="subscribeCanvas">
            {{ syncing ? "Syncing..." : "Connect Canvas" }}
          </button>
          <button class="btn ghost" :disabled="loading" @click="fetchEvents">
            {{ loading ? "Loading..." : "Refresh" }}
          </button>
        </div>
        <div v-if="status" class="note ok">{{ status }}</div>
        <div v-if="error" class="note err">{{ error }}</div>
      </div>

      <div class="cal">
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
                other: d.getMonth() !== monthCursor.getMonth(),
                selected: toYmd(d) === selectedDay,
                today: toYmd(d) === toYmd(new Date()),
              }"
              @click="selectedDay = toYmd(d)"
            >
              <div class="num">{{ d.getDate() }}</div>
              <div class="count" v-if="eventsByDay.get(toYmd(d))">
                {{ eventsByDay.get(toYmd(d)).length }}
              </div>
            </button>
          </div>
        </div>

        <div class="right">
          <div class="agenda-head">
            <div class="agenda-title">{{ selectedDay }}</div>
            <div class="agenda-sub muted">
              {{ selectedEvents.length }} event{{ selectedEvents.length === 1 ? '' : 's' }}
            </div>
          </div>

          <div class="agenda">
            <div v-if="selectedEvents.length === 0" class="empty">
              No events for this day.
            </div>

            <div v-for="e in selectedEvents" :key="`${e.subscriptionId}-${e.icalUid}-${e.recurrenceId || ''}`" class="evt" :class="{ cancelled: e.isCancelled }">
              <div class="time">
                <div class="t">{{ e.allDay ? "All day" : fmtTime(e.startAt) }}</div>
                <div class="chip" :class="e.provider">{{ e.provider }}</div>
              </div>
              <div class="meta">
                <div class="title">{{ e.summary || "(no title)" }}</div>
                <div class="sub muted" v-if="e.location">{{ e.location }}</div>
                <div class="sub muted" v-if="e.isCancelled">Cancelled</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  color: #e5e7eb;
}

.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

h1 {
  margin: 0;
  font-size: 22px;
}

.muted {
  margin: 6px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

.top-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.panel {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
  overflow: hidden;
}

.subscribe {
  padding: 14px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  background: rgba(15,23,42,0.55);
}

.row {
  display: flex;
  gap: 12px;
  align-items: end;
  flex-wrap: wrap;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 200px;
}

.field.grow {
  flex: 1;
  min-width: 280px;
}

label {
  font-size: 12px;
  font-weight: 900;
  color: #cbd5e1;
}

input {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 14px;
  padding: 10px 12px;
  color: #e5e7eb;
  outline: none;
}

input:focus {
  border-color: rgba(96,165,250,0.7);
  box-shadow: 0 0 0 3px rgba(37,99,235,0.15);
}

.btn {
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 900;
  padding: 10px 12px;
  border-radius: 14px;
  cursor: pointer;
}

.btn:hover { background: #1d4ed8; }

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.10);
}

.btn.ghost:hover { background: rgba(255,255,255,0.09); }

.note {
  margin-top: 10px;
  font-size: 13px;
  font-weight: 800;
}
.note.ok { color: #34d399; }
.note.err { color: #fb7185; }

.cal {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  min-height: 520px;
}

.left {
  padding: 14px;
  border-right: 1px solid rgba(255,255,255,0.06);
}

.month-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.month {
  font-weight: 1000;
  letter-spacing: 0.2px;
}

.dow {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.dowcell {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.6px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.cell {
  position: relative;
  min-height: 74px;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
  color: #e5e7eb;
  padding: 10px;
  text-align: left;
  cursor: pointer;
}

.cell:hover {
  background: rgba(255,255,255,0.05);
  transform: translateY(-1px);
}

.cell.other {
  opacity: 0.55;
}

.cell.selected {
  background: rgba(37,99,235,0.22);
  border-color: rgba(37,99,235,0.45);
}

.cell.today {
  box-shadow: inset 0 0 0 2px rgba(96,165,250,0.45);
}

.num {
  font-weight: 1000;
}

.count {
  position: absolute;
  right: 10px;
  bottom: 10px;
  font-size: 12px;
  font-weight: 1000;
  padding: 6px 9px;
  border-radius: 999px;
  background: rgba(37,99,235,0.25);
  border: 1px solid rgba(37,99,235,0.35);
  color: #bfdbfe;
}

.right {
  padding: 14px;
}

.agenda-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.agenda-title {
  font-weight: 1000;
}

.agenda {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

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
  grid-template-columns: 120px 1fr;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
}

.evt.cancelled {
  opacity: 0.6;
}

.time .t {
  font-weight: 1000;
}

.chip {
  display: inline-flex;
  align-items: center;
  height: 22px;
  margin-top: 8px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 1000;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.10);
  color: #cbd5e1;
}

.chip.canvas {
  background: rgba(34,197,94,0.14);
  border-color: rgba(34,197,94,0.22);
  color: #86efac;
}

.title {
  font-weight: 1000;
}

.sub {
  margin-top: 4px;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .cal {
    grid-template-columns: 1fr;
  }
  .left {
    border-right: none;
    border-bottom: 1px solid rgba(255,255,255,0.06);
  }
}
</style>
