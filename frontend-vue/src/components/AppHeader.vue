<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import notificationIconUrl from "../assets/Notifications.svg";

const router = useRouter();

const me = inject("me", null);

const props = defineProps({
  role: { type: String, default: null },
  firstName: { type: String, default: null },
  lastName: { type: String, default: null },
});

const roleLabel = computed(() => {
  const r = (props.role || 'STUDENT').trim().toUpperCase();
  if (r === 'PROFESSOR') return 'Professor';
  if (r === 'TA') return 'TA';
  return 'Student';
});

const displayName = computed(() => {
  const parts = [props.firstName, props.lastName].filter(Boolean);
  return parts.length ? parts.join(' ') : roleLabel.value;
});

const initials = computed(() => {
  const f = (props.firstName || "").trim();
  const l = (props.lastName || "").trim();
  return ((f ? f[0].toUpperCase() : "") + (l ? l[0].toUpperCase() : "")) || "?";
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
    if (res.status === 204 || !res.ok) return;
    photoUrl.value = URL.createObjectURL(await res.blob());
  } catch {}
}

// ── Notifications ──────────────────────────────────────────────────────────
const notifications = ref([]);
const showDropdown = ref(false);
const bellRef = ref(null);
let pollInterval = null;

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length);

const TYPE_META = {
  MEETING_REQUEST:  { label: 'Meeting Request', color: '#60a5fa' },
  MEETING_CONFIRMED: { label: 'Confirmed',       color: '#4ade80' },
  MEETING_DECLINED:  { label: 'Declined',        color: '#f87171' },
};

async function fetchNotifications() {
  try {
    const res = await fetch('/api/notifications/my', { credentials: 'include' });
    if (res.ok) notifications.value = await res.json();
  } catch {}
}

async function markRead(n) {
  if (n.read) return;
  try {
    await fetch(`/api/notifications/${n.id}/read`, { method: 'PATCH', credentials: 'include' });
    n.read = true;
  } catch {}
}

async function markAllRead() {
  try {
    await fetch('/api/notifications/read-all', { method: 'PATCH', credentials: 'include' });
    notifications.value.forEach(n => n.read = true);
  } catch {}
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value;
}

function onClickOutside(e) {
  if (bellRef.value && !bellRef.value.contains(e.target)) {
    showDropdown.value = false;
  }
}

function relativeTime(iso) {
  if (!iso) return '';
  const diff = Math.floor((Date.now() - new Date(iso)) / 1000);
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
}

const roleViewLabel = computed(() => `${roleLabel.value} View`);

function goProfile() {
  router.push("/app/profile");
}

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

onMounted(() => {
  refreshPhoto();
  fetchNotifications();
  pollInterval = setInterval(fetchNotifications, 30000);

  window.addEventListener("click", onWindowClick);
  document.addEventListener("click", onClickOutside);
});

onBeforeUnmount(() => {
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value);
  if (pollInterval) clearInterval(pollInterval);

  window.removeEventListener("click", onWindowClick);
  document.removeEventListener("click", onClickOutside);
});
</script>

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
      <!-- Notification bell -->
      <div class="notif-wrap" ref="bellRef">
        <button class="icon-btn" type="button" title="Notifications" aria-label="Notifications" @click="toggleDropdown">
          <img class="notif-icon" :src="notificationIconUrl" alt="" aria-hidden="true" />
          <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
        </button>

        <div v-if="showDropdown" class="notif-dropdown">
          <div class="notif-hdr">
            <span class="notif-title">Notifications</span>
            <button v-if="unreadCount > 0" class="mark-all-btn" @click="markAllRead">Mark all read</button>
          </div>

          <div v-if="notifications.length === 0" class="notif-empty">No notifications yet.</div>

          <div v-else class="notif-list">
            <div
              v-for="n in notifications"
              :key="n.id"
              class="notif-item"
              :class="{ unread: !n.read }"
              @click="markRead(n)"
            >
              <div class="notif-dot" :style="{ background: TYPE_META[n.type]?.color ?? '#9ca3af' }"></div>
              <div class="notif-body">
                <div class="notif-label" :style="{ color: TYPE_META[n.type]?.color ?? '#9ca3af' }">
                  {{ TYPE_META[n.type]?.label ?? n.type }}
                </div>
                <div class="notif-msg">{{ n.message }}</div>
                <div class="notif-time">{{ relativeTime(n.createdAt) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

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

.title-wrap { display: flex; flex-direction: column; line-height: 1.1; }

.role { margin-top: 4px; font-size: 12px; color: #9ca3af; }

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

.brand-syllabus { color: #e5e7eb; }
.brand-sync { color: #3b82f6; }

.right { display: flex; align-items: center; gap: 12px; }

/* ── Bell + dropdown wrapper ── */
.notif-wrap {
  position: relative;
}

.icon-btn {
  position: relative;
  height: 40px;
  width: 40px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
  color: #e5e7eb;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notif-icon {
  width: 20px;
  height: 20px;
  display: block;
  opacity: 0.95;
}

.icon-btn:hover { background: rgba(255,255,255,0.07); }

.badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  box-sizing: border-box;
}

.notif-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 320px;
  background: #1a1d2e;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
  z-index: 200;
  overflow: hidden;
}

.notif-hdr {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
}

.notif-title {
  font-size: 13px;
  font-weight: 700;
  color: #f3f4f6;
}

.mark-all-btn {
  background: none;
  border: none;
  font-size: 11px;
  color: #60a5fa;
  cursor: pointer;
  padding: 0;
}

.mark-all-btn:hover { text-decoration: underline; }

.notif-empty {
  padding: 20px 16px;
  font-size: 13px;
  color: #6b7280;
  text-align: center;
}

.notif-list {
  max-height: 360px;
  overflow-y: auto;
}

.notif-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  cursor: pointer;
  transition: background 0.12s;
}

.notif-item:last-child { border-bottom: none; }

.notif-item:hover { background: rgba(255,255,255,0.04); }

.notif-item.unread { background: rgba(255,255,255,0.03); }

.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 5px;
}

.notif-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.notif-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.notif-msg {
  font-size: 12px;
  color: #d1d5db;
  line-height: 1.4;
  word-break: break-word;
}

.notif-time {
  font-size: 11px;
  color: #6b7280;
  margin-top: 2px;
}

/* ── Profile ── */
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

.profile:hover { background: rgba(255,255,255,0.07); }

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,0.07);
  overflow: hidden;
}

.avatar-img { width: 100%; height: 100%; object-fit: cover; display: block; }

.avatar-fallback { font-weight: 900; color: #e5e7eb; font-size: 12px; }

.name { font-weight: 800; font-size: 13px; color: #e5e7eb; }

.sub { font-size: 12px; color: #9ca3af; margin-top: 2px; }
</style>
