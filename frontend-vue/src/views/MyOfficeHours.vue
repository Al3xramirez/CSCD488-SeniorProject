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

// Import from Syllabus
const showImportModal = ref(false);
const importStep = ref("pick"); // "pick" | "review"
const importClasses = ref([]);
const importQuarters = ref([]); // grouped by quarter/year
const importClassesLoading = ref(false);
const importClassesError = ref("");
const selectedImportQuarter = ref(null); // { quarter, year, classes[] }
const importSyllabi = ref([]); // one entry per class that has a syllabus
const importSyllabusLoading = ref(false);
const importSyllabusError = ref("");
const importWeekTemplate = ref([]);
const importQuarter = ref("Fall");
const importYear = ref(new Date().getFullYear());
const importSaveLoading = ref(false);
const importSaveError = ref("");
const importPrefilledFrom = ref(null); // classCode of the syllabus that drove pre-fill, or null

function normalizeToHHMM(raw) {
  let s = (raw || "").trim().toLowerCase();
  const isPM = /pm/.test(s);
  const isAM = /am/.test(s);
  s = s.replace(/[apm\s]/g, "");
  if (!s.includes(":")) s += ":00";
  const [hStr, mStr] = s.split(":");
  const h = parseInt(hStr, 10);
  const m = parseInt(mStr, 10);
  if (isNaN(h) || isNaN(m)) return null;
  let hour = h;
  if (isPM && hour !== 12) hour += 12;
  else if (isAM && hour === 12) hour = 0;
  else if (!isPM && !isAM && hour > 0 && hour < 8) hour += 12;
  if (hour > 23) return null;
  return `${String(hour).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}

const DAY_ORDER = ["MON", "TUE", "WED", "THU", "FRI"];
const DAY_LOOKUP = {
  monday: "MON", mon: "MON",
  tuesday: "TUE", tue: "TUE", tues: "TUE",
  wednesday: "WED", wed: "WED",
  thursday: "THU", thu: "THU", thur: "THU", thurs: "THU",
  friday: "FRI", fri: "FRI",
};

function parseDay(str) {
  return DAY_LOOKUP[(str || "").toLowerCase().trim()] || null;
}

function parseOfficeHoursText(text) {
  if (!text) return { days: [], times: [] };
  const days = new Set();

  if (/every\s*weekday|weekdays|every\s*day|everyday/i.test(text)) {
    DAY_ORDER.forEach(d => days.add(d));
  } else {
    // Handle "Monday through Thursday" / "Mon-Fri" ranges
    const rangeRe = /\b(monday|tuesday|wednesday|thursday|friday|mon|tues?|wed|thurs?|fri)\s*(?:through|to|-)\s*(monday|tuesday|wednesday|thursday|friday|mon|tues?|wed|thurs?|fri)\b/gi;
    for (const m of text.matchAll(rangeRe)) {
      const from = parseDay(m[1]), to = parseDay(m[2]);
      if (from && to) {
        const a = DAY_ORDER.indexOf(from), b = DAY_ORDER.indexOf(to);
        if (a <= b) for (let i = a; i <= b; i++) days.add(DAY_ORDER[i]);
      }
    }

    // Abbreviation combos
    if (/\bmwf\b/i.test(text)) { days.add("MON"); days.add("WED"); days.add("FRI"); }
    if (/\bmw\b/i.test(text)) { days.add("MON"); days.add("WED"); }
    if (/\b(tr|tth)\b/i.test(text)) { days.add("TUE"); days.add("THU"); }

    // Individual day names (only if no range already matched them)
    if (/\bmon(days?)?\b/i.test(text)) days.add("MON");
    if (/\btues?(days?)?\b/i.test(text)) days.add("TUE");
    if (/\bwed(nesdays?)?\b/i.test(text)) days.add("WED");
    if (/\bthurs?(days?)?\b/i.test(text)) days.add("THU");
    if (/\bfri(days?)?\b/i.test(text)) days.add("FRI");
  }

  const timeRe = /(\d{1,2}(?::\d{2})?(?:\s*[ap]m)?)\s*[-–—]\s*(\d{1,2}(?::\d{2})?(?:\s*[ap]m)?)/gi;
  const times = [...text.matchAll(timeRe)]
    .map(m => ({ start: normalizeToHHMM(m[1]), end: normalizeToHHMM(m[2]) }))
    .filter(t => t.start && t.end);

  return { days: [...days], times };
}

async function openImportModal() {
  showImportModal.value = true;
  importStep.value = "pick";
  selectedImportQuarter.value = null;
  importSyllabi.value = [];
  importClassesError.value = "";
  importSyllabusError.value = "";
  importSaveError.value = "";

  importClassesLoading.value = true;
  try {
    const res = await fetch("/api/classes/mine", { credentials: "include" });
    if (!res.ok) throw new Error("Failed to load classes");
    importClasses.value = await res.json();

    const grouped = {};
    for (const c of importClasses.value) {
      const key = `${c.quarter}-${c.year}`;
      if (!grouped[key]) grouped[key] = { quarter: c.quarter, year: c.year, classes: [] };
      grouped[key].classes.push(c);
    }
    importQuarters.value = Object.values(grouped)
      .sort((a, b) => parseInt(b.year) - parseInt(a.year) || a.quarter.localeCompare(b.quarter));
  } catch (e) {
    importClassesError.value = e?.message || "Failed to load classes";
  } finally {
    importClassesLoading.value = false;
  }
}

async function selectImportQuarter(qGroup) {
  selectedImportQuarter.value = qGroup;
  importSyllabusLoading.value = true;
  importSyllabusError.value = "";
  importSyllabi.value = [];

  try {
    const results = await Promise.allSettled(
      qGroup.classes.map(async c => {
        const jc = (c.joinCode || "").toString().trim();
        const r = await fetch(`/api/courses/${encodeURIComponent(jc)}/syllabus`, { credentials: "include" });
        if (!r.ok) return null;
        const data = await r.json();
        return { ...data, _classCode: c.classCode, _title: c.title };
      })
    );
    importSyllabi.value = results
      .filter(r => r.status === "fulfilled" && r.value)
      .map(r => r.value);

    importQuarter.value = qGroup.quarter;
    importYear.value = parseInt(qGroup.year, 10) || new Date().getFullYear();

    // Use the first syllabus with parseable office hours to pre-fill the template
    let parsedDays = [], parsedTimes = [];
    importPrefilledFrom.value = null;
    for (const syl of importSyllabi.value) {
      const { days, times } = parseOfficeHoursText(syl?.officeHours || "");
      if (days.length > 0 && times.length > 0) {
        parsedDays = days;
        parsedTimes = times;
        importPrefilledFrom.value = syl._classCode;
        break;
      }
    }

    const hasParsed = parsedDays.length > 0 && parsedTimes.length > 0;
    importWeekTemplate.value = WEEK_DAYS.map(d => ({
      ...d,
      enabled: hasParsed ? parsedDays.includes(d.day) : true,
      slots: hasParsed && parsedDays.includes(d.day)
        ? parsedTimes.map(t => ({ start: t.start, end: t.end }))
        : [{ start: "", end: "" }],
    }));

    importStep.value = "review";
  } catch (e) {
    importSyllabusError.value = e?.message || "Failed to load syllabi";
  } finally {
    importSyllabusLoading.value = false;
  }
}

function closeImportModal() {
  showImportModal.value = false;
}

async function saveImportedSchedule() {
  importSaveError.value = "";

  const quarter = importQuarter.value;
  const year = importYear.value;
  const toAdd = [];
  for (const d of importWeekTemplate.value) {
    if (!d.enabled) continue;
    for (const slot of d.slots) {
      if (slot.start && slot.end) toAdd.push({ dayOfWeek: d.day, startTime: slot.start, endTime: slot.end, quarter, year });
    }
  }
  if (!toAdd.length) { importSaveError.value = "Enable at least one day and set its start and end times."; return; }

  importSaveLoading.value = true;

  const results = await Promise.allSettled(
    toAdd.map(entry =>
      fetch("/api/office-hours/me/schedule", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(entry),
      }).then(async res => {
        if (!res.ok) throw new Error(await safeErrorMessage(res) || `Error ${res.status}`);
        return res.json();
      })
    )
  );

  const added = results.filter(r => r.status === "fulfilled").map(r => r.value);
  const failed = results.filter(r => r.status === "rejected");

  schedule.value = [...schedule.value, ...added];
  importSaveLoading.value = false;

  if (failed.length) {
    importSaveError.value = `${failed.length} day(s) failed to save.`;
  } else {
    closeImportModal();
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
      <div class="header-actions">
        <button v-if="isProfessor" class="btn ghost" type="button" @click="openImportModal">
          Import from Syllabus
        </button>
        <button v-if="isTA || isProfessor" class="btn" type="button" @click="openModal">
          + Add Schedule
        </button>
      </div>
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

  <!-- Import from Syllabus Modal -->
  <teleport to="body">
    <div v-if="showImportModal" class="overlay" @click.self="closeImportModal">
      <div class="modal">
        <div class="modal-header">
          <h3>Import from Syllabus</h3>
          <button class="close-btn" type="button" aria-label="Close" @click="closeImportModal">✕</button>
        </div>

        <!-- Step 1: Pick a quarter -->
        <template v-if="importStep === 'pick'">
          <p class="muted" style="margin: 10px 0 14px;">Select a quarter to import office hours from its syllabi.</p>
          <div v-if="importClassesLoading" class="muted">Loading classes…</div>
          <div v-else-if="importClassesError" class="alert">{{ importClassesError }}</div>
          <div v-else-if="!importQuarters.length" class="muted">No classes found.</div>
          <div v-else class="import-class-list">
            <button
              v-for="qg in importQuarters"
              :key="`${qg.quarter}-${qg.year}`"
              class="import-class-item"
              type="button"
              @click="selectImportQuarter(qg)"
            >
              <span class="import-class-code">{{ qg.quarter }} {{ qg.year }}</span>
              <span class="import-class-title">{{ qg.classes.map(c => c.classCode).join(' · ') }}</span>
            </button>
          </div>
          <div v-if="importSyllabusLoading" class="muted" style="margin-top: 12px;">Loading syllabi…</div>
          <div v-if="importSyllabusError" class="alert" style="margin-top: 12px;">{{ importSyllabusError }}</div>
          <div class="modal-actions">
            <button class="btn ghost" type="button" @click="closeImportModal">Cancel</button>
          </div>
        </template>

        <!-- Step 2: Review and set schedule -->
        <template v-else-if="importStep === 'review'">
          <template v-for="syl in importSyllabi" :key="syl._classCode">
            <div class="import-syllabus-ref" v-if="syl.officeHours">
              <div class="import-ref-label">{{ syl._classCode }} — {{ syl._title }}</div>
              <div class="import-ref-text">{{ syl.officeHours }}</div>
            </div>
          </template>
          <div v-if="importSyllabi.length && importSyllabi.every(s => !s.officeHours)" class="muted" style="margin: 12px 0 4px;">
            No office hours text found in any syllabus for this quarter.
          </div>

          <div v-if="importPrefilledFrom" class="import-prefill-note">
            Times pre-filled from <strong>{{ importPrefilledFrom }}</strong>. Review and adjust as needed.
          </div>
          <div v-else-if="importSyllabi.length" class="import-prefill-note">
            Could not parse times automatically — set them manually below.
          </div>

          <div class="quarter-row">
            <select v-model="importQuarter" class="input flex1">
              <option>Fall</option>
              <option>Winter</option>
              <option>Spring</option>
              <option>Summer</option>
            </select>
            <input v-model.number="importYear" class="input year-input" type="number" min="2000" max="2100" />
          </div>

          <div class="week-grid">
            <div
              v-for="row in importWeekTemplate"
              :key="row.day"
              class="day-group"
              :class="{ disabled: !row.enabled }"
            >
              <div class="day-group-header">
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
              </div>

              <template v-if="row.enabled">
                <div v-for="(slot, i) in row.slots" :key="i" class="slot-row">
                  <input v-model="slot.start" class="input time-input" type="time" />
                  <span class="to-sep">to</span>
                  <input v-model="slot.end" class="input time-input" type="time" />
                </div>
              </template>
              <span v-else class="unavailable-label">Not available</span>
            </div>
          </div>

          <div v-if="importSaveError" class="alert">{{ importSaveError }}</div>

          <div class="modal-actions">
            <button class="btn ghost" type="button" @click="importStep = 'pick'">Back</button>
            <button class="btn" type="button" :disabled="importSaveLoading" @click="saveImportedSchedule">
              {{ importSaveLoading ? "Saving…" : "Add to Schedule" }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </teleport>

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

/* Header actions row */
.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

/* Import modal */
.import-class-list {
  display: grid;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.import-class-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.08);
  cursor: pointer;
  text-align: left;
  transition: background 0.12s ease, border-color 0.12s ease;
}

.import-class-item:hover {
  background: rgba(37,99,235,0.12);
  border-color: rgba(37,99,235,0.35);
}

.import-class-code {
  font-weight: 900;
  color: #e5e7eb;
  font-size: 14px;
}

.import-class-title {
  color: #9ca3af;
  font-size: 13px;
}

.import-syllabus-ref {
  margin: 14px 0 10px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(234,179,8,0.06);
  border: 1px solid rgba(234,179,8,0.2);
}

.import-ref-label {
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #fde047;
  margin-bottom: 6px;
}

.import-ref-text {
  color: #e5e7eb;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.import-prefill-note {
  font-size: 12px;
  color: #9ca3af;
  margin: 10px 0 2px;
}

.import-prefill-note strong {
  color: #e5e7eb;
}

.day-group {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  transition: opacity 0.15s ease, border-color 0.15s ease;
  display: grid;
  gap: 8px;
}

.day-group.disabled {
  opacity: 0.45;
  border-color: rgba(255,255,255,0.04);
}

.day-group-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.slot-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 8px;
  padding-left: 48px;
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
