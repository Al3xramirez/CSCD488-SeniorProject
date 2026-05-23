
<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import SyllabusUpload from "./SyllabusUpload.vue";

const me = inject("me", null);

const role = computed(() => {
  const r = me?.value?.role;
  return (r || "STUDENT").toString().trim().toUpperCase();
});
const isProfessor = computed(() => role.value === "PROFESSOR");
const isStudentView = computed(() => !isProfessor.value);

const route = useRoute();
const router = useRouter();

const joinCode = computed(() => (route.params.joinCode || "").toString().trim().toUpperCase());

const classItem = ref(null);
const loadingClass = ref(false);
const error = ref("");

const rosterLoading = ref(false);
const rosterError = ref("");
const rosterUsers = ref([]);
const photoFailed = ref({});

const staffLoading = ref(false);
const staffError = ref("");
const staffUsers = ref([]);
const staffPhotoFailed = ref({});

const addTaEmail = ref("");
const addTaLoading = ref(false);
const addTaError = ref("");
const addTaSuccess = ref("");

const syllabusOpen = ref(false);

const rosterTAs = computed(() => rosterUsers.value.filter(u => (u?.role || "").toString().toUpperCase() === "TA"));
const rosterStudents = computed(() => rosterUsers.value.filter(u => (u?.role || "").toString().toUpperCase() === "STUDENT"));

async function safeErrorMessage(res) {
  try {
    const text = await res.text();
    try {
      const json = JSON.parse(text);
      return json.message || json.error || text;
    } catch {
      return text || "";
    }
  } catch {
    return "";
  }
}

async function loadClass() {
  if (!joinCode.value) return;
  loadingClass.value = true;
  error.value = "";

  try {
    const res = await fetch("/api/classes/mine", { credentials: "include" });
    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to load classes (${res.status})`);
    }

    const data = await res.json();
    const list = Array.isArray(data) ? data : [];
    classItem.value = list.find(c => (c?.joinCode || "").toString().toUpperCase() === joinCode.value) || null;

    if (!classItem.value) {
      throw new Error("Class not found (or you no longer have access).");
    }
  } catch (e) {
    error.value = e?.message || "Failed to load class";
    classItem.value = null;
  } finally {
    loadingClass.value = false;
  }
}

function rosterPhotoUrl(userId) {
  if (!joinCode.value || !userId) return "";
  return `/api/classes/${encodeURIComponent(joinCode.value)}/users/${encodeURIComponent(userId)}/photo`;
}

function staffPhotoUrl(userId) {
  if (!joinCode.value || !userId) return "";
  return `/api/classes/${encodeURIComponent(joinCode.value)}/staff/${encodeURIComponent(userId)}/photo`;
}

function initials(u) {
  const first = (u?.firstName || "").trim();
  const last = (u?.lastName || "").trim();
  const a = first ? first[0] : "";
  const b = last ? last[0] : "";
  const out = (a + b).toUpperCase();
  return out || "?";
}

function markPhotoFailed(userId) {
  if (!userId) return;
  photoFailed.value = { ...photoFailed.value, [userId]: true };
}

// ---- Functions to get staff photos, and availability status ----

function markStaffPhotoFailed(userId) {
  if (!userId) return;
  staffPhotoFailed.value = { ...staffPhotoFailed.value, [userId]: true };
}

function statusKey(u) {
  return (u?.availabilityStatus || "HIDDEN").toString().trim().toUpperCase();
}

function statusDotClass(u) {
  const s = statusKey(u).toLowerCase();
  return `status-dot status-dot--${s}`;
}

// loadStaff fetches the instructor and TA information for the class, 
// including their availability status and profile photos.

async function loadStaff() {
  if (!joinCode.value || !isStudentView.value) return;

  staffUsers.value = [];
  staffError.value = "";
  staffLoading.value = true;
  staffPhotoFailed.value = {};

  try {
    const [instrRes, tasRes] = await Promise.all([
      fetch(`/api/classes/${encodeURIComponent(joinCode.value)}/instructor`, { credentials: "include" }),
      fetch(`/api/classes/${encodeURIComponent(joinCode.value)}/tas`, { credentials: "include" }),
    ]);

    if (!instrRes.ok) {
      const msg = await safeErrorMessage(instrRes);
      throw new Error(msg || `Failed to load instructor (${instrRes.status})`);
    }

    const instructor = await instrRes.json();

    const tas = tasRes.ok ? await tasRes.json() : [];
    const taList = Array.isArray(tas) ? tas : [];

    const combined = [instructor, ...taList]
      .filter(Boolean);

    const seen = new Set();
    staffUsers.value = combined.filter(u => {
      const id = (u?.userId || "").toString();
      if (!id || seen.has(id)) return false;
      seen.add(id);
      return true;
    });
  } catch (e) {
    staffError.value = e?.message || "Failed to load staff";
  } finally {
    staffLoading.value = false;
  }
}

async function loadRoster() {
  if (!isProfessor.value || !joinCode.value) return;

  rosterUsers.value = [];
  rosterError.value = "";
  rosterLoading.value = true;
  photoFailed.value = {};

  try {
    const res = await fetch(`/api/classes/${encodeURIComponent(joinCode.value)}/students`, { credentials: "include" });
    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to load roster (${res.status})`);
    }

    const data = await res.json();
    rosterUsers.value = Array.isArray(data) ? data : [];
  } catch (e) {
    rosterError.value = e?.message || "Failed to load roster";
  } finally {
    rosterLoading.value = false;
  }
}

