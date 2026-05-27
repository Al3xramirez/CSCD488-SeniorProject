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
            <div class="kicker">Academic planning simplified</div>
            <h1 class="hero__title">SyllabusSync</h1>
            <p class="hero__subtitle">Turn your classes into a structured plan.</p>

            <p class="hero__body">
              Syllabus Sync was created to help three main groups: Students, TA's and professors. We want to make it easy for students to keep track of their course deadlines and meetings, for TA's to have a clear view of their office hours and meetings, and for Professors to have meetings and syllabi organized.
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
        <div class="section__header section__header--center">
          <h2 class="section__title section__title--splitSize">What you can do</h2>
          <p class="section__muted section__muted--features">A quick overview of the core workflow.</p>
        </div>

        <div class="grid3">
          <article class="panel panel--dashboard">
            <div class="panel__title">Syllabus</div>
            <div class="panel__text">Professors can upload their syllabus to their course instances which allows students and TAs to stay organized and informed.</div>
          </article>

          <article class="panel panel--dashboard">
            <div class="panel__title">Meetings &amp; Office Hours</div>
            <div class="panel__text">All roles can view class meetings and office hours at a glance so you never miss a session.</div>
          </article>

          <article class="panel panel--dashboard">
            <div class="panel__title">Workload Projections</div>
            <div class="panel__text">Students and TA's can see what their week could look like and plan ahead for busy stretches ahead.</div>
          </article>
        </div>
      </section>

      <section class="section" aria-label="How it works">
        <div class="split">
          <div class="split__side split__side--title">
            <h2 class="split__title">How it works</h2>
          </div>

          <div class="split__side split__side--content">
            <p class="split__lede">Simple steps, fast setup.</p>

            <ol class="splitList" aria-label="How it works steps">
              <li class="splitList__item">
                <div class="splitList__num">1</div>
                <div class="splitList__text"><strong>Professor creates a class instance</strong> to start a course workspace.</div>
              </li>
              <li class="splitList__item">
                <div class="splitList__num">2</div>
                <div class="splitList__text"><strong>Professor uploads the syllabus</strong> for that class.</div>
              </li>
              <li class="splitList__item">
                <div class="splitList__num">3</div>
                <div class="splitList__text"><strong>Students join via a join code</strong> and get access to the class.</div>
              </li>
              <li class="splitList__item">
                <div class="splitList__num">4</div>
                <div class="splitList__text">
                  <strong>Syllabus info populates automatically</strong> into calendars, workload projections, and office hours. Students and TAs can also
                  import workload through <strong>iCal links</strong> provided via the Canvas app.
                </div>
              </li>
              <li class="splitList__item">
                <div class="splitList__num">5</div>
                <div class="splitList__text">
                  <strong>Students and TAs track events and plan ahead</strong> by requesting meetings with TAs or professors, tracking deadlines, and viewing
                  workload calculations.
                </div>
              </li>
            </ol>
          </div>
        </div>
      </section>

      <section class="section" aria-label="Who it's for">
        <div class="split split--reverse split--who">
          <div class="split__side split__side--content">
            <p class="split__lede">Designed for real course workflows.</p>
            <p class="split__body">
              SyllabusSync is built for <strong>Students</strong> who want syllabus organization, the ability to request one on one meetings with professors, and have a general school organizer. <strong>TAs</strong> who need meetings and office hours organized while also helping students stay on top of their work.
              <strong>Professors</strong> who want course structure shared in a consistent format, and also want to streamline communication and scheduling with students and TAs.
            </p>
          </div>

          <div class="split__side split__side--title">
            <h2 class="split__title">Who it’s for</h2>
          </div>
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
  position: relative;
  overflow: hidden;
}

/* Stagnant (fixed) bubbly background */
.shell::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(circle at 10% 18%, rgba(37, 99, 235, 0.20), rgba(37, 99, 235, 0) 46%),
    radial-gradient(circle at 86% 16%, rgba(147, 51, 234, 0.16), rgba(147, 51, 234, 0) 44%),
    radial-gradient(circle at 92% 78%, rgba(37, 99, 235, 0.14), rgba(37, 99, 235, 0) 48%),
    radial-gradient(circle at 18% 84%, rgba(147, 51, 234, 0.12), rgba(147, 51, 234, 0) 50%);
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

