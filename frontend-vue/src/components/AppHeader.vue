<script setup>
import { computed } from "vue"; 
import { useRouter } from "vue-router";
const router = useRouter();

const props = defineProps({ // props is an object containing role, firstName, and lastName
  role: { type: String, default: null },
  firstName: { type: String, default: null },
  lastName: { type: String, default: null },
});

const roleLabel = computed(() => { // roleLabel is a computed property that returns a user-friendly role name based on the role prop; defaults to Student.
  const r = (props.role || 'STUDENT').trim().toUpperCase();
  if (r === 'PROFESSOR') return 'Professor';
  if (r === 'TA') return 'TA';
  return 'Student';
});

const displayName = computed(() => { // displayName is a computed property that returns the user's full name if available, otherwise falls back to the role label.
  const parts = [props.firstName, props.lastName].filter(Boolean);
  return parts.length ? parts.join(' ') : roleLabel.value;
});

const roleViewLabel = computed(() => `${roleLabel.value} View`);

function goProfile() {
  router.push("/app/profile");
}
</script>

<!-- AppHeader.vue: Top header bar with title, notifications, and profile -->
 
<template>
  <header class="header">
    <div class="left">
      <div class="title-wrap">
        <div class="role">{{ roleViewLabel }}</div>
      </div>
    </div>

    <div class="center" aria-hidden="true">
        <div class="brand-text">
          <span class="brand-syllabus">Syllabus</span>
          <span class="brand-sync">Sync</span>
        </div>
    </div>

    <div class="right">
      <button class="icon-btn" title="Notifications (placeholder)">
        🔔
      </button>

      <div class="profile" @click="goProfile" type="button" title="Open profile">
        <div class="avatar">👤</div>
        <div class="meta">
          <div class="name">{{ displayName }}</div>
          <div class="sub">Profile</div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  height: 68px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: rgba(255,255,255,0.03);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.left {
  display: flex;
  align-items: center;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.role {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.center {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.brand {
  display: grid;
  place-items: center;
}

.brand-text {
  font-weight: 900;
  font-size: 35px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  letter-spacing: 0.2px;
  line-height: 1;
  white-space: nowrap;
}

.brand-syllabus {
  color: #e5e7eb;
}

.brand-sync {
  color: #3b82f6;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  height: 40px;
  width: 40px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  color: #e5e7eb;
  cursor: pointer;
}

.icon-btn:hover {
  background: rgba(255,255,255,0.07);
}

.profile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  cursor: pointer;
}

.profile:hover {
  background: rgba(255,255,255,0.07);
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,0.07);
}

.name {
  font-weight: 800;
  font-size: 13px;
  color: #e5e7eb;
}

.sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
</style>