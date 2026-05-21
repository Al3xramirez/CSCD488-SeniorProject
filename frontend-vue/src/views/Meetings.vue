<script setup>
import { ref, computed, watch, inject, onMounted } from 'vue';
import CreatingClassMeetings from '../components/CreatingClassMeetings.vue';

const me = inject('me', null);
const role = computed(() => (me?.value?.role || 'STUDENT').toString().trim().toUpperCase());
const isProfessor = computed(() => role.value === 'PROFESSOR');

// ── Class selection ────────────────────────────────────────────────────────
const myClasses = ref([]);
const selectedJoinCode = ref(null);
const selectedClass = computed(() => myClasses.value.find(c => c.joinCode === selectedJoinCode.value) ?? null);

async function fetchMyClasses() {
  try {
    const res = await fetch('/api/classes/mine', { credentials: 'include' });
    if (res.ok) {
      myClasses.value = await res.json();
      if (myClasses.value.length > 0) selectedJoinCode.value = myClasses.value[0].joinCode;
    }
  } catch {}
}

// ── Class-wide meeting times ───────────────────────────────────────────────
const classMeetings = ref([]);
const loadingMeetings = ref(false);

async function fetchClassMeetings(classCode) {
  loadingMeetings.value = true;
  classMeetings.value = [];
  try {
    const res = await fetch(`/api/classes/${classCode}/class-meetings`, { credentials: 'include' });
    if (res.ok) classMeetings.value = await res.json();
  } catch {} finally {
    loadingMeetings.value = false;
  }
}

// Group meetings by recurring pattern (same startTime + endTime + notes)
// so we display "Mon / Wed / Fri  10:00 – 11:50 AM" instead of individual dates
const meetingPatterns = computed(() => {
  const map = new Map();
  for (const m of classMeetings.value) {
    const key = `${m.startTime}|${m.endTime}|${m.notes ?? ''}`;
    if (!map.has(key)) {
      map.set(key, { startTime: m.startTime, endTime: m.endTime, notes: m.notes, days: new Set() });
    }
    const dow = new Date(m.meetingDate + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();
    map.get(key).days.add(dow);
  }
  return [...map.values()].map(p => ({
    ...p,
    days: [...p.days].sort((a, b) => DAY_ORDER.indexOf(a) - DAY_ORDER.indexOf(b))
  }));
});

const DAY_ORDER = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
const DAY_SHORT = { SUNDAY: 'Sun', MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed', THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat' };

// Upcoming individual meetings (next 14 dates)
const upcomingMeetings = computed(() => {
  const today = new Date().toISOString().split('T')[0];
  return classMeetings.value
    .filter(m => m.meetingDate >= today)
    .slice(0, 14);
});

// ── Office hours ───────────────────────────────────────────────────────────
const officeHours = ref([]);
const loadingOH = ref(false);

async function fetchOfficeHours(classCode) {
  loadingOH.value = true;
  officeHours.value = [];
  try {
    const res = await fetch(`/api/office-hours/class/${classCode}`, { credentials: 'include' });
    if (res.ok) officeHours.value = await res.json();
  } catch {} finally {
    loadingOH.value = false;
  }
}

// ── My meetings (personal + class-wide across all enrolled classes) ────────
const myMeetings = ref([]);
const loadingMyMeetings = ref(false);

async function fetchMyMeetings() {
  loadingMyMeetings.value = true;
  try {
    const res = await fetch('/api/meetings/my', { credentials: 'include' });
    if (res.ok) myMeetings.value = await res.json();
  } catch {} finally {
    loadingMyMeetings.value = false;
  }
}

const upcomingMyMeetings = computed(() => {
  const today = new Date().toISOString().split('T')[0];
  return myMeetings.value.filter(m => m.meetingDate >= today);
});

// ── Confirm / Decline actions ──────────────────────────────────────────────
const actingOn = ref(new Set());

async function confirmMeeting(m) {
  if (actingOn.value.has(m.meetingId)) return;
  actingOn.value = new Set([...actingOn.value, m.meetingId]);
  try {
    const res = await fetch(`/api/meetings/${m.meetingId}/status`, {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'CONFIRMED' }),
    });
    if (res.ok) {
      const updated = await res.json();
      const idx = myMeetings.value.findIndex(x => x.meetingId === m.meetingId);
      if (idx !== -1) myMeetings.value[idx] = updated;
    }
  } finally {
    actingOn.value = new Set([...actingOn.value].filter(id => id !== m.meetingId));
  }
}

