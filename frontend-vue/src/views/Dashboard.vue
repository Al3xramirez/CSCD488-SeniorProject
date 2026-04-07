<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

// ------------- Setup & Context --------------------------
const me = inject("me", null);
const router = useRouter();

// ------------- User Info --------------------------
const role = computed(() => {
  const r = me?.value?.role;
  return (r || "STUDENT").toString().trim().toUpperCase();
});

// ------------- Classes + Syllabus UI --------------------------

const classes = ref([]);
const loadingSyllabus = ref(false);
const syllabusError = ref("");
const syllabusByKey = ref({});
const selectedClassKey = ref("");
const classMenuOpen = ref(false);
const classSelectEl = ref(null);

function classKey(c) {
  return `${c?.classCode || ""}-${c?.quarter || ""}-${c?.year || ""}`;
}

const selectedClass = computed(() => classes.value.find(c => classKey(c) === selectedClassKey.value) || null);

const selectedSyllabus = computed(() => {
  const key = selectedClassKey.value;
  return key ? syllabusByKey.value?.[key] ?? null : null;
});

async function safeErrorMessage(res) {

  try {
    const text = await res.text();
    return text || "";
  } catch {
    return "";
  }
}

// Fetch user's classes and their syllabi, then determine which syllabus to show based on selected class

