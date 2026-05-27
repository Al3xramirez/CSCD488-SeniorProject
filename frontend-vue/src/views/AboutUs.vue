<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import AlejandroPFP from "../assets/Alejandro PFP.png";
import BenPFP from "../assets/Ben PFP.png";
import RaulPFP from "../assets/Raul PFP.jpg";

import wordmarkUrl from "../assets/SyllabusSyncLogoLettering.png";

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

const makeAvatarDataUri = (accent1, accent2) => {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="320" height="320" viewBox="0 0 320 320">
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="${accent1}" stop-opacity="0.95" />
          <stop offset="1" stop-color="${accent2}" stop-opacity="0.95" />
        </linearGradient>
      </defs>
      <rect x="0" y="0" width="320" height="320" rx="160" fill="url(#bg)" />
      <circle cx="160" cy="132" r="54" fill="rgba(255,255,255,0.86)" />
      <path d="M66 292c18-54 62-86 94-86s76 32 94 86" fill="rgba(255,255,255,0.86)" />
    </svg>
  `.trim();

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
};

const teamMembers = [
  {
    name: "Benjamin Burns",
    role: "Role / Focus",
    summary: "Short summary",
    photo: BenPFP,
  },
  {
    name: "Raul Reno",
    role: "Role / Focus",
    summary: "Short summary",
    photo: RaulPFP,
  },
  {
    name: "Alejandro Ramirez",
    role: "Role / Focus",
    summary: "Short summary",
    photo: AlejandroPFP,
  },
];
</script>

<template>
  <div class="shell">
    <header class="header">
      <div class="header__inner">
        <RouterLink class="brand" to="/" aria-label="SyllabusSync Home">
          <img class="brand__img" :src="wordmarkUrl" alt="SyllabusSync" />
        </RouterLink>

        <nav class="nav" aria-label="Primary">
          <RouterLink class="link" :class="{ active: activeTab === 'home' }" to="/">Home</RouterLink>
          <RouterLink class="link" :class="{ active: activeTab === 'about' }" to="/about">About Us</RouterLink>
          <RouterLink class="link link--cta" :class="{ active: activeTab === 'login' }" to="/login">Login</RouterLink>
        </nav>
      </div>
    </header>

    <main class="content" aria-label="About content">
      <section class="section" aria-label="About Us">
        <div class="section__header section__header--center">
          <h1 class="title">About Us</h1>
          <p class="subtitle">SyllabusSync was developed by a three person computer science undergraduate team from Eastern Washington University for their Senior Capstone with the purpose of improving student course planning and management.</p>
        </div>

        <div class="teamGrid" aria-label="Team members">
          <article v-for="member in teamMembers" :key="member.name" class="member panel panel--dashboard">
            <img class="member__photo" :src="member.photo" :alt="member.name" />
            <div class="member__name">{{ member.name }}</div>
            <div class="member__role">{{ member.role }}</div>
            <p class="member__summary">{{ member.summary }}</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  color: #111827;
  position: relative;
  overflow: hidden;
}

/* Stagnant (fixed) bubbly background */
.shell::before {
  content: "";
  display: none;
}

.header,
.content {
  position: relative;
  z-index: 1;
}

.header {
  height: 68px;
  display: flex;
  align-items: center;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(17, 24, 39, 0.10);
}

.header__inner {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
}

.brand__img {
  height: 28px;
  width: auto;
  display: block;
}

.nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  text-decoration: none;
  color: rgba(17, 24, 39, 0.82);
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 0.01em;
  border: 1px solid transparent;
  background: transparent;
  transition: background 120ms ease, border-color 120ms ease;
}

.link:hover,
.link:focus-visible {
  background: rgba(37, 99, 235, 0.08);
  border-color: rgba(37, 99, 235, 0.20);
}

.link.active {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.35);
  color: #111827;
}

.link--cta {
  background: #2563eb;
  color: white;
}

.link--cta:hover,
.link--cta:focus-visible {
  background: #1d4ed8;
  border-color: transparent;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
}

.section {
  padding: 40px 0;
}

.section__header--center {
  max-width: 1040px;
  margin: 0 auto 18px;
  text-align: center;
}

.title {
  margin: 0;
  font-size: 44px;
  letter-spacing: -0.03em;
  line-height: 1.05;
  position: relative;
  display: inline-block;
}

.title::after {
  content: "";
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -10px;
  width: min(360px, 78%);
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.90), rgba(147, 51, 234, 0.90));
  opacity: 0.95;
}

.subtitle {
  margin: 50px auto 0;
  max-width: 72ch;
  color: rgba(17, 24, 39, 0.78);
  font-size: 15px;
  font-weight: 800;
}

.teamGrid {
  max-width: 1040px;
  margin: 28px auto 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.panel {
  border-radius: 18px;
  padding: 14px;
}

.panel--dashboard {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(17, 24, 39, 0.10);
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.08);
}

.member {
  display: grid;
  justify-items: center;
  text-align: center;
  gap: 10px;
  position: relative;
  overflow: hidden;
}

/* Card bubbles (match Home hero card feel) */
.member::before,
.member::after {
  content: "";
  position: absolute;
  width: 380px;
  height: 380px;
  border-radius: 999px;
  pointer-events: none;
  z-index: 0;
  background: radial-gradient(circle at 30% 30%, rgba(37, 100, 235, 0.527), rgba(37, 99, 235, 0));
}

.member::before {
  left: -220px;
  top: -240px;
}

.member::after {
  right: -240px;
  bottom: -240px;
  background: radial-gradient(circle at 30% 30%, rgba(0, 45, 248, 0.12), rgba(51, 115, 234, 0));
}

.member > * {
  position: relative;
  z-index: 1;
}

.member__photo {
  width: 112px;
  height: 112px;
  border-radius: 999px;
  border: 1px solid rgba(37, 99, 235, 0.26);
  box-shadow: 0 18px 30px rgba(37, 99, 235, 0.12);
}

.member__name {
  font-weight: 900;
  color: #111827;
  letter-spacing: -0.01em;
}

.member__role {
  font-size: 13px;
  font-weight: 800;
  color: rgba(17, 24, 39, 0.72);
}

.member__summary {
  margin: 0;
  color: rgba(17, 24, 39, 0.74);
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 900px) {
  .teamGrid {
    grid-template-columns: 1fr;
  }
}
</style>
