<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const firstName = ref('')
const lastName = ref('')
const email = ref('')
const password = ref('')
const role = ref('student')

const submitForm = async () => {
  const res = await fetch('/api/signup', {//this needs to be redirected to the webserver api which will 
    method: 'POST',                                            //then reach out to the SQL database for verify signup
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      firstName: firstName.value,
      lastName: lastName.value,
      email: email.value,
      password: password.value,
      role: role.value,
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

      <form class="form" @submit.prevent="submitForm">
        <input
          type="text"
          v-model="firstName"
          name="firstName"
          placeholder="First name"
          autocomplete="given-name"
          required
        />
        <input
          type="text"
          v-model="lastName"
          name="lastName"
          placeholder="Last name"
          autocomplete="family-name"
          required
        />
        <input 
          type="email" 
          v-model="email"
          name="email"
          placeholder="EWU Email (example@ewu.edu)" 
          autocomplete="email" 
          required 
        />
        <input 
          type="password" 
          v-model="password"
          name="password"
          placeholder="Password" 
          autocomplete="new-password" 
          required 
        />
        <select v-model="role" name="role" required>
          <option value="student">Student</option>
          <option value="ta">TA</option>
          <option value="professor">Professor</option>
        </select>
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

.form select {
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

.form select:focus {
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