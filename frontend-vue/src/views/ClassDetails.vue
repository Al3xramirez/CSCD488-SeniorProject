
<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import SyllabusUpload from "./SyllabusUpload.vue";

/**
 * Class details page. This is for professors to view details about a class, including the roster and syllabus management.
 * The join code is used to identify the class, and is passed as a route parameter.
 * This page also allows professors to delete the class, which will remove it from all enrolled users' dashboards and revoke access.
 * Students should not have access to this page nor the TA's
 */

const me = inject("me", null);

const role = computed(() => {
  const r = me?.value?.role;
  return (r || "STUDENT").toString().trim().toUpperCase();
});

const isProfessor = computed(() => role.value === "PROFESSOR");

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

const syllabusOpen = ref(false);

const rosterTAs = computed(() => rosterUsers.value.filter(u => (u?.role || "").toString().toUpperCase() === "TA"));
const rosterStudents = computed(() => rosterUsers.value.filter(u => (u?.role || "").toString().toUpperCase() === "STUDENT"));

async function safeErrorMessage(res) {
  try {
    const text = await res.text();
    return text || "";
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

function goBack() {
  router.push({ name: "classes" });
}

function toggleSyllabus() {
  syllabusOpen.value = !syllabusOpen.value;
}

onMounted(async () => {
  await loadClass();
  await loadRoster();
});

watch(joinCode, async () => {
  await loadClass();
  await loadRoster();
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

    <div v-if="!isProfessor" class="muted">
      This page is for professors.
    </div>

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
          <SyllabusUpload userRole="professor" :courseId="joinCode" />
        </div>
      </div>

      <div class="section">
        <div class="section-title">Enrolled Students</div>

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
.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 16px;
  color: #e5e7eb;
  box-shadow: 0 18px 40px rgba(0,0,0,0.25);
}

.card-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

h2 {
  margin: 0;
  font-size: 18px;
}

.muted {
  margin: 6px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

.alert {
  margin: 10px 0 14px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.10);
  color: #e5e7eb;
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
  min-width: 44px;
}

.btn:hover {
  background: #1d4ed8;
}

.btn.danger {
  background: rgb(239, 68, 68);
  border: 1px solid rgba(239, 68, 68, 0.55);
}

.btn.danger:hover {
  background: rgb(239, 68, 68);
  filter: brightness(0.92);
}

.btn.ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.10);
}

.btn.ghost:hover {
  background: rgba(255,255,255,0.09);
}

.btn.danger {
  white-space: nowrap;
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

.roster {
  margin-top: 12px;
}

.roster-section {
  margin-top: 12px;
}

.roster-section__title {
  font-size: 12px;
  color: #9ca3af;
  margin: 0 0 8px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.roster-list {
  display: grid;
  gap: 8px;
}

.roster-row {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
}

.roster-row__name {
  font-weight: 900;
  color: #e5e7eb;
}

.roster-row__role {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 900;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(15,23,42,0.65);
  display: grid;
  place-items: center;
}

.avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar__fallback {
  font-weight: 900;
  color: rgba(229,231,235,0.92);
}
</style>
