<script setup>
import { ref, onMounted } from 'vue'

const msg = ref('Loading...')

onMounted(async () => {
  const res = await fetch('/api/health')
  msg.value = await res.text()
})
</script>

<template>
  <h1>Backend says: {{ msg }}</h1>
</template>
