<script setup>
import { ref, computed, watch, onMounted } from "vue";
import CalendarGrid from "../components/CalendarGrid.vue";

const events = ref([]);
const selectedDay = ref("");
const dayEvents = ref([]);

const currentUser = ref("");
const currentUserEmail = ref("");
const meetingWith = ref('Professor');
const otherPerson = ref('');
const date = ref('');
const start = ref('');
const end = ref('');
const notes = ref('');

const errors = ref([]);
const success = ref(false);
const showSuccessToast = ref(false);
const blockedMeetings = ref([]);

const minDate = computed(() => {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
});

const currentUserNormalized = computed(() => currentUserEmail.value.trim().toLowerCase());

const canSubmit = computed(() => {
  if (!date.value || !start.value || !end.value) return false;
  if (meetingWith.value === 'Other' && !otherPerson.value) return false;
  if (start.value >= end.value) return false;
  return true;
});

function toYmd(d) {
  if (typeof d === "string") return d;
  return d.toISOString().split("T")[0];
}

function matchesCurrentUser(value) {
  if (!currentUserNormalized.value) return false;
  return String(value || '').trim().toLowerCase() === currentUserNormalized.value;
}

function isRelatedEvent(event) {
  return matchesCurrentUser(event.requester) || matchesCurrentUser(event.title);
}

function isRelatedMeeting(meeting) {
  return matchesCurrentUser(meeting.requesterID || meeting.requester) ||
         matchesCurrentUser(meeting.recipientID || meeting.title);
}

async function loadCurrentUser() {
  try {
    const res = await fetch("/api/session/current-user", {
      method: "GET",
      credentials: "include"
    });
    if (!res.ok) {
      currentUser.value = "";
      currentUserEmail.value = "";
      return;
    }
    const data = await res.json();
    currentUser.value = data.currentUser || "";
    currentUserEmail.value = data.currentUser || "";
  } catch (err) {
    console.error("Error loading current user:", err);
    currentUser.value = "";
    currentUserEmail.value = "";
  }
}

async function loadEvents() {
  try {
    const res = await fetch("/api/meetings", { credentials: "include" });
    if (!res.ok) {
      events.value = [];
      return;
    }
    const data = await res.json();
    events.value = data.map(e => ({
      id: e.meetingID,
      title: e.recipientID,
      requester: e.requesterID,
      start: `${e.meetingDate}T${e.startTime}`,
      end: `${e.meetingDate}T${e.endTime}`,
      date: e.meetingDate,
      startTime: e.startTime,
      endTime: e.endTime
    }));

    console.log("Events loaded:", events.value);
  } catch (err) {
    console.error("Error loading events:", err);
    events.value = [];
  }
}

async function loadMeetingsForDate(dateStr) {
  if (!dateStr || !currentUserNormalized.value) {
    blockedMeetings.value = [];
    return;
  }
  const startDate = dateStr + "T00:00:00";
  const endDate = dateStr + "T23:59:59";

  try {
    const res = await fetch(
      `/api/meetings?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`,
      { credentials: "include" }
    );
    if (!res.ok) {
      blockedMeetings.value = [];
      return;
    }
    const data = await res.json();
    blockedMeetings.value = data
      .filter(isRelatedMeeting)
      .map(m => ({
        start: new Date(m.meetingDate + "T" + m.startTime),
        end: new Date(m.meetingDate + "T" + m.endTime),
        raw: m
      }));
  } catch (e) {
    console.error("Error loading meetings:", e);
    blockedMeetings.value = [];
  }
}

function timeRangeOverlaps(startTime, endTime, dateStr) {
  if (!startTime || !endTime || !dateStr) return false;
  const s = new Date(dateStr + "T" + startTime);
  const e = new Date(dateStr + "T" + endTime);
  return blockedMeetings.value.some(b => !(e <= b.start || s >= b.end));
}

function handleDaySelect(day) {
  const selected = toYmd(day);
  selectedDay.value = selected;
  date.value = selected;
  dayEvents.value = events.value.filter(e => e.start.startsWith(selected));
}

