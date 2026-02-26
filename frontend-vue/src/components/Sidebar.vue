<script setup>
import { useRoute } from "vue-router";

const route = useRoute();

const links = [
  { to: "/app", label: "Dashboard" },
  { to: "/app/meetings", label: "Meeting Times" },
  { to: "/app/calendar", label: "Calendar" },
  { to: "/app/office-hours", label: "Office Hours Match" },
];

const isActive = (to) => route.path === to;
</script>


<template>
  <aside class="sidebar">
    <div class="top">
      <div class="logo" title="SyllabusSync">
        <span class="logo-inner">SS</span>
      </div>
    </div>

    <!-- Nav -->
    <nav class="nav">
      <router-link
        v-for="l in links"
        :key="l.to"
        :to="l.to"
        class="link"
        :class="{ active: isActive(l.to) }"
      >
        <span class="dot" />
        <span class="label">{{ l.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 260px;
  min-height: 100vh;
  background: #0f172a;
  color: white;
  padding: 18px;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
}

/* top logo */
.top {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 0 6px;
  margin-bottom: 10px;
}

.logo {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.logo-inner {
  font-weight: 900;
  letter-spacing: 0.6px;
  color: white;
}

/* nav */
.nav {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.link {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: #cbd5e1;
  padding: 16px 14px;              
  min-height: 54px;                
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: transform 0.08s ease, background 0.12s ease;
}

.link:hover {
  background: rgba(255, 255, 255, 0.06);
  transform: translateY(-1px);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}

.active {
  color: white;
  background: rgba(37, 99, 235, 0.22);
  border: 1px solid rgba(37, 99, 235, 0.45);
}

.active .dot {
  background: #60a5fa;
}
</style>