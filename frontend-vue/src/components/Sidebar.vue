<script setup>
import { computed } from "vue"; // computed is used to create reactive computed properties that automatically update when their dependencies change.
import { useRoute } from "vue-router";
import pureLogoUrl from '../assets/SyllabusSyncPureLogo.png'
import dashboardIconUrl from "../assets/Dashboard.svg";
import myClassesIconUrl from "../assets/My Classes.svg";
import scheduleIconUrl from "../assets/Schedule.svg";
import calendarIconUrl from "../assets/Calandar.svg";
import syllabusUploadIconUrl from "../assets/Syllabus Upload.svg";
import workloadIconUrl from "../assets/Workload.svg";
import officeHoursIconUrl from "../assets/Office Hours.svg";

/* props is an object containing role, firstName, and lastName, 
which have been passed down from the parent component (DashboardLayout) */
const props = defineProps({ 
  role: { type: String, default: null },
});

const route = useRoute();

/* normalizedRole is a computed property that returns the user's role in uppercase, defaulting to "STUDENT" if not provided.
This is basically a local variable that based on the user's role, will render the UI based off of that role
*/
const normalizedRole = computed(() => (props.role || "STUDENT").trim().toUpperCase()); 

const syllabusImportEnabled = (import.meta.env.VITE_SYLLABUS_IMPORT_ENABLED || "true").toString().toLowerCase() !== "false";

const iconByRoute = {
  "/app": dashboardIconUrl,
  "/app/classes": myClassesIconUrl,
  "/app/meetings": scheduleIconUrl,
  "/app/calendar": calendarIconUrl,
  "/app/syllabus-upload": syllabusUploadIconUrl,
  "/app/workload-projections": workloadIconUrl,
  "/app/office-hours": officeHoursIconUrl,
};

const withIcons = (items) =>
  items.map((item) => ({
    ...item,
    icon: iconByRoute[item.to] || null,
  }));

const links = computed(() => {
  if (normalizedRole.value === 'PROFESSOR') {
    const base = [
      { to: "/app", label: "Dashboard" },
      { to: "/app/classes", label: "My Classes" },
      { to: "/app/meetings", label: "Schedule" },
      { to: "/app/calendar", label: "Calendar" },
    ];

    if (syllabusImportEnabled) {
      base.push({ to: "/app/syllabus-upload", label: "Syllabus Upload" });
    }

    return withIcons(base);
  }

  if (normalizedRole.value === 'TA') {
    return withIcons([
      { to: "/app", label: "Dashboard" },
      { to: "/app/classes", label: "My Classes" },
      { to: "/app/meetings", label: "Schedule" },
      { to: "/app/calendar", label: "Calendar" },
    ]);
  }

  // STUDENT (default)
  return withIcons([
    { to: "/app", label: "Dashboard" },
    { to: "/app/classes", label: "My Classes" },
    { to: "/app/calendar", label: "Calendar" },
    { to: "/app/workload-projections", label: "Workload Projections" },
    { to: "/app/office-hours", label: "Office Hours" },
  ]);
});

const isActive = (to) => route.path === to;
</script>


<template>
  <aside class="sidebar">
    <div class="top">
      <div class="logo" title="SyllabusSync">
        <img class="logo-inner" :src="pureLogoUrl" alt="SyllabusSync Pure Logo" />
      </div>
    </div>

    <!-- Nav -->
    <nav class="nav">
      <router-link
        v-for="l in links"
        :key="l.to"
        :to="l.to"
        class="link"
        :class="{ active: isActive(l.to), 'link--syllabus': l.to === '/app/syllabus-upload' }"
      >
        <img
          v-if="l.icon"
          class="icon"
          :src="l.icon"
          alt=""
          aria-hidden="true"
        />
        <span v-else class="dot" />
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
  background: linear-gradient(135deg, #ffffff, #fdfdfd);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.14);
  overflow: hidden;
}

.logo-inner {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  padding: 8px;
  display: block;
  border-radius: inherit;
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

.icon {
  width: 35px;
  height: 35px;
  object-fit: contain;
  flex-shrink: 0;
  opacity: 0.9;
}

.active {
  color: white;
  background: rgba(37, 99, 235, 0.22);
  border: 1px solid rgba(37, 99, 235, 0.45);
}

.active .dot {
  background: #60a5fa;
}

.active .icon {
  opacity: 1;
}

/* Make Syllabus Upload stand out */
.link--syllabus {
  color: white;
  background: #2563eb;
  border: 1px solid transparent;
}

.link--syllabus:hover {
  background: #1d4ed8;
}

.link--syllabus.active {
  color: white;
  background: #1d4ed8;
  border: 1px solid transparent;
}
</style>