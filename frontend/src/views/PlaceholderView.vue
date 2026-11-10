
<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
</script>

<template>
    <div class="placeholder">
        <h1>{{ route.meta.title }}</h1>

        <p>
            This module is coming soon.
            We will implement it step by step.
        </p>
    </div>
</template>

<style scoped>
.placeholder {
    background: white;
    padding: 30px;
    border-radius: 10px;
    min-height: 250px;
}

h1 {
    color: #17365d;
}

p {
    color: #64748b;
}
</style>
