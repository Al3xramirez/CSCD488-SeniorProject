<script setup>
import { useRouter } from "vue-router";
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from "vue";
const router = useRouter();

// --------- Student profile retrieval ----------
const me = inject("me", null);
const firstName = computed(() => me?.value?.firstName || "—");
const lastName = computed(() => me?.value?.lastName || "—");
const email = computed(() => me?.value?.email || "—");
const department = computed(() => me?.value?.department || "—");

const role = computed(() => (me?.value?.role || "STUDENT").toString().trim().toUpperCase());
const roleLabel = computed(() => {
  if (role.value === "PROFESSOR") return "Professor";
  if (role.value === "TA") return "TA";
  return "Student";
});

const availabilityStatus = computed(() => (me?.value?.availabilityStatus || "HIDDEN").toString().trim().toUpperCase());
const canSetAvailability = computed(() => role.value === "PROFESSOR" || role.value === "TA");

const availabilityLabel = computed(() => {
  if (availabilityStatus.value === "AVAILABLE") return "Available";
  if (availabilityStatus.value === "IDLE") return "Idle";
  if (availabilityStatus.value === "DND") return "Do not disturb";
  return "Hidden";
});

// --------- Joined classes (provided by DashboardLayout) ----------
const classes = inject("classes", null);
const classesLoading = inject("classesLoading", null);
const classesError = inject("classesError", null);

const joinedClasses = computed(() => {
  const list = classes?.value;
  return Array.isArray(list) ? list : [];
});

// --------- Notification preferences (localStorage) ----------
const PREFS_KEY = "syllabussync.notificationPrefs.v1";
const prefEmailMeetingRequests = ref(true);
const prefEmailMeetingUpdates = ref(true);
const prefSaved = ref("");

function loadPrefs() {
  try {
    const raw = localStorage.getItem(PREFS_KEY);
    if (!raw) return;
    const data = JSON.parse(raw);
    if (typeof data?.emailMeetingRequests === "boolean") prefEmailMeetingRequests.value = data.emailMeetingRequests;
    if (typeof data?.emailMeetingUpdates === "boolean") prefEmailMeetingUpdates.value = data.emailMeetingUpdates;
  } catch {
    // ignore
  }
}

function savePrefs() {
  try {
    localStorage.setItem(
      PREFS_KEY,
      JSON.stringify({
        emailMeetingRequests: prefEmailMeetingRequests.value,
        emailMeetingUpdates: prefEmailMeetingUpdates.value,
      })
    );
    prefSaved.value = "Saved.";
    window.clearTimeout(savePrefs._t);
    savePrefs._t = window.setTimeout(() => (prefSaved.value = ""), 1200);
  } catch {
    // ignore
  }
}

watch([prefEmailMeetingRequests, prefEmailMeetingUpdates], savePrefs);

// --------- Availability update ----------
const settingAvailability = ref(false);
const availabilityError = ref("");
const availabilityMenuOpen = ref(false);

function toggleAvailabilityMenu() {
  if (!canSetAvailability.value) return;
  availabilityMenuOpen.value = !availabilityMenuOpen.value;
}

function closeAvailabilityMenu() {
  availabilityMenuOpen.value = false;
}

async function setAvailability(status) {
  if (!canSetAvailability.value || settingAvailability.value) return;
  availabilityError.value = "";
  settingAvailability.value = true;
  closeAvailabilityMenu();
  try {
    const res = await fetch("/api/me/availability", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ status }),
    });
    if (!res.ok) {
      availabilityError.value = `Failed to set status (${res.status})`;
      return;
    }
    const data = await res.json().catch(() => null);
    const newStatus = (data?.availabilityStatus || status || "HIDDEN").toString().trim().toUpperCase();
    if (me?.value) me.value = { ...me.value, availabilityStatus: newStatus };
  } catch {
    availabilityError.value = "Failed to set status";
  } finally {
    settingAvailability.value = false;
  }
}

