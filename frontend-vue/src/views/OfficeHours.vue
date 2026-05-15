<script setup>
import { ref, onMounted } from "vue";

const classes = ref([]);
const classTAs = ref({});
const loadingClasses = ref(false);
const classesError = ref("");

const selectedTA = ref(null);
const taHours = ref(null);
const taHoursLoading = ref(false);
const taHoursError = ref("");

const DAY_LABELS = {
  MON: "Monday", TUE: "Tuesday", WED: "Wednesday",
  THU: "Thursday", FRI: "Friday", SAT: "Saturday", SUN: "Sunday",
};

function fmtTime(t) {
  if (!t) return "";
  const [h, m] = t.split(":");
  const hour = parseInt(h, 10);
  const ampm = hour < 12 ? "AM" : "PM";
  const h12 = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour;
  return `${h12}:${m} ${ampm}`;
}

async function safeErrorMessage(res) {
  try {
    const text = await res.text();
    try { return JSON.parse(text).message || text; } catch { return text; }
  } catch { return ""; }
}

async function loadTAsForClass(joinCode) {
  classTAs.value[joinCode] = { loading: true, error: "", tas: [] };
  try {
    const res = await fetch(`/api/classes/${encodeURIComponent(joinCode)}/tas`, { credentials: "include" });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    const data = await res.json();
    classTAs.value[joinCode] = { loading: false, error: "", tas: Array.isArray(data) ? data : [] };
  } catch (e) {
    classTAs.value[joinCode] = { loading: false, error: e?.message || "Failed to load TAs", tas: [] };
  }
}

async function loadClasses() {
  loadingClasses.value = true;
  classesError.value = "";
  try {
    const res = await fetch("/api/classes/mine", { credentials: "include" });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    const data = await res.json();
    classes.value = Array.isArray(data) ? data : [];
    await Promise.all(classes.value.map(c => loadTAsForClass(c.joinCode)));
  } catch (e) {
    classesError.value = e?.message || "Failed to load classes";
  } finally {
    loadingClasses.value = false;
  }
}

async function selectTA(ta) {
  selectedTA.value = ta;
  taHours.value = null;
  taHoursError.value = "";
  taHoursLoading.value = true;
  try {
    const res = await fetch(`/api/office-hours/user/${encodeURIComponent(ta.userId)}`, { credentials: "include" });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    taHours.value = await res.json();
  } catch (e) {
    taHoursError.value = e?.message || "Failed to load office hours";
  } finally {
    taHoursLoading.value = false;
  }
}

function clearSelection() {
  selectedTA.value = null;
  taHours.value = null;
  taHoursError.value = "";
}

onMounted(loadClasses);
</script>

