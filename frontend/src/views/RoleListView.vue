
<template>
  <div class="role-page">

    <div class="page-header">
      <div>
        <h2>Role Management</h2>
        <p>Create and manage system roles and permissions.</p>
      </div>

      <button class="primary-btn" @click="createRole">
        + Create Role
      </button>
    </div>

    <div class="summary-card">
      <div>
        <span>Total Roles</span>
        <h2>{{ roles.length }}</h2>
      </div>
      <div>
        <span>Active Roles</span>
        <h2>{{ roles.filter(role => role.enabled).length }}</h2>
      </div>
    </div>

    <div class="filter-card">
      <input
        v-model="search"
        placeholder="Search roles..."
      />

      <select v-model="statusFilter">
        <option value="">All Statuses</option>
        <option value="true">Active</option>
        <option value="false">Disabled</option>
      </select>
    </div>

    <div v-if="loading" class="message">
      Loading roles...
    </div>

    <div v-else-if="errorMessage" class="error">
      {{ errorMessage }}
      <button @click="loadRoles">Retry</button>
    </div>

    <div v-else class="table-card">
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Role Name</th>
            <th>Description</th>
            <th>Permissions</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="(role, index) in filteredRoles" :key="role.id">
            <td>{{ index + 1 }}</td>
            <td><strong>{{ role.name }}</strong></td>
            <td>{{ role.description || 'No description' }}</td>
            <td>
              <span class="permission-badge">
                {{ role.permissions?.length || 0 }} permissions
              </span>
            </td>
            <td>
              <span :class="role.enabled ? 'active' : 'disabled'">
                {{ role.enabled ? 'Active' : 'Disabled' }}
              </span>
            </td>
            <td>
              <div class="actions">
                <button class="edit-btn" @click="editRole(role.id)">
                  Edit
                </button>
                <button class="delete-btn" @click="deleteRole(role)">
                  Delete
                </button>
              </div>
            </td>
          </tr>

          <tr v-if="filteredRoles.length === 0">
            <td colspan="6" class="empty">
              No roles found.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/api'

const router = useRouter()

const roles = ref([])
const loading = ref(false)
const errorMessage = ref('')

const search = ref('')
const statusFilter = ref('')

// Count active roles
const activeRoleCount = computed(() => {
  return roles.value.filter(role => role.enabled).length
})

// Search and filter roles
const filteredRoles = computed(() => {
  return roles.value.filter(role => {
    const searchText = search.value.toLowerCase()

    const matchesSearch =
      (role.name || '').toLowerCase().includes(searchText) ||
      (role.description || '').toLowerCase().includes(searchText)

    const matchesStatus =
      statusFilter.value === '' ||
      String(role.enabled) === statusFilter.value

    return matchesSearch && matchesStatus
  })
})

// Load roles
async function loadRoles() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await api.get('/roles')

    roles.value = Array.isArray(response.data)
      ? response.data
      : []

  } catch (error) {
    console.error('Error loading roles:', error)

    errorMessage.value =
      error.response?.data?.message ||
      `Unable to load roles (${error.response?.status || 'Network error'}).`

  } finally {
    loading.value = false
  }
}

// Navigate to create role
function createRole() {
  router.push('/system/roles/create')
}

// Navigate to edit role
function editRole(id) {
  router.push(`/system/roles/edit/${id}`)
}

// Delete role
async function deleteRole(role) {
  if (role.name?.toUpperCase() === 'ADMIN') {
    alert('The ADMIN role cannot be deleted.')
    return
  }

  const confirmed = window.confirm(
    `Are you sure you want to delete the "${role.name}" role?`
  )

  if (!confirmed) return

  try {
    await api.delete(`/roles/${role.id}`)

    alert('Role deleted successfully.')

    await loadRoles()

  } catch (error) {
    console.error('Error deleting role:', error)

    alert(
      error.response?.data?.message ||
      'Unable to delete role. It may be assigned to users.'
    )
  }
}

onMounted(() => {
  loadRoles()
})
</script>


<style scoped>
.role-page {
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
}

.page-header p {
  color: #7b8798;
  margin: 0;
}

button {
  cursor: pointer;
  border: none;
  border-radius: 7px;
  padding: 9px 14px;
  font-weight: 600;
}

.primary-btn {
  background: #2563eb;
  color: white;
}

.summary-card,
.filter-card,
.table-card {
  background: white;
  border: 1px solid #e5eaf0;
  border-radius: 10px;
  margin-bottom: 20px;
}

.summary-card {
  display: flex;
  gap: 70px;
  padding: 20px;
}

.summary-card span {
  color: #7b8798;
}

.summary-card h2 {
  margin: 8px 0 0;
}

.filter-card {
  display: flex;
  gap: 12px;
  padding: 18px;
}

.filter-card input,
.filter-card select {
  padding: 11px;
  border: 1px solid #dce2ea;
  border-radius: 7px;
  flex: 1;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th, td {
  padding: 15px;
  border-bottom: 1px solid #edf0f4;
}

th {
  background: #f8fafc;
  color: #718096;
}

.active, .disabled, .permission-badge {
  padding: 5px 9px;
  border-radius: 20px;
  font-size: 12px;
}

.active {
  color: #15803d;
  background: #dcfce7;
}

.disabled {
  color: #b91c1c;
  background: #fee2e2;
}

.permission-badge {
  color: #475569;
  background: #f1f5f9;
}

.actions {
  display: flex;
  gap: 8px;
}

.edit-btn {
  background: #e0f2fe;
  color: #0369a1;
}

.delete-btn {
  background: #fee2e2;
  color: #b91c1c;
}

.message, .empty {
  text-align: center;
  padding: 30px;
  color: #718096;
}

.error {
  color: #b91c1c;
  padding: 15px;
  background: #fee2e2;
  border-radius: 8px;
}

@media (max-width: 700px) {
  .page-header, .filter-card {
    flex-direction: column;
    align-items: stretch;
  }

  .table-card {
    overflow-x: auto;
  }

  table {
    min-width: 750px;
  }
}
</style>
