<script setup>
import { useRouter } from "vue-router";
import { computed, inject, onBeforeUnmount, onMounted, ref } from "vue";
const router = useRouter();

// --------- Student profile retrieval ----------
const me = inject("me", null);
const firstName = computed(() => me?.value?.firstName || "—");
const lastName = computed(() => me?.value?.lastName || "—");
const email = computed(() => me?.value?.email || "—");

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
onBeforeUnmount(() => {
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value);
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
      <h1>Student Profile</h1>
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

    <div class="bottom-actions">
      <button class="btn" @click="logout">Logout</button>
    </div>
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

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
  .wide { grid-column: span 1; }
}
</style>