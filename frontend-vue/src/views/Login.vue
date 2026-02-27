<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const password = ref('')

const submitForm = async () => {
  const res = await fetch('/api/login', { //this needs to be redirected to the webserver api which will 
    method: 'POST',                                   //then reach out to the SQL database for authenticate login
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    credentials: 'include',
    body: new URLSearchParams({
      username: username.value,
      password: password.value
    })
  })

  if(res.ok){
    router.push('/dashboard')
  } else {
    alert('Invalid login')
  }
}
</script>

<template>
  <div class="container">
    <main class="card">
      <header>
        <h1 class="logo">SyllabusSync</h1>
        <h2>Login</h2>
        <p class="subtitle">Login or create a account to begin using syllabus</p>
      </header>

      <form class="form" @submit.prevent="submitForm">
        <label>
          <span></span>
          <input
            placeholder="test@test.com"
            id="username"
            name="username"
            type="text"
            v-model="username"
            required
          />
        </label>

        <label>
          <span></span>
          <input
            placeholder="********"
            id="password"
            name="password"
            type="password"
            v-model="password"
            required
          />
        </label>

        <button type="submit">Login</button>
      </form>

      <p class="bottom">
        Dont have an account?
        <RouterLink class="link" to="/signup">Sign Up</RouterLink>
      </p>
    </main>
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

.form label {
  display: block;
  width: 100%;
}

.form input {
  width: 100%;
  box-sizing: border-box;
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
</style>