/* Hero card bubbles */
.hero__grid::before,
.hero__grid::after {
  content: "";
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 999px;
  pointer-events: none;
  z-index: 0;
  background: radial-gradient(circle at 30% 30%, rgba(37, 99, 235, 0.22), rgba(37, 99, 235, 0));
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
  padding: 40px 0;
}

/* Extra separation between major sections */
.section + .section {
  margin-top: 18px;
}

/* Make the two split sections breathe more */
.split {
  margin-top: 10px;
}

.section__header {
  margin-bottom: 12px;
}

.section__header--center {
  max-width: 1040px;
  margin: 0 auto 12px;
  text-align: center;
}

.section__title {
  margin: 0;
  font-size: 18px;
  letter-spacing: -0.01em;
}

/* Match the big split headings (e.g., "How it works") */
.section__title--splitSize {
  font-size: 44px;
  letter-spacing: -0.03em;
  line-height: 1.05;
  position: relative;
  display: inline-block;
}

/* Title underline so headings feel anchored (blue → purple) */
.section__title--splitSize::after,
.split__title::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: -10px;
  width: 62%;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.90), rgba(147, 51, 234, 0.90));
  opacity: 0.95;
}

/* Centered titles should have centered underline */
.section__header--center .section__title--splitSize::after {
  left: 50%;
  transform: translateX(-50%);
  width: min(360px, 78%);
}

.section__muted {
  margin: 6px 0 0;
  color: rgba(31, 41, 55, 0.70);
  font-size: 13px;
  font-weight: 600;
}

.section__muted--features {
  margin-top: 25px;
  color: rgba(17, 24, 39, 0.78);
  font-size: 15px;
  font-weight: 800;
}

.grid3 {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}


.split {
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: 18px;
  align-items: start;
}

.split--reverse {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
}

.split--reverse .split__side--title {
  order: 2;
}

.split--reverse .split__side--content {
  order: 1;
}

/* Vertically center split titles relative to their content */
.split__side--title {
  align-self: stretch;
  display: flex;
  align-items: center;
}

.split__side--content {
  align-self: start;
}

.split--who {
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.15fr);
  gap: 56px;
}

.split--who .split__side--content {
  max-width: 520px;
  justify-self: start;
}

.split--who .split__side--title {
  justify-self: end;
  justify-content: flex-end;
  text-align: right;
}

.split--who .split__title::after {
  left: auto;
  right: 0;
}

.split__title {
  margin: 0;
  font-size: 44px;
  letter-spacing: -0.03em;
  line-height: 1.05;
  position: relative;
  display: inline-block;
}

.split__lede {
  margin: 6px 0 0;
  color: rgba(31, 41, 55, 0.78);
  font-weight: 800;
}

.split__body {
  margin: 10px 0 0;
  color: rgba(31, 41, 55, 0.78);
  line-height: 1.6;
}

.splitList {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.splitList__item {
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 10px;
  align-items: start;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(17, 24, 39, 0.08);
}

.splitList__num {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-weight: 900;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.10);
  border: 1px solid rgba(37, 99, 235, 0.18);
}

.splitList__text {
  color: rgba(31, 41, 55, 0.82);
  line-height: 1.55;
}

.panel {
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(17, 24, 39, 0.10);
  border-radius: 18px;
  padding: 14px;
}

.panel--dashboard {
  background:
    linear-gradient(
      145deg,
      rgba(37, 99, 235, 0.45) 0%,
      rgba(147, 51, 234, 0.26) 34%,
      rgba(11, 18, 32, 0.00) 72%
    ),
    rgba(11, 18, 32, 0.98);
  border-color: rgba(37, 99, 235, 0.22);
  box-shadow: 0 18px 40px rgba(11, 18, 32, 0.45);
}

.panel--dashboard .panel__kicker,
.panel--dashboard .panel__title,
.panel--dashboard .panel__text {
  color: #ffffff;
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
  .split {
    grid-template-columns: 1fr;
  }

  .split--reverse {
    grid-template-columns: 1fr;
  }

  .split--reverse .split__side--title,
  .split--reverse .split__side--content {
    order: initial;
  }

  .hero__grid {
    grid-template-columns: 1fr;
  }
}
</style>
