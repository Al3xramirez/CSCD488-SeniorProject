<script setup>
import { computed, inject, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useMeetingReminders } from "../composables/useMeetingReminders";

const me = inject("me", null);
const router = useRouter();

const role = computed(() => {
  const r = me?.value?.role;
  return (r || "STUDENT").toString().trim().toUpperCase();
});

// ── Calendar events ────────────────────────────────────────────────
const loadingEvents = ref(false);
const events = ref([]);

const hasEvents = computed(() => events.value.length > 0);

const { now, reminderLabel, upcomingReminder } = useMeetingReminders(events);

const upcomingItems = computed(() => {
  // End of current week Sunday 23:59:59 — uses reactive `now` so list updates live
  const endOfWeek = new Date(now.value);
  endOfWeek.setDate(now.value.getDate() + (7 - now.value.getDay()));
  endOfWeek.setHours(23, 59, 59, 999);

  return events.value
    .filter(e => {
      if (e.isCancelled) return false;
      const start = parseLocalDateTime(e.startAt);
      return start >= now.value && start <= endOfWeek;
    })
    .sort((a, b) => parseLocalDateTime(a.startAt) - parseLocalDateTime(b.startAt))
    .slice(0, 5)
    .map(e => {
      const start = parseLocalDateTime(e.startAt);
      const dayAbbr = start.toLocaleDateString(undefined, { weekday: "short" });
      const dateStr = start.toLocaleDateString(undefined, { month: "short", day: "numeric" });
      const tz = localStorage.getItem("syllabussync_timezone") || undefined;
      const timeStr = e.allDay
        ? "All day"
        : start.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit", timeZone: tz });
      return {
        label: dayAbbr,
        title: e.summary || "(No title)",
        sub: `${dateStr} · ${timeStr}`,
        startAt: e.startAt,
        allDay: e.allDay,
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

function parseLocalDateTime(s) {
  if (!s) return new Date(NaN);
  const [datePart, timePart = "00:00:00"] = String(s).split("T");
  const [y, m, d] = datePart.split("-").map(Number);
  const [hh, mm, ssRaw] = timePart.split(":");
  const ss = (ssRaw || "0").split(".")[0];
  return new Date(y, m - 1, d, Number(hh || 0), Number(mm || 0), Number(ss || 0));
}

onMounted(fetchEvents);
</script>
<template>
  <div class="grid">
    <!-- Left: Syllabus Overview -->
    <section class="card big">
      <div class="card-header">
        <div>
          <h2>Syllabus Overview</h2>
          <p class="muted">
            Content organized from professor-uploaded syllabus PDF.
          </p>
        </div>
      </div>

      <div class="content">
        <div class="placeholder">
          <div class="line w60" />
          <div class="line w85" />
          <div class="line w70" />
          <div class="line w90" />
          <div class="line w55" />
        </div>

        <div class="mini-grid">
          <div class="mini">
            <div class="mini-title">Course</div>
            <div class="mini-value">—</div>
          </div>
          <div class="mini">
            <div class="mini-title">Professor</div>
            <div class="mini-value">—</div>
          </div>
          <div class="mini">
            <div class="mini-title">Key Policies</div>
            <div class="mini-value">—</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Right: Upcoming This Week -->
    <section class="card side">
      <div class="card-header">
        <div>
          <h2>Upcoming This Week</h2>
        </div>
        <button class="btn ghost" @click="router.push('/app/calendar')">Import Canvas .ics</button>
      </div>

      <div v-if="upcomingReminder" class="reminder-banner">
        <span class="reminder-dot" />
        <span class="reminder-text">
          <strong>{{ upcomingReminder.event.summary }}</strong> — {{ upcomingReminder.label }}
        </span>
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
          <div v-if="!row.allDay && reminderLabel(row.startAt)" class="countdown-chip">
            {{ reminderLabel(row.startAt) }}
          </div>
        </div>
      </div>

      <div class="list" v-else>
        <div class="empty-note">
          {{ hasEvents ? "Nothing due this week." : "No Canvas feed connected yet." }}
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
}

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
  background: rgba(255,255,255,0.06);
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

.reminder-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(234, 179, 8, 0.12);
  border: 1px solid rgba(234, 179, 8, 0.28);
}

.reminder-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #eab308;
  box-shadow: 0 0 6px rgba(234, 179, 8, 0.7);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.reminder-text {
  font-size: 13px;
  color: #fde68a;
}

.countdown-chip {
  margin-left: auto;
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  background: rgba(234, 179, 8, 0.14);
  border: 1px solid rgba(234, 179, 8, 0.28);
  color: #fde68a;
  white-space: nowrap;
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

@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .mini-grid {
    grid-template-columns: 1fr;
  }
}
</style>
