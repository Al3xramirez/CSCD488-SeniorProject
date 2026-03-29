<script setup>
import { computed, onMounted, provide, ref } from "vue";
import { useRoute } from "vue-router";
import Sidebar from "../components/Sidebar.vue";
import AppHeader from "../components/AppHeader.vue";

const route = useRoute();
const hideHeader = computed(() => route.meta?.hideHeader === true);

// me is a ref that will hold the current user's info after fetching from the /api/me endpoint. 
// It starts as null and will be populated with the user's email, role, firstName, and lastName if the fetch is successful.
const me = ref(null);
provide('me', me);

/* onMounted is a component that runs when the DashboardLayout component is mounted. 
It fetches the current user's info from the /api/me endpoint and stores it in the me ref. 
This allows child components to access the user's info via inject('me'). 
This is the parent component to which child components can subscribe to get the current user's info.
*/
onMounted(async () => {
  try {
    const res = await fetch('/api/me', { credentials: 'include' });
    if (res.ok) {
      me.value = await res.json();
    }
  } catch (e) {
    // ignore; nav guard will handle redirect if unauthenticated
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