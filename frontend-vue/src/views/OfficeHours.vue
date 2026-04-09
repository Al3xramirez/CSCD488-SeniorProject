<template>
  <div class="card">
    <div class="card-header">
      <div>
        <h2>Request a Meeting</h2>
        <p class="muted">Fill out the form to request a meeting.</p>
      </div>
    </div>

    <div class="content">
      <form @submit.prevent="onSubmit" novalidate>
        <label class="field">
          Your name
          <input v-model="name" class="input" type="text" required placeholder="Full name" />
        </label>

        <label class="field">
          Email
          <input v-model="email" class="input" type="email" required placeholder="you@example.com" />
        </label>

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
          <span v-if="success" style="color:#86efac; margin-left:8px;">Request submitted.</span>
        </div>

        <ul v-if="errors.length" class="errors">
          <li v-for="(err, i) in errors" :key="i">{{ err }}</li>
        </ul>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const name = ref('')
const email = ref('')
const meetingWith = ref('Professor')
const otherPerson = ref('')
const date = ref('')
const start = ref('')
const end = ref('')
const notes = ref('')

const errors = ref([])
const success = ref(false)

const minDate = computed(() => {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
})

const canSubmit = computed(() => {
  if (!name.value || !email.value || !date.value || !start.value || !end.value) return false
  if (meetingWith.value === 'Other' && !otherPerson.value) return false
  if (start.value >= end.value) return false
  return true
})

function validate() {
  errors.value = []
  if (!name.value.trim()) errors.value.push('Name is required.')
  if (!email.value.trim()) errors.value.push('Email is required.')
  else if (!/^\S+@\S+\.\S+$/.test(email.value)) errors.value.push('Email is invalid.')
  if (!date.value) errors.value.push('Date is required.')
  if (!start.value) errors.value.push('Start time is required.')
  if (!end.value) errors.value.push('End time is required.')
  if (start.value && end.value && start.value >= end.value) errors.value.push('End time must be after start time.')
  if (meetingWith.value === 'Other' && !otherPerson.value.trim()) errors.value.push('Please provide the other person\'s name.')
  return errors.value.length === 0
}

async function createMeeting(meetingData) {
  try {
    const res = await fetch('/api/meetings/create-meeting', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(meetingData)
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || 'Failed to create meeting')
    }
    return await res.json()
  } catch (err) {
    throw err
  }
}

// --- server-side overlap helpers moved to module scope ---
const blockedMeetings = ref([])

async function loadMeetingsForDate(dateStr) {
  if (!dateStr) {
    blockedMeetings.value = []
    return
  }
  const startDate = dateStr + 'T00:00:00'
  const endDate = dateStr + 'T23:59:59'
  try {
    const res = await fetch(
      `/api/meetings?start=${encodeURIComponent(startDate)}&end=${encodeURIComponent(endDate)}`
    )
    if (!res.ok) {
      blockedMeetings.value = []
      return
    }
    const data = await res.json()
    blockedMeetings.value = data.map(m => ({
      start: new Date(m.meetingDate + 'T' + m.startTime),
      end: new Date(m.meetingDate + 'T' + m.endTime),
      raw: m
    }))
  } catch (e) {
    console.error('Error loading meetings:', e)
    blockedMeetings.value = []
  }
}

function timeRangeOverlaps(startTime, endTime, dateStr) {
  if (!startTime || !endTime || !dateStr) return false
  const s = new Date(dateStr + 'T' + startTime)
  const e = new Date(dateStr + 'T' + endTime)
  return blockedMeetings.value.some(b => !(e <= b.start || s >= b.end))
}

// keep blockedMeetings up to date
onMounted(() => loadMeetingsForDate(date.value))
watch(date, (nv) => loadMeetingsForDate(nv))

// single onSubmit that runs sync validation + server overlap check + create
async function onSubmit() {
  success.value = false
  errors.value = []

  if (!validate()) return

  // load meetings for selected date and check overlap
  await loadMeetingsForDate(date.value)
  if (timeRangeOverlaps(start.value, end.value, date.value)) {
    errors.value = ['The selected time overlaps an existing meeting. Please choose a different time.']
    return
  }

  const payload = {
    requester: name.value,
    email: email.value,
    meetingWith: meetingWith.value === 'Other' ? otherPerson.value : meetingWith.value,
    date: date.value,
    start: start.value,
    end: end.value,
    notes: notes.value
  }

  try {
    const data = await createMeeting(payload)
    console.log('Meeting created:', data)
    success.value = true
    errors.value = []

    // refresh blocked meetings for that date (optional)
    await loadMeetingsForDate(date.value)

    // clear form
    name.value = ''
    email.value = ''
    meetingWith.value = 'Professor'
    otherPerson.value = ''
    date.value = ''
    start.value = ''
    end.value = ''
    notes.value = ''
  } catch (err) {
    errors.value = [err.message || 'Failed to create meeting.']
    success.value = false
    console.error('Error creating meeting:', err)
  }
}
</script>

<style scoped>
.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 18px;
  color: #e5e7eb;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
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
}
</style>