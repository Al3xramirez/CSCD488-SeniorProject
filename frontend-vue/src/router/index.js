import { createRouter, createWebHistory } from "vue-router";

import Login from "../views/Login.vue";
import Signup from "../views/Signup.vue";
import Dashboard from "../views/Dashboard.vue";
import Courses from "../views/Courses.vue";
import Profile from "../views/Profile.vue";
import Meetings from "../views/Meetings.vue";



const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", name: "Login", component: Login},
    { path: "/signup", name: "Signup", component: Signup },
    { path: "/dashboard", name: "Dashboard", component: Dashboard ,meta: { requiresAuth: true }},
    { path: "/profile", name: "Profile", component: Profile ,meta: { requiresAuth: true }},
    { path: "/courses", name: "Courses", component: Courses ,meta: { requiresAuth: true }},
    { path: "/meetings", name: "Meetings", component: Meetings ,meta: { requiresAuth: true }}
  ],
});

router.beforeEach((to, from, next) => {
  const isAuthenticated = localStorage.getItem("token")

  if (to.meta.requiresAuth && !isAuthenticated) {
    next("/login")
  } else {
    next()
  }
})

export default router;