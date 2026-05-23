<script setup>
import { computed, onMounted, provide, ref } from "vue";
import { useRoute } from "vue-router";
import Sidebar from "../components/Sidebar.vue";
import AppHeader from "../components/AppHeader.vue";

const route = useRoute();
const hideHeader = computed(() => route.meta?.hideHeader === true);

const me = ref(null);
provide('me', me);

const classes = ref([]);
const classesLoading = ref(false);
const classesError = ref("");
provide('classes', classes);
provide('classesLoading', classesLoading);
provide('classesError', classesError);

onMounted(async () => {
  try {
    const res = await fetch('/api/me', { credentials: 'include' });
    if (res.ok) me.value = await res.json();
  } catch (e) {
    // ignore; nav guard will handle redirect if unauthenticated
  }

  classesLoading.value = true;
  classesError.value = "";
  try {
    const res = await fetch('/api/classes/mine', { credentials: 'include' });
    if (res.ok) {
      classes.value = await res.json();
    } else {
      classesError.value = `Failed to load classes (${res.status})`;
    }
  } catch (e) {
    classesError.value = e?.message || "Failed to load classes";
  } finally {
    classesLoading.value = false;
  }
});
</script>

<template>
  <div class="shell">
    <Sidebar :role="me?.role" />

    <div class="main">
      <AppHeader v-if="!hideHeader" :role="me?.role" :first-name="me?.firstName" :last-name="me?.lastName" />

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