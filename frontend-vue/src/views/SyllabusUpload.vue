<template>
  <div class="card">

    <!-- Professor-only gate -->
    <div v-if="!isProfessor" class="role-gate">
      <div class="role-gate__icon">🔒</div>
      <p class="role-gate__msg">Only professors can upload syllabuses.</p>
      <p class="role-gate__sub">Students can view syllabus info from their enrolled courses.</p>
    </div>

    <template v-else>
      <div class="uploader-header">
        <h1>Syllabus Upload</h1>
        <span class="role-badge">Professor</span>
      </div>

      <!-- Step 0: Select class (standalone mode — no courseId prop) -->
      <div v-if="step === 'select'" class="select-step">
        <p class="select-sub">Choose which class to attach the syllabus to.</p>

        <div v-if="loadingClasses" class="muted-center">Loading classes…</div>

        <template v-else>
          <div v-if="myClasses.length" class="class-list">
            <button
              v-for="c in myClasses"
              :key="c.joinCode"
              class="class-option"
              @click="pickClass(c)"
            >
              <span class="class-option__code">{{ c.classCode }} · {{ c.quarter }} {{ c.year }}</span>
              <span class="class-option__title">{{ c.title }}</span>
            </button>
          </div>

          <p v-else class="no-classes-msg">You haven't created any classes yet.</p>

          <button class="btn-create-class" @click="showCreateClass = true">+ Create a new class</button>
        </template>
      </div>

      <!-- Step 1: Upload -->
      <div v-if="step === 'upload'">
        <!-- Loading existing syllabus -->
        <div v-if="fetchingExisting" class="fetching-state">
          <span class="spinner" />
          <span>Loading existing syllabus…</span>
        </div>

        <!-- Selected class banner (standalone mode) -->
        <div v-if="standaloneMode && selectedClass" class="selected-class-banner">
          <span class="selected-class-label">
            {{ selectedClass.classCode }} · {{ selectedClass.quarter }} {{ selectedClass.year }}
            <span class="selected-class-title">— {{ selectedClass.title }}</span>
          </span>
          <button class="btn-change-class" @click="step = 'select'">Change</button>
        </div>

        <div
          class="drop-zone"
          :class="{ 'drop-zone--active': isDragging, 'drop-zone--has-file': !!fileName }"
          @dragover.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @drop.prevent="onDrop"
        >
          <template v-if="!fileName">
            <p class="drop-zone__hint">Drop your syllabus PDF here or</p>
            <label class="btn-browse">
              Browse file
              <input type="file" accept="application/pdf" @change="onFileChange" hidden />
            </label>
            <p class="drop-zone__sub">PDF only</p>
          </template>
          <template v-else>
            <span class="drop-zone__file-name">{{ fileName }}</span>
            <button class="btn-clear" @click="clearFile">Remove</button>
          </template>
        </div>

        <button class="btn-primary" :disabled="!pdfFile || loading" @click="parseSyllabus">
          <span v-if="loading" class="spinner" />
          <span v-else>Extract &amp; review</span>
        </button>

        <p v-if="error" class="error-msg">{{ error }}</p>
      </div>

      <!-- Step 2: Review & Edit -->
      <div v-if="step === 'review'" class="review-panel">
        <div class="review-header">
          <div>
            <h2 class="review-title">{{ isEditing ? 'Edit syllabus' : 'Review extracted info' }}</h2>
            <p class="review-sub">{{ isEditing ? 'Update any field and save.' : 'Edit any field before saving. Flagged fields had lower confidence — please verify.' }}</p>
          </div>
          <button class="btn-ghost" @click="resetToUpload">← Re-upload</button>
        </div>

        <div class="legend">
          <span class="conf-dot conf-dot--high" /> High confidence
          <span class="conf-dot conf-dot--medium" /> Review recommended
          <span class="conf-dot conf-dot--low" /> Needs verification
        </div>

        <!-- Class Meeting Times -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Class meeting times</span>
            <span class="conf-badge" :class="`conf-badge--${draft.classMeetingTimes.confidence}`">{{ draft.classMeetingTimes.confidence }}</span>
          </div>
          <div class="field-section__body" :class="{ 'field-section__body--warn': draft.classMeetingTimes.confidence !== 'high' }">
            <div class="day-picker">
              <label v-for="d in ALL_DAYS" :key="d" class="day-chip" :class="{ 'day-chip--active': draft.classMeetingTimes.value.days.includes(d) }">
                <input type="checkbox" :value="d" v-model="draft.classMeetingTimes.value.days" hidden />
                {{ d }}
              </label>
            </div>
            <div class="row">
              <div class="select-group">
                <label class="select-label">Start</label>
                <input class="edit-input edit-input--time" type="time" v-model="draft.classMeetingTimes.value.startTime" />
              </div>
              <div class="select-group">
                <label class="select-label">End</label>
                <input class="edit-input edit-input--time" type="time" v-model="draft.classMeetingTimes.value.endTime" />
              </div>
              <div class="select-group" style="flex:1">
                <label class="select-label">Location</label>
                <input class="edit-input edit-input--grow" type="text" v-model="draft.classMeetingTimes.value.location" placeholder="Room / building" />
              </div>
            </div>
            <div class="row" style="margin-top: 4px;">
              <div class="select-group" style="flex:1">
                <label class="select-label">Quarter Start Date</label>
                <input class="edit-input edit-input--date" type="date" v-model="draft.classMeetingTimes.value.startDate" />
              </div>
              <div class="select-group" style="flex:1">
                <label class="select-label">Quarter End Date</label>
                <input class="edit-input edit-input--date" type="date" v-model="draft.classMeetingTimes.value.endDate" />
              </div>
            </div>
          </div>
        </section>

        <!-- Office Hours -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Office hours</span>
            <span class="conf-badge" :class="`conf-badge--${draft.officeHours.confidence}`">{{ draft.officeHours.confidence }}</span>
          </div>
          <textarea class="edit-textarea" :class="{ 'edit-textarea--warn': draft.officeHours.confidence !== 'high' }"
            v-model="draft.officeHours.value" rows="2" placeholder="Schedule and location..." />
        </section>

        <!-- Grade Scale -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Grade scale</span>
            <span class="conf-badge" :class="`conf-badge--${draft.gradeScale.confidence}`">{{ draft.gradeScale.confidence }}</span>
          </div>
          <div class="field-section__body" :class="{ 'field-section__body--warn': draft.gradeScale.confidence !== 'high' }">
            <div v-for="(g, i) in draft.gradeScale.value" :key="i" class="row">
              <input class="edit-input edit-input--sm" v-model="g.letter" placeholder="A" />
              <input class="edit-input edit-input--grow" v-model="g.range" placeholder="93–100%" />
              <button class="btn-remove-row" @click="draft.gradeScale.value.splice(i, 1)">✕</button>
            </div>
            <button class="btn-add-row" @click="draft.gradeScale.value.push({ letter: '', range: '' })">+ Add grade</button>
          </div>
        </section>

        <!-- Grade Breakdown -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Grade breakdown</span>
            <span class="conf-badge" :class="`conf-badge--${draft.gradeBreakdown.confidence}`">{{ draft.gradeBreakdown.confidence }}</span>
          </div>
          <div class="field-section__body" :class="{ 'field-section__body--warn': draft.gradeBreakdown.confidence !== 'high' }">
            <div v-for="(item, i) in draft.gradeBreakdown.value" :key="i" class="row">
              <input class="edit-input edit-input--grow" v-model="item.component" placeholder="Assignments" />
              <input class="edit-input edit-input--pct" v-model="item.weight" placeholder="60%" />
              <button class="btn-remove-row" @click="draft.gradeBreakdown.value.splice(i, 1)">✕</button>
            </div>
            <button class="btn-add-row" @click="draft.gradeBreakdown.value.push({ component: '', weight: '' })">+ Add component</button>
          </div>
        </section>

        <!-- Pass Conditions -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Pass conditions</span>
            <span class="conf-badge" :class="`conf-badge--${draft.passConditions.confidence}`">{{ draft.passConditions.confidence }}</span>
          </div>
          <div class="field-section__body" :class="{ 'field-section__body--warn': draft.passConditions.confidence !== 'high' }">
            <div v-for="(_, i) in draft.passConditions.value" :key="i" class="row">
              <input class="edit-input edit-input--grow" v-model="draft.passConditions.value[i]" placeholder="e.g. Minimum 70% exam average required to pass" />
              <button class="btn-remove-row" @click="draft.passConditions.value.splice(i, 1)">✕</button>
            </div>
            <button class="btn-add-row" @click="draft.passConditions.value.push('')">+ Add condition</button>
          </div>
        </section>

        <!-- Attendance -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Attendance</span>
            <span class="conf-badge" :class="`conf-badge--${draft.attendance.confidence}`">{{ draft.attendance.confidence }}</span>
          </div>
          <div class="field-section__body" :class="{ 'field-section__body--warn': draft.attendance.confidence !== 'high' }">
            <div class="row row--wrap">
              <div class="select-group">
                <label class="select-label">Tracked?</label>
                <select class="edit-select" v-model="draft.attendance.value.tracked">
                  <option value="yes">Yes</option>
                  <option value="no">No</option>
                  <option value="partial">Partial</option>
                </select>
              </div>
              <div class="select-group">
                <label class="select-label">Affects grade?</label>
                <select class="edit-select" v-model="draft.attendance.value.affectsGrade">
                  <option value="yes">Yes</option>
                  <option value="no">No</option>
                  <option value="extra_credit">Extra credit only</option>
                </select>
              </div>
            </div>
            <textarea class="edit-textarea edit-textarea--inbody" v-model="draft.attendance.value.details"
              rows="2" placeholder="Details about attendance policy..." />
          </div>
        </section>

        <!-- Due Dates -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Due dates</span>
            <span class="conf-badge" :class="`conf-badge--${draft.dueDates.confidence}`">{{ draft.dueDates.confidence }}</span>
          </div>
          <div class="field-section__body" :class="{ 'field-section__body--warn': draft.dueDates.confidence !== 'high' }">
            <div v-for="(item, i) in draft.dueDates.value" :key="i" class="row">
              <input class="edit-input edit-input--grow" v-model="item.name" placeholder="Assignment name" />
              <input class="edit-input edit-input--date" v-model="item.date" placeholder="Date" />
              <button class="btn-remove-row" @click="draft.dueDates.value.splice(i, 1)">✕</button>
            </div>
            <button class="btn-add-row" @click="draft.dueDates.value.push({ name: '', date: '' })">+ Add date</button>
          </div>
        </section>

        <!-- Late Work Policy -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">Late work policy</span>
            <span class="conf-badge" :class="`conf-badge--${draft.lateWorkPolicy.confidence}`">{{ draft.lateWorkPolicy.confidence }}</span>
          </div>
          <textarea class="edit-textarea" :class="{ 'edit-textarea--warn': draft.lateWorkPolicy.confidence !== 'high' }"
            v-model="draft.lateWorkPolicy.value" rows="3" placeholder="Describe the late work policy..." />
        </section>

        <!-- AI Policy -->
        <section class="field-section">
          <div class="field-section__header">
            <span class="field-label">AI policy</span>
            <span class="conf-badge" :class="`conf-badge--${draft.aiPolicy.confidence}`">{{ draft.aiPolicy.confidence }}</span>
          </div>
          <textarea class="edit-textarea" :class="{ 'edit-textarea--warn': draft.aiPolicy.confidence !== 'high' }"
            v-model="draft.aiPolicy.value" rows="3" placeholder="AI/tool use policy, or leave blank if not mentioned..." />
        </section>

        <div class="review-actions">
          <p v-if="lowConfidenceCount > 0" class="warn-summary">
            ⚠ {{ lowConfidenceCount }} field{{ lowConfidenceCount > 1 ? 's' : '' }} flagged — please review before saving.
          </p>
          <button class="btn-primary" @click="confirmSave" :disabled="saving">
            <span v-if="saving" class="spinner" />
            <span v-else>Confirm &amp; save</span>
          </button>
        </div>
      </div>

      <!-- Step 3: Saved -->
      <div v-if="step === 'saved'" class="saved-state">
        <div class="saved-state__icon">✓</div>
        <h3 class="saved-state__title">Syllabus saved</h3>
        <p class="saved-state__sub">Students enrolled in this course can now view the syllabus information.</p>
        <button class="btn-ghost" @click="resetToUpload">Upload another</button>
      </div>

    </template>

    <!-- Create class popup (standalone mode) -->
    <div v-if="showCreateClass" class="overlay" @click.self="showCreateClass = false">
      <div class="modal" role="dialog" aria-modal="true" aria-label="Create class">
        <div class="modal-header">
          <h3>Create a Class</h3>
          <button class="btn-ghost" type="button" @click="showCreateClass = false">Close</button>
        </div>

        <p v-if="createClassError" class="error-msg" style="margin: 8px 0 0;">{{ createClassError }}</p>

        <div class="create-form">
          <label class="create-field">
            <span class="create-label">Class Code</span>
            <input v-model="createForm.classCode" class="create-input" placeholder="CSCD 488" />
          </label>
          <label class="create-field">
            <span class="create-label">Quarter</span>
            <input v-model="createForm.quarter" class="create-input" placeholder="Fall" />
          </label>
          <label class="create-field">
            <span class="create-label">Year</span>
            <input v-model="createForm.year" class="create-input" placeholder="2026" />
          </label>
          <label class="create-field">
            <span class="create-label">Title</span>
            <input v-model="createForm.title" class="create-input" placeholder="Senior Project" />
          </label>
        </div>

        <div class="modal-actions">
          <button class="btn-primary" :disabled="creating" @click="submitCreateClass">
            <span v-if="creating" class="spinner" />
            <span v-else>Create &amp; select</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  userRole: { type: String, default: 'professor' },
  courseId: { type: [String, Number], required: false }
})

