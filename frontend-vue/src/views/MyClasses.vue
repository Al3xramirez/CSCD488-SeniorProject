<script setup>
// ------- Imports and retrieving user info -------
import { computed, inject, onMounted, ref } from "vue";
import SyllabusUpload from "./SyllabusUpload.vue";

const me = inject("me", null);

const role = computed(() => {
  const r = me?.value?.role;
  return (r || "STUDENT").toString().trim().toUpperCase();
});

//------ Setting variables and functions for class management per user role -------
const isProfessor = computed(() => role.value === "PROFESSOR");
const isStudentOrTa = computed(() => role.value === "STUDENT" || role.value === "TA");

const classes = ref([]);
const loading = ref(false);
const error = ref("");

const joinCode = ref("");
const joining = ref(false);
const joinError = ref("");

const showCreate = ref(false);
const createError = ref("");
const creating = ref(false);

const form = ref({classCode: "", quarter: "",year: "",title: "",});

// ------- API call for loading classes, creating a class, and joining a class -------
//TODO Have another API call for leaving a class or for a professor to delete a class
async function loadMyClasses() {
  loading.value = true;
  error.value = "";

  try {
    const res = await fetch("/api/classes/mine", { credentials: "include" });
    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to load classes (${res.status})`);
    }

    const data = await res.json();
    classes.value = Array.isArray(data) ? data : [];
  } catch (e) {
    error.value = e?.message || "Failed to load classes";
  } finally {
    loading.value = false;
  }
}
// Function to open the create class modal
function openCreate() {
  createError.value = "";
  form.value = { classCode: "", quarter: "", year: "", title: "" };
  showCreate.value = true;
}
// Function to close the create class modal
function closeCreate() {
  showCreate.value = false;
  createError.value = "";
}

// Function that fetches the API to create a class
async function submitCreate() {
  //If check to ensure only professor can create class
  if (!isProfessor.value) return;

  creating.value = true;
  createError.value = "";

  // Basic validation to ensure all fields are filled
  try {
    const payload = {
      classCode: form.value.classCode,
      quarter: form.value.quarter,
      year: form.value.year,
      title: form.value.title,
    };
    //finally fetch call to create class endpoint with jsonified payload and credentials included
    const res = await fetch("/api/classes", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(payload),
    });

    //Throw error messge if not
    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to create class (${res.status})`);
    }
    // If successful, get the created class from the response and prepend it to the classes list so it shows immediately
    const created = await res.json();
    // Add new class to the list once the API is called, it is awaited to ensure it only gets added if the call is successful
    classes.value = [created, ...classes.value];

    closeCreate();
  } catch (e) {
    createError.value = e?.message || "Failed to create class";
  } finally {
    creating.value = false;
  }
}

