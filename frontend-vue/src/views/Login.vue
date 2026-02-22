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

</style>
<script setup>
import { ref } from 'vue'

const email = ref('')
const password = ref('')
</script>

<template>
    <main>
        <header>
            <h1 class = "logo">Secret App</h1>
            <h2>Login</h2>
            <p>Login or createan account to begin using syllabus </p>
        </header>

        <form @submit.prevent="" action="/login" method="get">
            
            <label>
                <span>Enter your email</span>
                <input 
                    type="email" 
                    v-model="email" 
                    placeholder="test@test.com" 
                    id="Email" 
                    name="Email"/>
            </label>
            <br>
            <label>
                <span>Enter your password</span>
                <input 
                    type="password" 
                    v-model="password" 
                    placeholder="********" 
                    id="Password" 
                    name="Password"/>
            </label>
            <br>
            <input 
                type="submit" 
                value="Login"/>
        
        </form>
        <footer>
            <p>
                Dont have an account? <RouterLink to = "/signup">Register</RouterLink> Signup Here
            </p>
        </footer>
    </main>
</template>
