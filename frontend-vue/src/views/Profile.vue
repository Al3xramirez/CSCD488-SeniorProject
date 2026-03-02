<script setup>
import { onMounted, ref } from 'vue'

const canvasIcalUrl = ref('')
const hasFeed = ref(false)
const lastSyncedAt = ref(null)
const statusMessage = ref('')
const errorMessage = ref('')
const saving = ref(false)
const syncing = ref(false)

const loadFeedStatus = async () => {
  errorMessage.value = ''
  try {
    const res = await fetch('/api/calendar/canvas-feed', {
      method: 'GET',
      credentials: 'include'
    })

    if (!res.ok) {
      throw new Error('Failed to load Canvas feed status')
    }

    const data = await res.json()
    hasFeed.value = !!data.hasFeed
    lastSyncedAt.value = data.lastSyncedAt || null
  } catch (err) {
    errorMessage.value = err.message || 'Unable to load Canvas feed status'
  }
}

const saveFeed = async () => {
  statusMessage.value = ''
  errorMessage.value = ''

  if (!canvasIcalUrl.value.trim()) {
    errorMessage.value = 'Please enter your Canvas iCal URL.'
    return
  }

  saving.value = true
  try {
    const res = await fetch('/api/calendar/canvas-feed', {
      method: 'PUT',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url: canvasIcalUrl.value.trim() })
    })

    if (!res.ok) {
      const message = await res.text()
      throw new Error(message || 'Failed to save Canvas feed URL')
    }

    statusMessage.value = 'Canvas feed URL saved.'
    await loadFeedStatus()
  } catch (err) {
    errorMessage.value = err.message || 'Failed to save Canvas feed URL'
  } finally {
    saving.value = false
  }
}

const syncCanvas = async () => {
  statusMessage.value = ''
  errorMessage.value = ''

  syncing.value = true
  try {
    const res = await fetch('/api/calendar/canvas/sync', {
      method: 'POST',
      credentials: 'include'
    })

    if (!res.ok) {
      const message = await res.text()
      throw new Error(message || 'Sync failed')
    }

    statusMessage.value = await res.text()
    await loadFeedStatus()
  } catch (err) {
    errorMessage.value = err.message || 'Sync failed'
  } finally {
    syncing.value = false
  }
}

onMounted(loadFeedStatus)
</script>

<template>
  <main class="profile-page">
    <section class="card">
      <h1>Canvas Calendar</h1>
      <p class="subtext">Paste your Canvas iCal feed URL to import due dates into your SyllabusSync calendar.</p>

      <label for="canvas-url">Canvas iCal URL</label>
      <input
        id="canvas-url"
        v-model="canvasIcalUrl"
        type="url"
        placeholder="https://.../calendar.ics"
      />

      <div class="actions">
        <button :disabled="saving" @click="saveFeed">
          {{ saving ? 'Saving...' : 'Save Feed URL' }}
        </button>
        <button class="secondary" :disabled="syncing || !hasFeed" @click="syncCanvas">
          {{ syncing ? 'Syncing...' : 'Sync Now' }}
        </button>
      </div>

      <p v-if="hasFeed" class="ok">Feed connected.</p>
      <p v-if="lastSyncedAt" class="subtext">Last synced: {{ new Date(lastSyncedAt).toLocaleString() }}</p>
      <p v-if="statusMessage" class="ok">{{ statusMessage }}</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </section>
  </main>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background: linear-gradient(140deg, #f8fbff, #eef5ff);
}

.card {
  width: 100%;
  max-width: 620px;
  padding: 28px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 32px rgba(16, 24, 40, 0.1);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

h1 {
  margin: 0;
  color: #0f172a;
}

label {
  font-weight: 600;
  color: #0f172a;
}

input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px 12px;
}

.actions {
  display: flex;
  gap: 10px;
}

button {
  border: none;
  border-radius: 8px;
  padding: 10px 14px;
  font-weight: 600;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}

button.secondary {
  background: #0f172a;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.subtext {
  margin: 0;
  color: #475569;
}

.ok {
  color: #0f766e;
  margin: 0;
}

.error {
  color: #b91c1c;
  margin: 0;
}
</style>
