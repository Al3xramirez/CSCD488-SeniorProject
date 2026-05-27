<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

import wordmarkUrl from "../assets/SyllabusSyncLogoLettering.png";
import pureLogoUrl from "../assets/SyllabusSyncPureLogo-TransparentBackground.png";

/** This Vue page represents the Home page and manages the active tab state based on the current route.
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

    <main class="content" aria-label="Home content">
      <section class="hero" aria-label="Intro">
        <div class="hero__grid">
          <div class="hero__copy">
            <div class="kicker">Academic planning, simplified</div>
            <h1 class="hero__title">SyllabusSync</h1>
            <p class="hero__subtitle">Turn your syllabus into a clear plan for the quarter.</p>

            <p class="hero__body">
              Upload a course syllabus and SyllabusSync helps organize key dates, meetings, and deadlines so you can stay on track
              without digging through PDFs.
            </p>

            <div class="hero__actions">
              <RouterLink class="btnPrimary" to="/login">Log in</RouterLink>
              <RouterLink class="btnSecondary" to="/signup">Create an account</RouterLink>
            </div>
          </div>

          <div class="hero__visual" aria-hidden="true">
            <div class="visualCard">
              <img class="visualLogo" :src="pureLogoUrl" alt="" />
              <div class="visualGlow" />
            </div>
          </div>
        </div>
      </section>

      <section class="section" aria-label="Features">
        <div class="section__header">
          <h2 class="section__title">What you can do</h2>
          <p class="section__muted">A quick overview of the core workflow.</p>
        </div>

        <div class="grid3">
          <article class="panel">
            <div class="panel__title">Syllabus → Schedule</div>
            <div class="panel__text">Extract important course information and keep it accessible in one place.</div>
          </article>

          <article class="panel">
            <div class="panel__title">Meetings &amp; Office Hours</div>
            <div class="panel__text">View class meetings and office hours at a glance so you never miss a session.</div>
          </article>

          <article class="panel">
            <div class="panel__title">Workload Projections</div>
            <div class="panel__text">See what your week could look like and plan ahead for busy stretches.</div>
          </article>
        </div>
      </section>

      <section class="section" aria-label="How it works">
        <div class="section__header">
          <h2 class="section__title">How it works</h2>
          <p class="section__muted">Simple steps, fast setup.</p>
        </div>

        <div class="steps">
          <div class="panel">
            <div class="panel__kicker">Step 1</div>
            <div class="panel__title">Upload your syllabus</div>
          </div>
          <div class="panel">
            <div class="panel__kicker">Step 2</div>
            <div class="panel__title">Review extracted details</div>
          </div>
          <div class="panel">
            <div class="panel__kicker">Step 3</div>
            <div class="panel__title">Track events and plan ahead</div>
          </div>
        </div>
      </section>

      <section class="section" aria-label="Who it's for">
        <div class="section__header">
          <h2 class="section__title">Who it’s for</h2>
          <p class="section__muted">Designed for real course workflows.</p>
        </div>

        <div class="grid3">
          <article class="panel">
            <div class="panel__title">Students</div>
            <div class="panel__text">Stay ahead of deadlines and see your schedule clearly.</div>
          </article>
          <article class="panel">
            <div class="panel__title">TAs</div>
            <div class="panel__text">Keep sections, office hours, and responsibilities organized.</div>
          </article>
          <article class="panel">
            <div class="panel__title">Professors</div>
            <div class="panel__text">Share course structure and meeting info in a consistent format.</div>
          </article>
        </div>
      </section>

      <footer class="footer" aria-label="Footer">
        <div class="footer__text">Built for academic planning—simple, fast, and focused on what matters.</div>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  color: #111827;
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

.hero {
  padding: 10px 0 18px;
}

.hero__grid {
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  align-items: center;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.90);
  border: 1px solid rgba(17, 24, 39, 0.10);
  position: relative;
  overflow: hidden;
}

.hero__grid::before,
.hero__grid::after {
  content: "";
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 30%, rgba(37, 99, 235, 0.22), rgba(37, 99, 235, 0));
  pointer-events: none;
}

.hero__grid::before {
  left: -240px;
  top: -240px;
}

.hero__grid::after {
  right: -260px;
  bottom: -260px;
}

.hero__copy {
  position: relative;
  z-index: 1;
}

.kicker {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(37, 99, 235, 0.95);
  background: rgba(37, 99, 235, 0.10);
  border: 1px solid rgba(37, 99, 235, 0.20);
}

.hero__title {
  margin: 0;
  font-size: 46px;
  letter-spacing: -0.03em;
}

.hero__subtitle {
  margin: 8px 0 0;
  font-size: 16px;
  color: #1f2937;
  font-weight: 700;
}

.hero__body {
  margin: 10px 0 0;
  color: rgba(31, 41, 55, 0.80);
  max-width: 66ch;
  line-height: 1.55;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.hero__visual {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
}

.visualCard {
  width: min(420px, 100%);
  aspect-ratio: 4 / 3;
  border-radius: 24px;
  background: transparent;
  border: none;
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
}

.visualLogo {
  width: min(240px, 70%);
  height: auto;
  display: block;
  filter: drop-shadow(0 18px 30px rgba(37, 99, 235, 0.22));
}

.visualGlow {
  display: none;
}

.btnPrimary,
.btnSecondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 14px;
  border-radius: 14px;
  text-decoration: none;
  font-weight: 900;
  border: 1px solid transparent;
}

.btnPrimary {
  background: #2563eb;
  color: white;
}

.btnPrimary:hover {
  background: #1d4ed8;
}

.btnSecondary {
  background: rgba(15, 23, 42, 0.03);
  border-color: rgba(17, 24, 39, 0.12);
  color: #111827;
}

.btnSecondary:hover {
  background: rgba(37, 99, 235, 0.06);
  border-color: rgba(37, 99, 235, 0.20);
}

.section {
  padding: 16px 0;
}

.section__header {
  margin-bottom: 12px;
}

.section__title {
  margin: 0;
  font-size: 18px;
  letter-spacing: -0.01em;
}

.section__muted {
  margin: 6px 0 0;
  color: rgba(31, 41, 55, 0.70);
  font-size: 13px;
  font-weight: 600;
}

.grid3 {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.panel {
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(17, 24, 39, 0.10);
  border-radius: 18px;
  padding: 14px;
}

.panel__kicker {
  font-size: 12px;
  font-weight: 900;
  color: rgba(37, 99, 235, 0.95);
  text-transform: uppercase;
  letter-spacing: 0.10em;
}

.panel__title {
  margin-top: 6px;
  font-weight: 900;
  color: #111827;
}

.panel__text {
  margin-top: 6px;
  color: rgba(31, 41, 55, 0.75);
  font-size: 13px;
  line-height: 1.45;
}

.footer {
  padding: 18px 0 28px;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
  margin-top: 10px;
}

.footer__text {
  color: rgba(31, 41, 55, 0.70);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 900px) {
  .grid3,
  .steps {
    grid-template-columns: 1fr;
  }

  .hero__grid {
    grid-template-columns: 1fr;
  }
}
</style>