const emit = defineEmits(['saved'])

const isProfessor = computed(() => props.userRole === 'professor')

// When no courseId prop is provided, we're in standalone (route) mode and need class selection
const standaloneMode = computed(() => !props.courseId)
const effectiveCourseId = computed(() => props.courseId || selectedClassId.value)

// Class selection state (standalone mode only)
const myClasses       = ref([])
const loadingClasses  = ref(false)
const selectedClassId = ref(null)
const selectedClass   = computed(() => myClasses.value.find(c => c.joinCode === selectedClassId.value) ?? null)

// Create class modal state
const showCreateClass  = ref(false)
const createForm       = ref({ classCode: '', quarter: '', year: '', title: '' })
const creating         = ref(false)
const createClassError = ref('')

const ALL_DAYS   = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']
const ALL_FIELDS = [
  'classMeetingTimes', 'officeHours', 'gradeScale', 'gradeBreakdown',
  'passConditions', 'attendance', 'dueDates', 'lateWorkPolicy', 'aiPolicy'
]

const step             = ref(props.courseId ? 'upload' : 'select')
const pdfFile          = ref(null)
const fileName         = ref('')
const isDragging       = ref(false)
const loading          = ref(false)
const saving           = ref(false)
const error            = ref('')
const draft            = ref(null)
const fetchingExisting = ref(false)
const isEditing        = ref(false)

