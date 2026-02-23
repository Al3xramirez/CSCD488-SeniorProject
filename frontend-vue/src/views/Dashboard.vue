

<script setup>
import { ref } from 'vue'
</script>

<template>
    <main>
        <div class = "sidebar__wrapper">
            <ul class = "sidebar__list">
                <li>
                    <button class = "sidebar__item">
                        <span class="item--heading"> Profile </span>
                    </button>
                </li>
                <li>
                    <button class = "sidebar__item">
                        <span class="item--heading"> Office Hours </span>
                    </button>
                </li>
                <li>
                    <button class = "sidebar__item">
                        <span class="item--heading"> Classes </span>
                    </button>
                </li>
            </ul>
        </div>
    </main>
</template>

<style scoped>
main {
    display: flex;
}

.sidebar__wrapper {
    --list-gap: 0.5rem;
    display: flex;
    flex-flow: column;
    gap: var(--list-gap);

    min-height: 100vh;     /* makes it full height */
    width: 100px;          /* sidebar width */
    padding: 1rem;
}
.sidebar__list:hover {
    min-height: 100vh;     /* makes it full height */
    width: 100px;          /* sidebar width */
    padding: 1rem;
    background-color: #3b0ea4;
}

.sidebar__list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-flow: column;
    gap: var(--list-gap);
    overflow: hidden;
}
.sidebar__item {
    block-size: 40px;
    border-radius: 6px;
}
.sidebar__item:hover {
    background-color: #1abc9c;
}
.item--heading {
    display: flex;
    align-items: flex-end;
}
</style>