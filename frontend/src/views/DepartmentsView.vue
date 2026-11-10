
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Departments</h1>
        <p>Manage your organization's departments.</p>
      </div>
      <button class="primary" @click="openForm()">
        + Add Department
      </button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="showForm" class="panel">
      <h2>{{ editingId ? 'Edit Department' : 'New Department' }}</h2>

      <form @submit.prevent="saveDepartment">
        <label>Department Name</label>
        <input v-model="form.name" required placeholder="e.g. IT" />

        <label>Description</label>
        <textarea v-model="form.description"
          placeholder="Department description"></textarea>

        <label class="check">
          <input type="checkbox" v-model="form.active" />
          Active
        </label>

        <button class="primary" type="submit" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save Department' }}
        </button>
        <button type="button" @click="closeForm">Cancel</button>
      </form>
    </div>

    <div class="panel">
      <h2>Department List</h2>

      <p v-if="loading">Loading departments...</p>

      <table v-else>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Description</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="department in departments" :key="department.id">
            <td>{{ department.id }}</td>
            <td>{{ department.name }}</td>
            <td>{{ department.description }}</td>
            <td>{{ department.active ? 'Active' : 'Inactive' }}</td>
            <td>
              <button @click="openForm(department)">Edit</button>
              <button class="danger"
                @click="deleteDepartment(department.id)">Delete</button>
            </td>
          </tr>

          <tr v-if="departments.length === 0">
            <td colspan="5">No departments found.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../api/api';

const departments = ref([]);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const showForm = ref(false);
const editingId = ref(null);

const emptyForm = () => ({
  name: '',
  description: '',
  active: true
});

const form = ref(emptyForm());

async function loadDepartments() {
  loading.value = true;
  error.value = '';

  try {
    const response = await api.get('/departments');
    departments.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not load departments.';
  } finally {
    loading.value = false;
  }
}

function openForm(department = null) {
  error.value = '';
  editingId.value = department?.id ?? null;

  form.value = department
    ? {
        name: department.name,
        description: department.description || '',
        active: department.active
      }
    : emptyForm();

  showForm.value = true;
}

function closeForm() {
  showForm.value = false;
  editingId.value = null;
  form.value = emptyForm();
}

async function saveDepartment() {
  saving.value = true;
  error.value = '';

  try {
    if (editingId.value) {
      await api.put(`/departments/${editingId.value}`, form.value);
    } else {
      await api.post('/departments', form.value);
    }

    closeForm();
    await loadDepartments();
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not save department.';
  } finally {
    saving.value = false;
  }
}

async function deleteDepartment(id) {
  if (!confirm('Are you sure you want to delete this department?')) {
    return;
  }

  error.value = '';

  try {
    await api.delete(`/departments/${id}`);
    await loadDepartments();
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not delete department. It may have employees assigned.';
  }
}

onMounted(loadDepartments);
</script>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 12px; }
h1, h2 { margin-top: 0; }
.page-header p { color: #64748b; }
.panel { background: white; padding: 22px; border-radius: 12px; margin-bottom: 20px; box-shadow: 0 2px 10px #0000000d; overflow-x: auto; }
form { display: grid; gap: 10px; max-width: 550px; }
input:not([type="checkbox"]), textarea { width: 100%; padding: 11px; border: 1px solid #cbd5e1; border-radius: 7px; box-sizing: border-box; }
textarea { min-height: 80px; }
.check { display: flex; align-items: center; gap: 8px; }
button { padding: 9px 13px; border: 1px solid #cbd5e1; border-radius: 7px; cursor: pointer; margin: 3px; }
.primary { background: #2563eb; color: white; border: none; }
.danger { color: #dc2626; }
table { width: 100%; border-collapse: collapse; text-align: left; }
th, td { padding: 13px; border-bottom: 1px solid #e2e8f0; }
.error { background: #fee2e2; color: #991b1b; padding: 12px; margin-bottom: 15px; border-radius: 8px; }
</style>
