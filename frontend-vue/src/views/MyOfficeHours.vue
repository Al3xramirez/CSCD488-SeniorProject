<script setup>
import { ref, onMounted } from "vue";
import { useMe } from "../composables/useMe.js";

const { isTA, isProfessor } = useMe();

const WEEK_DAYS = [
  { day: "MON", label: "Monday" },
  { day: "TUE", label: "Tuesday" },
  { day: "WED", label: "Wednesday" },
  { day: "THU", label: "Thursday" },
  { day: "FRI", label: "Friday" },
];

const showModal = ref(false);
const weekTemplate = ref(WEEK_DAYS.map(d => ({ ...d, enabled: true, start: "", end: "" })));
const templateQuarter = ref("Fall");
const templateYear = ref(new Date().getFullYear());
const saveLoading = ref(false);
const saveError = ref("");

const schedule = ref([]);
const scheduleLoading = ref(false);
const scheduleError = ref("");

const exceptions = ref([]);
const exceptionsLoading = ref(false);
const exceptionsError = ref("");

const addExDate = ref("");
const addExStart = ref("");
const addExEnd = ref("");
const addExUnavailable = ref(true);
const addExNote = ref("");
const addExLoading = ref(false);
const addExError = ref("");

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

function openModal() {
  weekTemplate.value = WEEK_DAYS.map(d => ({ ...d, enabled: true, start: "", end: "" }));
  templateQuarter.value = "Fall";
  templateYear.value = new Date().getFullYear();
  saveError.value = "";
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

async function saveWeekTemplate() {
  saveError.value = "";

  const toAdd = weekTemplate.value.filter(d => d.enabled && d.start && d.end);
  if (!toAdd.length) { saveError.value = "Enable at least one day and set its start and end times."; return; }

  saveLoading.value = true;
  const quarter = templateQuarter.value;
  const year = parseInt(templateYear.value, 10);

  const results = await Promise.allSettled(
    toAdd.map(d =>
      fetch("/api/office-hours/me/schedule", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ dayOfWeek: d.day, startTime: d.start, endTime: d.end, quarter, year }),
      }).then(async res => {
        if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
        return res.json();
      })
    )
  );

  const added = results.filter(r => r.status === "fulfilled").map(r => r.value);
  const failed = results.filter(r => r.status === "rejected");

  schedule.value = [...schedule.value, ...added];
  saveLoading.value = false;

  if (failed.length) {
    saveError.value = `${failed.length} day(s) failed to save.`;
  } else {
    closeModal();
  }
}

async function loadSchedule() {
  scheduleLoading.value = true;
  scheduleError.value = "";
  try {
    const res = await fetch("/api/office-hours/me/schedule", { credentials: "include" });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    schedule.value = await res.json();
  } catch (e) {
    scheduleError.value = e?.message || "Failed to load schedule";
  } finally {
    scheduleLoading.value = false;
  }
}

async function deleteScheduleBlock(id) {
  scheduleError.value = "";
  try {
    const res = await fetch(`/api/office-hours/me/schedule/${id}`, { method: "DELETE", credentials: "include" });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    schedule.value = schedule.value.filter(b => b.id !== id);
  } catch (e) {
    scheduleError.value = e?.message || "Failed to remove block";
  }
}

async function loadExceptions() {
  exceptionsLoading.value = true;
  exceptionsError.value = "";
  try {
    const res = await fetch("/api/office-hours/me/exceptions", { credentials: "include" });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    exceptions.value = await res.json();
  } catch (e) {
    exceptionsError.value = e?.message || "Failed to load exceptions";
  } finally {
    exceptionsLoading.value = false;
  }
}

async function addException() {
  addExLoading.value = true;
  addExError.value = "";
  try {
    const res = await fetch("/api/office-hours/me/exceptions", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        exceptionDate: addExDate.value,
        startTime: addExStart.value || null,
        endTime: addExEnd.value || null,
        unavailable: addExUnavailable.value,
        note: addExNote.value.trim() || null,
      }),
    });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    exceptions.value = [...exceptions.value, await res.json()];
    addExDate.value = "";
    addExStart.value = "";
    addExEnd.value = "";
    addExNote.value = "";
    addExUnavailable.value = true;
  } catch (e) {
    addExError.value = e?.message || "Failed to add exception";
  } finally {
    addExLoading.value = false;
  }
}

