<script setup>
import { ref, computed } from "vue";

const props = defineProps({
  events: { type: Array, default: () => [] }
});

const emit = defineEmits(["select-day"]);

const monthCursor = ref(startOfMonth(new Date()));
const selectedDay = ref(localYmd(new Date()));

const monthLabel = computed(() =>
  monthCursor.value.toLocaleString(undefined, {
    month: "long",
    year: "numeric"
  })
);

const days = computed(() => buildMonthGrid(monthCursor.value));

const eventsByDay = computed(() => {
  const map = new Map();

  for (const e of props.events) {
    // Use the date field directly if available, otherwise parse from start
    let key;
    if (e.date) {
      key = e.date;
    } else if (e.start) {
      key = e.start.split("T")[0];
    } else {
      continue;
    }

    if (!map.has(key)) map.set(key, []);
    map.get(key).push(e);
  }

  return map;
});

function selectDay(d) {
  selectedDay.value = localYmd(d);
  emit("select-day", d);
}

function prevMonth() {
  monthCursor.value = new Date(
    monthCursor.value.getFullYear(),
    monthCursor.value.getMonth() - 1,
    1
  );
}

function nextMonth() {
  monthCursor.value = new Date(
    monthCursor.value.getFullYear(),
    monthCursor.value.getMonth() + 1,
    1
  );
}

// utils
function startOfMonth(d) {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function localYmd(d) {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function buildMonthGrid(monthStart) {
  const first = new Date(monthStart.getFullYear(), monthStart.getMonth(), 1);
  const offset = (first.getDay() + 6) % 7;

  const start = new Date(first);
  start.setDate(first.getDate() - offset);

  const out = [];
  for (let i = 0; i < 42; i++) {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    out.push(d);
  }

  return out;
}
</script>

<template>
  <div class="calendar">
    <div class="header">
      <button @click="prevMonth">←</button>
      <div class="title">{{ monthLabel }}</div>
      <button @click="nextMonth">→</button>
    </div>

    <div class="dow">
      <div v-for="d in ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']" :key="d">
        {{ d }}
      </div>
    </div>

    <div class="grid">
      <div
        v-for="d in days"
        :key="d.toISOString()"
        class="cell"
        :class="{
          other: d.getMonth() !== monthCursor.getMonth(),
          selected: localYmd(d) === selectedDay
        }"
        @click="selectDay(d)"
      >
        <div class="num">{{ d.getDate() }}</div>

        <div v-if="eventsByDay.get(localYmd(d))" class="events-container">
          <div
            v-for="e in eventsByDay.get(localYmd(d)).slice(0, 2)"
            :key="e.id"
            class="event-bubble"
            :title="`${e.title} - ${e.startTime}`"
          >
            <span class="event-label">{{ e.title }}</span>
          </div>
          <div v-if="eventsByDay.get(localYmd(d)).length > 2" class="event-overflow">
            +{{ eventsByDay.get(localYmd(d)).length - 2 }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.calendar {
  padding: 12px;
}

.header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.header button {
  background: rgba(37, 99, 235, 0.2);
  border: 1px solid rgba(37, 99, 235, 0.4);
  color: white;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.header button:hover {
  background: rgba(37, 99, 235, 0.4);
}

.title {
  font-weight: bold;
  color: #e5e7eb;
}

.dow {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 6px;
  color: #9ca3af;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cell {
  min-height: 90px;
  border-radius: 10px;
  padding: 8px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cell:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.15);
}

.cell.other {
  opacity: 0.3;
}

.cell.selected {
  background: rgba(37,99,235,0.3);
  border-color: rgba(37,99,235,0.6);
}

.num {
  font-weight: bold;
  color: #e5e7eb;
  font-size: 14px;
  margin-bottom: 6px;
}

.events-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  overflow: hidden;
}

.event-bubble {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.4), rgba(59, 130, 246, 0.3));
  border: 1px solid rgba(37, 99, 235, 0.6);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 11px;
  color: #60a5fa;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.2s ease;
}

.event-bubble:hover {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.6), rgba(59, 130, 246, 0.5));
  border-color: rgba(37, 99, 235, 0.8);
}

.event-overflow {
  font-size: 10px;
  color: #9ca3af;
  padding: 2px 4px;
  font-style: italic;
}
</style>