async function declineMeeting(m) {
  if (actingOn.value.has(m.meetingId)) return;
  actingOn.value = new Set([...actingOn.value, m.meetingId]);
  try {
    const res = await fetch(`/api/meetings/${m.meetingId}`, {
      method: 'DELETE',
      credentials: 'include',
    });
    if (res.ok) {
      myMeetings.value = myMeetings.value.filter(x => x.meetingId !== m.meetingId);
    }
  } finally {
    actingOn.value = new Set([...actingOn.value].filter(id => id !== m.meetingId));
  }
}

// ── Create-meetings form (professor only) ──────────────────────────────────
const showCreateForm = ref(false);

function onMeetingsCreated() {
  showCreateForm.value = false;
  if (selectedClass.value) fetchClassMeetings(selectedClass.value.classCode);
  fetchMyMeetings();
}

// ── Watchers & mount ───────────────────────────────────────────────────────
watch(selectedJoinCode, () => {
  showCreateForm.value = false;
  const cls = selectedClass.value;
  if (!cls) return;
  fetchClassMeetings(cls.classCode);
  fetchOfficeHours(cls.classCode);
});

onMounted(() => {
  fetchMyClasses();
  fetchMyMeetings();
});

// ── Formatters ─────────────────────────────────────────────────────────────
function fmtTime(t) {
  if (!t) return '';
  const [hh, mm] = t.split(':').map(Number);
  const ampm = hh < 12 ? 'AM' : 'PM';
  return `${hh % 12 || 12}:${String(mm).padStart(2, '0')} ${ampm}`;
}

