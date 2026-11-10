<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Create User</h1>
        <p>Create a user account and assign a system role.</p>
      </div>

      <router-link to="/users" class="btn">Back to Users</router-link>
    </div>

    <form class="form-card" @submit.prevent="createUser">
      <div class="field">
        <label>Full Name</label>
        <input
          v-model.trim="form.fullName"
          type="text"
          placeholder="Enter full name"
          required
        />
      </div>

      <div class="field">
        <label>Email Address</label>
        <input
          v-model.trim="form.email"
          type="email"
          placeholder="Enter email address"
          required
        />
      </div>

      <div class="field">
        <label>Password</label>
        <input
          v-model="form.password"
          type="password"
          placeholder="Enter password"
          minlength="6"
          required
        />
      </div>

      <div class="field">
        <label>Assign Role</label>
        <select v-model="form.role" required>
          <option value="" disabled>Select a role</option>

          <option
            v-for="role in roles"
            :key="role.id"
            :value="role.name"
          >
            {{ role.name }}
          </option>
        </select>

        <small v-if="selectedRole">
          {{ selectedRole.description || 'No role description available.' }}
        </small>
      </div>

      <label class="enabled">
        <input v-model="form.enabled" type="checkbox" />
        Enable User Account
      </label>

      <div v-if="selectedRole" class="role-info">
        <h3>Role Permissions</h3>

        <p v-if="selectedRole.permissionCodes?.length">
          This user will inherit the following permissions:
        </p>

        <div class="permission-list">
          <span
            v-for="permission in selectedRole.permissionCodes || []"
            :key="permission"
            class="permission-tag"
          >
            {{ permission }}
          </span>
        </div>

        <p v-if="!selectedRole.permissionCodes?.length">
          No permissions are currently assigned to this role.
        </p>
      </div>

      <p v-if="loadingRoles" class="info">Loading roles...</p>
      <p v-if="error" class="error">{{ error }}</p>

      <div class="form-actions">
        <router-link to="/users" class="btn">Cancel</router-link>

        <button
          type="submit"
          class="btn primary"
          :disabled="saving || loadingRoles || roles.length === 0"
        >
          {{ saving ? 'Creating...' : 'Create User' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/api'

const router = useRouter()

const roles = ref([])
const loadingRoles = ref(false)
const saving = ref(false)
const error = ref('')

const form = ref({
  fullName: '',
  email: '',
  password: '',
  role: '',
  enabled: true
})

const selectedRole = computed(() => {
  return roles.value.find(role => role.name === form.value.role)
})

async function loadRoles() {
  loadingRoles.value = true
  error.value = ''

  try {
    const response = await api.get('/roles')
    const allRoles = Array.isArray(response.data)
      ? response.data
      : response.data.content ?? []

    roles.value = allRoles.filter(role => role.enabled)
  } catch (err) {
    error.value = err.response?.data?.message || 'Unable to load roles.'
  } finally {
    loadingRoles.value = false
  }
}

async function createUser() {
  saving.value = true
  error.value = ''

  try {
    await api.post('/users', {
      fullName: form.value.fullName,
      email: form.value.email,
      password: form.value.password,
      role: form.value.role,
      enabled: form.value.enabled
    })

    alert('User created successfully.')
    router.push('/users')
  } catch (err) {
    error.value = err.response?.data?.message || 'Unable to create user.'
  } finally {
    saving.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }
.page-header p { color: #64748b; }
.form-card { max-width: 750px; background: white; padding: 24px; border-radius: 10px; box-shadow: 0 2px 10px #0000000a; }
.field { margin-bottom: 18px; }
.field label { display: block; font-weight: 600; margin-bottom: 7px; }
.field input, .field select { width: 100%; box-sizing: border-box; padding: 11px; border: 1px solid #cbd5e1; border-radius: 6px; font: inherit; }
.field small { display: block; color: #64748b; margin-top: 7px; }
.enabled { display: flex; align-items: center; gap: 8px; margin: 20px 0; }
.role-info { background: #f8fafc; padding: 16px; border-radius: 8px; margin-top: 20px; }
.role-info h3 { margin-top: 0; }
.role-info p { color: #64748b; }
.permission-list { display: flex; flex-wrap: wrap; gap: 8px; }
.permission-tag { background: #dbeafe; color: #1e40af; padding: 6px 9px; border-radius: 5px; font-size: 12px; }
.btn { display: inline-block; padding: 9px 14px; border: 0; border-radius: 6px; background: #e2e8f0; color: #1e293b; text-decoration: none; cursor: pointer; }
.primary { background: #2563eb; color: white; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
.error { color: #dc2626; }
.info { color: #2563eb; }
</style>