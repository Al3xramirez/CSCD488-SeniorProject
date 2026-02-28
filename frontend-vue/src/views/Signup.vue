<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

const username = ref("");
const email = ref("");
const password = ref("");
const isLoading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

const submitSignup = async () => {
  isLoading.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const response = await fetch("/api/auth/signup", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: username.value,
        email: email.value,
        password: password.value,
      }),
    });

    const data = await response.json().catch(() => ({ message: "Signup failed." }));
    if (!response.ok) {
      throw new Error(data.message || "Signup failed.");
    }

    successMessage.value = data.message;
    setTimeout(() => router.push("/login"), 900);
  } catch (error) {
    errorMessage.value = error.message || "Signup failed.";
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <div class="container">
    <div class="card">
      <h1>Create account</h1>
      <p class="subtitle">Basic signup for now</p>

      <form class="form" @submit.prevent="submitSignup">
        <input 
          type="text" 
          placeholder="Username" 
          autocomplete="username" 
          v-model="username"
          :disabled="isLoading"
          required 
        />
        <input 
          type="email" 
          placeholder="EWU Email (example@ewu.edu)" 
          autocomplete="email" 
          v-model="email"
          :disabled="isLoading"
          required 
        />
        <input 
          type="password" 
          placeholder="Password" 
          autocomplete="new-password" 
          v-model="password"
          :disabled="isLoading"
          required 
        />
        <button type="submit" :disabled="isLoading">
          {{ isLoading ? "Signing up..." : "Sign Up" }}
        </button>
      </form>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>

      <p class="bottom">Already have an account?<router-link class="link" to="/login">Log in</router-link> 
        <!-- router-link is used to navigate to the login page -->
      </p>
    </div>
  </div>
</template>

<style scoped>
.container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #ffffff, #f5f5f5);
  font-family: system-ui, -apple-system, sans-serif;
}

.card {
  background: #111827;
  padding: 40px;
  border-radius: 16px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  color: white;
  text-align: center;
}

h1 {
  margin: 0 0 8px;
}

.subtitle {
  margin: 0 0 22px;
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

.bottom {
  margin-top: 14px;
  font-size: 13px;
  color: #9ca3af;
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
