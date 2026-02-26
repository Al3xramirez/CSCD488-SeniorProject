<script setup>
import { useRouter } from "vue-router";
const router = useRouter();
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
          <div class="label">Name</div>
          <div class="value">—</div>
        </div>
        <div class="field">
          <div class="label">Email</div>
          <div class="value">—</div>
        </div>
        <div class="field">
          <div class="label">Major</div>
          <div class="value">—</div>
        </div>
      </section>

      <section class="card">
        <h2>Preferences</h2>
        <div class="field">
          <div class="label">Time zone</div>
          <div class="value">—</div>
        </div>
        <div class="field">
          <div class="label">Notifications</div>
          <div class="value">—</div>
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

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
  .wide { grid-column: span 1; }
}
</style>