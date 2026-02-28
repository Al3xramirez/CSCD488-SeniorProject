<script setup>
// This is just a backend health check to see if the API to the backend is working
import { ref, onMounted } from "vue";

const backendStatus = ref("Checking backend...");
const email = ref("");
const password = ref("");
const isLoading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

onMounted(async () => {
  try {
    const response = await fetch("/api/health");
    backendStatus.value = await response.text();
  } catch (error) {
    backendStatus.value = "Backend not reachable";
  }
});

const submitLogin = async () => {
  isLoading.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: email.value,
        password: password.value,
      }),
    });

    const data = await response.json().catch(() => ({ message: "Login failed." }));
    if (!response.ok) {
      throw new Error(data.message || "Login failed.");
    }

    successMessage.value = data.message;
  } catch (error) {
    errorMessage.value = error.message || "Login failed.";
  } finally {
    isLoading.value = false;
  }
};

</script>

<template>

  <div class="container">
    <div class="login-card">
      <h1>SyllabusSync</h1>
      <p class="subtitle">Login to your account</p>

      <form class="form" @submit.prevent="submitLogin"> 
        <!-- submit.prevent is used to prevent the page from refreshing when the form is submitted -->
        <input type="email" placeholder="Email" v-model="email" :disabled="isLoading" required />
        <input type="password" placeholder="Password" v-model="password" :disabled="isLoading" required />
        <button type="submit" :disabled="isLoading">
          {{ isLoading ? "Logging in..." : "Login" }}
        </button>
        <p class="bottom"> Don’t have an account? <router-link class="link" to="/signup">Sign up here</router-link> <!-- router-link is used to navigate to the signup page -->
        </p>
      </form>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>

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
  background: linear-gradient(135deg, #fdfdfd, #fbfbfc);
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

.error {
  margin-top: 12px;
  color: #fca5a5;
  font-size: 13px;
}

.success {
  margin-top: 12px;
  color: #86efac;
  font-size: 13px;
}

</style>
