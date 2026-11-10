
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api/api';

const router = useRouter();

const email = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
const showPassword = ref(false);

const login = async () => {
    errorMessage.value = '';

    if (!email.value || !password.value) {
        errorMessage.value = 'Please enter email and password.';
        return;
    }

    loading.value = true;

    try {
        const response = await api.post('/auth/login', {
            email: email.value,
            password: password.value
        });

        // Save JWT token
        localStorage.setItem('token', response.data.token);

        // Save user information
        localStorage.setItem('user', JSON.stringify({
            id: response.data.id,
            fullName: response.data.fullName,
            email: response.data.email,
            role: response.data.role
        }));

        // Navigate to dashboard
        router.push('/dashboard');

    } catch (error) {
        if (error.response) {
            errorMessage.value =
                error.response.data.message ||
                'Invalid email or password.';
        } else {
            errorMessage.value =
                'Unable to connect to the server.';
        }
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <div class="login-page">

        <!-- Background overlay -->
        <div class="background-overlay"></div>

        <div class="login-wrapper">

            <!-- Left branding section -->
            <section class="branding-section">

                <div class="brand-logo">
                    <svg viewBox="0 0 100 100" fill="none">
                        <circle cx="50" cy="39" r="27"
                            stroke="white" stroke-width="5"/>

                        <circle cx="50" cy="39" r="6"
                            fill="white"/>

                        <path d="M50 12V20 M50 58V66
                                 M23 39H31 M69 39H77
                                 M31 20L37 26 M63 52L69 58
                                 M69 20L63 26 M37 52L31 58"
                            stroke="white"
                            stroke-width="5"
                            stroke-linecap="round"/>

                        <path d="M17 72 Q34 62 50 72
                                 Q67 82 83 72"
                            stroke="white"
                            stroke-width="5"
                            stroke-linecap="round"/>

                        <path d="M22 83 Q50 95 78 83"
                            stroke="#60A5FA"
                            stroke-width="5"
                            stroke-linecap="round"/>
                    </svg>
                </div>

                <h1>Office Workflow Management System</h1>

                <p class="brand-description">
                    Manage. Organize. Grow.
                </p>

                <div class="brand-divider"></div>

                <div class="features">

                    <div class="feature">
                        <span class="feature-icon">✓</span>
                        <div>
                            <h3>Secure Access</h3>
                            <p>Your information is protected.</p>
                        </div>
                    </div>

                    <div class="feature">
                        <span class="feature-icon">▥</span>
                        <div>
                            <h3>Efficient Management</h3>
                            <p>Simplify your daily operations.</p>
                        </div>
                    </div>

                    <div class="feature">
                        <span class="feature-icon">↗</span>
                        <div>
                            <h3>Better Organization</h3>
                            <p>Manage your workforce effectively.</p>
                        </div>
                    </div>

                </div>

                <p class="branding-footer">
                    A smarter way to manage your organization.
                </p>

            </section>

            <!-- Right login section -->
            <section class="login-section">

                <div class="login-card">

                    <!-- Logo -->
                    <div class="login-logo">
                        <svg viewBox="0 0 100 100" fill="none">
                            <circle cx="50" cy="39" r="27"
                                stroke="#174A8B" stroke-width="5"/>

                            <circle cx="50" cy="39" r="6"
                                fill="#174A8B"/>

                            <path d="M50 12V20 M50 58V66
                                     M23 39H31 M69 39H77
                                     M31 20L37 26 M63 52L69 58
                                     M69 20L63 26 M37 52L31 58"
                                stroke="#174A8B"
                                stroke-width="5"
                                stroke-linecap="round"/>

                            <path d="M17 72 Q34 62 50 72
                                     Q67 82 83 72"
                                stroke="#174A8B"
                                stroke-width="5"
                                stroke-linecap="round"/>

                            <path d="M22 83 Q50 95 78 83"
                                stroke="#3B82F6"
                                stroke-width="5"
                                stroke-linecap="round"/>
                        </svg>
                    </div>

                    <h2>Welcome Back</h2>

                    <p class="login-subtitle">
                        Sign in to access your account
                    </p>

                    <form @submit.prevent="login">

                        <!-- Email -->
                        <div class="input-group">

                            <label>Email Address</label>

                            <div class="input-wrapper">
                                <span class="input-icon">✉</span>

                                <input
                                    v-model="email"
                                    type="email"
                                    placeholder="Enter your email address"
                                    autocomplete="username"
                                    required
                                />
                            </div>

                        </div>

                        <!-- Password -->
                        <div class="input-group">

                            <label>Password</label>

                            <div class="input-wrapper">
                                <span class="input-icon">▣</span>

                                <input
                                    v-model="password"
                                    :type="showPassword ? 'text' : 'password'"
                                    placeholder="Enter your password"
                                    autocomplete="current-password"
                                    required
                                />

                                <button
                                    type="button"
                                    class="password-toggle"
                                    @click="showPassword = !showPassword"
                                    :aria-label="showPassword ? 'Hide password' : 'Show password'"
                                >
                                    {{ showPassword ? 'Hide' : 'Show' }}
                                </button>
                            </div>

                        </div>

                        <!-- Error -->
                        <div v-if="errorMessage" class="error-message">
                            {{ errorMessage }}
                        </div>

                        <!-- Login button -->
                        <button
                            type="submit"
                            class="login-button"
                            :disabled="loading"
                        >
                            <span>
                                {{ loading ? 'Signing in...' : 'Sign In' }}
                            </span>

                            <span v-if="!loading">→</span>
                        </button>

                    </form>

                    <div class="secure-message">
                        <span>🛡</span>
                        Secure and Protected Access
                    </div>

                    <div class="card-footer">
                        Office Workflow Management System
                        <span>© 2026</span>
                    </div>

                </div>

            </section>

        </div>

    </div>
</template>

<style scoped>
* {
    box-sizing: border-box;
}

.login-page {
    min-height: 100vh;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;

    background-image:
        linear-gradient(
            90deg,
            rgba(7, 35, 75, 0.92),
            rgba(7, 35, 75, 0.48)
        ),
        url('/images/ship-background.jpg');

    background-position: center;
    background-size: cover;
    background-repeat: no-repeat;

    padding: 35px;
    font-family: Arial, sans-serif;
}

.background-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(
        135deg,
        rgba(15, 54, 105, 0.25),
        transparent
    );
}

.login-wrapper {
    position: relative;
    z-index: 1;
    width: 100%;
    max-width: 1150px;

    display: grid;
    grid-template-columns: 1fr 480px;
    align-items: center;
    gap: 55px;
}

/* Branding */

.branding-section {
    color: white;
    padding: 30px;
}

.brand-logo {
    width: 100px;
    height: 100px;
    margin-bottom: 20px;
}

.brand-logo svg,
.login-logo svg {
    width: 100%;
    height: 100%;
}

.branding-section h1 {
    font-size: 38px;
    line-height: 1.25;
    max-width: 500px;
    margin: 0;
}

.brand-description {
    font-size: 19px;
    color: #bfdbfe;
    letter-spacing: 2px;
    margin-top: 18px;
}

.brand-divider {
    width: 75px;
    height: 4px;
    background: #60a5fa;
    margin: 30px 0;
    border-radius: 10px;
}

.features {
    display: flex;
    flex-direction: column;
    gap: 25px;
}

.feature {
    display: flex;
    align-items: center;
    gap: 17px;
}

.feature-icon {
    width: 42px;
    height: 42px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid rgba(255,255,255,0.7);
    border-radius: 50%;
    font-size: 21px;
    flex-shrink: 0;
}

.feature h3 {
    font-size: 16px;
    margin: 0 0 6px;
}

.feature p {
    font-size: 13px;
    color: #dbeafe;
    margin: 0;
}

.branding-footer {
    margin-top: 45px;
    color: #bfdbfe;
    font-size: 13px;
}

/* Login card */

.login-section {
    width: 100%;
}

.login-card {
    background: rgba(255, 255, 255, 0.97);
    padding: 45px 40px;
    border-radius: 18px;

    box-shadow:
        0 20px 60px rgba(0, 0, 0, 0.25);

    border: 1px solid rgba(255,255,255,0.7);
}

.login-logo {
    width: 75px;
    height: 75px;
    margin: 0 auto 18px;
}

.login-card h2 {
    text-align: center;
    font-size: 30px;
    color: #17365d;
    margin: 0;
}

.login-subtitle {
    text-align: center;
    color: #64748b;
    margin: 12px 0 35px;
    font-size: 15px;
}

/* Inputs */

.input-group {
    margin-bottom: 22px;
}

.input-group label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: #334155;
    margin-bottom: 9px;
}