<template>
  <section class="card">
    <div class="card-header">
      <div>
        <h2>Office Hours</h2>
        <p class="muted">Browse TA office hours for your enrolled classes.</p>
      </div>
      <button v-if="selectedTA" class="btn ghost" type="button" @click="clearSelection">Back</button>
    </div>

    <!-- TA detail view -->
    <template v-if="selectedTA">
      <div class="ta-header">
        <div class="ta-avatar-lg">
          {{ (selectedTA.firstName?.[0] ?? "") + (selectedTA.lastName?.[0] ?? "") }}
        </div>
        <div>
          <div class="ta-name">{{ selectedTA.firstName }} {{ selectedTA.lastName }}</div>
          <div class="tag role-tag">{{ (taHours?.role ?? "TA").toUpperCase() }}</div>
        </div>
      </div>

      <div v-if="taHoursLoading" class="muted">Loading office hours…</div>
      <div v-else-if="taHoursError" class="alert">{{ taHoursError }}</div>
      <template v-else-if="taHours">
        <div class="section">
          <div class="section-title">Regular Schedule</div>
          <div v-if="!taHours.schedule?.length" class="muted">No regular schedule posted.</div>
          <div v-else class="block-list">
            <div v-for="b in taHours.schedule" :key="b.id" class="block-row">
              <div class="block-day">{{ DAY_LABELS[b.dayOfWeek] ?? b.dayOfWeek }}</div>
              <div class="block-time">{{ fmtTime(b.startTime) }} – {{ fmtTime(b.endTime) }}</div>
              <div class="block-meta">{{ b.quarter }} {{ b.year }}</div>
            </div>
          </div>
        </div>

        <div class="section">
          <div class="section-title">Upcoming Exceptions</div>
          <div v-if="!taHours.exceptions?.length" class="muted">No upcoming exceptions in the next 90 days.</div>
          <div v-else class="block-list">
            <div v-for="ex in taHours.exceptions" :key="ex.id" class="block-row">
              <div class="block-day">{{ ex.exceptionDate }}</div>
              <div class="block-time">
                <span v-if="ex.unavailable && !ex.startTime" class="tag cancelled">Cancelled</span>
                <span v-else-if="ex.startTime">{{ fmtTime(ex.startTime) }} – {{ fmtTime(ex.endTime) }}</span>
                <span v-else class="tag modified">Modified</span>
              </div>
              <div class="block-meta">{{ ex.note || "" }}</div>
            </div>
          </div>
        </div>
      </template>
    </template>

    <!-- Class / TA browse view -->
    <template v-else>
      <div v-if="classesError" class="alert">{{ classesError }}</div>
      <div v-else-if="loadingClasses" class="muted">Loading classes…</div>
      <div v-else-if="!classes.length" class="muted">You are not enrolled in any classes.</div>
      <div v-for="c in classes" :key="c.joinCode" class="class-section">
        <div class="class-title">{{ c.classCode }} · {{ c.quarter }} {{ c.year }}</div>
        <p v-if="c.title" class="muted class-subtitle">{{ c.title }}</p>

        <div v-if="classTAs[c.joinCode]?.loading" class="muted indent">Loading TAs…</div>
        <div v-else-if="classTAs[c.joinCode]?.error" class="muted indent">{{ classTAs[c.joinCode].error }}</div>
        <div v-else-if="!classTAs[c.joinCode]?.tas?.length" class="muted indent">No TAs assigned to this class.</div>
        <div v-else class="ta-list">
          <button
            v-for="ta in classTAs[c.joinCode].tas"
            :key="ta.userId"
            class="ta-card"
            type="button"
            @click="selectTA(ta)"
          >
            <div class="ta-avatar">
              {{ (ta.firstName?.[0] ?? "") + (ta.lastName?.[0] ?? "") }}
            </div>
            <div>
              <div class="ta-card-name">{{ ta.firstName }} {{ ta.lastName }}</div>
              <div class="ta-card-hint">View office hours →</div>
            </div>
          </button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
h2 { margin: 0; font-size: 18px; }

.section {
  margin-top: 20px;
}

.section-title {
  font-size: 12px;
  color: #9ca3af;
  margin: 0 0 10px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.class-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.class-title {
  font-size: 15px;
  font-weight: 900;
  color: #e5e7eb;
}

.class-subtitle {
  margin-top: 2px;
}

.indent {
  margin-top: 8px;
}

.ta-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.ta-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
  cursor: pointer;
  text-align: left;
  color: #e5e7eb;
  width: 100%;
}

.ta-card:hover {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.3);
}

.ta-avatar {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.2);
  border: 1px solid rgba(37, 99, 235, 0.3);
  display: grid;
  place-items: center;
  font-weight: 900;
  color: #93c5fd;
  font-size: 14px;
  flex-shrink: 0;
}

.ta-avatar-lg {
  width: 46px;
  height: 46px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.2);
  border: 1px solid rgba(37, 99, 235, 0.3);
  display: grid;
  place-items: center;
  font-weight: 900;
  color: #93c5fd;
  font-size: 16px;
  flex-shrink: 0;
}

.ta-card-name {
  font-weight: 900;
  font-size: 14px;
}

.ta-card-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.ta-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.ta-name {
  font-size: 17px;
  font-weight: 900;
  color: #e5e7eb;
  margin-bottom: 4px;
}

.tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 900;
  padding: 2px 8px;
  border-radius: 999px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.role-tag {
  background: rgba(37, 99, 235, 0.15);
  color: #93c5fd;
  border: 1px solid rgba(37, 99, 235, 0.3);
}

.tag.cancelled {
  background: rgba(239, 68, 68, 0.15);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.tag.modified {
  background: rgba(234, 179, 8, 0.12);
  color: #fde047;
  border: 1px solid rgba(234, 179, 8, 0.3);
}

.block-list {
  display: grid;
  gap: 8px;
}

.block-row {
  display: grid;
  grid-template-columns: 110px 1fr 1fr;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
}

.block-day {
  font-weight: 900;
  color: #e5e7eb;
}

.block-time {
  color: #e5e7eb;
  font-size: 14px;
}

.block-meta {
  color: #9ca3af;
  font-size: 13px;
}
</style>