function fmtDate(d) {
  if (!d) return '';
  return new Date(d + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
}

const OH_DAY_SHORT = { MON: 'Mon', TUE: 'Tue', WED: 'Wed', THU: 'Thu', FRI: 'Fri', SAT: 'Sat', SUN: 'Sun' };
</script>

<template>
  <div class="wrap">

    <!-- Header + class selector -->
    <div class="page-header">
      <div>
        <h1>Meeting Times</h1>
        <p class="muted">Lecture schedule and office hours for your class.</p>
      </div>
      <select v-if="myClasses.length > 1" class="class-select" v-model="selectedJoinCode">
        <option v-for="c in myClasses" :key="c.joinCode" :value="c.joinCode">
          {{ c.classCode }} · {{ c.quarter }} {{ c.year }}
        </option>
      </select>
    </div>

    <!-- No classes -->
    <div v-if="myClasses.length === 0" class="empty-card">
      You are not enrolled in any classes yet.
    </div>

    <!-- ── My Upcoming Meetings (all classes + personal) ── -->
    <section class="card">
      <div class="section-header">
        <h2>My Upcoming Meetings</h2>
      </div>

      <div v-if="loadingMyMeetings" class="loading">Loading…</div>

      <template v-else-if="upcomingMyMeetings.length">
        <div v-for="m in upcomingMyMeetings" :key="m.meetingId" class="my-row">
          <span class="my-date">{{ fmtDate(m.meetingDate) }}</span>
          <span class="my-time">{{ fmtTime(m.startTime) }} – {{ fmtTime(m.endTime) }}</span>
          <span class="my-code">{{ m.classCode }}</span>
          <span class="my-type" :class="m.recipientId ? 'type-meeting' : 'type-class'">
            {{ m.recipientId ? 'Meeting' : 'Class' }}
          </span>
          <span v-if="m.recipientId && m.status" class="my-status" :class="`status-${(m.status || '').toLowerCase()}`">
            {{ m.status }}
          </span>
          <span v-if="m.notes" class="my-notes">{{ m.notes }}</span>
          <template v-if="m.recipientId && m.status !== 'CONFIRMED'">
            <div class="my-actions">
              <button
                class="btn-confirm"
                :disabled="actingOn.has(m.meetingId)"
                @click="confirmMeeting(m)"
              >Confirm</button>
              <button
                class="btn-decline"
                :disabled="actingOn.has(m.meetingId)"
                @click="declineMeeting(m)"
              >Decline</button>
            </div>
          </template>
        </div>
      </template>

      <div v-else-if="!loadingMyMeetings" class="empty-note">No upcoming meetings.</div>
    </section>

    <template v-if="myClasses.length > 0">

      <!-- ── Lecture / Class Meeting Times ── -->
      <section class="card">
        <div class="section-header">
          <h2>Class Meeting Times</h2>
          <!-- Professor: toggle create form -->
          <button v-if="isProfessor" class="btn" @click="showCreateForm = !showCreateForm">
            {{ showCreateForm ? 'Cancel' : '+ Set Up Schedule' }}
          </button>
        </div>

        <!-- Create recurring meetings form (professor only) -->
        <div v-if="isProfessor && showCreateForm && selectedClass" class="create-form-wrap">
          <CreatingClassMeetings
            :classCode="selectedClass.classCode"
            :quarter="selectedClass.quarter"
            :year="selectedClass.year"
            @meetings-created="onMeetingsCreated"
          />
        </div>

        <div v-if="loadingMeetings" class="loading">Loading schedule…</div>

        <template v-else-if="meetingPatterns.length">
          <!-- Weekly pattern summary -->
          <div class="pattern-list">
            <div v-for="(p, i) in meetingPatterns" :key="i" class="pattern-row">
              <div class="pattern-days">
                <span v-for="d in p.days" :key="d" class="day-chip">{{ DAY_SHORT[d] }}</span>
              </div>
              <div class="pattern-time">{{ fmtTime(p.startTime) }} – {{ fmtTime(p.endTime) }}</div>
              <div v-if="p.notes" class="pattern-notes">{{ p.notes }}</div>
            </div>
          </div>

          <!-- Upcoming individual dates -->
          <div class="upcoming-label">Upcoming dates</div>
          <div class="upcoming-list">
            <div v-for="m in upcomingMeetings" :key="m.meetingId" class="upcoming-row">
              <span class="upcoming-date">{{ fmtDate(m.meetingDate) }}</span>
              <span class="upcoming-time">{{ fmtTime(m.startTime) }} – {{ fmtTime(m.endTime) }}</span>
              <span v-if="m.notes" class="upcoming-notes">{{ m.notes }}</span>
            </div>
          </div>
        </template>

        <div v-else-if="!loadingMeetings" class="empty-note">
          {{ isProfessor ? 'No lecture schedule set up yet. Use "Set Up Schedule" above.' : 'No lecture schedule has been posted yet.' }}
        </div>
      </section>

      <!-- ── Office Hours ── -->
      <section class="card">
        <div class="section-header">
          <h2>Office Hours</h2>
        </div>

        <div v-if="loadingOH" class="loading">Loading office hours…</div>

        <template v-else-if="officeHours.length">
          <div v-for="person in officeHours" :key="person.userId" class="person-block">
            <div class="person-name">
              {{ person.firstName }} {{ person.lastName }}
              <span class="role-badge" :class="person.role === 'TA' ? 'badge-ta' : 'badge-prof'">
                {{ person.role === 'PROFESSOR' ? 'Professor' : 'TA' }}
              </span>
            </div>
            <div v-if="person.schedule?.length" class="oh-schedule">
              <div v-for="block in person.schedule" :key="block.id" class="oh-row">
                <span class="oh-day">{{ OH_DAY_SHORT[block.dayOfWeek] ?? block.dayOfWeek }}</span>
                <span class="oh-time">{{ fmtTime(block.startTime) }} – {{ fmtTime(block.endTime) }}</span>
              </div>
            </div>
            <div v-else class="oh-empty">No office hours posted.</div>
          </div>
        </template>

        <div v-else-if="!loadingOH" class="empty-note">
          No office hours have been posted for this class yet.
        </div>
      </section>

    </template>
  </div>
</template>

<style scoped>
.wrap {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

h1 {
  margin: 0;
  font-size: 20px;
  color: #f3f4f6;
}

h2 {
  margin: 0;
  font-size: 16px;
  color: #e5e7eb;
}

.muted {
  margin: 4px 0 0;
  font-size: 13px;
  color: #9ca3af;
}

.class-select {
  font-size: 13px;
  padding: 8px 12px;
  border-radius: 12px;
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 700;
  outline: none;
  cursor: pointer;
  flex-shrink: 0;
}

.class-select:hover { background: #1d4ed8; }

.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 18px;
  color: #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.btn {
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 700;
  padding: 8px 14px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
  flex-shrink: 0;
}

.btn:hover { background: #1d4ed8; }

.create-form-wrap {
  padding: 14px;
  border-radius: 12px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.08);
}

.loading {
  font-size: 13px;
  color: #6b7280;
}

/* ── Pattern (weekly summary) ── */
.pattern-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pattern-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(37,99,235,0.1);
  border: 1px solid rgba(37,99,235,0.25);
  flex-wrap: wrap;
}

.pattern-days {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.day-chip {
  font-size: 12px;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 6px;
  background: rgba(37,99,235,0.25);
  border: 1px solid rgba(37,99,235,0.4);
  color: #bfdbfe;
}

.pattern-time {
  font-size: 14px;
  font-weight: 700;
  color: #e5e7eb;
}

.pattern-notes {
  font-size: 12px;
  color: #9ca3af;
  margin-left: auto;
}

/* ── Upcoming dates ── */
.upcoming-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
}

.upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.upcoming-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  font-size: 13px;
  flex-wrap: wrap;
}