.input-wrapper {
    height: 52px;
    display: flex;
    align-items: center;

    border: 1px solid #cbd5e1;
    border-radius: 8px;
    background: white;

    transition: 0.2s;
}

.input-wrapper:focus-within {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.input-icon {
    padding: 0 15px;
    color: #2563eb;
    font-size: 19px;
}

.input-wrapper input {
    flex: 1;
    width: 100%;
    height: 100%;
    border: none;
    outline: none;
    background: transparent;
    font-size: 14px;
    color: #1e293b;
}

.input-wrapper input::placeholder {
    color: #94a3b8;
}

.password-toggle {
    border: none;
    background: transparent;
    color: #2563eb;
    cursor: pointer;
    padding: 10px;
    font-size: 12px;
    font-weight: 600;
}

/* Login button */

.login-button {
    width: 100%;
    height: 53px;

    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;

    border: none;
    border-radius: 8px;

    background: linear-gradient(
        110deg,
        #2563eb,
        #174a8b
    );

    color: white;
    font-size: 16px;
    font-weight: 600;

    cursor: pointer;
    transition: 0.2s;
    margin-top: 10px;
}

.login-button:hover {
    transform: translateY(-2px);
    box-shadow: 0 7px 18px rgba(37, 99, 235, 0.25);
}

.login-button:disabled {
    opacity: 0.65;
    cursor: not-allowed;
    transform: none;
}

.error-message {
    background: #fee2e2;
    color: #b91c1c;
    padding: 12px;
    border-radius: 7px;
    font-size: 13px;
    margin-bottom: 15px;
}

/* Footer */

.secure-message {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    color: #174a8b;
    font-size: 13px;
    margin-top: 28px;
}

.card-footer {
    border-top: 1px solid #e2e8f0;
    margin-top: 30px;
    padding-top: 18px;
    display: flex;
    justify-content: space-between;
    color: #94a3b8;
    font-size: 11px;
}

/* Responsive */

@media (max-width: 850px) {
    .login-wrapper {
        grid-template-columns: 1fr;
        max-width: 480px;
        gap: 20px;
    }

    .branding-section {
        text-align: center;
        padding: 10px;
    }

    .brand-logo {
        margin: 0 auto 15px;
    }

    .branding-section h1 {
        font-size: 27px;
    }

    .brand-divider,
    .features,
    .branding-footer {
        display: none;
    }
}

@media (max-width: 500px) {
    .login-page {
        padding: 18px;
    }

    .login-card {
        padding: 30px 22px;
    }

    .login-card h2 {
        font-size: 25px;
    }

    .branding-section h1 {
        font-size: 23px;
    }
}
</style>
