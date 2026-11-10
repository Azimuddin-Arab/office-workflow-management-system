<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>User Management</h1>
        <p>Manage system users and their assigned roles.</p>
      </div>

      <router-link to="/users/create" class="btn primary">
        + Create User
      </router-link>
    </div>

    <div class="summary">
      <div class="summary-card">
        <span>Total Users</span>
        <strong>{{ users.length }}</strong>
      </div>

      <div class="summary-card">
        <span>Enabled Users</span>
        <strong>{{ users.filter(user => user.enabled).length }}</strong>
      </div>

      <div class="summary-card">
        <span>Disabled Users</span>
        <strong>{{ users.filter(user => !user.enabled).length }}</strong>
      </div>
    </div>

    <p v-if="loading">Loading users...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <div v-else class="table-card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Full Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Permissions</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.fullName }}</td>
            <td>{{ user.email }}</td>
            <td>
              <span class="role-label">{{ user.role || 'No role' }}</span>
            </td>
            <td>{{ user.permissions?.length ?? 0 }}</td>
            <td>
              <span :class="user.enabled ? 'status active' : 'status inactive'">
                {{ user.enabled ? 'Enabled' : 'Disabled' }}
              </span>
            </td>
            <td>
              <button class="btn small danger" @click="deleteUser(user)">
                Delete
              </button>
            </td>
          </tr>

          <tr v-if="users.length === 0">
            <td colspan="7" class="empty">No users found.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/api'

const users = ref([])
const loading = ref(false)
const error = ref('')

async function loadUsers() {
  loading.value = true
  error.value = ''

  try {
    const response = await api.get('/users')
    users.value = Array.isArray(response.data)
      ? response.data
      : response.data.content ?? []
  } catch (err) {
    error.value = err.response?.data?.message || 'Unable to load users.'
  } finally {
    loading.value = false
  }
}

async function deleteUser(user) {
  if (!confirm(`Are you sure you want to delete ${user.fullName}?`)) {
    return
  }

  try {
    await api.delete(`/users/${user.id}`)
    await loadUsers()
  } catch (err) {
    alert(err.response?.data?.message || 'Unable to delete user.')
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }
.page-header p { color: #64748b; }
.summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 16px; margin-bottom: 22px; }
.summary-card { background: white; padding: 20px; border-radius: 9px; box-shadow: 0 2px 10px #0000000a; display: flex; flex-direction: column; gap: 10px; }
.summary-card span { color: #64748b; }
.summary-card strong { font-size: 25px; }
.table-card { background: white; padding: 16px; border-radius: 10px; overflow-x: auto; box-shadow: 0 2px 10px #0000000a; }
table { width: 100%; min-width: 850px; border-collapse: collapse; }
th, td { padding: 13px; border-bottom: 1px solid #e5e7eb; text-align: left; }
th { background: #f8fafc; }
.btn { display: inline-block; padding: 9px 13px; border: 0; border-radius: 6px; background: #e2e8f0; color: #1e293b; text-decoration: none; cursor: pointer; }
.primary { background: #2563eb; color: white; }
.small { padding: 6px 10px; font-size: 13px; }
.danger { background: #fee2e2; color: #b91c1c; }
.role-label { background: #eff6ff; color: #1d4ed8; padding: 5px 9px; border-radius: 5px; }
.status { padding: 5px 9px; border-radius: 20px; font-size: 12px; }
.active { background: #dcfce7; color: #166534; }
.inactive { background: #fee2e2; color: #991b1b; }
.error { color: #dc2626; }
.empty { text-align: center; color: #64748b; }
</style>