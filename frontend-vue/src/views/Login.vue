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

        <form @submit.prevent="" action="/login" method="post">
            <!-- <label for="fname">First name:</label>
            <input type="text" id="fname" name="fname"><br><br>
            <label for="lname">Last name:</label>
            <input type="text" id="lname" name="lname"><br><br>
            <label for="Email">Email:</label> -->
            
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
                Dont have an account? <RouterLink to = "/register">Register</RouterLink>
            </p>
        </footer>
    </main>
</template>

<style scoped>
main {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: flex-start;
    height: 100vh;
}

header {
    padding: 1.5rem;
}
footer {
    width: 100%;
    /* color: */
    text-align: center;
}
form {
    flex: 1 1 0%
}
</style>