const lowConfidenceCount = computed(() => {
  if (!draft.value) return 0
  return ALL_FIELDS.filter(f => draft.value[f]?.confidence !== 'high').length
})

onMounted(() => {
  if (standaloneMode.value && isProfessor.value) fetchMyClasses()
  else if (props.courseId) loadExistingSyllabus()
})

async function loadExistingSyllabus() {
  fetchingExisting.value = true
  try {
    const res = await fetch(`/api/courses/${props.courseId}/syllabus`, { credentials: 'include' })
    if (!res.ok) return // no syllabus yet — stay on upload step
    const saved = await res.json()
    if (saved && typeof saved === 'object' && Object.keys(saved).length > 1) {
      draft.value = wrapSavedData(saved)
      isEditing.value = true
      step.value = 'review'
    }
  } catch {
    // non-fatal — stay on upload step
  } finally {
    fetchingExisting.value = false
  }
}

function wrapSavedData(saved) {
  return ensureDefaults({
    classMeetingTimes: { value: saved.classMeetingTimes, confidence: 'high' },
    officeHours:       { value: saved.officeHours,       confidence: 'high' },
    gradeScale:        { value: saved.gradeScale,        confidence: 'high' },
    gradeBreakdown:    { value: saved.gradeBreakdown,    confidence: 'high' },
    passConditions:    { value: saved.passConditions,    confidence: 'high' },
    attendance:        { value: saved.attendance,        confidence: 'high' },
    dueDates:          { value: saved.dueDates,          confidence: 'high' },
    lateWorkPolicy:    { value: saved.lateWorkPolicy,    confidence: 'high' },
    aiPolicy:          { value: saved.aiPolicy ?? '',    confidence: 'high' },
  })
}

