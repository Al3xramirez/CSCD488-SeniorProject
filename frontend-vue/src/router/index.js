import { createRouter, createWebHistory } from "vue-router";

import Login from "../views/Login.vue";
import Signup from "../views/Signup.vue";
import Dashboard from "../views/Dashboard.vue";
import Profile from "../views/Profile.vue";
import Meetings from "../views/Meetings.vue";
import DashboardLayout from "../layout/DashboardLayout.vue";
import Calendar from "../views/Calendar.vue";
import OfficeHours from "../views/OfficeHours.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", name: "Login", component: Login},
    { path: "/signup", name: "Signup", component: Signup },
    { path: "/dashboard", redirect: "/app" }, // Redirect just in case our code somewhere tries to navigate to /dashboard
    
    {
      path: "/app",
      component: DashboardLayout,
      children: [
        { path: "", name: "dashboard", component: Dashboard }, // /app
        { path: "meetings", name: "meetings", component: Meetings },
        { path: "calendar", name: "calendar", component: Calendar },
        { path: "office-hours", name: "office-hours", component: OfficeHours },
        { path: "profile", name: "profile", component: Profile, meta: { hideHeader: true } },
      ],
    },
  ],
});

// This navigation guard checks if the route requires authentication and verifies the user's session by making a request to the backend.
//  If the user is not authenticated, they are redirected to the login page.
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