async function fetchClassesAndSyllabi() {

  loadingSyllabus.value = true;
  syllabusError.value = "";

  classMenuOpen.value = false;

  try {
    const res = await fetch("/api/classes/mine", { credentials: "include" });
    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to load classes (${res.status})`);
    }

    const data = await res.json();
    classes.value = Array.isArray(data) ? data : [];

    if (!selectedClassKey.value && classes.value.length) {
      selectedClassKey.value = classKey(classes.value[0]);
    }

    const results = {};
    await Promise.all(
      classes.value.map(async c => {
        const params = new URLSearchParams({
          classCode: c.classCode,
          quarter: c.quarter,
          year: c.year,
        });

        const r = await fetch(`/api/syllabus?${params.toString()}`, { credentials: "include" });
        if (r.ok) {
          results[classKey(c)] = await r.json();
        } else {
          results[classKey(c)] = null;
        }
      })
    );

    syllabusByKey.value = results;

    if (classes.value.length) {
      const firstWithSyllabus = classes.value.map(c => classKey(c)).find(k => results[k]);
      if (firstWithSyllabus && !results[selectedClassKey.value]) {
        selectedClassKey.value = firstWithSyllabus;
      }
    }
  } catch (e) {
    syllabusError.value = e?.message || "Failed to load syllabus";
  } finally {
    loadingSyllabus.value = false;
  }
}

// ------------- Calendar events --------------------------
const loadingEvents = ref(false);
const events = ref([]);

const hasEvents = computed(() => events.value.length > 0);

const upcomingItems = computed(() => {
  const now = new Date();
  // End of current week Sunday 23:59:59
  const endOfWeek = new Date(now);
  endOfWeek.setDate(now.getDate() + (7 - now.getDay()));
  endOfWeek.setHours(23, 59, 59, 999);

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

function parseLocalDateTime(s) {
  if (!s) return new Date(NaN);
  const [datePart, timePart = "00:00:00"] = String(s).split("T");
  const [y, m, d] = datePart.split("-").map(Number);
  const [hh, mm, ssRaw] = timePart.split(":");
  const ss = (ssRaw || "0").split(".")[0];
  return new Date(y, m - 1, d, Number(hh || 0), Number(mm || 0), Number(ss || 0));
}

onMounted(() => {
  fetchEvents();
  fetchClassesAndSyllabi();
});

function onDocumentPointerDown(e) {
  const el = classSelectEl.value;
  if (!el) return;
  if (el.contains(e.target)) return;
  classMenuOpen.value = false;
}

onMounted(() => {
  document.addEventListener("pointerdown", onDocumentPointerDown);
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onDocumentPointerDown);
});

function toggleClassMenu() {
  if (!classes.value.length) return;
  classMenuOpen.value = !classMenuOpen.value;
}

function selectClass(c) {
  selectedClassKey.value = classKey(c);
  classMenuOpen.value = false;
}
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

        <div v-if="classes.length" class="header-actions">
          <div
            ref="classSelectEl"
            class="class-select"
            @keydown.esc.prevent="classMenuOpen = false"
          >
            <button
              type="button"
              class="btn ghost class-toggle class-toggle-lg"
              :aria-expanded="classMenuOpen ? 'true' : 'false'"
              aria-haspopup="listbox"
              @click="toggleClassMenu"
            >
              <span class="class-toggle-label">{{ selectedClass ? selectedClass.classCode : "Select class" }}</span>
              <span class="class-toggle-caret" aria-hidden="true">▾</span>
            </button>

            <div v-if="classMenuOpen" class="class-menu" role="listbox">
              <button
                v-for="c in classes.filter(x => classKey(x) !== selectedClassKey)"
                :key="classKey(c)"
                type="button"
                class="btn ghost btn-sm class-menu-item"
                @click="selectClass(c)"
              >
                {{ c.classCode }}
              </button>

              <div v-if="classes.length <= 1" class="class-menu-empty">No other classes</div>
            </div>
          </div>
        </div>
      </div>

      <div class="content">
        <div v-if="syllabusError" class="placeholder">{{ syllabusError }}</div>
        <div v-else-if="loadingSyllabus" class="placeholder">Loading syllabus…</div>
        <div v-else-if="!classes.length" class="placeholder">
          No classes yet — join or create a class first.
        </div>

        <div v-else class="syllabus-wrap">

          <div class="mini-grid">
            <div class="mini">
              <div class="mini-title">Class</div>
              <div class="mini-value">
                {{ selectedClass ? `${selectedClass.classCode} · ${selectedClass.quarter} ${selectedClass.year}` : "—" }}
              </div>
            </div>
            <div class="mini">
              <div class="mini-title">Role</div>
              <div class="mini-value">{{ role }}</div>
            </div>
            <div class="mini">
              <div class="mini-title">Syllabus</div>
              <div class="mini-value">{{ selectedSyllabus ? "Available" : "Not uploaded" }}</div>
            </div>
          </div>

          <div v-if="selectedSyllabus" class="syllabus-sections">
            <div class="syllabus-section">
              <h3>Grading Scale</h3>
              <div class="syllabus-text">{{ selectedSyllabus.gradingScale || "—" }}</div>
            </div>
            <div class="syllabus-section">
              <h3>Attendance Policy</h3>
              <div class="syllabus-text">{{ selectedSyllabus.attendancePolicy || "—" }}</div>
            </div>
            <div class="syllabus-section">
              <h3>Late Policy</h3>
              <div class="syllabus-text">{{ selectedSyllabus.latePolicy || "—" }}</div>
            </div>
            <div class="syllabus-section">
              <h3>Exam Info</h3>
              <div class="syllabus-text">{{ selectedSyllabus.examInfo || "—" }}</div>
            </div>
          </div>

          <div v-else class="placeholder">
            No syllabus uploaded for this class yet.
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

.header-actions {
  display: flex;
  align-items: center;
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

.btn.btn-sm {
  padding: 6px 10px;
  border-radius: 12px;
}

.btn.active {
  border-color: rgba(37,99,235,0.7);
  box-shadow: 0 0 0 2px rgba(37,99,235,0.25);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.syllabus-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.toggle-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.class-select {
  position: relative;
  width: fit-content;
}

.class-toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.class-toggle-lg {
  padding: 10px 12px;
  border-radius: 14px;
}

.class-toggle-label {
  font-weight: 900;
}

.class-toggle-caret {
  opacity: 0.85;
}

.class-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 10;
  min-width: 180px;
  padding: 10px;
  border-radius: 16px;
  background: rgba(15,23,42,0.92);
  border: 1px solid rgba(255,255,255,0.10);
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.class-menu-item {
  width: 100%;
  text-align: left;
}

.class-menu-empty {
  padding: 10px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.10);
  font-size: 13px;
  color: #9ca3af;
  font-weight: 800;
}

.syllabus-sections {
  display: grid;
  gap: 12px;
}

.syllabus-section {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
}

.syllabus-text {
  margin-top: 8px;
  color: #9ca3af;
  font-size: 13px;
  white-space: pre-wrap;
  line-height: 1.4;
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

@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .mini-grid {
    grid-template-columns: 1fr;
  }
}
</style>
