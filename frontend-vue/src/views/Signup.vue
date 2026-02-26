<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const email = ref('')
const password = ref('')

const submitForm = async () => {
  const res = await fetch('http://localhost:8080/api/signup', {//this needs to be redirected to the webserver api which will 
    method: 'POST',                                            //then reach out to the SQL database for verify signup
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: username.value,
      email: email.value,
      password: password.value
    })
  })

  if(res.ok){
    router.push('/login')
  } else {
    alert('Invalid signup')
  }
}
</script>

<template>
  <div class="container">
    <div class="card">
      <h1>Create account</h1>
      <p class="subtitle">Basic signup for now</p>

      <form class="form" @submit.prevent="submitForm">
        <input 
          type="text" 
          v-model="username"
          name = "username"
          placeholder="Username" 
          autocomplete="username" 
          required 
        />
        <input 
          type="email" 
          v-model="email"
          name = "email"
          placeholder="EWU Email (example@ewu.edu)" 
          autocomplete="email" 
          required 
        />
        <input 
          type="password" 
          v-model="password"
          name = "password"
          placeholder="Password" 
          autocomplete="new-password" 
          required 
        />
        <button type="submit">Sign Up</button>
      </form>

      <p class="bottom">Already have an account?<router-link class="link" to="/login">Log in</router-link> 
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
</style>