watch([events, selectedDay], ([eventsVal, selected]) => {
  if (selected) {
    dayEvents.value = eventsVal.filter(e => e.start.startsWith(selected));
  }
});

watch(currentUserEmail, () => {
  loadEvents();
  if (date.value) loadMeetingsForDate(date.value);
});

async function createMeeting(meetingData) {
  const res = await fetch("/api/meetings/create-meeting", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(meetingData)
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Failed to create meeting");
  }
  return await res.json();
}

function validate() {
  errors.value = [];
  if (!date.value) errors.value.push('Date is required.');
  if (!start.value) errors.value.push('Start time is required.');
  if (!end.value) errors.value.push('End time is required.');
  if (start.value && end.value && start.value >= end.value) errors.value.push('End time must be after start time.');
  if (meetingWith.value === 'Other' && !otherPerson.value.trim()) errors.value.push('Please provide the other person\'s name.');
  return errors.value.length === 0;
}

async function onSubmit() {
  success.value = false;
  errors.value = [];
  showSuccessToast.value = false;

  if (!validate()) return;

  await loadMeetingsForDate(date.value);
  if (timeRangeOverlaps(start.value, end.value, date.value)) {
    errors.value = ["The selected time overlaps an existing meeting. Please choose a different time."];
    return;
  }

  const payload = {
    recipientID: meetingWith.value === "Other" ? otherPerson.value : meetingWith.value,
    date: date.value,
    start: start.value,
    end: end.value,
    notes: notes.value
  };

  try {
    const data = await createMeeting(payload);
    console.log("Meeting created:", data);
    success.value = true;
    errors.value = [];
    showSuccessToast.value = true;

    // Reset form
    meetingWith.value = "Professor";
    otherPerson.value = "";
    date.value = "";
    start.value = "";
    end.value = "";
    notes.value = "";

    // Refresh events and reload calendar
    await loadEvents();
    
    // If a day was selected, reload meetings for that day and update agenda
    if (selectedDay.value) {
      await loadMeetingsForDate(selectedDay.value);
      dayEvents.value = events.value.filter(e => e.start.startsWith(selectedDay.value));
    }

    // Hide toast after 3 seconds
    setTimeout(() => {
      showSuccessToast.value = false;
    }, 3000);
  } catch (err) {
    errors.value = [err.message || "Failed to create meeting."];
    success.value = false;
    console.error("Error creating meeting:", err);
  }
}

onMounted(async () => {
  await loadCurrentUser();
  await loadEvents();
  if (date.value) await loadMeetingsForDate(date.value);
});
</script>