// Function that fetches the API to join a class using a join code,same error handling, and validation as other function
async function submitJoin() {
  if (!isStudentOrTa.value) return;

  joining.value = true;
  joinError.value = "";

  try {
    const payload = { joinCode: (joinCode.value || "").trim().toUpperCase() };
    const res = await fetch("/api/classes/join", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to join class (${res.status})`);
    }

    const joined = await res.json();
    const key = `${joined.classCode}-${joined.quarter}-${joined.year}`;
    const existingKeys = new Set(classes.value.map(c => `${c.classCode}-${c.quarter}-${c.year}`));
    if (!existingKeys.has(key)) {
      classes.value = [joined, ...classes.value];
    }
    joinCode.value = "";
  } catch (e) {
    joinError.value = e?.message || "Failed to join class";
  } finally {
    joining.value = false;
  }
}

// Helper function to extract error message from response, with fallback
async function safeErrorMessage(res) {
  try {
    const text = await res.text();
    return text || "";
  } catch {
    return "";
  }
}

// Function to delete class, when class does delete, it filters out the deleted class from list
async function submitDelete(joinCode) {
  if (!isProfessor.value) return;

  try {
    const payload = { joinCode };
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

    const deleted = await res.json();
    classes.value = classes.value.filter(c => !(c.classCode === deleted.classCode && c.quarter === deleted.quarter && c.year === deleted.year));
  } catch (e) {
    error.value = e?.message || "Failed to delete class";
  }
}

function confirmAndDelete(c) {
  if (!isProfessor.value) return;
  const classLabel = `${c?.classCode || ""} ${c?.quarter || ""} ${c?.year || ""}`.trim();
  const ok = window.confirm(
    `Are you sure you want to delete this class${classLabel ? ` (${classLabel})` : ""}?`
  );
  if (!ok) return;
  submitDelete(c.joinCode);
}

// Syllabus upload modal state
const syllabusClass = ref(null);

function openSyllabusUpload(c) {
  syllabusClass.value = c;
}

function closeSyllabusUpload() {
  syllabusClass.value = null;
}

// Roster modal state
const rosterClass = ref(null);
const rosterLoading = ref(false);
const rosterError = ref("");
const rosterUsers = ref([]);
const photoFailed = ref({});

function closeRoster() {
  rosterClass.value = null;
  rosterUsers.value = [];
  rosterError.value = "";
  rosterLoading.value = false;
  photoFailed.value = {};
}

function rosterPhotoUrl(userId) {
  const jc = rosterClass.value?.joinCode;
  if (!jc || !userId) return "";
  return `/api/classes/${encodeURIComponent(jc)}/users/${encodeURIComponent(userId)}/photo`;
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

const rosterTAs = computed(() => rosterUsers.value.filter(u => (u?.role || "").toString().toUpperCase() === "TA"));
const rosterStudents = computed(() => rosterUsers.value.filter(u => (u?.role || "").toString().toUpperCase() === "STUDENT"));

async function openRoster(c) {
  if (!isProfessor.value) return;
  rosterClass.value = c;
  rosterUsers.value = [];
  rosterError.value = "";
  rosterLoading.value = true;
  photoFailed.value = {};

  try {
    const res = await fetch(`/api/classes/${encodeURIComponent(c.joinCode)}/students`, { credentials: "include" });
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

// Edit class modal state
const editClassItem = ref(null);
const editForm      = ref({ classCode: "", quarter: "", year: "", title: "" });
const editError     = ref("");
const editing       = ref(false);

function openEdit(c) {
  editClassItem.value = c;
  editForm.value = { classCode: c.classCode, quarter: c.quarter, year: c.year, title: c.title };
  editError.value = "";
}

function closeEdit() {
  editClassItem.value = null;
  editError.value = "";
}

async function submitEdit() {
  if (!isProfessor.value || !editClassItem.value) return;
  editing.value = true;
  editError.value = "";
  try {
    const payload = {
      joinCode:  editClassItem.value.joinCode,
      classCode: editForm.value.classCode,
      quarter:   editForm.value.quarter,
      year:      editForm.value.year,
      title:     editForm.value.title,
    };
    const res = await fetch("/api/classes/update", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const msg = await safeErrorMessage(res);
      throw new Error(msg || `Failed to update class (${res.status})`);
    }
    const updated = await res.json();
    classes.value = classes.value.map(c =>
      c.joinCode === updated.joinCode ? updated : c
    );
    closeEdit();
  } catch (e) {
    editError.value = e?.message || "Failed to update class";
  } finally {
    editing.value = false;
  }
}

/* onMounted is used to call the LoadMyclasses function when the component is first mounted.
just to make sure users classes are loaded when they open up the page */
onMounted(loadMyClasses);
</script>

<template>
  <!-- Main card for displaying user's classes, if student or ta, it shows classes, if prof, it shows a button where you can add a class -->
  <section class="card">
    <div class="card-header">
      <div>
        <h2>My Classes</h2>
        <p class="muted" v-if="isProfessor">
          Classes you’ve created. Use Create class to add another.
        </p>
        <p class="muted" v-else-if="isStudentOrTa">
          Classes you’ve joined.
        </p>
      </div>
    </div>

    <div v-if="error" class="alert">
      {{ error }}
    </div>
    <!-- If the user is a student or ta, show the join code input and button -->
    <div v-if="isStudentOrTa">
      <div v-if="joinError" class="alert">{{ joinError }}</div>
      <div class="row">
        <input v-model="joinCode" class="input" placeholder="Join code (ex: 8H7K2M9Q)" />
        <button class="btn" type="button" :disabled="joining" @click="submitJoin">
          {{ joining ? "Joining…" : "Join" }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="muted">Loading…</div>
    <!-- If not loading and no error, show the classes or an empty state message -->
    <div v-else class="class-grid">
      <!-- If the user is a professor, show the create class button -->
      <button
        v-if="isProfessor"
        type="button"
        class="class-box create-class"
        @click="openCreate"
        aria-label="Create class"
      >
        <span class="create-class__plus" aria-hidden="true">+</span>
        <span class="create-class__label">Create class</span>
      </button>

      <div
        v-for="c in classes"
        :key="`${c.classCode}-${c.quarter}-${c.year}`"
        class="class-box"
        :class="{ 'class-box--clickable': isProfessor }"
        role="button"
        :tabindex="isProfessor ? 0 : undefined"
        @click="openRoster(c)"
        @keydown.enter.prevent="openRoster(c)"
        @keydown.space.prevent="openRoster(c)"
      >
        <div class="class-top">
          <div class="class-code">{{ c.classCode }} · {{ c.quarter }} {{ c.year }}</div>
          <div v-if="isProfessor" class="class-actions">
            <button class="btn ghost btn-sm" type="button" @click.stop="openEdit(c)">Edit</button>
            <button class="btn ghost btn-sm delete-btn" type="button" @click.stop="confirmAndDelete(c)">Delete</button>
          </div>
        </div>
        <div class="class-title">{{ c.title }}</div>
        <div v-if="isProfessor" class="class-join">Join code: <span class="join-code">{{ c.joinCode }}</span></div>
        <button
          v-if="isProfessor"
          class="btn ghost btn-sm syllabus-btn"
          type="button"
          @click.stop="openSyllabusUpload(c)"
        >
          Manage Syllabus
        </button>
      </div>

      <div v-if="!classes.length" class="empty">
        <span v-if="isProfessor">No classes yet — click Create class to add one.</span>
        <span v-else-if="isStudentOrTa">No classes yet — enter a join code above.</span>
      </div>
    </div>
  </section>

  <!-- Creates the overlay for creating a new class -->
  <div v-if="showCreate" class="overlay" @click.self="closeCreate">
    <div class="modal" role="dialog" aria-modal="true" aria-label="Create class">
      <div class="modal-header">
        <h3>Create a Class</h3>
        <button class="btn ghost" type="button" @click="closeCreate">Close</button>
      </div>

      <div v-if="createError" class="alert">
        {{ createError }}
      </div>

      <div class="form">
        <label class="field">
          <span class="label">Class Code</span>
          <input v-model="form.classCode" class="input" placeholder="CSCD 488" />
        </label>

        <label class="field">
          <span class="label">Quarter</span>
          <input v-model="form.quarter" class="input" placeholder="Fall" />
        </label>

        <label class="field">
          <span class="label">Year</span>
          <input v-model="form.year" class="input" placeholder="2026" />
        </label>

        <label class="field">
          <span class="label">Title</span>
          <input v-model="form.title" class="input" placeholder="Senior Project" />
        </label>
      </div>

      <div class="modal-actions">
        <button class="btn" type="button" :disabled="creating" @click="submitCreate">
          {{ creating ? "Creating…" : "Create" }}
        </button>
      </div>
    </div>
  </div>

  <!-- Edit class modal -->
  <div v-if="editClassItem" class="overlay" @click.self="closeEdit">
    <div class="modal" role="dialog" aria-modal="true" aria-label="Edit class">
      <div class="modal-header">
        <h3>Edit Class</h3>
        <button class="btn ghost" type="button" @click="closeEdit">Close</button>
      </div>

      <div v-if="editError" class="alert">{{ editError }}</div>

      <div class="form">
        <label class="field">
          <span class="label">Class Code</span>
          <input v-model="editForm.classCode" class="input" placeholder="CSCD 488" />
        </label>
        <label class="field">
          <span class="label">Quarter</span>
          <input v-model="editForm.quarter" class="input" placeholder="Fall" />
        </label>
        <label class="field">
          <span class="label">Year</span>
          <input v-model="editForm.year" class="input" placeholder="2026" />
        </label>
        <label class="field">
          <span class="label">Title</span>
          <input v-model="editForm.title" class="input" placeholder="Senior Project" />
        </label>
      </div>

      <div class="modal-actions">
        <button class="btn" type="button" :disabled="editing" @click="submitEdit">
          {{ editing ? "Saving…" : "Save changes" }}
        </button>
      </div>
    </div>
  </div>

  <!-- Syllabus upload modal -->
  <div v-if="syllabusClass" class="overlay" @click.self="closeSyllabusUpload">
    <div class="modal modal--wide" role="dialog" aria-modal="true" aria-label="Upload syllabus">
      <div class="modal-header">
        <h3>{{ syllabusClass.classCode }} · {{ syllabusClass.quarter }} {{ syllabusClass.year }}</h3>
        <button class="btn ghost" type="button" @click="closeSyllabusUpload">Close</button>
      </div>
      <SyllabusUpload
        userRole="professor"
        :courseId="syllabusClass.joinCode"
        @saved="closeSyllabusUpload"
      />
    </div>
  </div>

  <!-- Roster modal (professor) -->
  <div v-if="rosterClass" class="overlay" @click.self="closeRoster">
    <div class="modal modal--wide" role="dialog" aria-modal="true" aria-label="Class roster">
      <div class="modal-header">
        <h3>{{ rosterClass.classCode }} · {{ rosterClass.quarter }} {{ rosterClass.year }} — Roster</h3>
        <button class="btn ghost" type="button" @click="closeRoster">Close</button>
      </div>

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

        <!-- Fallback for unexpected roles -->
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
  </div>
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

h3 {
  margin: 0;
  font-size: 16px;
}

.muted {
  margin: 6px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

.row {
  margin-top: 12px;
  display: flex;
  gap: 10px;
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

.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn.ghost {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.10);
}

.btn.ghost:hover {
  background: rgba(255,255,255,0.09);
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
  margin-top: 8px;
}

.class-box {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07);
}

.class-box--clickable {
  cursor: pointer;
}

.class-box--clickable:hover {
  border-color: rgba(255,255,255,0.12);
  background: rgba(255,255,255,0.035);
}

.create-class {
  min-height: 126px;
  width: 100%;
  display: grid;
  place-items: center;
  gap: 10px;
  cursor: pointer;
  background: rgba(37,99,235,0.14);
  border: 1px dashed rgba(37,99,235,0.55);
  color: #e5e7eb;
  text-align: center;
}

.create-class:hover {
  background: rgba(37,99,235,0.18);
  border-color: rgba(37,99,235,0.70);
}

.create-class:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(37,99,235,0.45);
}

.create-class__plus {
  font-size: 46px;
  line-height: 1;
  font-weight: 900;
  color: rgba(229,231,235,0.92);
}

.create-class__label {
  font-weight: 900;
  letter-spacing: 0.2px;
}

.class-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.btn.btn-sm {
  padding: 6px 10px;
  border-radius: 12px;
  min-width: auto;
}

.class-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.delete-btn {
  white-space: nowrap;
}

.syllabus-btn {
  margin-top: 10px;
  width: 100%;
  justify-content: center;
}

.modal--wide {
  max-width: 680px;
  max-height: 90vh;
  overflow-y: auto;
}

.class-code {
  font-weight: 900;
  color: #e5e7eb;
}

.class-title {
  margin-top: 6px;
  color: #9ca3af;
  font-size: 13px;
}

.class-join {
  margin-top: 10px;
  font-size: 12px;
  color: #9ca3af;
}

.join-code {
  color: #e5e7eb;
  font-weight: 900;
}

.empty {
  grid-column: 1 / -1;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.10);
  color: #9ca3af;
  font-size: 13px;
}

/* Modal */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.55);
  display: grid;
  place-items: center;
  padding: 18px;
  z-index: 50;
}

.modal {
  width: 100%;
  max-width: 520px;
  background: rgba(15,23,42,0.95);
  border: 1px solid rgba(255,255,255,0.10);
  border-radius: 18px;
  padding: 16px;
  color: #e5e7eb;
  box-shadow: 0 18px 40px rgba(0,0,0,0.35);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-top: 12px;
}

.field {
  display: grid;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: #9ca3af;
}

.input {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,0.10);
  background: rgba(15,23,42,0.6);
  color: #e5e7eb;
  outline: none;
}

.input:focus {
  box-shadow: 0 0 0 2px rgba(37,99,235,0.5);
  border-color: rgba(37,99,235,0.6);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

/* Roster */
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