// --------- Delete account ----------
const deletingAccount = ref(false);
const deleteError = ref("");
async function deleteAccount() {
  deleteError.value = "";
  if (deletingAccount.value) return;
  const ok = window.confirm(
    "Delete your account? This will remove your profile and class relationships. This cannot be undone."
  );
  if (!ok) return;

  deletingAccount.value = true;
  try {
    const res = await fetch("/api/me", { method: "DELETE", credentials: "include" });
    if (!res.ok) {
      deleteError.value = `Delete failed (${res.status})`;
      return;
    }
    try {
      await fetch("/logout", { method: "POST", credentials: "include" });
    } catch {
      // ignore
    }
    router.push("/signup");
  } catch {
    deleteError.value = "Delete failed.";
  } finally {
    deletingAccount.value = false;
  }
}

// --------- Profile photo upload ----------
const initials = computed(() => {
  const f = (me?.value?.firstName || "").trim();
  const l = (me?.value?.lastName || "").trim();
  const first = f ? f[0].toUpperCase() : "";
  const last = l ? l[0].toUpperCase() : "";
  return (first + last) || "?";
});

const photoUrl = ref(null);
const uploading = ref(false);
const uploadError = ref("");
const uploadStatus = ref("");
const selectedFile = ref(null);
const fileInput = ref(null);

// Fetches the current profile photo from the backend and updates the photoUrl
async function refreshPhoto() {
  uploadError.value = "";
  uploadStatus.value = "";

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

function openFilePicker() {
  if (uploading.value) return;
  fileInput.value?.click?.();
}

// Handles the file input change event and uploads immediately
async function onPickFile(e) {
  const file = e?.target?.files?.[0] || null;
  selectedFile.value = file;
  if (file) await uploadPhoto();
}

// Uploads the selected profile photo to the backend and refreshes the displayed photo
async function uploadPhoto() {
  uploadError.value = "";
  uploadStatus.value = "";

  if (!selectedFile.value) {
    uploadError.value = "Choose a photo first.";
    return;
  }

  uploading.value = true;
  try {
    const fd = new FormData();
    fd.append("photo", selectedFile.value);

    const res = await fetch("/api/me/photo", {
      method: "POST",
      body: fd,
      credentials: "include",
    });

    if (!res.ok) {
      uploadError.value = res.status === 413
        ? "Photo too large. Maximum size is 2MB."
        : `Upload failed (${res.status})`;
      return;
    }

    uploadStatus.value = "Photo uploaded.";
    selectedFile.value = null;
    if (fileInput.value) fileInput.value.value = "";
    await refreshPhoto();
  } catch (e) {
    uploadError.value = "Upload failed.";
  } finally {
    uploading.value = false;
  }
}

onMounted(refreshPhoto);
onMounted(loadPrefs);
onMounted(() => {
  document.addEventListener("click", closeAvailabilityMenu);
});
onBeforeUnmount(() => {
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value);
  document.removeEventListener("click", closeAvailabilityMenu);
});

async function logout() {
  try {
    await fetch("/logout", {
      method: "POST",
      credentials: "include",
    });
  } finally {
    router.push("/login");
  }
}

</script>

