<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

/** This Vue page represents the About Us page and manages the active tab state based on the current route.
 * It uses the `useRoute` hook to access the current route and a computed property `activeTab` to determine
 * which tab should be highlighted as active. 
 * The template includes navigation links for Home, About Us, and Login
 */

const route = useRoute();

const activeTab = computed(() => {
  if (route.path === "/") return "home";
  if (route.path === "/about") return "about";
  if (route.path === "/login") return "login";
  return "";
});
</script>

<template>
  <div class="page">
    <nav class="tabs" aria-label="Primary">
      <RouterLink class="tab" :class="{ active: activeTab === 'home' }" to="/">
        Home
      </RouterLink>
      <RouterLink class="tab" :class="{ active: activeTab === 'about' }" to="/about">
        About Us
      </RouterLink>
      <RouterLink class="tab" :class="{ active: activeTab === 'login' }" to="/login">
        Login
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
}

.tabs {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.tab {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 10px;
  text-decoration: none;
}

.tab.active {
  font-weight: 700;
  text-decoration: underline;
}
</style>