async function fetchMyClasses() {
  loadingClasses.value = true
  try {
    const res = await fetch('/api/classes/mine', { credentials: 'include' })
    if (!res.ok) throw new Error()
    const data = await res.json()
    myClasses.value = Array.isArray(data) ? data : []
  } catch {
    // Non-fatal — list stays empty, user can still create
  } finally {
    loadingClasses.value = false
  }
}

function pickClass(c) {
  selectedClassId.value = c.joinCode
  step.value = 'upload'
}

async function submitCreateClass() {
  creating.value = true
  createClassError.value = ''
  try {
    const res = await fetch('/api/classes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(createForm.value)
    })
    if (!res.ok) {
      const msg = await res.text().catch(() => `HTTP ${res.status}`)
      throw new Error(msg || `Server error ${res.status}`)
    }
    const created = await res.json()
    myClasses.value = [created, ...myClasses.value]
    showCreateClass.value = false
    createForm.value = { classCode: '', quarter: '', year: '', title: '' }
    pickClass(created)
  } catch (e) {
    createClassError.value = e.message || 'Failed to create class.'
  } finally {
    creating.value = false
  }
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) setFile(file)
}

function onDrop(e) {
  isDragging.value = false
  const file = e.dataTransfer.files?.[0]
  if (file?.type === 'application/pdf') setFile(file)
  else error.value = 'Please drop a PDF file.'
}

