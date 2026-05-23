<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from "vue"; 
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

const availabilityStatus = computed(() => {
  const s = me?.value?.availabilityStatus;
  return (s || "HIDDEN").toString().trim().toUpperCase();
});

const canSetAvailability = computed(() => {
  const r = (props.role || "STUDENT").toString().trim().toUpperCase();
  return r === "PROFESSOR" || r === "TA";
});

const menuOpen = ref(false);
const settingStatus = ref(false);
const statusError = ref("");

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

function goProfile() {
  router.push("/app/profile");
}

//  ------  Functions for handling avatar click, menu interactions, and setting availability status ----
function onAvatarClick() {
  if (!canSetAvailability.value) {
    goProfile();
    return;
  }
  statusError.value = "";
  menuOpen.value = !menuOpen.value;
}

function closeMenu() {
  menuOpen.value = false;
}

// setAvailability sends a POST request to the backend to update the user's availability status
async function setAvailability(status) {
  if (!canSetAvailability.value || settingStatus.value) return;
  statusError.value = "";
  settingStatus.value = true;
  try {
    const res = await fetch("/api/me/availability", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ status }),
    });
    if (!res.ok) {
      statusError.value = `Failed to set status (${res.status})`;
      return;
    }
    const data = await res.json().catch(() => null);
    const newStatus = (data?.availabilityStatus || status || "HIDDEN").toString().trim().toUpperCase();
    if (me?.value) me.value = { ...me.value, availabilityStatus: newStatus };
    closeMenu();
  } catch (e) {
    statusError.value = "Failed to set status";
  } finally {
    settingStatus.value = false;
  }
}

function onWindowClick() {
  if (menuOpen.value) closeMenu();
}

onMounted(refreshPhoto);
onMounted(() => window.addEventListener("click", onWindowClick));
onBeforeUnmount(() => {
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value);
  window.removeEventListener("click", onWindowClick);
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
        <div class="avatar" aria-label="Profile photo" @click.stop="onAvatarClick" :title="canSetAvailability ? 'Set availability' : 'Open profile'">
          <img v-if="photoUrl" class="avatar-img" :src="photoUrl" alt="Profile photo" />
          <div v-else class="avatar-fallback">{{ initials }}</div>
          <span
            v-if="availabilityStatus !== 'HIDDEN'"
            class="status-dot"
            :class="`status-dot--${availabilityStatus.toLowerCase()}`"
            aria-hidden="true"
          />
        </div>
        <div class="meta">
          <div class="name">{{ displayName }}</div>
          <div class="sub">Profile</div>
        </div>

        <div v-if="menuOpen" class="status-menu" role="menu" @click.stop>
          <button class="status-item" type="button" role="menuitem" :disabled="settingStatus" @click="setAvailability('AVAILABLE')">
            <span class="status-dot status-dot--available" aria-hidden="true" />
            <span class="status-text">Available</span>
          </button>
          <button class="status-item" type="button" role="menuitem" :disabled="settingStatus" @click="setAvailability('IDLE')">
            <span class="status-dot status-dot--idle" aria-hidden="true" />
            <span class="status-text">Idle</span>
          </button>
          <button class="status-item" type="button" role="menuitem" :disabled="settingStatus" @click="setAvailability('DND')">
            <span class="status-dot status-dot--dnd" aria-hidden="true" />
            <span class="status-text">Do not disturb</span>
          </button>
          <button class="status-item" type="button" role="menuitem" :disabled="settingStatus" @click="setAvailability('HIDDEN')">
            <span class="status-dot status-dot--hidden" aria-hidden="true" />
            <span class="status-text">Hidden</span>
          </button>

          <div v-if="statusError" class="status-error">{{ statusError }}</div>
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

.profile {
  position: relative;
}

.avatar {
  position: relative;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  border: 2px solid rgba(15,23,42,0.95);
}

.avatar > .status-dot {
  position: absolute;
  right: 2px;
  bottom: 2px;
  z-index: 2;
  pointer-events: none;
}

.status-menu .status-dot {
  position: static;
  border: none;
}

.status-dot--available {
  background: #86efac;
}

.status-dot--idle {
  background: #facc15;
}

.status-dot--dnd {
  background: rgb(239, 68, 68);
}

.status-dot--hidden {
  background: transparent;
  border: 2px solid rgba(229,231,235,0.35);
}

.status-menu {
  position: absolute;
  top: 52px;
  right: 0;
  width: 190px;
  background: rgba(15,23,42,0.95);
  border: 1px solid rgba(255,255,255,0.10);
  border-radius: 14px;
  padding: 8px;
  box-shadow: 0 18px 40px rgba(0,0,0,0.35);
  z-index: 20;
}

.status-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 12px;
  border: 1px solid transparent;
  background: transparent;
  color: #e5e7eb;
  cursor: pointer;
  font-weight: 800;
  text-align: left;
}

.status-item:hover {
  background: rgba(255,255,255,0.06);
  border-color: rgba(255,255,255,0.08);
}

.status-item:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.status-text {
  font-size: 13px;
}

.status-error {
  margin-top: 8px;
  font-size: 12px;
  color: #9ca3af;
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
  overflow: hidden;
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