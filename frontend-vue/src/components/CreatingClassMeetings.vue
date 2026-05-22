<template>
  <form @submit.prevent="submitRecurringMeetings">

    <div v-if="!classCode" class="field">
      <label class="field-label">Class Code</label>
      <input class="input" type="text" v-model="localClassCode" required placeholder="e.g. CSCD488" />
    </div>

    <div class="row">
      <div class="field">
        <label class="field-label">Start Date</label>
        <input class="input" type="date" v-model="startDate" required />
      </div>
      <div class="field">
        <label class="field-label">End Date</label>
        <input class="input" type="date" v-model="endDate" required />
      </div>
    </div>

    <div class="row">
      <div class="field">
        <label class="field-label">Start Time</label>
        <input class="input" type="time" v-model="startTime" required />
      </div>
      <div class="field">
        <label class="field-label">End Time</label>
        <input class="input" type="time" v-model="endTime" required />
      </div>
    </div>

    <div class="field">
      <label class="field-label">Days of Week</label>
      <div class="day-picker">
        <label
          v-for="day in weekdays"
          :key="day.value"
          class="day-chip"
          :class="{ 'day-chip--active': selectedDays.includes(day.value) }"
        >
          <input type="checkbox" :value="day.value" v-model="selectedDays" hidden />
          {{ day.label }}
        </label>
      </div>
    </div>

    <div class="field">
      <label class="field-label">Notes (optional)</label>
      <input class="input" type="text" v-model="notes" placeholder="e.g. Lecture — Room 101" />
    </div>

    <button class="btn" type="submit" :disabled="submitting || !canSubmit">
      <span v-if="submitting" class="spinner" />
      <span v-else>Create Recurring Meetings</span>
    </button>

    <p v-if="message" class="msg" :class="{ 'msg--error': isError }">{{ message }}</p>
  </form>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  classCode: { type: String, default: '' },
  quarter:   { type: String, default: '' },
  year:      { type: Number, default: null }
});

const emit = defineEmits(['meetings-created']);

const localClassCode = ref('');
const startDate      = ref('');
const endDate        = ref('');
const startTime      = ref('');
const endTime        = ref('');
const selectedDays   = ref([]);
const notes          = ref('');
const submitting     = ref(false);
const message        = ref('');
const isError        = ref(false);

const weekdays = [
  { label: 'Mon', value: 'MONDAY' },
  { label: 'Tue', value: 'TUESDAY' },
  { label: 'Wed', value: 'WEDNESDAY' },
  { label: 'Thu', value: 'THURSDAY' },
  { label: 'Fri', value: 'FRIDAY' },
  { label: 'Sat', value: 'SATURDAY' },
  { label: 'Sun', value: 'SUNDAY' },
];

const effectiveClassCode = computed(() => props.classCode || localClassCode.value);

const canSubmit = computed(() =>
  effectiveClassCode.value &&
  startDate.value &&
  endDate.value &&
  startTime.value &&
  endTime.value &&
  selectedDays.value.length > 0
);

async function submitRecurringMeetings() {
  message.value  = '';
  isError.value  = false;
  submitting.value = true;

  const payload = {
    startDate:  startDate.value,
    endDate:    endDate.value,
    startTime:  startTime.value,
    endTime:    endTime.value,
    classCode:  effectiveClassCode.value,
    quarter:    props.quarter || undefined,
    year:       props.year    || undefined,
    daysOfWeek: selectedDays.value,
    notes:      notes.value || undefined,
  };

  try {
    const res = await fetch('/api/meetings/recurring', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) throw new Error(await res.text() || 'Failed to create meetings');

    message.value = 'Recurring meetings created successfully.';
    // Reset form
    startDate.value    = '';
    endDate.value      = '';
    startTime.value    = '';
    endTime.value      = '';
    selectedDays.value = [];
    notes.value        = '';
    localClassCode.value = '';

    emit('meetings-created');
  } catch (err) {
    isError.value = true;
    message.value = err.message || 'Something went wrong.';
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row {
  display: flex;
  gap: 10px;
}

.row .field { flex: 1; }

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #6b7280;
}

.input {
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(15,23,42,0.6);
  color: #e5e7eb;
  outline: none;
  font-size: 13px;
  font-family: inherit;
  width: 100%;
  box-sizing: border-box;
}

.input:focus {
  border-color: rgba(37,99,235,0.6);
  box-shadow: 0 0 0 2px rgba(37,99,235,0.25);
}

.day-picker {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.day-chip {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 7px;
  cursor: pointer;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(255,255,255,0.03);
  color: #6b7280;
  user-select: none;
  transition: all 0.12s;
}

.day-chip--active {
  background: rgba(37,99,235,0.2);
  border-color: rgba(37,99,235,0.5);
  color: #93c5fd;
}

.btn {
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 700;
  padding: 10px 14px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn:hover:not(:disabled) { background: #1d4ed8; }

.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.spinner {
  width: 13px;
  height: 13px;
  border: 2px solid rgba(255,255,255,0.2);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.msg {
  font-size: 13px;
  color: #4ade80;
  margin: 0;
}

.msg--error { color: #f87171; }

@media (max-width: 480px) {
  .row { flex-direction: column; }
}
</style>