function setFile(file) {
  pdfFile.value  = file
  fileName.value = file.name
  error.value    = ''
}

function clearFile() {
  pdfFile.value  = null
  fileName.value = ''
  error.value    = ''
}

function resetToUpload() {
  step.value     = 'upload'
  pdfFile.value  = null
  fileName.value = ''
  draft.value    = null
  error.value    = ''
}

function ensureDefaults(parsed) {
  if (!parsed.classMeetingTimes) {
    parsed.classMeetingTimes = { value: { days: [], startTime: '', endTime: '', location: '', startDate: '', endDate: '' }, confidence: 'low' }
  } else if (!parsed.classMeetingTimes.value || typeof parsed.classMeetingTimes.value !== 'object') {
    parsed.classMeetingTimes.value = { days: [], startTime: '', endTime: '', location: '', startDate: '', endDate: '' }
  } else {
    if (!Array.isArray(parsed.classMeetingTimes.value.days)) parsed.classMeetingTimes.value.days = []
    parsed.classMeetingTimes.value.startTime ??= ''
    parsed.classMeetingTimes.value.endTime   ??= ''
    parsed.classMeetingTimes.value.location  ??= ''
    parsed.classMeetingTimes.value.startDate ??= ''
    parsed.classMeetingTimes.value.endDate   ??= ''
  }

  if (!parsed.attendance) {
    parsed.attendance = { value: { tracked: 'no', affectsGrade: 'no', details: '' }, confidence: 'low' }
  } else if (!parsed.attendance.value || typeof parsed.attendance.value !== 'object') {
    parsed.attendance.value = { tracked: 'no', affectsGrade: 'no', details: '' }
  } else {
    parsed.attendance.value.tracked      ??= 'no'
    parsed.attendance.value.affectsGrade ??= 'no'
    parsed.attendance.value.details      ??= ''
  }

  for (const f of ['gradeScale', 'gradeBreakdown', 'passConditions', 'dueDates']) {
    if (!parsed[f]) parsed[f] = { value: [], confidence: 'low' }
    else if (!Array.isArray(parsed[f].value)) parsed[f].value = []
  }

  for (const f of ['officeHours', 'lateWorkPolicy', 'aiPolicy']) {
    if (!parsed[f]) parsed[f] = { value: '', confidence: 'low' }
    else parsed[f].value ??= ''
  }

  return parsed
}

async function parseSyllabus() {
  if (!pdfFile.value) return
  loading.value = true
  error.value   = ''

  try {
    const formData = new FormData()
    formData.append('file', pdfFile.value)

    const res = await fetch(`/api/courses/${effectiveCourseId.value}/syllabus/parse`, {
      method: 'POST',
      body: formData,
      credentials: 'include'
    })

    if (!res.ok) {
      const msg = await res.text().catch(() => `HTTP ${res.status}`)
      throw new Error(msg || `Server error ${res.status}`)
    }

    const parsed = await res.json()
    draft.value  = ensureDefaults(parsed)
    step.value   = 'review'
  } catch (e) {
    error.value = e.message || 'Something went wrong. Please try again.'
  } finally {
    loading.value = false
  }
}
async function loadClassRecipients(classCode) {
  if (!classCode) {
    classRecipients.value = [];
    return;
  }
  try {
    const res = await fetch(`/api/classes/${classCode}/members`, {
      method: "GET",
      credentials: "include"
    });
    if (res.ok) {
      const data = await res.json();
      classRecipients.value = (data || []).filter(
        member => member.email?.toLowerCase() !== currentUserNormalized.value
      );
    } else {
      classRecipients.value = [];
    }
  } catch (err) {
    console.error("Error loading class recipients:", err);
    classRecipients.value = [];
  }
}
async function createRecurringMeetings(request) {
  const res = await fetch('/api/meetings/recurring', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  })
  if (!res.ok) throw new Error(await res.text())
  return await res.json()
}

