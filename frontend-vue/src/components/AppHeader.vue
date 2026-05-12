<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from "vue"; 
import { useRouter } from "vue-router";
const router = useRouter();

const me = inject('me', null);

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

const initials = computed(() => {
  const f = (props.firstName || "").trim();
  const l = (props.lastName || "").trim();
  const first = f ? f[0].toUpperCase() : "";
  const last = l ? l[0].toUpperCase() : "";
  return (first + last) || "?";
});

const photoUrl = ref(null);

async function refreshPhoto() {
  const prev = photoUrl.value;
  photoUrl.value = null;
  if (prev) URL.revokeObjectURL(prev);

  try {
    const res = await fetch("/api/me/photo", { credentials: "include" });
    if (res.status === 204) return;
    if (!res.ok) return;
    const blob = await res.blob();
    photoUrl.value = URL.createObjectURL(blob);
  } catch (e) {
    // ignore
  }
}

const roleViewLabel = computed(() => `${roleLabel.value} View`);

const isStaff = computed(() => {
  const r = (props.role || 'STUDENT').toString().trim().toUpperCase();
  return r === 'PROFESSOR' || r === 'TA';
});

const availability = ref('AVAILABLE');

// watch is used to instantly update availability whenever me.value.availabilityStatus changes.
watch(
  () => me?.value?.availabilityStatus,
  (v) => {
    const next = (v || '').toString().trim().toUpperCase();
    availability.value = next || 'AVAILABLE';
  },
  { immediate: true }
);

function availabilityDotClass(status) {
  const v = (status || '').toString().trim().toUpperCase();
  if (v === 'IDLE') return 'status-dot--idle';
  if (v === 'DND') return 'status-dot--dnd';
  return 'status-dot--available';
}

// API call to cycle availability status between AVAILABLE, IDLE, and DND for staff users.
async function cycleAvailability() {
  if (!isStaff.value) return;

  const current = availability.value;
  const next = current === 'AVAILABLE' ? 'IDLE' : current === 'IDLE' ? 'DND' : 'AVAILABLE';

  try {
    const res = await fetch('/api/me/availability', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ status: next }),
    });
    if (!res.ok) return;

    availability.value = next;
    if (me?.value) {
      me.value = { ...me.value, availabilityStatus: next };
    }
  } catch (e) {
    // ignore
  }
}

function goProfile() {
  router.push("/app/profile");
}

onMounted(refreshPhoto);
onBeforeUnmount(() => {
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value);
});
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
        <div class="avatar" aria-label="Profile photo">
          <div class="avatar-clip">
            <img v-if="photoUrl" class="avatar-img" :src="photoUrl" alt="Profile photo" />
            <div v-else class="avatar-fallback">{{ initials }}</div>
          </div>

          <button
            v-if="isStaff"
            class="status-dot"
            type="button"
            :class="availabilityDotClass(availability)"
            title="Toggle availability"
            aria-label="Toggle availability"
            @click.stop="cycleAvailability"
          />
        </div>
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
  overflow: visible;
  position: relative;
}

.avatar-clip {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  overflow: hidden;
  display: grid;
  place-items: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  font-weight: 900;
  color: #e5e7eb;
  font-size: 12px;
}

.status-dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  border: 2px solid rgba(11, 18, 32, 1);
  padding: 0;
  cursor: pointer;
  background: rgb(34, 197, 94);
}

.status-dot--available { background: rgb(34, 197, 94); }
.status-dot--idle { background: rgb(250, 204, 21); }
.status-dot--dnd { background: rgb(239, 68, 68); }

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