
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Employees</h1>
        <p>Manage employees and their department assignments.</p>
      </div>
      <button class="primary" @click="openForm()">
        + Add Employee
      </button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div class="filters">
      <input
        v-model="search"
        placeholder="Search employee by name..."
      />

      <select v-model="departmentFilter">
        <option value="">All Departments</option>
        <option v-for="d in departments" :key="d.id" :value="String(d.id)">
          {{ d.name }}
        </option>
      </select>

      <button @click="loadEmployees">Search / Filter</button>
    </div>

    <div v-if="showForm" class="panel">
      <h2>{{ editingId ? 'Edit Employee' : 'Add Employee' }}</h2>

      <form @submit.prevent="saveEmployee">
        <label>Full Name</label>
        <input v-model="form.fullName" required />

        <label>Email</label>
        <input v-model="form.email" type="email" required />

        <label>Phone</label>
        <input v-model="form.phone" />

        <label>Job Title</label>
        <input v-model="form.jobTitle" />

        <label>Department</label>
        <select v-model="form.departmentId">
          <option value="">No Department</option>
          <option v-for="d in departments" :key="d.id" :value="String(d.id)">
            {{ d.name }}
          </option>
        </select>

        <label>Status</label>
        <select v-model="form.status">
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>

        <button class="primary" type="submit" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save Employee' }}
        </button>
        <button type="button" @click="closeForm">Cancel</button>
      </form>
    </div>

    <div class="panel">
      <h2>Employee List</h2>

      <p v-if="loading">Loading employees...</p>

      <table v-else>
        <thead>
          <tr>
            <th>ID</th>
            <th>Full Name</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Job Title</th>
            <th>Department</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="employee in employees" :key="employee.id">
            <td>{{ employee.id }}</td>
            <td>{{ employee.fullName }}</td>
            <td>{{ employee.email }}</td>
            <td>{{ employee.phone }}</td>
            <td>{{ employee.jobTitle }}</td>
            <td>{{ employee.department?.name || 'Not assigned' }}</td>
            <td>{{ employee.status }}</td>
            <td>
              <button @click="openForm(employee)">Edit</button>
              <button class="danger"
                @click="deleteEmployee(employee.id)">Delete</button>
            </td>
          </tr>

          <tr v-if="employees.length === 0">
            <td colspan="8">No employees found.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../api/api';

const employees = ref([]);
const departments = ref([]);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const search = ref('');
const departmentFilter = ref('');
const showForm = ref(false);
const editingId = ref(null);

const emptyForm = () => ({
  fullName: '',
  email: '',
  phone: '',
  jobTitle: '',
  status: 'ACTIVE',
  departmentId: ''
});

const form = ref(emptyForm());

async function loadDepartments() {
  try {
    const response = await api.get('/departments');
    departments.value = response.data;
  } catch (err) {
    error.value = 'Could not load departments.';
  }
}

async function loadEmployees() {
  loading.value = true;
  error.value = '';

  try {
    let response;

    if (departmentFilter.value) {
      response = await api.get(
        `/employees/department/${departmentFilter.value}`
      );
    } else if (search.value.trim()) {
      response = await api.get('/employees/search', {
        params: { name: search.value.trim() }
      });
    } else {
      response = await api.get('/employees');
    }

    employees.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not load employees.';
  } finally {
    loading.value = false;
  }
}

function openForm(employee = null) {
  error.value = '';
  editingId.value = employee?.id ?? null;

  form.value = employee
    ? {
        fullName: employee.fullName,
        email: employee.email,
        phone: employee.phone || '',
        jobTitle: employee.jobTitle || '',
        status: employee.status || 'ACTIVE',
        departmentId: employee.department?.id
          ? String(employee.department.id)
          : ''
      }
    : emptyForm();

  showForm.value = true;
}

function closeForm() {
  showForm.value = false;
  editingId.value = null;
  form.value = emptyForm();
}

async function saveEmployee() {
  saving.value = true;
  error.value = '';

  const payload = {
    ...form.value,
    departmentId: form.value.departmentId
      ? Number(form.value.departmentId)
      : null
  };

  try {
    if (editingId.value) {
      await api.put(`/employees/${editingId.value}`, payload);
    } else {
      await api.post('/employees', payload);
    }

    closeForm();
    await loadEmployees();
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not save employee.';
  } finally {
    saving.value = false;
  }
}

async function deleteEmployee(id) {
  if (!confirm('Are you sure you want to delete this employee?')) {
    return;
  }

  error.value = '';

  try {
    await api.delete(`/employees/${id}`);
    await loadEmployees();
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not delete employee.';
  }
}

onMounted(async () => {
  await loadDepartments();
  await loadEmployees();
});
</script>

<style scoped>
.page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 12px; }
h1, h2 { margin-top: 0; }
.page-header p { color: #64748b; }
.panel { background: white; padding: 22px; border-radius: 12px; margin-top: 20px; box-shadow: 0 2px 10px #0000000d; overflow-x: auto; }
.filters { display: flex; gap: 10px; flex-wrap: wrap; }
input, select { padding: 11px; border: 1px solid #cbd5e1; border-radius: 7px; }
form { display: grid; gap: 10px; max-width: 550px; }
form input, form select { width: 100%; box-sizing: border-box; }
button { padding: 9px 13px; border: 1px solid #cbd5e1; border-radius: 7px; cursor: pointer; margin: 3px; }
.primary { background: #2563eb; color: white; border: none; }
.danger { color: #dc2626; }
table { width: 100%; border-collapse: collapse; text-align: left; }
th, td { padding: 13px; border-bottom: 1px solid #e2e8f0; }
.error { background: #fee2e2; color: #991b1b; padding: 12px; margin-bottom: 15px; border-radius: 8px; }
</style>
