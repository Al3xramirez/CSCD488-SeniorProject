<script setup>
// This is just a backend health check to see if the API to the backend is working
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

//const router = useRouter();
const backendStatus = ref("Checking backend...");

onMounted(async () => {
  try {
    const response = await fetch("/api/health");
    backendStatus.value = await response.text();
  } catch (error) {
    backendStatus.value = "Backend not reachable";
  }
});


</script>

<template>
  <div class="container">
    <div class="login-card">
      <h1>SyllabusSync</h1>
      <p class="subtitle">Login to your account</p>

      <form class="form" @submit.prevent>
        <input type="email" placeholder="Email" required />
        <input type="password" placeholder="Password" required />
        <button type="submit">Login</button>

        <p class="bottom">
          Don’t have an account?
          <router-link class="link" to="/signup">Sign up here</router-link>
        </p>
      </form>

      <div class="backend-check">
        <span>Backend status:</span>
        <code>{{ backendStatus }}</code>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #161a3a, #ffffff);
  font-family: system-ui, -apple-system, sans-serif;
}

.login-card {
  background: #111827;
  padding: 40px;
  border-radius: 16px;
  width: 100%;
  max-width: 380px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  color: white;
  text-align: center;
}

h1 {
  margin-bottom: 8px;
}

.subtitle {
  margin-bottom: 24px;
  color: #9ca3af;
  font-size: 14px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form input {
  padding: 12px;
  border-radius: 8px;
  border: none;
  outline: none;
  background: #1f2937;
  color: white;
}

.form input:focus {
  box-shadow: 0 0 0 2px #3b82f6;
}

.form button {
  padding: 12px;
  border-radius: 8px;
  border: none;
  background: #3b82f6;
  color: white;
  font-weight: bold;
  cursor: pointer;
}

.form button:hover {
  background: #2563eb;
}

.backend-check {
  margin-top: 20px;
  font-size: 12px;
  color: #9ca3af;
}

.bottom {
  margin-top: 14px;
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
}

.link {
  color: #93c5fd;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>