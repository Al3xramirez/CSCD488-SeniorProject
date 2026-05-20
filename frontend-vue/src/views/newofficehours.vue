<script setup>
import { ref, computed, watch, onMounted } from "vue";
import CalendarGrid from "../components/CalendarGrid.vue";

const events = ref([]);
const selectedDay = ref("");
const dayEvents = ref([]);

const currentUser = ref("");
const currentUserEmail = ref("");
const userClasses = ref([]);
const selectedClass = ref("");
const classRecipients = ref([]);

const meetingWith = ref("");
const date = ref('');
const start = ref('');
const end = ref('');
const notes = ref('');

const errors = ref([]);
const success = ref(false);
const showSuccessToast = ref(false);
const showErrorToast = ref(false);

const minDate = computed(() => {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
});

const currentUserNormalized = computed(() => currentUserEmail.value.trim().toLowerCase());

const canSubmit = computed(() => {
  if (!selectedClass.value || !date.value || !start.value || !end.value) return false;
  if (!meetingWith.value) return false;
  if (start.value >= end.value) return false;
  return true;
});

function toYmd(d) {
  if (typeof d === "string") return d;
  return d.toISOString().split("T")[0];
}

function getOtherPerson(meeting) {
  if (meeting.requesterID?.toLowerCase() === currentUserNormalized.value) {
    return meeting.recipientID;
  } else {
    return meeting.requesterID;
  }
}

function getRole(meeting) {
  if (meeting.requesterID?.toLowerCase() === currentUserNormalized.value) {
    return "To: " + meeting.recipientID;
  } else {
    return "From: " + meeting.requesterID;
  }
}

function truncateNotes(notes, maxLength = 60) {
  if (!notes) return "";
  if (notes.length <= maxLength) return notes;
  return notes.substring(0, maxLength) + "...";
}

async function loadCurrentUser() {
  try {
    const res = await fetch("/api/auth/current-user", {
      method: "GET",
      credentials: "include"
    });
    if (res.ok) {
      const data = await res.json();
      currentUserEmail.value = data.currentUser || "";
    }
  } catch (err) {
    console.error("Error loading current user:", err);
  }
}

async function loadUserClasses() {
  if (!currentUserEmail.value) return;
  try {
    const res = await fetch("/api/classes/user-classes", {
      method: "GET",
      credentials: "include"
    });
    if (res.ok) {
      const data = await res.json();
      userClasses.value = data || [];
    }
  } catch (err) {
    console.error("Error loading user classes:", err);
  }
}

async function loadClassRecipients(classCode) {
  if (!classCode) {
    classRecipients.value = [];
    return;
  }
  try {
    const res = await fetch(`/api/classes/${classCode}/members`, {
      method: "GET",
      credentials: "include"
    });
    if (res.ok) {
      const data = await res.json();
      classRecipients.value = (data || []).filter(
        member => member.email?.toLowerCase() !== currentUserNormalized.value
      );
    } else {
      classRecipients.value = [];
    }
  } catch (err) {
    console.error("Error loading class recipients:", err);
    classRecipients.value = [];
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
    console.log("Raw meetings from API:", data);
    
    // Create a cache for user details to avoid duplicate API calls
    const userCache = new Map();
    
    const getUserName = async (email) => {
      if (userCache.has(email)) {
        return userCache.get(email);
      }
      try {
        const userRes = await fetch(`/api/users/by-email/${encodeURIComponent(email)}`, { credentials: "include" });
        if (userRes.ok) {
          const user = await userRes.json();
          const name = `${user.firstName} ${user.lastName}`;
          userCache.set(email, name);
          console.log(`Cached user: ${email} -> ${name}`);
          return name;
        } else {
          console.error(`User lookup failed for ${email}: ${userRes.status}`);
        }
      } catch (err) {
        console.error("Error loading user:", err);
      }
      return email; // Fallback to email if lookup fails
    };
    
    events.value = await Promise.all(
      data
        .filter(e => {
          const isRequester = e.requesterID?.toLowerCase() === currentUserNormalized.value;
          const isRecipient = e.recipientID?.toLowerCase() === currentUserNormalized.value;
          console.log(`Meeting ${e.meetingID}: requester=${e.requesterID}, recipient=${e.recipientID}, isRequester=${isRequester}, isRecipient=${isRecipient}`);
          return isRequester || isRecipient;
        })
        .map(async (e) => {
          // Get the name of the other person
          const otherEmail = e.requesterID?.toLowerCase() === currentUserNormalized.value 
            ? e.recipientID 
            : e.requesterID;
          const otherName = await getUserName(otherEmail);
          
          // Determine role
          const role = e.requesterID?.toLowerCase() === currentUserNormalized.value 
            ? `To: ${otherName}` 
            : `From: ${otherName}`;
          
          return {
            id: e.meetingID,
            requesterID: e.requesterID,
            recipientID: e.recipientID,
            otherPerson: otherName,
            role: role,
            start: `${e.meetingDate}T${e.startTime}`,
            end: `${e.meetingDate}T${e.endTime}`,
            date: e.meetingDate,
            startTime: e.startTime,
            endTime: e.endTime,
            classCode: e.classCode,
            notes: e.notes
          };
        })
    );

    console.log("Filtered events for current user:", events.value);
  } catch (err) {
    console.error("Error loading events:", err);
    events.value = [];
  }
}

