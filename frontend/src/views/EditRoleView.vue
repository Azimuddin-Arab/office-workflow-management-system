<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Edit Role</h1>
        <p>Update role information and assigned permissions.</p>
      </div>

      <router-link to="/roles" class="btn">Back to Roles</router-link>
    </div>

    <p v-if="loading">Loading role...</p>

    <form v-else class="form-card" @submit.prevent="updateRole">
      <div class="field">
        <label>Role Name</label>
        <input v-model.trim="form.name" required />
      </div>

      <div class="field">
        <label>Description</label>
        <textarea v-model.trim="form.description" rows="3"></textarea>
      </div>

      <label class="enabled">
        <input v-model="form.enabled" type="checkbox" />
        Role Enabled
      </label>

      <div class="permissions-header">
        <h2>Permissions</h2>
        <span>{{ form.permissionIds.length }} selected</span>
      </div>

      <p v-if="permissionError" class="error">{{ permissionError }}</p>

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

      <p v-if="error" class="error">{{ error }}</p>

      <div class="form-actions">
        <router-link to="/roles" class="btn">Cancel</router-link>
        <button class="btn primary" type="submit" :disabled="saving">
          {{ saving ? 'Updating...' : 'Update Role' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api/api'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const permissionError = ref('')
const permissions = ref([])

const form = ref({
  name: '',
  description: '',
  enabled: true,
  permissionIds: []
})

const groupedPermissions = computed(() => {
  return permissions.value.reduce((groups, permission) => {
    const moduleName = permission.module || 'Other'
    if (!groups[moduleName]) groups[moduleName] = []
    groups[moduleName].push(permission)
    return groups
  }, {})
})

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

async function loadData() {
  loading.value = true
  error.value = ''
  permissionError.value = ''

  try {
    const [roleResponse, permissionResponse] = await Promise.all([
      api.get(`/roles/${route.params.id}`),
      api.get('/permissions')
    ])

    const role = roleResponse.data
    permissions.value = Array.isArray(permissionResponse.data)
      ? permissionResponse.data
      : permissionResponse.data.content ?? []

    form.value = {
      name: role.name ?? '',
      description: role.description ?? '',
      enabled: role.enabled ?? true,
      permissionIds: role.permissionIds ??
        permissions.value
          .filter(permission =>
            (role.permissionCodes ?? []).includes(permission.code)
          )
          .map(permission => permission.id)
    }
  } catch (err) {
    error.value = err.response?.data?.message || 'Unable to load role details.'
    permissionError.value = 'Check that the permissions endpoint is accessible.'
  } finally {
    loading.value = false
  }
}

async function updateRole() {
  saving.value = true
  error.value = ''

  try {
    await api.put(`/roles/${route.params.id}`, {
      name: form.value.name,
      description: form.value.description,
      enabled: form.value.enabled,
      permissionIds: form.value.permissionIds
    })

    alert('Role updated successfully.')
    router.push('/roles')
  } catch (err) {
    error.value = err.response?.data?.message || 'Unable to update role.'
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }
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