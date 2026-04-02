<script setup>
import { useRouter } from "vue-router";
import { computed, inject, ref, watch } from "vue";
const router = useRouter();

// --------- Student profile retrieval ----------
const me = inject("me", null);
const firstName = computed(() => me?.value?.firstName || "—");
const lastName = computed(() => me?.value?.lastName || "—");
const email = computed(() => me?.value?.email || "—");

// --------- Preferences (persisted to localStorage) ----------
const TZ_KEY = "syllabussync_timezone";
const NOTIF_KEY = "syllabussync_notifications";

const allTimezones = [
  { label: "Eastern (ET)",  value: "America/New_York" },
  { label: "Central (CT)",  value: "America/Chicago" },
  { label: "Mountain (MT)", value: "America/Denver" },
  { label: "Pacific (PT)",  value: "America/Los_Angeles" },
];
const timezone = ref(
  localStorage.getItem(TZ_KEY) || Intl.DateTimeFormat().resolvedOptions().timeZone
);
const notificationsOn = ref(localStorage.getItem(NOTIF_KEY) !== "false");

watch(timezone, (v) => localStorage.setItem(TZ_KEY, v));
watch(notificationsOn, (v) => localStorage.setItem(NOTIF_KEY, String(v)));

</script>

<template>
  <div class="wrap">
    <div class="title-row">
      <h1>Student Profile</h1>
      <button class="btn ghost" @click="router.push('/app')">Back to Dashboard</button>
    </div>

    <div class="grid">
      <section class="card">
        <h2>Account</h2>
        <div class="field">
          <div class="label">First name</div>
          <div class="value">{{ firstName }}</div>
        </div>
        <div class="field">
          <div class="label">Last name</div>
          <div class="value">{{ lastName }}</div>
        </div>
        <div class="field">
          <div class="label">Email</div>
          <div class="value">{{ email }}</div>
        </div>
      </section>

      <section class="card">
        <h2>Preferences</h2>
        <div class="field">
          <div class="label">Time zone</div>
          <select class="pref-select" v-model="timezone">
            <option v-for="tz in allTimezones" :key="tz.value" :value="tz.value">{{ tz.label }}</option>
          </select>
        </div>
        <div class="field">
          <div class="label">Meeting reminders</div>
          <button class="toggle" :class="{ on: notificationsOn }" @click="notificationsOn = !notificationsOn">
            <span class="toggle-thumb" />
            <span class="toggle-label">{{ notificationsOn ? "On" : "Off" }}</span>
          </button>
        </div>
      </section>

      <section class="card wide">
        <h2>Joined Classes</h2>
        <p class="muted">Placeholder list — will come from backend later.</p>
        <div class="pill-row">
          <span class="pill">—</span>
          <span class="pill">—</span>
          <span class="pill">—</span>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  color: #e5e7eb;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

h1 {
  margin: 0;
  font-size: 22px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
}

.wide {
  grid-column: span 2;
}

h2 {
  margin: 0 0 12px;
  font-size: 16px;
}

.field {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid rgba(255,255,255,0.06);
}
.field:first-of-type {
  border-top: none;
}

.label {
  color: #9ca3af;
  font-size: 13px;
  font-weight: 700;
}
.value {
  font-weight: 800;
}

.muted {
  margin: 0 0 10px;
  color: #9ca3af;
  font-size: 13px;
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pill {
  padding: 8px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  color: #cbd5e1;
  font-weight: 700;
  font-size: 13px;
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
.btn:hover { background: #1d4ed8; }

.btn.ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.10);
}
.btn.ghost:hover { background: rgba(255,255,255,0.09); }

.pref-select {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 10px;
  padding: 6px 10px;
  color: #e5e7eb;
  font-size: 13px;
  font-weight: 700;
  outline: none;
  cursor: pointer;
  max-width: 220px;
}
.pref-select:focus {
  border-color: rgba(96,165,250,0.6);
}

.toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 999px;
  padding: 5px 12px 5px 6px;
  cursor: pointer;
  color: #9ca3af;
  font-size: 13px;
  font-weight: 800;
  transition: background 0.2s, border-color 0.2s;
}
.toggle.on {
  background: rgba(37,99,235,0.22);
  border-color: rgba(37,99,235,0.45);
  color: #bfdbfe;
}
.toggle-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #4b5563;
  transition: background 0.2s;
}
.toggle.on .toggle-thumb {
  background: #60a5fa;
}

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
  .wide { grid-column: span 1; }
}
</style>