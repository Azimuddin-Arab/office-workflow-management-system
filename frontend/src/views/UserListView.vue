
<template>
  <div class="user-page">

    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h2>User Management</h2>
        <p>Manage system users, roles and permissions.</p>
      </div>

      <button class="btn-primary" @click="createUser">
        + Create User
      </button>
    </div>

    <!-- Statistics -->
    <div class="stats-grid">

      <div class="stat-card">
        <span>Total Users</span>
        <h2>{{ users.length }}</h2>
      </div>

      <div class="stat-card">
        <span>Administrators</span>
        <h2>{{ adminCount }}</h2>
      </div>

      <div class="stat-card">
        <span>Other Role Users</span>
        <h2>{{ otherRoleCount }}</h2>
      </div>

      <div class="stat-card">
        <span>Active Users</span>
        <h2>{{ activeCount }}</h2>
      </div>

    </div>

    <!-- Filters -->
    <div class="filter-card">

      <input
        v-model="search"
        type="text"
        placeholder="Search by name or email..."
      />

      <select v-model="roleFilter">
        <option value="">All Roles</option>

        <option
          v-for="role in roles"
          :key="role.id"
          :value="role.name"
        >
          {{ role.name }}
        </option>
      </select>

      <select v-model="statusFilter">
        <option value="">All Statuses</option>
        <option value="true">Active</option>
        <option value="false">Disabled</option>
      </select>

      <button class="btn-secondary" @click="resetFilters">
        Reset
      </button>

    </div>

    <!-- Loading -->
    <div v-if="loading" class="message">
      Loading users...
    </div>

    <!-- Error -->
    <div v-else-if="errorMessage" class="error-message">
      {{ errorMessage }}

      <button @click="loadUsers">
        Retry
      </button>
    </div>

    <!-- Users Table -->
    <div v-else class="table-card">

      <div class="table-header">
        <h3>System Users</h3>
        <span>{{ filteredUsers.length }} users found</span>
      </div>

      <div class="table-responsive">

        <table>

          <thead>
            <tr>
              <th>#</th>
              <th>User</th>
              <th>Email</th>
              <th>Role</th>
              <th>Permissions</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>

            <tr
              v-for="(user, index) in filteredUsers"
              :key="user.id"
            >

              <td>{{ index + 1 }}</td>

              <td>
                <div class="user-info">

                  <div class="avatar">
                    {{ user.fullName?.charAt(0)?.toUpperCase() || '?' }}
                  </div>

                  <strong>{{ user.fullName }}</strong>

                </div>
              </td>

              <td>{{ user.email }}</td>

              <td>
                <span
                  class="role-badge"
                  :class="user.role?.toUpperCase() === 'ADMIN' ? 'admin' : 'user'"
                >
                  {{ user.role || 'No Role' }}
                </span>
              </td>

              <td>
                <span class="permission-count">
                  {{ user.permissions?.length || 0 }} permissions
                </span>
              </td>

              <td>
                <span
                  class="status-badge"
                  :class="user.enabled ? 'active' : 'disabled'"
                >
                  {{ user.enabled ? 'Active' : 'Disabled' }}
                </span>
              </td>

              <td>
                <div class="actions">

                  <button
                    class="edit-btn"
                    @click="editUser(user.id)"
                  >
                    Edit
                  </button>

                  <button
                    class="delete-btn"
                    @click="deleteUser(user)"
                  >
                    Delete
                  </button>

                </div>
              </td>

            </tr>

            <tr v-if="filteredUsers.length === 0">
              <td colspan="7" class="empty-state">
                No users found.
              </td>
            </tr>

          </tbody>

        </table>

      </div>

    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/api'

const router = useRouter()

// =====================================
// STATE
// =====================================

const users = ref([])
const roles = ref([])

const loading = ref(false)
const loadingRoles = ref(false)
const errorMessage = ref('')

const search = ref('')
const roleFilter = ref('')
const statusFilter = ref('')

// =====================================
// STATISTICS
// =====================================

// Total administrators
const adminCount = computed(() => {
  return users.value.filter(
    user => user.role?.toUpperCase() === 'ADMIN'
  ).length
})

// Users with roles other than ADMIN
const otherRoleCount = computed(() => {
  return users.value.filter(
    user => user.role?.toUpperCase() !== 'ADMIN'
  ).length
})

// Active users
const activeCount = computed(() => {
  return users.value.filter(
    user => user.enabled === true
  ).length
})

// =====================================
// SEARCH AND FILTER
// =====================================

const filteredUsers = computed(() => {
  return users.value.filter(user => {

    const searchText = search.value.toLowerCase().trim()

    const matchesSearch =
      (user.fullName || '').toLowerCase().includes(searchText) ||
      (user.email || '').toLowerCase().includes(searchText)

    const matchesRole =
      !roleFilter.value ||
      user.role === roleFilter.value

    const matchesStatus =
      statusFilter.value === '' ||
      String(user.enabled) === statusFilter.value

    return (
      matchesSearch &&
      matchesRole &&
      matchesStatus
    )
  })
})