async function deleteClass() {
  if (!isProfessor.value || !joinCode.value) return;

  const classLabel = classItem.value
    ? `${classItem.value.classCode || ""} ${classItem.value.quarter || ""} ${classItem.value.year || ""}`.trim()
    : joinCode.value;

  const ok = window.confirm(
    `Are you sure you want to delete this class${classLabel ? ` (${classLabel})` : ""}?`
  );
  if (!ok) return;

  try {
    const payload = { joinCode: joinCode.value };
    const res = await fetch("/api/classes/delete", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to delete class (${res.status})`);
    }

    await router.push({ name: "classes" });
  } catch (e) {
    error.value = e?.message || "Failed to delete class";
  }
}

async function submitAddTa() {
  if (!isProfessor.value || !joinCode.value) return;
  addTaLoading.value = true;
  addTaError.value = "";
  addTaSuccess.value = "";

  try {
    const res = await fetch(`/api/classes/${encodeURIComponent(joinCode.value)}/ta`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ taEmail: addTaEmail.value.trim() }),
    });

    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to add TA (${res.status})`);
    }

    const ta = await res.json();
    rosterUsers.value = [...rosterUsers.value, ta];
    addTaSuccess.value = `${ta.firstName} ${ta.lastName} added as TA.`;
    addTaEmail.value = "";
  } catch (e) {
    addTaError.value = e?.message || "Failed to add TA";
  } finally {
    addTaLoading.value = false;
  }
}

function goBack() {
  router.push({ name: "classes" });
}

function toggleSyllabus() {
  syllabusOpen.value = !syllabusOpen.value;
}

onMounted(async () => {
  await loadClass();
  await loadRoster();
  await loadStaff();
});

watch(joinCode, async () => {
  await loadClass();
  await loadRoster();
  await loadStaff();
});
</script>

