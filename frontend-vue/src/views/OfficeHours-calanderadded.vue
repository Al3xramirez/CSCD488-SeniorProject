<script setup>
import { ref, computed, watch, onMounted } from "vue";
import CalendarGrid from "@/components/CalendarGrid.vue";

const events = ref([]);
const selectedDay = ref("");
const dayEvents = ref([]);

// fetch meetings from your API
async function loadEvents() {
  const res = await fetch("/api/meetings");
  const data = await res.json();

  // normalize
  events.value = data.map(e => ({
    id: e.id,
    title: e.meetingWith,
    start: `${e.meetingDate}T${e.startTime}`,
    end: `${e.meetingDate}T${e.endTime}`
  }));
}

// when user clicks a day
function handleDaySelect(day) {
  selectedDay.value = day;

  dayEvents.value = events.value.filter(e =>
    e.start.startsWith(day)
  );
}

loadEvents();

const name = ref('');
const email = ref('');
const meetingWith = ref('Professor');
const otherPerson = ref('');
const date = ref('');
const start = ref('');
const end = ref('');
const notes = ref('');

const errors = ref([]);
const success = ref(false);

const minDate = computed(() => {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
});

const canSubmit = computed(() => {
  if (!name.value || !email.value || !date.value || !start.value || !end.value) return false;
  if (meetingWith.value === 'Other' && !otherPerson.value) return false;
  if (start.value >= end.value) return false;
  return true;
});

function validate() {
  errors.value = [];
  if (!name.value.trim()) errors.value.push('Name is required.');
  if (!email.value.trim()) errors.value.push('Email is required.');
  else if (!/^\S+@\S+\.\S+$/.test(email.value)) errors.value.push('Email is invalid.');
  if (!date.value) errors.value.push('Date is required.');
  if (!start.value) errors.value.push('Start time is required.');
  if (!end.value) errors.value.push('End time is required.');
  if (start.value && end.value && start.value >= end.value) errors.value.push('End time must be after start time.');
  if (meetingWith.value === 'Other' && !otherPerson.value.trim()) errors.value.push('Please provide the other person\'s name.');
  return errors.value.length === 0;
}

async function createMeeting(meetingData) {
  try {
    const res = await fetch('/api/meetings/create-meeting', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(meetingData)
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'Failed to create meeting');
    }
    return await res.json();
  } catch (err) {
    throw err;
  }
}


const blockedMeetings = ref([]);

async function loadMeetingsForDate(dateStr) {
  if (!dateStr) {
    blockedMeetings.value = [];
    return;
  }
  const startDate = dateStr + 'T00:00:00';
  const endDate = dateStr + 'T23:59:59';
  try {
    const res = await fetch(
      `/api/meetings?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`
    );
    if (!res.ok) {
      blockedMeetings.value = [];
      return;
    }
    const data = await res.json();
    blockedMeetings.value = data.map(m => ({
      start: new Date(m.meetingDate + 'T' + m.startTime),
      end: new Date(m.meetingDate + 'T' + m.endTime),
      raw: m
    }));
  } catch (e) {
    console.error('Error loading meetings:', e);
    blockedMeetings.value = [];
  }
}

function timeRangeOverlaps(startTime, endTime, dateStr) {
  if (!startTime || !endTime || !dateStr) return false;
  const s = new Date(dateStr + 'T' + startTime);
  const e = new Date(dateStr + 'T' + endTime);
  return blockedMeetings.value.some(b => !(e <= b.start || s >= b.end));
}

// keep blockedMeetings up to date
onMounted(() => loadMeetingsForDate(date.value));
watch(date, (nv) => loadMeetingsForDate(nv));

// single onSubmit that runs sync validation + server overlap check + create
async function onSubmit() {
  success.value = false;
  errors.value = [];

  if (!validate()) return;

  // load meetings for selected date and check overlap
  await loadMeetingsForDate(date.value);
  if (timeRangeOverlaps(start.value, end.value, date.value)) {
    errors.value = ['The selected time overlaps an existing meeting. Please choose a different time.'];
    return;
  }

  const payload = {
    requester: name.value,
    email: email.value,
    meetingWith: meetingWith.value === 'Other' ? otherPerson.value : meetingWith.value,
    date: date.value,
    start: start.value,
    end: end.value,
    notes: notes.value
  };

  try {
    const data = await createMeeting(payload);
    console.log('Meeting created:', data);
    success.value = true;
    errors.value = [];

    // refresh blocked meetings for that date (optional)
    await loadMeetingsForDate(date.value);

    // clear form
    name.value = '';
    email.value = '';
    meetingWith.value = 'Professor';
    otherPerson.value = '';
    date.value = '';
    start.value = '';
    end.value = '';
    notes.value = '';
  } catch (err) {
    errors.value = [err.message || 'Failed to create meeting.'];
    success.value = false;
    console.error('Error creating meeting:', err);
  }
}
</script>

<template>
  <div class="wrap">
    <h2>Office Hours</h2>

    <CalendarGrid
      :events="events"
      @select-day="handleDaySelect"
    />

    <div class="agenda">
      <h3>{{ selectedDay || "Select a day" }}</h3>

      <div v-if="dayEvents.length === 0">
        No meetings
      </div>

      <div v-for="e in dayEvents" :key="e.id" class="evt">
        <strong>{{ e.title }}</strong><br />
        {{ e.start.slice(11,16) }} - {{ e.end.slice(11,16) }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  padding: 16px;
  color: white;
}

.agenda {
  margin-top: 16px;
}

.evt {
  margin-top: 8px;
  padding: 8px;
  border-radius: 10px;
  background: rgba(255,255,255,0.05);
}
</style>