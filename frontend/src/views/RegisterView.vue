
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api/api';

const router = useRouter();

const fullName = ref('');
const email = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const register = async () => {
    errorMessage.value = '';
    successMessage.value = '';

    if (!fullName.value || !email.value || !password.value) {
        errorMessage.value = 'Please complete all fields.';
        return;
    }

    if (password.value.length < 8) {
        errorMessage.value = 'Password must contain at least 8 characters.';
        return;
    }

    loading.value = true;

    try {
        await api.post('/auth/register', {
            fullName: fullName.value,
            email: email.value,
            password: password.value
        });

        successMessage.value = 'Registration successful!';

        setTimeout(() => {
            router.push('/login');
        }, 1000);

    } catch (error) {
        errorMessage.value =
            error.response?.data?.message ||
            'Registration failed. Please try again.';
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <div class="register-container">
        <div class="register-card">

            <h1>Create Account</h1>
            <p class="subtitle">Register a new account</p>

            <form @submit.prevent="register">

                <div class="form-group">
                    <label>Full Name</label>
                    <input
                        v-model="fullName"
                        type="text"
                        placeholder="Enter your full name"
                    />
                </div>

                <div class="form-group">
                    <label>Email Address</label>
                    <input
                        v-model="email"
                        type="email"
                        placeholder="Enter your email"
                    />
                </div>

                <div class="form-group">
                    <label>Password</label>
                    <input
                        v-model="password"
                        type="password"
                        placeholder="Minimum 8 characters"
                    />
                </div>

                <p v-if="errorMessage" class="error">
                    {{ errorMessage }}
                </p>

                <p v-if="successMessage" class="success">
                    {{ successMessage }}
                </p>

                <button type="submit" :disabled="loading">
                    {{ loading ? 'Registering...' : 'Register' }}
                </button>

            </form>

            <p class="login-link">
                Already have an account?
                <RouterLink to="/login">Login here</RouterLink>
            </p>

        </div>
    </div>
</template>

<style scoped>
.register-container {
    min-height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background: #f1f5f9;
    padding: 20px;
}

.register-card {
    width: 100%;
    max-width: 430px;
    background: white;
    padding: 35px;
    border-radius: 12px;
    box-shadow: 0 5px 25px rgba(0, 0, 0, 0.08);
}

h1 {
    text-align: center;
    color: #17365d;
}

.subtitle {
    text-align: center;
    color: #64748b;
    margin-bottom: 30px;
}

.form-group {
    margin-bottom: 20px;
}

label {
    display: block;
    margin-bottom: 8px;
    font-weight: 600;
    color: #334155;
}

input {
    width: 100%;
    padding: 12px;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    box-sizing: border-box;
}

button {
    width: 100%;
    padding: 13px;
    background: #2563eb;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-weight: 600;
}

button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.error {
    color: #dc2626;
}

.success {
    color: #16a34a;
}

.login-link {
    text-align: center;
    margin-top: 25px;
    font-size: 14px;
}
</style>