.upcoming-date { font-weight: 600; color: #e5e7eb; }
.upcoming-time { color: #9ca3af; }
.upcoming-notes { color: #6b7280; font-size: 12px; margin-left: auto; }

/* ── Office hours ── */
.person-block {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.person-name {
  font-size: 14px;
  font-weight: 700;
  color: #f3f4f6;
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}

.badge-ta {
  background: rgba(251,191,36,0.12);
  color: #fbbf24;
  border: 1px solid rgba(251,191,36,0.25);
}

.badge-prof {
  background: rgba(74,222,128,0.12);
  color: #4ade80;
  border: 1px solid rgba(74,222,128,0.25);
}

.oh-schedule {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.oh-row {
  display: flex;
  gap: 12px;
  font-size: 13px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.oh-row:last-child { border-bottom: none; }

.oh-day {
  width: 36px;
  font-weight: 600;
  color: #93c5fd;
  flex-shrink: 0;
}

.oh-time { color: #d1d5db; }

.oh-empty {
  font-size: 13px;
  color: #6b7280;
  font-style: italic;
}

/* ── My Upcoming Meetings rows ── */
.my-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.05);
  font-size: 13px;
  flex-wrap: wrap;
}

.my-date {
  font-weight: 600;
  color: #e5e7eb;
  min-width: 80px;
}

.my-time { color: #9ca3af; }

.my-code {
  font-weight: 700;
  color: #93c5fd;
  font-size: 12px;
}

.my-type {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
}

.type-class {
  background: rgba(37,99,235,0.2);
  border: 1px solid rgba(37,99,235,0.4);
  color: #93c5fd;
}

.type-meeting {
  background: rgba(139,92,246,0.15);
  border: 1px solid rgba(139,92,246,0.35);
  color: #c4b5fd;
}

.my-status {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
}

.status-confirmed {
  background: rgba(74,222,128,0.12);
  border: 1px solid rgba(74,222,128,0.3);
  color: #4ade80;
}

.status-pending {
  background: rgba(251,191,36,0.12);
  border: 1px solid rgba(251,191,36,0.3);
  color: #fbbf24;
}

.status-cancelled {
  background: rgba(248,113,113,0.12);
  border: 1px solid rgba(248,113,113,0.3);
  color: #f87171;
}

.my-notes {
  font-size: 12px;
  color: #6b7280;
  margin-left: auto;
}

.my-actions {
  display: flex;
  gap: 6px;
  margin-left: auto;
}

.btn-confirm,
.btn-decline {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: opacity 0.15s;
}

.btn-confirm:disabled,
.btn-decline:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-confirm {
  background: rgba(74,222,128,0.15);
  border: 1px solid rgba(74,222,128,0.35);
  color: #4ade80;
}

.btn-confirm:hover:not(:disabled) {
  background: rgba(74,222,128,0.28);
}

.btn-decline {
  background: rgba(248,113,113,0.12);
  border: 1px solid rgba(248,113,113,0.3);
  color: #f87171;
}

.btn-decline:hover:not(:disabled) {
  background: rgba(248,113,113,0.25);
}

/* ── Empty states ── */
.empty-card {
  padding: 24px;
  border-radius: 18px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.08);
  color: #6b7280;
  font-size: 13px;
  text-align: center;
}

.empty-note {
  font-size: 13px;
  color: #6b7280;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255,255,255,0.02);
  border: 1px dashed rgba(255,255,255,0.07);
}
</style>