async function confirmSave() {
  saving.value = true
  error.value  = ''
  try {
    const payload = {
      courseId:          String(effectiveCourseId.value),
      classMeetingTimes: draft.value.classMeetingTimes.value,
      officeHours:       draft.value.officeHours.value,
      gradeScale:        draft.value.gradeScale.value,
      gradeBreakdown:    draft.value.gradeBreakdown.value,
      passConditions:    draft.value.passConditions.value,
      attendance:        draft.value.attendance.value,
      dueDates:          draft.value.dueDates.value,
      lateWorkPolicy:    draft.value.lateWorkPolicy.value,
      aiPolicy:          draft.value.aiPolicy.value ?? null
    }

    const res = await fetch(`/api/courses/${effectiveCourseId.value}/syllabus`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload)
    })

    if (!res.ok) {
      const msg = await res.text().catch(() => `HTTP ${res.status}`)
      throw new Error(msg || `Server error ${res.status}`)
    }

    emit('saved', payload)

    const courseId = String(effectiveCourseId.value)
    const meetingTimes = draft.value.classMeetingTimes.value

    if (meetingTimes.days?.length && meetingTimes.startTime && meetingTimes.endTime &&
        meetingTimes.startDate && meetingTimes.endDate) {
      const membersRes = await fetch(`/api/classes/${courseId}/members`, { credentials: 'include' })
      const members = membersRes.ok ? await membersRes.json() : []

      const DAY_MAP = { MON: 'MONDAY', TUE: 'TUESDAY', WED: 'WEDNESDAY', THU: 'THURSDAY', FRI: 'FRIDAY', SAT: 'SATURDAY', SUN: 'SUNDAY' }
      const daysOfWeek = meetingTimes.days.map(d => DAY_MAP[d] || d)
      const notes = `${courseId} Lecture${meetingTimes.location ? ' ' + meetingTimes.location : ''}`

      for (const member of members) {
        const request = {
          startDate: meetingTimes.startDate,
          endDate: meetingTimes.endDate,
          startTime: meetingTimes.startTime,
          endTime: meetingTimes.endTime,
          classCode: courseId,
          daysOfWeek,
          notes,
          recipientId: member.email
        }
        await createRecurringMeetings(request)
      }
    }

    step.value = 'saved'
  } catch (e) {
    error.value = 'Failed to save. Please try again.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
/* ── base card ── */
.card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 18px;
  padding: 18px;
  color: #e5e7eb;
}