async function getMeetingsForUserOnDate(userEmail, dateStr) {
  try {
    const res = await fetch(
      `/api/meetings/user/${encodeURIComponent(userEmail)}/date/${dateStr}`,
      { credentials: "include" }
    );
    if (!res.ok) return [];
    return await res.json();
  } catch (e) {
    console.error("Error loading meetings:", e);
    return [];
  }
}

function timeToMinutes(timeStr) {
  const [hours, minutes] = timeStr.split(":").map(Number); 
  return (hours * 60) + minutes; } 
function hasTimeConflict(requestedStart, requestedEnd, meetings) { 
  const reqStart = timeToMinutes(requestedStart); 
  const reqEnd = timeToMinutes(requestedEnd); 
  return meetings.some(m => { 
      const meetStart = timeToMinutes(m.startTime); 
      const meetEnd = timeToMinutes(m.endTime); // overlap check 
      
      return reqStart < meetEnd && reqEnd > meetStart; }
); }

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
  loadUserClasses();
});

watch(selectedClass, (newClassId) => {
  meetingWith.value = "";
  if (newClassId) {
    loadClassRecipients(newClassId);
  } else {
    classRecipients.value = [];
  }
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
  if (!selectedClass.value) errors.value.push('Please select a class.');
  if (!meetingWith.value) errors.value.push('Please select a recipient.');
  if (!date.value) errors.value.push('Date is required.');
  if (!start.value) errors.value.push('Start time is required.');
  if (!end.value) errors.value.push('End time is required.');
  if (start.value && end.value && start.value >= end.value) errors.value.push('End time must be after start time.');
  return errors.value.length === 0;
}

async function onSubmit() {
  success.value = false;
  errors.value = [];
  showSuccessToast.value = false;
  showErrorToast.value = false;

  if (!validate()) {
    showErrorToast.value = true;
    return;
  }

  try {
    // Get current user's meetings for the selected date
    const currentUserMeetings = await getMeetingsForUserOnDate(currentUserEmail.value, date.value);
    
    // Get recipient's meetings for the selected date
    const recipientMeetings = await getMeetingsForUserOnDate(meetingWith.value, date.value);

    // Check for conflicts with current user's meetings
    if (hasTimeConflict(start.value, end.value, currentUserMeetings)) {
      errors.value = ["The selected time conflicts with your existing meeting."];
      showErrorToast.value = true;
      return;
    }

    // Check for conflicts with recipient's meetings
    if (hasTimeConflict(start.value, end.value, recipientMeetings)) {
      errors.value = ["The selected time conflicts with the recipient's schedule."];
      showErrorToast.value = true;
      return;
    }

    // Find the selected class details
    const selectedClassObj = userClasses.value.find(c => c.classID === selectedClass.value);
    if (!selectedClassObj) {
      errors.value = ["Selected class not found"];
      showErrorToast.value = true;
      return;
    }

    const payload = {
      classCode: selectedClass.value,
      quarter: selectedClassObj.quarter,
      year: selectedClassObj.year,
      requesterId: currentUserEmail.value,
      recipientId: meetingWith.value,
      meetingDate: date.value,
      startTime: start.value,
      endTime: end.value,
      status: "PENDING",
      notes: notes.value
    };

    const data = await createMeeting(payload);
    console.log("Meeting created:", data);
    
    const meeting = { 
          classCode: selectedClass.value, 
          quarter: selectedClassObj.quarter,
          year: selectedClassObj.year,
          requesterId: currentUserEmail.value,  
          meetingDate: date.value,
          startTime: start.value,
          endTime: end.value,  
          notes: notes.value};
    
    const res = await fetch(`/api/emails/send-meeting-notification?recipientUserId=${meetingWith.value}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(meeting)
    })
    if (!res.ok) {
      console.error("Failed to send email notification:", await res.text());
    } else {
      console.log("Email notification sent successfully");
    }
    success.value = true;
    errors.value = [];
    showSuccessToast.value = true;

    // Reset form
    selectedClass.value = "";
    meetingWith.value = "";
    date.value = "";
    start.value = "";
    end.value = "";
    notes.value = "";
    classRecipients.value = [];

    // Refresh events
    await loadEvents();
    
    if (selectedDay.value) {
      //await loadMeetingsForDate(selectedDay.value);
      dayEvents.value = events.value.filter(e => e.start.startsWith(selectedDay.value));
    }

    setTimeout(() => {
      showSuccessToast.value = false;
    }, 3000);
  } catch (err) {
    errors.value = [err.message || "Failed to create meeting."];
    showErrorToast.value = true;
    success.value = false;
    console.error("Error creating meeting:", err);
  }
}

onMounted(async () => {
  await loadCurrentUser();
  await loadEvents();
  await loadUserClasses();
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

    <!-- Error Toast Notification -->
    <Transition name="slide-fade">
      <div v-if="showErrorToast && errors.length > 0" class="error-toast">
        <div class="toast-content">
          <svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>{{ errors[0] }}</span>
        </div>
        <button @click="showErrorToast = false" class="toast-close">×</button>
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
            <strong class="meeting-with">{{ e.otherPerson }}</strong>
            <span class="meeting-role">{{ e.role }}</span>
          </div>
          <div class="meeting-details">
            <div class="meeting-time">
              🕐 {{ e.startTime }} - {{ e.endTime }}
            </div>
            <div class="meeting-class">
              📚 {{ e.classCode }}
            </div>
            <div v-if="e.notes" class="meeting-notes">
              📝 {{ truncateNotes(e.notes) }}
            </div>
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
            Class
            <select v-model="selectedClass" class="input">
              <option value="">-- Select a class --</option>
              <option v-for="cls in userClasses" :key="cls.classID" :value="cls.classID">
                {{ cls.courseCode }} - {{ cls.className }}
              </option>
            </select>
          </label>

          <label class="field" v-if="selectedClass">
            Recipient
            <select v-model="meetingWith" class="input">
              <option value="">-- Select a person --</option>
              <option v-for="person in classRecipients" :key="person.userID" :value="person.email">
                {{ person.name }} ({{ person.role }})
              </option>
            </select>
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

.error-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.95), rgba(220, 38, 38, 0.95));
  border: 1px solid rgba(239, 68, 68, 0.5);
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

.error-toast .toast-content {
  color: #fee2e2;
}

.toast-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: #86efac;
  stroke-width: 3;
}

.error-toast .toast-icon {
  color: #fca5a5;
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

.error-toast .toast-close {
  color: #fca5a5;
}

.error-toast .toast-close:hover {
  color: #fee2e2;
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

.meeting-role {
  font-size: 12px;
  color: #9ca3af;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 8px;
  border-radius: 6px;
}

.meeting-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.meeting-time {
  color: #d1d5db;
  font-weight: 500;
}

.meeting-class {
  color: #9ca3af;
}

.meeting-notes {
  background: rgba(255, 255, 255, 0.05);
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #c7d2e0;
  border-left: 2px solid rgba(37, 99, 235, 0.5);
  line-height: 1.4;
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

.btn:disabled {
  background: #6b7280;
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 980px) {
  .row { flex-direction: column; align-items: stretch; }

  .success-toast,
  .error-toast {
    top: auto;
    bottom: 20px;
    right: 20px;
    left: 20px;
    max-width: none;
  }
}
</style>