<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import Sidebar from "../components/Sidebar.vue";
import AppHeader from "../components/AppHeader.vue";

const route = useRoute();
const hideHeader = computed(() => route.meta?.hideHeader === true);
</script>

<template>
  <div class="shell">
    <Sidebar />

    <div class="main">
      <AppHeader v-if="!hideHeader" />

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