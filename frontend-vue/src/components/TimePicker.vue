<script setup>
/**
 * Reusable time picker — three styled dropdowns: H / MM / AM·PM
 * Props  : modelValue — "HH:MM" 24-hour string  (e.g. "14:30")
 * Emits  : update:modelValue — same format
 * Usage  : <TimePicker v-model="someRef" />
 */
import { ref, watch } from "vue";

const props = defineProps({
  modelValue: { type: String, default: "12:00" },
});
const emit = defineEmits(["update:modelValue"]);

const hours   = Array.from({ length: 12 }, (_, i) => i + 1); // 1–12
const minutes = Array.from({ length: 12 }, (_, i) => i * 5); // 0, 5, 10 … 55

function parse(val) {
  const [hh, mm] = (val || "12:00").split(":").map(Number);
  return {
    h: isNaN(hh) ? 12 : (hh % 12 || 12),
    m: isNaN(mm) ? 0  : Math.round(mm / 5) * 5 % 60,
    p: (!isNaN(hh) && hh >= 12) ? "PM" : "AM",
  };
}

const { h: ih, m: im, p: ip } = parse(props.modelValue);
const selH = ref(ih);
const selM = ref(im);
const selP = ref(ip);

function emit24() {
  let h = selH.value % 12;
  if (selP.value === "PM") h += 12;
  emit("update:modelValue", `${String(h).padStart(2, "0")}:${String(selM.value).padStart(2, "0")}`);
}

watch(() => props.modelValue, (val) => {
  const { h, m, p } = parse(val);
  selH.value = h;
  selM.value = m;
  selP.value = p;
});
</script>

<template>
  <div class="tp">
    <!-- Hours -->
    <select class="seg seg-h" v-model="selH" @change="emit24">
      <option v-for="h in hours" :key="h" :value="h">{{ h }}</option>
    </select>

    <span class="colon">:</span>

    <!-- Minutes -->
    <select class="seg seg-m" v-model="selM" @change="emit24">
      <option v-for="m in minutes" :key="m" :value="m">{{ String(m).padStart(2, "0") }}</option>
    </select>

    <!-- AM / PM -->
    <select class="seg seg-p" v-model="selP" @change="emit24">
      <option value="AM">AM</option>
      <option value="PM">PM</option>
    </select>
  </div>
</template>

<style scoped>
.tp {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.seg {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.10);
  border-radius: 10px;
  color: #e5e7eb;
  font-size: 14px;
  font-weight: 700;
  padding: 9px 8px;
  outline: none;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  text-align: center;
}

.seg:focus {
  border-color: rgba(37, 99, 235, 0.60);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25);
}

.seg-h { flex: 0 0 56px; }
.seg-m { flex: 0 0 56px; }
.seg-p { flex: 0 0 60px; }

.colon {
  font-size: 17px;
  font-weight: 700;
  color: rgba(229, 231, 235, 0.50);
  flex-shrink: 0;
  margin: 0 -2px;
}
</style>
