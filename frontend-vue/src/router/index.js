import { createRouter, createWebHistory } from "vue-router";

import Login from "../views/Login.vue";
import Signup from "../views/Signup.vue";
import Dashboard from "../views/Dashboard.vue";
import Profile from "../views/Profile.vue";
import Meetings from "../views/Meetings.vue";
import DashboardLayout from "../layout/DashboardLayout.vue";
import Calendar from "../views/Calendar.vue";
import MyOfficeHours from "../views/MyOfficeHours.vue";
import SyllabusUpload from "../views/SyllabusUpload.vue";
import WorkloadProjections from "../views/WorkloadProjections.vue";
import MyClasses from "../views/MyClasses.vue";
import OfficeHours from "../views/newofficehours.vue";
import ClassDetails from "../views/ClassDetails.vue";

const syllabusImportEnabled = (import.meta.env.VITE_SYLLABUS_IMPORT_ENABLED || "true").toString().toLowerCase() !== "false";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },// set default route to dashboard?? we redirect to login as auth feature thoughts?
    { path: "/login", name: "Login", component: Login},
    { path: "/signup", name: "Signup", component: Signup },    
    {
      path: "/app",
      component: DashboardLayout,
      meta: { requiresAuth: true }, // This route and all its children require authentication
      children: [
        { path: "", name: "dashboard", component: Dashboard }, // /app
        { path: "classes", name: "classes", component: MyClasses },
        { path: "classes/:joinCode", name: "class-details", component: ClassDetails }, // sub-route for class details
        { path: "meetings", name: "meetings", component: Meetings },
        { path: "calendar", name: "calendar", component: Calendar },
        { path: "office-hours", name: "office-hours", component: OfficeHours },
        { path: "my-office-hours", name: "my-office-hours", component: MyOfficeHours },
        { path: "syllabus-upload", name: "syllabus-upload", component: SyllabusUpload },
        { path: "workload-projections", name: "workload-projections", component: WorkloadProjections },
        { path: "profile", name: "profile", component: Profile, meta: { hideHeader: true } },
      ],
    },
  ],
});

// This navigation guard checks if the route requires authentication and verifies the user's session by making a request to the backend.
//  If the user is not authenticated, they are redirected to the login page.
router.beforeEach(async (to, from, next) => {
  if (to.name === 'syllabus-upload' && !syllabusImportEnabled) {
    next('/app');
    return;
  }

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