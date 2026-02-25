<script setup>
import Sidebar from "../components/Sidebar.vue";
import AppHeader from "../components/AppHeader.vue";
</script>

<template>
  <div class="shell">
    <Sidebar />

    <div class="main">
      <AppHeader />

      <div class="page">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
  background: #0b1220;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.page {
  padding: 22px;
  flex: 1;
}
</style>