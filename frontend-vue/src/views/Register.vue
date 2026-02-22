<template>
<main>
    <h1>
        Register
    </h1>
    <form @submit.prevent="" action="/register" method="post">
            <!-- <label for="fname">First name:</label>
            <input type="text" id="fname" name="fname"><br><br>
            <label for="lname">Last name:</label>
            <input type="text" id="lname" name="lname"><br><br>
            <label for="Email">Email:</label> -->
            
            <label>
                <span>First name</span>
                <input 
                    type="password" 
                    v-model="password" 
                    placeholder="********" 
                    id="Password" 
                    name="Password"/>
            </label>
            <br>
            <label>
                <span>last name</span>
                <input 
                    type="password" 
                    v-model="password" 
                    placeholder="********" 
                    id="Password" 
                    name="Password"/>
            </label>
            <br>
            <label>
                <span>Enter your school email</span>
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
            <label>
                <span>Confirm password</span>
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
</main>

</template>