async function deleteException(id) {
  exceptionsError.value = "";
  try {
    const res = await fetch(`/api/office-hours/me/exceptions/${id}`, { method: "DELETE", credentials: "include" });
    if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
    exceptions.value = exceptions.value.filter(e => e.id !== id);
  } catch (e) {
    exceptionsError.value = e?.message || "Failed to remove exception";
  }
}

onMounted(() => Promise.all([loadSchedule(), loadExceptions()]));
</script>

<template>
  <section class="card">
    <div class="card-header">
      <div>
        <h2>My Office Hours</h2>
        <p class="muted">Your posted schedule and exceptions.</p>
      </div>
      <button v-if="isTA || isProfessor" class="btn" type="button" @click="openModal">
        + Add Schedule
      </button>
    </div>

    <div v-if="!isTA && !isProfessor" class="muted">
      Only TAs and Professors can manage office hours.
    </div>

    <template v-else>
      <!-- Saved Schedule -->
      <div class="section">
        <div class="section-title">Schedule</div>

        <div v-if="scheduleError" class="alert">{{ scheduleError }}</div>
        <div v-else-if="scheduleLoading" class="muted">Loading…</div>
        <div v-else-if="!schedule.length" class="muted">No schedule posted yet. Use "Add Schedule" to get started.</div>
        <div v-else class="block-list">
          <div v-for="b in schedule" :key="b.id" class="block-row">
            <div class="block-day">{{ DAY_LABELS[b.dayOfWeek] ?? b.dayOfWeek }}</div>
            <div class="block-time">{{ fmtTime(b.startTime) }} – {{ fmtTime(b.endTime) }}</div>
            <div class="block-meta">{{ b.quarter }} {{ b.year }}</div>
            <button class="btn danger sm" type="button" @click="deleteScheduleBlock(b.id)">Remove</button>
          </div>
        </div>
      </div>

      <!-- Exceptions -->
      <div class="section">
        <div class="section-title">Exceptions</div>
        <p class="muted" style="margin-bottom: 12px;">
          Dates when your hours are cancelled or differ from the regular schedule.
        </p>

        <div v-if="exceptionsError" class="alert">{{ exceptionsError }}</div>
        <div v-else-if="exceptionsLoading" class="muted">Loading…</div>
        <div v-else-if="!exceptions.length" class="muted">No exceptions added.</div>
        <div v-else class="block-list">
          <div v-for="ex in exceptions" :key="ex.id" class="block-row">
            <div class="block-day">{{ ex.exceptionDate }}</div>
            <div class="block-time">
              <span v-if="ex.unavailable && !ex.startTime" class="tag cancelled">Cancelled</span>
              <span v-else-if="ex.startTime">{{ fmtTime(ex.startTime) }} – {{ fmtTime(ex.endTime) }}</span>
              <span v-else class="tag modified">Modified</span>
            </div>
            <div class="block-meta note-text">{{ ex.note || "" }}</div>
            <button class="btn danger sm" type="button" @click="deleteException(ex.id)">Remove</button>
          </div>
        </div>

        <div class="add-form">
          <div class="form-row wrap">
            <input v-model="addExDate" class="input" type="date" />
            <input v-model="addExStart" class="input" type="time" />
            <input v-model="addExEnd" class="input" type="time" />
            <input v-model="addExNote" class="input flex1" placeholder="Note (optional)" />
          </div>
          <div class="form-row" style="margin-top: 8px;">
            <label class="checkbox-label">
              <input v-model="addExUnavailable" type="checkbox" />
              Mark as unavailable
            </label>
            <button
              class="btn"
              type="button"
              :disabled="addExLoading || !addExDate"
              @click="addException"
            >
              {{ addExLoading ? "Adding…" : "Add Exception" }}
            </button>
          </div>
          <div v-if="addExError" class="alert">{{ addExError }}</div>
        </div>
      </div>
    </template>
  </section>

  <!-- Add Schedule Modal -->
  <teleport to="body">
    <div v-if="showModal" class="overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h3>Add Schedule</h3>
          <button class="close-btn" type="button" aria-label="Close" @click="closeModal">✕</button>
        </div>

        <div class="quarter-row">
          <select v-model="templateQuarter" class="input flex1">
            <option>Fall</option>
            <option>Winter</option>
            <option>Spring</option>
            <option>Summer</option>
          </select>
          <input v-model.number="templateYear" class="input year-input" type="number" min="2000" max="2100" />
        </div>

        <div class="week-grid">
          <div
            v-for="row in weekTemplate"
            :key="row.day"
            class="day-row"
            :class="{ disabled: !row.enabled }"
          >
            <button
              class="day-toggle"
              :class="{ active: row.enabled }"
              type="button"
              :aria-label="row.enabled ? `Disable ${row.label}` : `Enable ${row.label}`"
              @click="row.enabled = !row.enabled"
            >
              <span class="toggle-dot" />
            </button>

            <span class="day-label">{{ row.label }}</span>

            <template v-if="row.enabled">
              <input v-model="row.start" class="input time-input" type="time" />
              <span class="to-sep">to</span>
              <input v-model="row.end" class="input time-input" type="time" />
            </template>
            <span v-else class="unavailable-label">Not available</span>
          </div>
        </div>

        <div v-if="saveError" class="alert">{{ saveError }}</div>

        <div class="modal-actions">
          <button class="btn ghost" type="button" @click="closeModal">Cancel</button>
          <button class="btn" type="button" :disabled="saveLoading" @click="saveWeekTemplate">
            {{ saveLoading ? "Saving…" : "Add to Schedule" }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
h2 { margin: 0; font-size: 18px; }
h3 { margin: 0; font-size: 16px; }

.section {
  margin-top: 22px;
}

.section-title {
  font-size: 12px;
  color: #9ca3af;
  margin: 0 0 12px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

/* Saved blocks */
.block-list {
  display: grid;
  gap: 8px;
}

.block-row {
  display: grid;
  grid-template-columns: 110px 1fr 1fr auto;
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

.note-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Tags */
.tag {
  font-size: 11px;
  font-weight: 900;
  padding: 2px 8px;
  border-radius: 999px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
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

/* Exception form */
.add-form {
  margin-top: 12px;
}

.form-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.form-row.wrap {
  flex-wrap: wrap;
}

.flex1 {
  flex: 1;
}

.btn.sm {
  padding: 6px 10px;
  font-size: 12px;
  min-width: unset;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e5e7eb;
  font-size: 13px;
  cursor: pointer;
  flex: 1;
}

.checkbox-label input[type="checkbox"] {
  accent-color: #2563eb;
  width: 16px;
  height: 16px;
  cursor: pointer;
}

/* Modal internals */
.close-btn {
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 16px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  line-height: 1;
}

.close-btn:hover {
  color: #e5e7eb;
  background: rgba(255,255,255,0.06);
}

.quarter-row {
  display: flex;
  gap: 8px;
  margin: 14px 0 12px;
}

.year-input {
  width: 100px;
}

.week-grid {
  display: grid;
  gap: 8px;
}

.day-row {
  display: grid;
  grid-template-columns: 38px 110px 1fr auto 1fr;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  transition: opacity 0.15s ease, border-color 0.15s ease;
}

.day-row.disabled {
  opacity: 0.45;
  border-color: rgba(255,255,255,0.04);
}

.day-toggle {
  width: 32px;
  height: 20px;
  border-radius: 999px;
  background: rgba(255,255,255,0.10);
  border: 1px solid rgba(255,255,255,0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 0 3px;
  transition: background 0.15s ease, border-color 0.15s ease;
  flex-shrink: 0;
}

.day-toggle.active {
  background: #2563eb;
  border-color: rgba(37, 99, 235, 0.6);
  justify-content: flex-end;
}

.toggle-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: white;
  flex-shrink: 0;
}

.day-label {
  font-weight: 900;
  color: #e5e7eb;
  font-size: 14px;
}

.time-input {
  width: 100%;
}

.to-sep {
  color: #9ca3af;
  font-size: 13px;
  text-align: center;
}

.unavailable-label {
  grid-column: 3 / 6;
  color: #6b7280;
  font-size: 13px;
  font-style: italic;
}
</style>