h1 { font-size: 18px; font-weight: 500; margin: 0; color: #f3f4f6; }

/* ── header ── */
.uploader-header { display: flex; align-items: center; gap: 10px; margin-bottom: 1.25rem; }
.role-badge {
  font-size: 11px; padding: 3px 8px; border-radius: 6px;
  background: rgba(100,210,120,0.12); color: #6ee7b7; font-weight: 500;
}

/* ── role gate ── */
.role-gate { text-align: center; padding: 2.5rem 1rem; }
.role-gate__icon { font-size: 28px; margin-bottom: 10px; }
.role-gate__msg  { font-size: 14px; font-weight: 500; margin: 0 0 6px; color: #f3f4f6; }
.role-gate__sub  { font-size: 13px; margin: 0; color: #9ca3af; }

/* ── select step ── */
.select-step { display: flex; flex-direction: column; gap: 10px; }
.select-sub  { font-size: 13px; color: #9ca3af; margin: 0 0 4px; }

.class-list { display: flex; flex-direction: column; gap: 6px; }

.class-option {
  display: flex; flex-direction: column; gap: 3px;
  text-align: left; padding: 11px 14px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.08); background: rgba(255,255,255,0.03);
  color: #e5e7eb; cursor: pointer; transition: background 0.15s, border-color 0.15s;
}
.class-option:hover { background: rgba(96,165,250,0.08); border-color: rgba(96,165,250,0.3); }
.class-option__code  { font-size: 13px; font-weight: 600; color: #f3f4f6; }
.class-option__title { font-size: 12px; color: #9ca3af; }

.no-classes-msg {
  font-size: 13px; color: #6b7280; margin: 0;
  padding: 14px; border: 1px dashed rgba(255,255,255,0.08);
  border-radius: 10px; text-align: center;
}

.muted-center { font-size: 13px; color: #6b7280; text-align: center; padding: 16px 0; }

.btn-create-class {
  font-size: 13px; padding: 8px 14px; border-radius: 8px;
  border: 1px dashed rgba(255,255,255,0.12); background: transparent;
  color: #9ca3af; cursor: pointer; align-self: flex-start; transition: all 0.15s;
}
.btn-create-class:hover { background: rgba(255,255,255,0.04); color: #e5e7eb; }

/* ── fetching state ── */
.fetching-state {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #9ca3af; padding: 12px 0;
}

/* ── selected class banner ── */
.selected-class-banner {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 8px 12px; margin-bottom: 12px;
  background: rgba(96,165,250,0.07); border: 1px solid rgba(96,165,250,0.2);
  border-radius: 8px; font-size: 13px;
}
.selected-class-label { color: #93c5fd; font-weight: 500; }
.selected-class-title { color: #6b7280; font-weight: 400; }
.btn-change-class {
  font-size: 12px; padding: 3px 9px; border-radius: 6px;
  border: 1px solid rgba(96,165,250,0.25); background: transparent;
  color: #60a5fa; cursor: pointer; white-space: nowrap; transition: background 0.15s;
  flex-shrink: 0;
}
.btn-change-class:hover { background: rgba(96,165,250,0.1); }

/* ── drop zone ── */
.drop-zone {
  border: 1.5px dashed rgba(255,255,255,0.12);
  border-radius: 10px; padding: 1.75rem; text-align: center;
  background: rgba(255,255,255,0.02);
  transition: border-color 0.15s, background 0.15s; margin-bottom: 12px;
}
.drop-zone--active     { border-color: #60a5fa; background: rgba(96,165,250,0.06); }
.drop-zone--has-file   { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 1rem 1.5rem; }
.drop-zone__hint       { font-size: 13px; color: #9ca3af; margin: 0 0 8px; }
.drop-zone__sub        { font-size: 12px; color: #6b7280; margin: 8px 0 0; }
.drop-zone__file-name  { font-size: 13px; font-weight: 500; color: #e5e7eb; }

/* ── buttons ── */
.btn-browse {
  display: inline-block; font-size: 13px; padding: 6px 14px; border-radius: 7px;
  border: 1px solid rgba(255,255,255,0.12); background: rgba(255,255,255,0.06);
  color: #e5e7eb; cursor: pointer; transition: background 0.15s;
}
.btn-browse:hover { background: rgba(255,255,255,0.1); }

.btn-clear {
  font-size: 12px; padding: 4px 10px; border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.1); background: transparent;
  color: #9ca3af; cursor: pointer;
}
.btn-clear:hover { background: rgba(255,255,255,0.06); }

.btn-primary {
  width: 100%; padding: 10px; font-size: 14px; font-weight: 500; border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.12); background: rgba(255,255,255,0.06);
  color: #f3f4f6; cursor: pointer; display: flex; align-items: center;
  justify-content: center; gap: 8px; transition: background 0.15s; margin-top: 4px;
}
.btn-primary:hover:not(:disabled) { background: rgba(255,255,255,0.1); }
.btn-primary:disabled { opacity: 0.35; cursor: not-allowed; }

.btn-ghost {
  font-size: 13px; padding: 5px 10px; border-radius: 7px;
  border: 1px solid rgba(255,255,255,0.1); background: transparent;
  color: #9ca3af; cursor: pointer; white-space: nowrap; transition: background 0.15s;
}
.btn-ghost:hover { background: rgba(255,255,255,0.06); }

.spinner {
  width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.15);
  border-top-color: #9ca3af; border-radius: 50%;
  animation: spin 0.7s linear infinite; flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

.error-msg { font-size: 13px; color: #f87171; margin: 8px 0 0; }

/* ── review panel ── */
.review-panel { display: flex; flex-direction: column; gap: 10px; }

.review-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 2px; }
.review-title  { font-size: 15px; font-weight: 500; margin: 0 0 3px; color: #f3f4f6; }
.review-sub    { font-size: 12px; color: #9ca3af; margin: 0; }

.legend {
  display: flex; align-items: center; gap: 14px; font-size: 12px; color: #6b7280;
  padding: 7px 12px; background: rgba(255,255,255,0.02);
  border-radius: 8px; border: 1px solid rgba(255,255,255,0.06);
}
.conf-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.conf-dot--high   { background: #4ade80; }
.conf-dot--medium { background: #fbbf24; }
.conf-dot--low    { background: #f87171; }

.conf-badge { font-size: 11px; padding: 2px 7px; border-radius: 5px; font-weight: 500; }
.conf-badge--high   { background: rgba(74,222,128,0.12);  color: #4ade80; }
.conf-badge--medium { background: rgba(251,191,36,0.12);  color: #fbbf24; }
.conf-badge--low    { background: rgba(248,113,113,0.12); color: #f87171; }

/* ── field sections ── */
.field-section {
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 10px; overflow: hidden;
  background: rgba(255,255,255,0.02);
}
.field-section__header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 9px 13px; background: rgba(255,255,255,0.03);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.field-label {
  font-size: 11px; font-weight: 500; text-transform: uppercase;
  letter-spacing: 0.05em; color: #6b7280;
}
.field-section__body { padding: 11px 13px; display: flex; flex-direction: column; gap: 7px; }
.field-section__body--warn { border-left: 2px solid #fbbf24; }

/* ── day-of-week chips ── */
.day-picker { display: flex; gap: 6px; flex-wrap: wrap; }
.day-chip {
  font-size: 12px; padding: 3px 9px; border-radius: 5px; cursor: pointer;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.03);
  color: #6b7280; user-select: none; transition: all 0.12s;
}
.day-chip--active { background: rgba(96,165,250,0.15); border-color: #60a5fa; color: #93c5fd; }

/* ── inputs ── */
.edit-input {
  font-size: 13px; padding: 6px 10px; border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04);
  color: #e5e7eb; outline: none; font-family: inherit;
}
.edit-input:focus { border-color: #60a5fa; box-shadow: 0 0 0 2px rgba(96,165,250,0.15); }
.edit-input--grow { flex: 1; min-width: 0; }
.edit-input--sm   { width: 46px; flex-shrink: 0; }
.edit-input--pct  { width: 68px; flex-shrink: 0; }
.edit-input--date { width: 116px; flex-shrink: 0; }
.edit-input--time { width: 100px; flex-shrink: 0; }

.edit-textarea {
  width: 100%; font-size: 13px; padding: 10px 13px; border: none; border-radius: 0;
  background: rgba(255,255,255,0.02); color: #e5e7eb;
  outline: none; font-family: inherit; line-height: 1.6; resize: vertical; box-sizing: border-box;
}
.edit-textarea:focus { background: rgba(255,255,255,0.04); }
.edit-textarea--warn { border-left: 2px solid #fbbf24; padding-left: 11px; }
.edit-textarea--inbody { border-radius: 6px; border: 1px solid rgba(255,255,255,0.1); margin-top: 2px; }
.edit-textarea--inbody:focus { border-color: #60a5fa; }

/* ── select (attendance) ── */
.row { display: flex; align-items: center; gap: 8px; }
.row--wrap { flex-wrap: wrap; }
.select-group { display: flex; flex-direction: column; gap: 4px; }
.select-label { font-size: 11px; color: #6b7280; }
.edit-select {
  font-size: 13px; padding: 5px 8px; border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04);
  color: #e5e7eb; outline: none; font-family: inherit; cursor: pointer;
}
.edit-select:focus { border-color: #60a5fa; }

/* ── row actions ── */
.btn-remove-row {
  font-size: 10px; width: 20px; height: 20px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.1); background: transparent;
  color: #6b7280; cursor: pointer; display: flex; align-items: center;
  justify-content: center; flex-shrink: 0; padding: 0; transition: all 0.15s;
}
.btn-remove-row:hover { background: rgba(248,113,113,0.12); color: #f87171; border-color: rgba(248,113,113,0.3); }

.btn-add-row {
  font-size: 12px; padding: 4px 10px; border-radius: 6px;
  border: 1px dashed rgba(255,255,255,0.1); background: transparent;
  color: #6b7280; cursor: pointer; align-self: flex-start; transition: all 0.15s;
}
.btn-add-row:hover { background: rgba(255,255,255,0.04); color: #9ca3af; }

/* ── review actions ── */
.review-actions { display: flex; flex-direction: column; gap: 10px; padding-top: 4px; }
.warn-summary {
  font-size: 13px; color: #fbbf24; background: rgba(251,191,36,0.08);
  padding: 8px 12px; border-radius: 8px; margin: 0;
  border: 1px solid rgba(251,191,36,0.15);
}

/* ── saved state ── */
.saved-state { text-align: center; padding: 2.5rem 1rem; }
.saved-state__icon {
  width: 44px; height: 44px; border-radius: 50%;
  background: rgba(74,222,128,0.12); color: #4ade80;
  font-size: 20px; display: flex; align-items: center; justify-content: center; margin: 0 auto 14px;
}
.saved-state__title { font-size: 15px; font-weight: 500; margin: 0 0 5px; color: #f3f4f6; }
.saved-state__sub   { font-size: 13px; color: #9ca3af; margin: 0 0 18px; }

/* ── create class modal ── */
.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.55);
  display: grid; place-items: center; padding: 18px; z-index: 50;
}
.modal {
  width: 100%; max-width: 420px;
  background: rgba(15,23,42,0.97); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 18px; padding: 18px; color: #e5e7eb;
  box-shadow: 0 18px 40px rgba(0,0,0,0.4);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 14px;
}
.modal-header h3 { margin: 0; font-size: 15px; font-weight: 500; color: #f3f4f6; }
.modal-actions { display: flex; justify-content: flex-end; margin-top: 14px; }
.modal-actions .btn-primary { width: auto; }

.create-form { display: grid; gap: 10px; }
.create-field { display: grid; gap: 5px; }
.create-label { font-size: 12px; color: #9ca3af; }
.create-input {
  padding: 9px 12px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.04);
  color: #e5e7eb; outline: none; font-size: 13px; font-family: inherit;
}
.create-input:focus { border-color: #60a5fa; box-shadow: 0 0 0 2px rgba(96,165,250,0.15); }

@media (max-width: 480px) {
  .row { flex-wrap: wrap; }
  .edit-input--date,
  .edit-input--time { width: 100%; }
}
</style>
