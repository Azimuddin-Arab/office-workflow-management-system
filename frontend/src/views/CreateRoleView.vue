<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Create Role</h1>
        <p>Create a role and select the permissions it should have.</p>
      </div>

      <router-link to="/roles" class="btn">Back to Roles</router-link>
    </div>

    <form class="form-card" @submit.prevent="createRole">
      <div class="field">
        <label>Role Name</label>
        <input
          v-model.trim="form.name"
          type="text"
          placeholder="Example: PROJECT_MANAGER"
          required
        />
      </div>

      <div class="field">
        <label>Description</label>
        <textarea
          v-model.trim="form.description"
          placeholder="Describe this role"
          rows="3"
        ></textarea>
      </div>

      <label class="enabled">
        <input v-model="form.enabled" type="checkbox" />
        Role Enabled
      </label>

      <div class="permissions-section">
        <div class="permissions-header">
          <h2>Assign Permissions</h2>
          <span>{{ form.permissionIds.length }} selected</span>
        </div>

        <p v-if="loadingPermissions">Loading permissions...</p>
        <p v-else-if="permissionError" class="error">{{ permissionError }}</p>

        <div
          v-for="(items, moduleName) in groupedPermissions"
          :key="moduleName"
          class="module-card"
        >
          <div class="module-header">
            <h3>{{ moduleName }}</h3>
            <button type="button" class="text-button" @click="toggleModule(items)">
              {{ isModuleSelected(items) ? 'Unselect all' : 'Select all' }}
            </button>
          </div>

          <div class="permission-grid">
            <label
              v-for="permission in items"
              :key="permission.id"
              class="permission-item"
            >
              <input
                v-model="form.permissionIds"
                type="checkbox"
                :value="permission.id"
              />
              <span>
                <strong>{{ permission.name }}</strong>
                <small>{{ permission.code }}</small>
              </span>
            </label>
          </div>
        </div>
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <div class="form-actions">
        <router-link to="/roles" class="btn">Cancel</router-link>
        <button type="submit" class="btn primary" :disabled="saving">
          {{ saving ? 'Saving...' : 'Create Role' }}
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

const permissions = ref([])
const loadingPermissions = ref(false)
const permissionError = ref('')
const saving = ref(false)
const error = ref('')

const form = ref({
  name: '',
  description: '',
  enabled: true,
  permissionIds: []
})

const groupedPermissions = computed(() => {
  return permissions.value.reduce((groups, permission) => {
    const moduleName = permission.module || 'Other'

    if (!groups[moduleName]) {
      groups[moduleName] = []
    }

    groups[moduleName].push(permission)
    return groups
  }, {})
})

async function loadPermissions() {
  loadingPermissions.value = true
  permissionError.value = ''

  try {
    const response = await api.get('/permissions')
    permissions.value = Array.isArray(response.data)
      ? response.data
      : response.data.content ?? []
  } catch (err) {
    permissionError.value =
      err.response?.data?.message || 'Unable to load permissions.'
  } finally {
    loadingPermissions.value = false
  }
}

function isModuleSelected(items) {
  return items.length > 0 &&
    items.every(item => form.value.permissionIds.includes(item.id))
}

function toggleModule(items) {
  if (isModuleSelected(items)) {
    const ids = items.map(item => item.id)
    form.value.permissionIds = form.value.permissionIds.filter(
      id => !ids.includes(id)
    )
  } else {
    const ids = new Set(form.value.permissionIds)

    items.forEach(item => ids.add(item.id))
    form.value.permissionIds = [...ids]
  }
}

async function createRole() {
  saving.value = true
  error.value = ''

  try {
    await api.post('/roles', {
      name: form.value.name,
      description: form.value.description,
      enabled: form.value.enabled,
      permissionIds: form.value.permissionIds
    })

    alert('Role created successfully.')
    router.push('/roles')
  } catch (err) {
    error.value = err.response?.data?.message || 'Unable to create role.'
  } finally {
    saving.value = false
  }
}

onMounted(loadPermissions)
</script>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }
h1, h2, h3 { margin-top: 0; }
.page-header p { color: #64748b; }
.form-card { background: white; padding: 24px; border-radius: 10px; box-shadow: 0 2px 10px #0000000a; }
.field { margin-bottom: 18px; }
.field label { display: block; font-weight: 600; margin-bottom: 7px; }
.field input, .field textarea { width: 100%; box-sizing: border-box; padding: 11px; border: 1px solid #cbd5e1; border-radius: 6px; font: inherit; }
.enabled { display: flex; align-items: center; gap: 8px; margin: 15px 0 25px; }
.permissions-header, .module-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.permissions-header span { color: #2563eb; }
.module-card { border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 14px 0; }
.module-header h3 { margin-bottom: 12px; }
.permission-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.permission-item { display: flex; align-items: flex-start; gap: 9px; padding: 10px; background: #f8fafc; border-radius: 6px; }
.permission-item span { display: flex; flex-direction: column; gap: 4px; }
.permission-item small { color: #64748b; }
.text-button { border: 0; background: none; color: #2563eb; cursor: pointer; }
.btn { display: inline-block; padding: 9px 14px; border: 0; border-radius: 6px; background: #e2e8f0; color: #1e293b; text-decoration: none; cursor: pointer; }
.primary { background: #2563eb; color: white; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
.error { color: #dc2626; }
</style>