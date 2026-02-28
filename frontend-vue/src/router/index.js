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

/* This nav guard just checks if the route requires authentication. 
So if someone has a session cookie and it becomes authenticated,
then they can skip the login page and go straight to the dashboard, 
if not they will be redirected to the login page */ 
router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth) {
    try {
      const res = await fetch('/api/check-auth', {
        credentials: 'include'
      });

      if (res.ok) next();
      else next('/login');
    } catch (e) {
      next('/login');
    }
  } else {
    next();
  }
});

export default router;