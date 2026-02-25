import { createRouter, createWebHistory } from "vue-router";

import Login from "../views/Login.vue";
import Signup from "../views/Signup.vue";

import DashboardLayout from "../layouts/DashboardLayout.vue";

import DashboardHome from "../views/DashboardHome.vue";
import MeetingTimes from "../views/MeetingTimes.vue";
import CalendarView from "../views/CalendarView.vue";
import OfficeHoursMatch from "../views/OfficeHoursMatch.vue";

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
        { path: "", name: "dashboard", component: DashboardHome }, // /app
        { path: "meeting-times", name: "meeting-times", component: MeetingTimes },
        { path: "calendar", name: "calendar", component: CalendarView },
        { path: "office-hours", name: "office-hours", component: OfficeHoursMatch },
      ],
    },
  ],
});

export default router;