<template>
  <div class="wrap">
    <div class="title-row">
      <h1>User Profile</h1>
      <div class="actions">
        <button class="btn ghost" @click="router.push('/app')">Back to Dashboard</button>
      </div>
    </div>

    <div class="grid">
      <section class="card">
        <h2>Account</h2>

        <div class="avatar-row">
          <div class="avatar" aria-label="Profile photo">
            <img v-if="photoUrl" class="avatar-img" :src="photoUrl" alt="Profile photo" />
            <div v-else class="avatar-fallback">{{ initials }}</div>
          </div>

          <div class="avatar-actions">
            <input ref="fileInput" class="file" type="file" accept="image/png,image/jpeg" @change="onPickFile" />
            <button class="btn" :disabled="uploading" @click="openFilePicker">
              {{ uploading ? "Uploading..." : "Upload photo" }}
            </button>
          </div>
        </div>

        <div v-if="uploadError" class="note err">{{ uploadError }}</div>
        <div v-else-if="uploadStatus" class="note ok">{{ uploadStatus }}</div>

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
        <div class="field">
          <div class="label">Role</div>
          <div class="value">{{ roleLabel }}</div>
        </div>
        <div class="field">
          <div class="label">Department</div>
          <div class="value">{{ department }}</div>
        </div>
      </section>

      <section class="card card-compact">
        <h2>Availability</h2>
        <div class="field">
          <div class="label">Status</div>
          <div class="value availability-value">
            <span :class="['dot', `dot-${availabilityStatus}`]" aria-hidden="true" />
            <span>{{ availabilityStatus }}</span>
          </div>
        </div>

        <div class="pref-row">
          <button
            class="status-trigger"
            type="button"
            :disabled="!canSetAvailability || settingAvailability"
            @click.stop="toggleAvailabilityMenu"
          >
            <span class="status-left">
              <span class="status-dot" :class="`status-dot--${availabilityStatus.toLowerCase()}`" aria-hidden="true" />
              <span class="status-text">{{ availabilityLabel }}</span>
            </span>
            <span class="status-caret" aria-hidden="true">▾</span>
          </button>
          <span v-if="!canSetAvailability" class="hint">Only TA/Professor can change.</span>

          <div v-if="availabilityMenuOpen" class="status-menu" role="menu" @click.stop>
            <button class="status-item" type="button" role="menuitem" :disabled="settingAvailability" @click="setAvailability('AVAILABLE')">
              <span class="status-dot status-dot--available" aria-hidden="true" />
              <span class="status-text">Available</span>
            </button>
            <button class="status-item" type="button" role="menuitem" :disabled="settingAvailability" @click="setAvailability('IDLE')">
              <span class="status-dot status-dot--idle" aria-hidden="true" />
              <span class="status-text">Idle</span>
            </button>
            <button class="status-item" type="button" role="menuitem" :disabled="settingAvailability" @click="setAvailability('DND')">
              <span class="status-dot status-dot--dnd" aria-hidden="true" />
              <span class="status-text">Do not disturb</span>
            </button>
            <button class="status-item" type="button" role="menuitem" :disabled="settingAvailability" @click="setAvailability('HIDDEN')">
              <span class="status-dot status-dot--hidden" aria-hidden="true" />
              <span class="status-text">Hidden</span>
            </button>
          </div>
        </div>

        <div v-if="availabilityError" class="note err">{{ availabilityError }}</div>
      </section>

      <section class="card">
        <h2>Notification Preferences</h2>

        <label class="check">
          <input v-model="prefEmailMeetingRequests" type="checkbox" />
          <span>Email meeting requests</span>
        </label>

        <label class="check">
          <input v-model="prefEmailMeetingUpdates" type="checkbox" />
          <span>Email meeting updates (confirmed/declined)</span>
        </label>

        <div v-if="prefSaved" class="muted">{{ prefSaved }}</div>
      </section>

      <section class="card wide">
        <h2>Joined Classes</h2>
        <p v-if="classesLoading?.value" class="muted">Loading…</p>
        <p v-else-if="classesError?.value" class="muted">{{ classesError.value }}</p>

        <div v-else-if="joinedClasses.length" class="class-list">
          <router-link
            v-for="c in joinedClasses"
            :key="c.joinCode || `${c.classCode}-${c.quarter}-${c.year}`"
            class="class-item"
            :to="`/app/classes/${c.joinCode}`"
          >
            <div class="class-main">
              <div class="class-title">{{ c.title || c.classCode }}</div>
              <div class="class-sub">
                {{ [c.classCode, c.quarter, c.year].filter(Boolean).join(' • ') }}
                <span v-if="c.instructorName"> • {{ c.instructorName }}</span>
              </div>
            </div>
            <div class="class-code">{{ c.joinCode }}</div>
          </router-link>
        </div>

        <p v-else class="muted">No classes yet.</p>
      </section>
    </div>

    <div class="bottom-actions">
      <button class="btn" @click="logout">Logout</button>
      <button class="btn ghost danger" :disabled="deletingAccount" @click="deleteAccount">
        {{ deletingAccount ? "Deleting…" : "Delete account" }}
      </button>
    </div>

    <div v-if="deleteError" class="note err" style="margin-top: 12px">{{ deleteError }}</div>
  </div>
