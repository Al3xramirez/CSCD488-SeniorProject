<template>
  <form @submit.prevent="submitRecurringMeetings">
    <div>
      <label>Start Date</label>
      <input type="date" v-model="startDate" required />
    </div>

    <div>
      <label>End Date</label>
      <input type="date" v-model="endDate" required />
    </div>

    <div>
      <label>Start Time</label>
      <input type="time" v-model="startTime" required />
    </div>

    <div>
      <label>End Time</label>
      <input type="time" v-model="endTime" required />
    </div>

    <div>
      <label>Class Code</label>
      <input type="text" v-model="classCode" required />
    </div>

    <div>
      <p>Days of Week</p>
      <div v-for="day in weekdays" :key="day.value">
        <label>
          <input
            type="checkbox"
            :value="day.value"
            v-model="selectedDays"
          />
          {{ day.label }}
        </label>
      </div>
    </div>

    <button type="submit">Create Recurring Meetings</button>

    <p v-if="message">{{ message }}</p>
  </form>
</template>

<script setup>
import { ref } from 'vue';

const startDate = ref('');
const endDate = ref('');
const startTime = ref('');
const endTime = ref('');
const classCode = ref('');
const selectedDays = ref([]);
const message = ref('');

const weekdays = [
  { label: 'Monday', value: 'MONDAY' },
  { label: 'Tuesday', value: 'TUESDAY' },
  { label: 'Wednesday', value: 'WEDNESDAY' },
  { label: 'Thursday', value: 'THURSDAY' },
  { label: 'Friday', value: 'FRIDAY' },
  { label: 'Saturday', value: 'SATURDAY' },
  { label: 'Sunday', value: 'SUNDAY' },
];

const submitRecurringMeetings = async () => {
  message.value = '';

  const payload = {
    startDate: startDate.value,
    endDate: endDate.value,
    startTime: startTime.value,
    endTime: endTime.value,
    classCode: classCode.value,
    daysOfWeek: selectedDays.value,
  };

  try {
    const response = await fetch('/api/meetings/recurring', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to create meetings');
    }

    message.value = 'Recurring meetings created';
  } catch (error) {
    message.value = error.message;
  }
};
</script>