<template>
  <div class="wrap">
    <h2>Office Hours</h2>

    <div class="user-info" v-if="currentUserEmail">
      <p>Logged in as: <strong>{{ currentUserEmail }}</strong></p>
    </div>

    <!-- Success Toast Notification -->
    <Transition name="slide-fade">
      <div v-if="showSuccessToast" class="success-toast">
        <div class="toast-content">
          <svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <span>Meeting request submitted successfully!</span>
        </div>
        <button @click="showSuccessToast = false" class="toast-close">×</button>
      </div>
    </Transition>

    <CalendarGrid :events="events" @select-day="handleDaySelect" />

    <div class="agenda">
      <h3>{{ selectedDay || "Select a day" }}</h3>

      <div v-if="dayEvents.length === 0" class="no-meetings">
        No meetings scheduled
      </div>

      <div v-else class="meetings-list">
        <div v-for="e in dayEvents" :key="e.id" class="meeting-bubble">
          <div class="meeting-header">
            <strong class="meeting-with">{{ e.title }}</strong>
            <span class="meeting-requester">{{ e.requester }}</span>
          </div>
          <div class="meeting-time">
            {{ e.startTime }} - {{ e.endTime }}
          </div>
        </div>
      </div>
    </div>

    <div class="card form-card">
      <div class="card-header">
        <div>
          <h2>Request a Meeting</h2>
          <p class="muted">Fill out the form to request a meeting.</p>
        </div>
      </div>

      <div class="content">
        <form @submit.prevent="onSubmit" novalidate>
          <label class="field">
            Meeting with
            <select v-model="meetingWith" class="input">
              <option>Professor</option>
              <option>TA</option>
              <option>Other</option>
            </select>
          </label>

          <label v-if="meetingWith === 'Other'" class="field">
            Other (name)
            <input v-model="otherPerson" class="input" type="text" placeholder="Person's name" />
          </label>

          <div class="row">
            <label class="field" style="flex:1;">
              Date
              <input v-model="date" class="input" type="date" required :min="minDate" />
            </label>
            <label class="field">
              Start
              <input v-model="start" class="input" type="time" required />
            </label>
            <label class="field">
              End
              <input v-model="end" class="input" type="time" required />
            </label>
          </div>

          <label class="field">
            Notes (optional)
            <textarea v-model="notes" class="input" rows="3" placeholder="Agenda or questions"></textarea>
          </label>

          <div class="row" style="align-items:center; margin-top:12px;">
            <button type="submit" :disabled="!canSubmit" class="btn" style="padding:8px 12px; border-radius:8px;">
              Request Meeting
            </button>
          </div>

          <ul v-if="errors.length" class="errors">
            <li v-for="(err, i) in errors" :key="i">{{ err }}</li>
          </ul>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  padding: 16px;
  color: white;
}

.user-info {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 8px;
  font-size: 14px;
  color: #93c5fd;
}

.user-info p {
  margin: 0;
}

.user-info strong {
  color: #60a5fa;
}

/* Success Toast Styles */
.success-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.95), rgba(22, 163, 74, 0.95));
  border: 1px solid rgba(34, 197, 94, 0.5);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  max-width: 400px;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #dcfce7;
  font-weight: 500;
  font-size: 14px;
}

.toast-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: #86efac;
  stroke-width: 3;
}

.toast-close {
  background: none;
  border: none;
  color: #86efac;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.toast-close:hover {
  color: #dcfce7;
}

/* Slide-fade transition */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  transform: translateX(400px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(400px);
  opacity: 0;
}

.agenda {
  margin-top: 16px;
  padding: 16px;
  background: rgba(255,255,255,0.02);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.05);
}

.agenda h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #e5e7eb;
}

.no-meetings {
  color: #9ca3af;
  font-style: italic;
}

.meetings-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meeting-bubble {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.15), rgba(59, 130, 246, 0.1));
  border: 1.5px solid rgba(37, 99, 235, 0.4);
  border-radius: 12px;
  padding: 12px 16px;
  transition: all 0.2s ease;
}

.meeting-bubble:hover {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.25), rgba(59, 130, 246, 0.2));
  border-color: rgba(37, 99, 235, 0.6);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.meeting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.meeting-with {
  font-size: 15px;
  color: #60a5fa;
}

.meeting-requester {
  font-size: 13px;
  color: #9ca3af;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 8px;
  border-radius: 6px;
}

.meeting-time {
  font-size: 13px;
  color: #d1d5db;
  font-weight: 500;
}

.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 18px;
  color: #e5e7eb;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
  margin-top: 24px;
}

.card-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

h2 {
  margin: 0;
  font-size: 18px;
}

.muted {
  margin: 6px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.field { display:block; margin-top:8px; }
.input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(15,23,42,0.6);
  color: #e5e7eb;
  outline: none;
  margin-top:6px;
  box-sizing: border-box;
}

.input:focus {
  box-shadow: 0 0 0 2px rgba(37,99,235,0.5);
  border-color: rgba(37,99,235,0.6);
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

.errors {
  color: #fca5a5;
  margin-top: 8px;
  padding-left: 18px;
}

@media (max-width: 980px) {
  .row { flex-direction: column; align-items: stretch; }

  .success-toast {
    top: auto;
    bottom: 20px;
    right: 20px;
    left: 20px;
    max-width: none;
  }
}
</style>