</template>

<style scoped>
.wrap {
  color: #e5e7eb;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.bottom-actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
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

.card-compact {
  padding: 10px;
}
.card-compact h2 {
  margin-bottom: 8px;
}
.card-compact .field {
  padding: 6px 0;
}
.card-compact .pref-row {
  margin-top: 6px;
}
.card-compact .status-trigger {
  padding: 8px 10px;
  border-radius: 12px;
}
.card-compact .status-menu {
  top: 46px;
}

.avatar-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 0 14px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04);
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
}

.avatar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.file {
  display: none;
}

.note {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 800;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(255,255,255,0.04);
}

.note.ok {
  border-color: rgba(34, 197, 94, 0.30);
  background: rgba(34, 197, 94, 0.10);
  color: rgba(187, 247, 208, 0.95);
}

.note.err {
  border-color: rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.10);
  color: rgba(254, 202, 202, 0.95);
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

.availability-value {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  display: inline-block;
  box-shadow: 0 0 0 3px rgba(255,255,255,0.05);
}

.dot-AVAILABLE { background: #86efac; }
.dot-IDLE { background: #facc15; }
.dot-DND { background: rgb(239, 68, 68); }
.dot-HIDDEN {
  background: transparent;
  border: 2px solid rgba(229,231,235,0.35);
  box-shadow: none;
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

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.10);
}
.btn.ghost:hover { background: rgba(255,255,255,0.09); }

.btn.ghost.danger {
  background: rgba(239, 68, 68, 0.22);
  border-color: rgba(239, 68, 68, 0.55);
  color: rgba(255, 255, 255, 0.95);
}
.btn.ghost.danger:hover {
  background: rgba(239, 68, 68, 0.32);
}

.pref-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
  position: relative;
}

.status-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(15,23,42,0.95);
  border: 1px solid rgba(255,255,255,0.10);
  color: #e5e7eb;
  font-weight: 900;
  cursor: pointer;
}

.status-trigger:hover {
  background: rgba(15,23,42,0.98);
  border-color: rgba(255,255,255,0.14);
}

.status-trigger:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.status-left {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.status-caret {
  color: rgba(229,231,235,0.85);
  font-weight: 900;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  border: 2px solid rgba(15,23,42,0.95);
}

.status-dot--available { background: #86efac; }
.status-dot--idle { background: #facc15; }
.status-dot--dnd { background: rgb(239, 68, 68); }
.status-dot--hidden {
  background: transparent;
  border: 2px solid rgba(229,231,235,0.35);
}

.status-menu {
  position: absolute;
  top: 52px;
  left: 0;
  right: 0;
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

.hint {
  color: #9ca3af;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.check {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid rgba(255,255,255,0.06);
  font-weight: 800;
}

.check:first-of-type {
  border-top: none;
}

.check input {
  width: 18px;
  height: 18px;
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.class-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 12px;
  border-radius: 16px;
  text-decoration: none;
  color: inherit;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
}

.class-item:hover {
  background: rgba(255,255,255,0.06);
}

.class-title {
  font-weight: 900;
}

.class-sub {
  margin-top: 4px;
  color: #9ca3af;
  font-size: 12px;
  font-weight: 700;
}

.class-code {
  align-self: center;
  color: #cbd5e1;
  font-weight: 900;
}

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
  .wide { grid-column: span 1; }
}
</style>