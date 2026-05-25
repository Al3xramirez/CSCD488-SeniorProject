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
  return myMeetings.value.filter(m => {
    if (m.meetingDate < today) return false;
    const notes = (m.notes ?? '').toLowerCase();
    return !notes.includes('lecture') && !notes.includes('office hours');
  });
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

onMounted(async () => {
  fetchMyClasses();
  fetchMyMeetings();
  try {
    const r = await fetch('/api/auth/current-user', { credentials: 'include' });
    if (r.ok) currentUserEmail.value = (await r.json()).currentUser || '';
  } catch {}
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

// ── Week view: student meeting requests ────────────────────────────────────
const weekStart = ref(getMonday(new Date()));
const requestModal = ref({ show: false, personName: '', userId: '', date: '', startTime: '', endTime: '', notes: '' });
const requestLoading = ref(false);
const requestError = ref('');
const currentUserEmail = ref('');
const emailCache = {};

function getMonday(d) {
  const c = new Date(d); c.setHours(0, 0, 0, 0);
  const diff = c.getDay() === 0 ? -6 : 1 - c.getDay();
  c.setDate(c.getDate() + diff);
  return c;
}

function ymd(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const weekDays = computed(() =>
  Array.from({ length: 5 }, (_, i) => {
    const d = new Date(weekStart.value);
    d.setDate(d.getDate() + i);
    return {
      dateStr: ymd(d),
      label: d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' }),
    };
  })
);

const weekLabel = computed(() => {
  const s = weekDays.value[0].dateStr;
  const e = weekDays.value[4].dateStr;
  const fmt = str => new Date(str + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  return `${fmt(s)} – ${fmt(e)}, ${e.slice(0, 4)}`;
});

function prevWeek() { const d = new Date(weekStart.value); d.setDate(d.getDate() - 7); weekStart.value = d; }
function nextWeek() { const d = new Date(weekStart.value); d.setDate(d.getDate() + 7); weekStart.value = d; }

const DOW_TO_3 = { MONDAY: 'MON', TUESDAY: 'TUE', WEDNESDAY: 'WED', THURSDAY: 'THU', FRIDAY: 'FRI' };

const CAL_START = 6;
const PX_PER_HOUR = 60;
const calHours = Array.from({ length: 17 }, (_, i) => i + CAL_START); // 6 am – 10 pm

function timeToY(t) {
  if (!t) return 0;
  const [h, m] = t.split(':').map(Number);
  return (h + m / 60 - CAL_START) * PX_PER_HOUR;
}

function timeToDuration(s, e) {
  if (!s || !e) return PX_PER_HOUR;
  const [sh, sm] = s.split(':').map(Number);
  const [eh, em] = e.split(':').map(Number);
  return Math.max(((eh + em / 60) - (sh + sm / 60)) * PX_PER_HOUR, 22);
}

function calEventStyle(ev) {
  return { top: `${timeToY(ev.startTime)}px`, height: `${timeToDuration(ev.startTime, ev.endTime)}px` };
}

function eventsForDay(dateStr) {
  const dow = new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();
  const key = DOW_TO_3[dow];
  const out = [];

  for (const m of classMeetings.value) {
    if (m.meetingDate === dateStr)
      out.push({ type: 'class', startTime: m.startTime, endTime: m.endTime });
  }

  for (const person of officeHours.value) {
    for (const b of person.schedule ?? []) {
      if (b.dayOfWeek === key)
        out.push({ type: 'oh', startTime: b.startTime, endTime: b.endTime, personName: `${person.firstName} ${person.lastName}`, userId: person.userId, role: person.role, date: dateStr });
    }
  }

  for (const m of myMeetings.value) {
    if (m.meetingDate === dateStr && m.recipientId)
      out.push({ type: 'personal', startTime: m.startTime, endTime: m.endTime, status: m.status });
  }

  return out.sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''));
}

function openRequestModal(ev) {
  requestError.value = '';
  requestModal.value = { show: true, personName: ev.personName, userId: ev.userId, date: ev.date, startTime: ev.startTime, endTime: ev.endTime, notes: '' };
}

async function submitMeetingRequest() {
  requestLoading.value = true;
  requestError.value = '';
  try {
    const cls = selectedClass.value;
    if (!cls) throw new Error('No class selected');

    let recipientEmail = emailCache[requestModal.value.userId];
    if (!recipientEmail) {
      const r = await fetch(`/api/users/${requestModal.value.userId}`, { credentials: 'include' });
      if (!r.ok) throw new Error('Could not find recipient');
      recipientEmail = (await r.json()).email;
      emailCache[requestModal.value.userId] = recipientEmail;
    }

    const res = await fetch('/api/meetings/create-meeting', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        classCode: cls.classCode,
        quarter: cls.quarter,
        year: cls.year,
        requesterId: currentUserEmail.value,
        recipientId: recipientEmail,
        meetingDate: requestModal.value.date,
        startTime: requestModal.value.startTime,
        endTime: requestModal.value.endTime,
        status: 'PENDING',
        notes: requestModal.value.notes,
      }),
    });
    if (!res.ok) throw new Error(await res.text() || 'Failed to create meeting');
    requestModal.value.show = false;
    fetchMyMeetings();
  } catch (e) {
    requestError.value = e.message || 'Failed to send request';
  } finally {
    requestLoading.value = false;
  }
}
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

      <!-- ── Request a Meeting: week view (students only) ── -->
      <section v-if="!isProfessor" class="card">
        <div class="section-header">
          <h2>Request a Meeting</h2>
          <div class="week-nav">
            <button class="btn-ghost" @click="prevWeek">‹</button>
            <span class="week-label">{{ weekLabel }}</span>
            <button class="btn-ghost" @click="nextWeek">›</button>
          </div>
        </div>

        <div v-if="!officeHours.length && !loadingOH" class="empty-note">
          No office hours posted for this class yet — nothing to request a meeting with.
        </div>

        <template v-else>
          <div class="cal-wrap">
            <!-- Time labels -->
            <div class="cal-time-col">
              <div class="cal-hdr-spacer"></div>
              <div v-for="h in calHours" :key="h" class="cal-hour-label">
                {{ h === 12 ? '12 pm' : h < 12 ? `${h} am` : `${h - 12} pm` }}
              </div>
            </div>
            <!-- Day columns -->
            <div class="cal-days">
              <div v-for="day in weekDays" :key="day.dateStr" class="cal-day-col">
                <div class="cal-day-hdr">{{ day.label }}</div>
                <div class="cal-day-body">
                  <div v-for="h in calHours" :key="h" class="cal-hour-line"></div>
                  <template v-for="ev in eventsForDay(day.dateStr)" :key="`${day.dateStr}-${ev.type}-${ev.startTime}`">
                    <button
                      v-if="ev.type === 'oh'"
                      class="cal-event cal-event-oh"
                      :style="calEventStyle(ev)"
                      @click="openRequestModal(ev)"
                    >
                      <span class="cal-ev-time">{{ fmtTime(ev.startTime) }}</span>
                      <span class="cal-ev-name">{{ ev.personName }}</span>
                    </button>
                    <div v-else-if="ev.type === 'class'" class="cal-event cal-event-class" :style="calEventStyle(ev)">
                      <span class="cal-ev-time">{{ fmtTime(ev.startTime) }}</span>
                      <span class="cal-ev-name">Class</span>
                    </div>
                    <div v-else-if="ev.type === 'personal'" class="cal-event cal-event-personal" :style="calEventStyle(ev)">
                      <span class="cal-ev-time">{{ fmtTime(ev.startTime) }}</span>
                      <span class="cal-ev-name">My Meeting</span>
                      <span class="cal-ev-status" :class="`status-${(ev.status||'').toLowerCase()}`">{{ ev.status }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <div class="legend">
            <span class="leg leg-oh">Office Hours — click to request</span>
            <span class="leg leg-class">Class</span>
            <span class="leg leg-personal">My Meetings</span>
          </div>
        </template>
      </section>

    </template>
  </div>

  <!-- Request Meeting Modal -->
  <teleport to="body">
    <div v-if="requestModal.show" class="overlay" @click.self="requestModal.show = false">
      <div class="modal">
        <div class="modal-hdr">
          <h3>Request a Meeting</h3>
          <button class="close-btn" @click="requestModal.show = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="mfield">
            <label>With</label>
            <span class="mval">{{ requestModal.personName }}</span>
          </div>
          <div class="mfield">
            <label>Date</label>
            <input v-model="requestModal.date" type="date" class="input" />
          </div>
          <div class="mfield">
            <label>Start</label>
            <input v-model="requestModal.startTime" type="time" class="input" />
          </div>
          <div class="mfield">
            <label>End</label>
            <input v-model="requestModal.endTime" type="time" class="input" />
          </div>
          <div class="mfield mfield-col">
            <label>Notes</label>
            <textarea v-model="requestModal.notes" class="input" rows="3" placeholder="Optional agenda or questions" />
          </div>
          <div v-if="requestError" class="req-error">{{ requestError }}</div>
          <div class="modal-actions">
            <button class="btn-ghost" @click="requestModal.show = false">Cancel</button>
            <button
              class="btn"
              :disabled="requestLoading || !requestModal.date || !requestModal.startTime || !requestModal.endTime"
              @click="submitMeetingRequest"
            >{{ requestLoading ? 'Sending…' : 'Send Request' }}</button>
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

/* ── Week nav ── */
.week-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.week-label {
  font-size: 13px;
  font-weight: 600;
  color: #e5e7eb;
  min-width: 148px;
  text-align: center;
}

.btn-ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  color: #e5e7eb;
  border-radius: 8px;
  padding: 4px 11px;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.btn-ghost:hover { background: rgba(255,255,255,0.12); }

/* ── Calendar grid ── */
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

.cal-hdr-spacer {
  height: 36px;
  flex-shrink: 0;
}

.cal-hour-label {
  height: 60px;
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
  min-width: 380px;
}

.cal-day-col {
  border-left: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
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

.cal-day-body {
  position: relative;
}

.cal-hour-line {
  height: 60px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.cal-event {
  position: absolute;
  left: 2px;
  right: 2px;
  border-radius: 6px;
  padding: 3px 6px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
  text-align: left;
  z-index: 1;
  border: none;
}

.cal-event-oh {
  background: rgba(74,222,128,0.18);
  border: 1px solid rgba(74,222,128,0.45) !important;
  color: #4ade80;
  cursor: pointer;
}

.cal-event-oh:hover { background: rgba(74,222,128,0.3); }

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

.cal-ev-time {
  font-size: 10px;
  opacity: 0.75;
}

.cal-ev-name {
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cal-ev-status {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: 3px;
  align-self: flex-start;
}

/* ── Legend ── */
.legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  padding-top: 2px;
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
  display: inline-block;
}

.leg-oh::before {
  background: rgba(74,222,128,0.35);
  border: 1px solid rgba(74,222,128,0.6);
}

.leg-class::before {
  background: rgba(37,99,235,0.35);
  border: 1px solid rgba(37,99,235,0.6);
}

.leg-personal::before {
  background: rgba(139,92,246,0.3);
  border: 1px solid rgba(139,92,246,0.5);
}

/* ── Request meeting modal ── */
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

.modal-hdr h3 {
  margin: 0;
  font-size: 15px;
  color: #f3f4f6;
}

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

.mfield {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mfield label {
  width: 48px;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  flex-shrink: 0;
}

.mfield-col {
  flex-direction: column;
  align-items: flex-start;
}

.mfield-col label { width: auto; }

.mval {
  font-size: 13px;
  font-weight: 600;
  color: #e5e7eb;
}

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
</style>
