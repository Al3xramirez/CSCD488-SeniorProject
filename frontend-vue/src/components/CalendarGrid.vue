<script setup>
import { ref, computed } from "vue";

const props = defineProps({
  events: { type: Array, default: () => [] }
});

const emit = defineEmits(["select-day"]);

const monthCursor = ref(startOfMonth(new Date()));
const selectedDay = ref(toYmd(new Date()));

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
    const key = toYmd(new Date(e.start));
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(e);
  }

  return map;
});

function selectDay(d) {
  selectedDay.value = toYmd(d);
  emit("select-day", selectedDay.value);
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

function toYmd(d) {
  return d.toISOString().split("T")[0];
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
          selected: toYmd(d) === selectedDay
        }"
        @click="selectDay(d)"
      >
        <div class="num">{{ d.getDate() }}</div>

        <div v-if="eventsByDay.get(toYmd(d))" class="count">
          {{ eventsByDay.get(toYmd(d)).length }}
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

.title {
  font-weight: bold;
}

.dow {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 6px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cell {
  min-height: 70px;
  border-radius: 10px;
  padding: 6px;
  background: rgba(255,255,255,0.04);
  cursor: pointer;
  position: relative;
}

.cell.other {
  opacity: 0.4;
}

.cell.selected {
  background: rgba(37,99,235,0.3);
}

.num {
  font-weight: bold;
}

.count {
  position: absolute;
  bottom: 6px;
  right: 6px;
  font-size: 12px;
}
</style>