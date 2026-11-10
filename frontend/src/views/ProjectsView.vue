
<template>
  <div class="page">

    <div class="page-header">
      <div>
        <h1>Project Management</h1>
        <p>Create and manage organizational projects.</p>
      </div>

      <button class="primary" @click="openForm()">
        + New Project
      </button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <!-- Project Form -->
    <div v-if="showForm" class="panel">
      <h2>{{ editingId ? 'Edit Project' : 'Create Project' }}</h2>

      <form @submit.prevent="saveProject">

        <label>Project Name</label>
        <input v-model="form.projectName" required />

        <label>Project Code</label>
        <input v-model="form.projectCode" required />

        <label>Description</label>
        <textarea v-model="form.description"></textarea>

        <div class="form-grid">
          <div>
            <label>Start Date</label>
            <input v-model="form.startDate" type="date" />
          </div>

          <div>
            <label>End Date</label>
            <input v-model="form.endDate" type="date" />
          </div>
        </div>

        <label>Project Manager</label>
        <select v-model="form.managerId">
          <option value="">Select Project Manager</option>
          <option
            v-for="employee in employees"
            :key="employee.id"
            :value="String(employee.id)"
          >
            {{ employee.fullName }}
          </option>
        </select>

        <label>Budget</label>
        <input v-model="form.budget" type="number" min="0" step="0.01" />

        <label>Status</label>
        <select v-model="form.status">
          <option value="PLANNING">Planning</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="ON_HOLD">On Hold</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>

        <div class="actions">
          <button class="primary" type="submit" :disabled="saving">
            {{ saving ? 'Saving...' : 'Save Project' }}
          </button>
          <button type="button" @click="closeForm">Cancel</button>
        </div>

      </form>
    </div>

    <!-- Search and Filter -->
    <div class="filters">
      <input
        v-model="search"
        placeholder="Search project name..."
        @keyup.enter="loadProjects"
      />

      <select v-model="statusFilter" @change="loadProjects">
        <option value="">All Statuses</option>
        <option value="PLANNING">Planning</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="ON_HOLD">On Hold</option>
        <option value="COMPLETED">Completed</option>
        <option value="CANCELLED">Cancelled</option>
      </select>

      <button @click="loadProjects">Search</button>
    </div>

    <!-- Projects Table -->
    <div class="panel">
      <h2>Projects List</h2>

      <p v-if="loading">Loading projects...</p>

      <table v-else>
        <thead>
          <tr>
            <th>ID</th>
            <th>Project</th>
            <th>Code</th>
            <th>Manager</th>
            <th>Start Date</th>
            <th>End Date</th>
            <th>Budget</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="project in projects" :key="project.id">
            <td>{{ project.id }}</td>
            <td>{{ project.projectName }}</td>
            <td>{{ project.projectCode }}</td>
            <td>{{ project.projectManager?.fullName || 'Not assigned' }}</td>
            <td>{{ project.startDate || '-' }}</td>
            <td>{{ project.endDate || '-' }}</td>
            <td>{{ project.budget ?? '-' }}</td>
            <td>
              <span class="status" :class="project.status">
                {{ project.status }}
              </span>
            </td>
            <td>
              <button @click="openForm(project)">Edit</button>
              <button class="danger"
                @click="deleteProject(project.id)">Delete</button>
            </td>
          </tr>

          <tr v-if="projects.length === 0">
            <td colspan="9">No projects found.</td>
          </tr>
        </tbody>
      </table>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../api/api';

const projects = ref([]);
const employees = ref([]);

const loading = ref(false);
const saving = ref(false);
const error = ref('');

const showForm = ref(false);
const editingId = ref(null);

const search = ref('');
const statusFilter = ref('');

const emptyForm = () => ({
  projectName: '',
  projectCode: '',
  description: '',
  startDate: '',
  endDate: '',
  status: 'PLANNING',
  budget: '',
  managerId: ''
});

const form = ref(emptyForm());

async function loadEmployees() {
  try {
    const response = await api.get('/employees');
    employees.value = response.data;
  } catch (err) {
    error.value = 'Could not load employees for project managers.';
  }
}

async function loadProjects() {
  loading.value = true;
  error.value = '';

  try {
    let response;

    if (statusFilter.value) {
      response = await api.get(
        `/projects/status/${statusFilter.value}`
      );
    } else if (search.value.trim()) {
      response = await api.get('/projects/search', {
        params: { name: search.value.trim() }
      });
    } else {
      response = await api.get('/projects');
    }

    projects.value = response.data;
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not load projects.';
  } finally {
    loading.value = false;
  }
}

function openForm(project = null) {
  error.value = '';
  editingId.value = project?.id ?? null;

  form.value = project
    ? {
        projectName: project.projectName,
        projectCode: project.projectCode,
        description: project.description || '',
        startDate: project.startDate || '',
        endDate: project.endDate || '',
        status: project.status || 'PLANNING',
        budget: project.budget ?? '',
        managerId: project.projectManager?.id
          ? String(project.projectManager.id)
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

async function saveProject() {
  saving.value = true;
  error.value = '';

  const payload = {
    ...form.value,
    budget: form.value.budget === ''
      ? null
      : Number(form.value.budget),
    managerId: form.value.managerId
      ? Number(form.value.managerId)
      : null
  };

  try {
    if (editingId.value) {
      await api.put(`/projects/${editingId.value}`, payload);
    } else {
      await api.post('/projects', payload);
    }

    closeForm();
    await loadProjects();
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not save project.';
  } finally {
    saving.value = false;
  }
}

async function deleteProject(id) {
  if (!confirm('Are you sure you want to delete this project?')) {
    return;
  }

  error.value = '';

  try {
    await api.delete(`/projects/${id}`);
    await loadProjects();
  } catch (err) {
    error.value = err.response?.data?.message ||
      'Could not delete project.';
  }
}

onMounted(async () => {
  await loadEmployees();
  await loadProjects();
});
</script>

<style scoped>
.page {
  padding: 5px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
  margin-bottom: 24px;
}

.page-header p {
  color: #64748b;
}

.panel {
  background: white;
  padding: 22px;
  border-radius: 12px;
  margin: 20px 0;
  box-shadow: 0 2px 10px #0000000d;
  overflow-x: auto;
}

form {
  display: grid;
  gap: 10px;
  max-width: 650px;
}

input, select, textarea {
  width: 100%;
  padding: 11px;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  box-sizing: border-box;
}

textarea {
  min-height: 85px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filters input {
  max-width: 300px;
}

button {
  padding: 9px 13px;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  cursor: pointer;
  margin: 3px;
}

.primary {
  background: #2563eb;
  color: white;
  border: none;
}

.danger {
  color: #dc2626;
}

.actions {
  margin-top: 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th, td {
  padding: 13px;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
}

.status {
  display: inline-block;
  padding: 5px 9px;
  border-radius: 15px;
  font-size: 12px;
  background: #e2e8f0;
}

.status.PLANNING { background: #dbeafe; color: #1d4ed8; }
.status.IN_PROGRESS { background: #dcfce7; color: #15803d; }
.status.ON_HOLD { background: #fef3c7; color: #92400e; }
.status.COMPLETED { background: #ede9fe; color: #6d28d9; }
.status.CANCELLED { background: #fee2e2; color: #b91c1c; }

.error {
  background: #fee2e2;
  color: #991b1b;
  padding: 12px;
  margin-bottom: 15px;
  border-radius: 8px;
}

@media (max-width: 650px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