// =====================================
// LOAD USERS
// =====================================

async function loadUsers() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await api.get('/users')

    users.value = Array.isArray(response.data)
      ? response.data
      : []

  } catch (error) {
    console.error('Unable to load users:', error)

    errorMessage.value =
      error.response?.data?.message ||
      `Unable to load users (${error.response?.status || 'Network error'}).`

  } finally {
    loading.value = false
  }
}

// =====================================
// LOAD DYNAMIC ROLES
// =====================================

async function loadRoles() {
  loadingRoles.value = true

  try {
    const response = await api.get('/roles')

    roles.value = Array.isArray(response.data)
      ? response.data
      : []

  } catch (error) {
    console.error('Unable to load roles:', error)

    // Do not hide the users table if role loading fails.
    roles.value = []

  } finally {
    loadingRoles.value = false
  }
}

// =====================================
// NAVIGATION
// =====================================

function createUser() {
  router.push('/users/create')
}

function editUser(id) {
  router.push(`/users/edit/${id}`)
}

// =====================================
// DELETE USER
// =====================================

async function deleteUser(user) {

  const confirmed = window.confirm(
    `Are you sure you want to delete ${user.fullName}?`
  )

  if (!confirmed) return

  try {
    await api.delete(`/users/${user.id}`)

    alert('User deleted successfully.')

    await loadUsers()

  } catch (error) {
    console.error('Unable to delete user:', error)

    alert(
      error.response?.data?.message ||
      'Unable to delete this user.'
    )
  }
}

// =====================================
// RESET FILTERS
// =====================================

function resetFilters() {
  search.value = ''
  roleFilter.value = ''
  statusFilter.value = ''
}

// =====================================
// INITIALIZE
// =====================================

onMounted(async () => {
  await Promise.all([
    loadUsers(),
    loadRoles()
  ])
})
</script>

<style scoped>
.user-page {
  padding: 25px;
  color: #243247;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.page-header h2 {
  margin: 0 0 7px;
  font-size: 25px;
}

.page-header p {
  color: #7b8798;
  margin: 0;
}

button {
  cursor: pointer;
  border: none;
  border-radius: 7px;
  padding: 10px 16px;
  font-weight: 600;
}

.btn-primary {
  background: #2563eb;
  color: white;
}

.btn-secondary {
  background: #e9eef5;
  color: #334155;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 22px;
}

.stat-card {
  background: white;
  border: 1px solid #e5eaf0;
  border-radius: 10px;
  padding: 20px;
}

.stat-card span {
  color: #7b8798;
  font-size: 14px;
}

.stat-card h2 {
  font-size: 27px;
  margin: 12px 0 0;
}

.filter-card,
.table-card {
  background: white;
  border: 1px solid #e5eaf0;
  border-radius: 10px;
  margin-bottom: 20px;
}

.filter-card {
  padding: 18px;
  display: flex;
  gap: 12px;
}

.filter-card input,
.filter-card select {
  padding: 11px;
  border: 1px solid #dce2ea;
  border-radius: 7px;
  outline: none;
  flex: 1;
  min-width: 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px;
  border-bottom: 1px solid #e5eaf0;
}

.table-header h3 {
  margin: 0;
}

.table-header span {
  color: #7b8798;
  font-size: 13px;
}

.table-responsive {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  white-space: nowrap;
}

th,
td {
  padding: 15px 18px;
  border-bottom: 1px solid #edf0f4;
  font-size: 14px;
}

th {
  color: #718096;
  background: #f8fafc;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 35px;
  height: 35px;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
}

.role-badge,
.status-badge,
.permission-count {
  padding: 5px 9px;
  border-radius: 20px;
  font-size: 12px;
}

.admin {
  background: #ede9fe;
  color: #6d28d9;
}

.user {
  background: #e0f2fe;
  color: #0369a1;
}

.active {
  background: #dcfce7;
  color: #15803d;
}

.disabled {
  background: #fee2e2;
  color: #b91c1c;
}

.permission-count {
  background: #f1f5f9;
  color: #475569;
}

.actions {
  display: flex;
  gap: 7px;
}

.edit-btn {
  background: #e0f2fe;
  color: #0369a1;
}

.delete-btn {
  background: #fee2e2;
  color: #b91c1c;
}

.message,
.empty-state {
  text-align: center;
  padding: 35px;
  color: #718096;
}

.error-message {
  background: #fee2e2;
  color: #b91c1c;
  padding: 15px;
  border-radius: 8px;
}

.error-message button {
  margin-left: 10px;
  background: white;
  color: #b91c1c;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .page-header,
  .filter-card {
    flex-direction: column;
    align-items: stretch;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