<template>
  <section class="card">
    <div class="card-header">
      <div>
        <h2 v-if="classItem">{{ classItem.classCode }} · {{ classItem.quarter }} {{ classItem.year }}</h2>
        <h2 v-else>Class</h2>
        <p v-if="classItem" class="muted">{{ classItem.title }}</p>
        <p v-else-if="loadingClass" class="muted">Loading…</p>
      </div>

      <button class="btn" type="button" @click="goBack">Back</button>
    </div>

    <div v-if="error" class="alert">{{ error }}</div>

    <template v-if="isStudentView">
      <div class="section">
        <div class="section-title">Professor &amp; TAs</div>

        <div v-if="staffError" class="alert">{{ staffError }}</div>
        <div v-else-if="staffLoading" class="muted">Loading…</div>

        <div v-else class="roster">
          <div class="roster-list">
            <div v-for="u in staffUsers" :key="u.userId" class="roster-row">
              <div class="avatar" aria-label="Profile photo">
                <img
                  v-if="!staffPhotoFailed[u.userId]"
                  class="avatar__img"
                  :src="staffPhotoUrl(u.userId)"
                  :alt="`${u.firstName} ${u.lastName}`"
                  @error="markStaffPhotoFailed(u.userId)"
                />
                <div v-else class="avatar__fallback">{{ initials(u) }}</div>
                <span v-if="statusKey(u) !== 'HIDDEN'" :class="statusDotClass(u)" aria-hidden="true" />
              </div>

              <div>
                <div class="roster-row__name">{{ u.firstName }} {{ u.lastName }}</div>
                <div class="roster-row__role">{{ (u.role || '').toString().toUpperCase() }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div v-if="classItem" class="meta">
        <div class="meta-row">
          <div class="meta-label">Join code</div>
          <div class="meta-value"><span class="join-code">{{ classItem.joinCode }}</span></div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">Actions</div>
        <div class="actions">
          <button class="btn danger" type="button" @click="deleteClass">Delete class</button>
        </div>
      </div>

      <div class="section">
        <div class="section-title">Syllabus</div>
        <button class="btn ghost" type="button" @click="toggleSyllabus">
          {{ syllabusOpen ? "Hide syllabus upload" : "Manage syllabus" }}
        </button>

        <div v-if="syllabusOpen" class="syllabus-body">
          <SyllabusUpload
            userRole="professor"
            :courseId="joinCode"
            :classCode="classItem?.classCode"
            :quarter="classItem?.quarter"
            :year="classItem?.year"
          />
        </div>
      </div>

      <div class="section">
        <div class="section-title">Enrolled Students</div>

        <div class="add-ta-row">
          <input v-model="addTaEmail" class="input" placeholder="TA email address" type="email" />
          <button class="btn" type="button" :disabled="addTaLoading || !addTaEmail.trim()" @click="submitAddTa">
            {{ addTaLoading ? "Adding…" : "Add TA" }}
          </button>
        </div>
        <div v-if="addTaError" class="alert">{{ addTaError }}</div>
        <div v-if="addTaSuccess" class="alert alert--success">{{ addTaSuccess }}</div>

        <div v-if="rosterError" class="alert">{{ rosterError }}</div>
        <div v-else-if="rosterLoading" class="muted">Loading roster…</div>

        <div v-else class="roster">
          <div v-if="!rosterUsers.length" class="muted">No students enrolled yet.</div>

          <div v-if="rosterTAs.length" class="roster-section">
            <div class="roster-section__title">TAs</div>
            <div class="roster-list">
              <div v-for="u in rosterTAs" :key="u.userId" class="roster-row">
                <div class="avatar">
                  <img
                    v-if="!photoFailed[u.userId]"
                    class="avatar__img"
                    :src="rosterPhotoUrl(u.userId)"
                    alt=""
                    @error="markPhotoFailed(u.userId)"
                  />
                  <div v-else class="avatar__fallback" aria-hidden="true">{{ initials(u) }}</div>
                </div>
                <div class="roster-row__name">{{ u.firstName }} {{ u.lastName }}</div>
                <div class="roster-row__role">TA</div>
              </div>
            </div>
          </div>

          <div v-if="rosterStudents.length" class="roster-section">
            <div class="roster-section__title">Students</div>
            <div class="roster-list">
              <div v-for="u in rosterStudents" :key="u.userId" class="roster-row">
                <div class="avatar">
                  <img
                    v-if="!photoFailed[u.userId]"
                    class="avatar__img"
                    :src="rosterPhotoUrl(u.userId)"
                    alt=""
                    @error="markPhotoFailed(u.userId)"
                  />
                  <div v-else class="avatar__fallback" aria-hidden="true">{{ initials(u) }}</div>
                </div>
                <div class="roster-row__name">{{ u.firstName }} {{ u.lastName }}</div>
                <div class="roster-row__role">Student</div>
              </div>
            </div>
          </div>

          <div v-if="!rosterTAs.length && !rosterStudents.length && rosterUsers.length" class="roster-section">
            <div class="roster-section__title">Enrolled</div>
            <div class="roster-list">
              <div v-for="u in rosterUsers" :key="u.userId" class="roster-row">
                <div class="avatar">
                  <img
                    v-if="!photoFailed[u.userId]"
                    class="avatar__img"
                    :src="rosterPhotoUrl(u.userId)"
                    alt=""
                    @error="markPhotoFailed(u.userId)"
                  />
                  <div v-else class="avatar__fallback" aria-hidden="true">{{ initials(u) }}</div>
                </div>
                <div class="roster-row__name">{{ u.firstName }} {{ u.lastName }}</div>
                <div class="roster-row__role">{{ (u.role || "").toString().toUpperCase() }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  position: absolute;
  right: 3px;
  bottom: 3px;
  z-index: 2;
  pointer-events: none;
  border: 2px solid rgba(15,23,42,0.95);
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
}

.avatar {
  position: relative;
}
</style>

<style scoped>
h2 {
  margin: 0;
  font-size: 18px;
}

.meta {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.meta-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
}

.meta-label {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.meta-value {
  font-weight: 900;
  color: #e5e7eb;
}

.join-code {
  color: #e5e7eb;
  font-weight: 900;
}

.section {
  margin-top: 16px;
}

.section-title {
  font-size: 12px;
  color: #9ca3af;
  margin: 0 0 8px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.actions {
  display: flex;
  gap: 10px;
}

.syllabus-body {
  margin-top: 12px;
}

.add-ta-row {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.add-ta-row .input {
  flex: 1;
}

</style>
