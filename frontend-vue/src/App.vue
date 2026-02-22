<<<<<<< HEAD
=======
<script setup>
import { ref, onMounted } from 'vue'

const msg = ref('Loading...')

onMounted(async () => {
  const res = await fetch('/api/health')
  msg.value = await res.text()
})

</script>

>>>>>>> testing/LocalDBSetup
<template>
  <div>
    <router-view></router-view>
  </div>
</template>
