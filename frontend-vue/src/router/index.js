import { createRouter, createWebHistory } from "vue-router";

import Login from "../views/Login.vue";
import Signup from "../views/Signup.vue";

import DashboardLayout from "../layouts/DashboardLayout.vue";

import Dashboard from "../views/Dashboard.vue";
import Meetings from "../views/Meetings.vue";
import Calendar from "../views/Calendar.vue";
import OfficeHours from "../views/OfficeHours.vue";
import Profile from "../views/Profile.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", name: "login", component: Login },
    { path: "/signup", name: "signup", component: Signup },

    // These are all nested under /app, which uses the DashboardLayout. The children are the actual pages. 
    // According to Vue and Copilot this is actually a very good way to do this as it gives authentication guards
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

export default router;