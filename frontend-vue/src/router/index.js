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
    { path: "/login", name: "login", component: Login},
    { path: "/signup", name: "signup", component: Signup },
    { path: "/Dashboard", name: "Dashboard", component: Dashboard },
    { path: "/Profile", name: "Profile", component: Profile },
    { path: "/Courses", name: "Courses", component: Courses },
    { path: "/Meetings", name: "Meetings", component: Meetings }
  ],
});

export default router;