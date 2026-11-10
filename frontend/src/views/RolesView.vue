<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Role Management</h1>
        <p>Manage system roles and their permissions.</p>
      </div>

      <router-link to="/roles/create" class="btn primary">
        + Create Role
      </router-link>
    </div>

    <p v-if="loading">Loading roles...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <div v-else class="table-card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Role Name</th>
            <th>Description</th>
            <th>Permissions</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="role in roles" :key="role.id">
            <td>{{ role.id }}</td>
            <td>{{ role.name }}</td>
            <td>{{ role.description || '-' }}</td>
            <td>{{ role.permissionCodes?.length ?? role.permissionIds?.length ?? 0 }}</td>
            <td>
              <span :class="role.enabled ? 'status active' : 'status inactive'">
                {{ role.enabled ? 'Enabled' : 'Disabled' }}
              </span>
            </td>
            <td class="actions">
              <router-link :to="`/roles/edit/${role.id}`" class="btn small">
                Edit
              </router-link>

              <button
                class="btn small danger"
                @click="deleteRole(role)"
              >
                Delete
              </button>
            </td>
          </tr>

          <tr v-if="roles.length === 0">
            <td colspan="6" class="empty">No roles found.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/api'

const roles = ref([])
const loading = ref(false)
const error = ref('')

async function loadRoles() {
  loading.value = true
  error.value = ''

  try {
    const response = await api.get('/roles')
    roles.value = Array.isArray(response.data)
      ? response.data
      : response.data.content ?? []
  } catch (err) {
    error.value = err.response?.data?.message || 'Unable to load roles.'
  } finally {
    loading.value = false
  }
}

async function deleteRole(role) {
  if (!confirm(`Are you sure you want to delete the role "${role.name}"?`)) {
    return
  }

  try {
    await api.delete(`/roles/${role.id}`)
    await loadRoles()
  } catch (err) {
    alert(err.response?.data?.message || 'Unable to delete this role.')
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }
h1 { margin: 0 0 6px; }
p { color: #64748b; }
.table-card { background: white; border-radius: 10px; padding: 16px; overflow-x: auto; box-shadow: 0 2px 10px #0000000a; }
table { width: 100%; border-collapse: collapse; min-width: 700px; }
th, td { padding: 13px; border-bottom: 1px solid #e5e7eb; text-align: left; }
th { background: #f8fafc; }
.actions { display: flex; gap: 8px; }
.btn { display: inline-block; border: 0; border-radius: 6px; padding: 9px 13px; text-decoration: none; cursor: pointer; background: #e2e8f0; color: #1e293b; }
.primary { background: #2563eb; color: white; }
.small { padding: 6px 10px; font-size: 13px; }
.danger { background: #fee2e2; color: #b91c1c; }
.status { padding: 5px 9px; border-radius: 20px; font-size: 12px; }
.active { background: #dcfce7; color: #166534; }
.inactive { background: #fee2e2; color: #991b1b; }
.error { color: #dc2626; }
.empty { text-align: center; color: #64748b; }
</style>