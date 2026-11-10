
<script setup>
import { ref, onMounted, computed } from 'vue';
import api from '../api/api';

// Data
const projects = ref([]);
const employees = ref([]);
const teamMembers = ref([]);

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

// Filters
const selectedProject = ref('');

// Form
const showForm = ref(false);
const editingId = ref(null);

const form = ref({
    projectId: '',
    employeeId: '',
    teamRole: '',
    joinedDate: new Date().toISOString().slice(0, 10)
});

// Available team roles
const teamRoles = [
    'Project Manager',
    'Team Leader',
    'Backend Developer',
    'Frontend Developer',
    'Full Stack Developer',
    'UI/UX Designer',
    'Database Administrator',
    'Tester',
    'Business Analyst',
    'Other'
];

// Computed
const filteredMembers = computed(() => {
    if (!selectedProject.value) {
        return teamMembers.value;
    }

    return teamMembers.value.filter(
        member => member.projectId === Number(selectedProject.value)
    );
});

// Load initial data
onMounted(async () => {
    await loadProjects();
    await loadEmployees();
    await loadTeamMembers();
});

// Load projects
async function loadProjects() {
    try {
        const response = await api.get('/projects');
        projects.value = response.data;
    } catch (error) {
        showError('Failed to load projects.');
    }
}

// Load employees
async function loadEmployees() {
    try {
        const response = await api.get('/employees');
        employees.value = response.data;
    } catch (error) {
        showError('Failed to load employees.');
    }
}

// Load team members
async function loadTeamMembers() {
    loading.value = true;

    try {
        const response = await api.get('/project-teams');
        teamMembers.value = response.data;
    } catch (error) {
        showError('Failed to load project team members.');
    } finally {
        loading.value = false;
    }
}

// Reset form
function resetForm() {
    editingId.value = null;

    form.value = {
        projectId: selectedProject.value || '',
        employeeId: '',
        teamRole: '',
        joinedDate: new Date().toISOString().slice(0, 10)
    };

    showForm.value = true;
}

// Edit member
function editMember(member) {
    editingId.value = member.id;

    form.value = {
        projectId: member.projectId,
        employeeId: member.employeeId,
        teamRole: member.teamRole,
        joinedDate: member.joinedDate || ''
    };

    showForm.value = true;
}

// Save member
async function saveMember() {
    errorMessage.value = '';
    successMessage.value = '';

    if (
        !form.value.projectId ||
        !form.value.employeeId ||
        !form.value.teamRole
    ) {
        showError('Please complete all required fields.');
        return;
    }

    const payload = {
        projectId: Number(form.value.projectId),
        employeeId: Number(form.value.employeeId),
        teamRole: form.value.teamRole,
        joinedDate: form.value.joinedDate || null
    };

    saving.value = true;

    try {
        if (editingId.value) {
            await api.put(
                `/project-teams/${editingId.value}`,
                payload
            );

            successMessage.value = 'Team member updated successfully.';
        } else {
            await api.post('/project-teams', payload);

            successMessage.value = 'Employee added to project successfully.';
        }

        showForm.value = false;
        await loadTeamMembers();

        if (!selectedProject.value) {
            selectedProject.value = String(payload.projectId);
        }

    } catch (error) {
        showError(
            error.response?.data ||
            'Unable to save project team member.'
        );
    } finally {
        saving.value = false;
    }
}

// Delete member
async function deleteMember(member) {
    const confirmed = window.confirm(
        `Remove ${member.employeeName} from ${member.projectName}?`
    );

    if (!confirmed) return;

    try {
        await api.delete(`/project-teams/${member.id}`);

        successMessage.value = 'Team member removed successfully.';

        await loadTeamMembers();

    } catch (error) {
        showError('Failed to remove team member.');
    }
}

// Error helper
function showError(message) {
    errorMessage.value =
        typeof message === 'string'
            ? message
            : 'An unexpected error occurred.';
}
</script>

<template>
    <div class="team-page">

        <!-- Header -->
        <div class="page-header">
            <div>
                <h1>Project Teams</h1>
                <p>Manage employees assigned to your projects.</p>
            </div>

            <button class="primary-btn" @click="resetForm">
                + Add Team Member
            </button>
        </div>

        <!-- Messages -->
        <div v-if="successMessage" class="alert success">
            {{ successMessage }}
            <button @click="successMessage = ''">×</button>
        </div>

        <div v-if="errorMessage" class="alert error">
            {{ errorMessage }}
            <button @click="errorMessage = ''">×</button>
        </div>

        <!-- Statistics -->
        <div class="stats-grid">
            <div class="stat-card">
                <span>Total Assignments</span>
                <strong>{{ teamMembers.length }}</strong>
            </div>

            <div class="stat-card">
                <span>Total Projects</span>
                <strong>{{ projects.length }}</strong>
            </div>

            <div class="stat-card">
                <span>Total Employees</span>
                <strong>{{ employees.length }}</strong>
            </div>
        </div>

        <!-- Form -->
        <div v-if="showForm" class="form-card">

            <div class="form-header">
                <h2>
                    {{ editingId ? 'Edit Team Member' : 'Add Team Member' }}
                </h2>

                <button class="close-btn" @click="showForm = false">
                    ×
                </button>
            </div>

            <form @submit.prevent="saveMember">

                <div class="form-grid">

                    <div class="form-group">
                        <label>Project *</label>

                        <select v-model="form.projectId" required>
                            <option value="" disabled>Select Project</option>

                            <option
                                v-for="project in projects"
                                :key="project.id"
                                :value="project.id"
                            >
                                {{ project.projectName }}
                            </option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Employee *</label>

                        <select v-model="form.employeeId" required>
                            <option value="" disabled>Select Employee</option>

                            <option
                                v-for="employee in employees"
                                :key="employee.id"
                                :value="employee.id"
                            >
                                {{ employee.fullName }}
                            </option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Team Role *</label>

                        <select v-model="form.teamRole" required>
                            <option value="" disabled>Select Role</option>

                            <option
                                v-for="role in teamRoles"
                                :key="role"
                                :value="role"
                            >
                                {{ role }}
                            </option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Joined Date</label>

                        <input
                            type="date"
                            v-model="form.joinedDate"
                        />
                    </div>

                </div>

                <div class="form-actions">
                    <button
                        type="button"
                        class="secondary-btn"
                        @click="showForm = false"
                    >
                        Cancel
                    </button>

                    <button
                        type="submit"
                        class="primary-btn"
                        :disabled="saving"
                    >
                        {{ saving ? 'Saving...' : 'Save Member' }}
                    </button>
                </div>

            </form>
        </div>

        <!-- Filter -->
        <div class="filter-card">
            <div class="filter-group">
                <label>Filter by Project</label>

                <select v-model="selectedProject">
                    <option value="">All Projects</option>

                    <option
                        v-for="project in projects"
                        :key="project.id"
                        :value="String(project.id)"
                    >
                        {{ project.projectName }}
                    </option>
                </select>
            </div>

            <button class="secondary-btn" @click="loadTeamMembers">
                Refresh
            </button>
        </div>

        <!-- Team Table -->
        <div class="table-card">

            <div class="table-header">
                <h2>Project Team Members</h2>
                <span>{{ filteredMembers.length }} Members</span>
            </div>

            <div v-if="loading" class="empty-state">
                Loading team members...
            </div>

            <div
                v-else-if="filteredMembers.length === 0"
                class="empty-state"
            >
                No team members found.
            </div>

            <div v-else class="table-responsive">

                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Employee</th>
                            <th>Email</th>
                            <th>Project</th>
                            <th>Team Role</th>
                            <th>Joined Date</th>
                            <th>Actions</th>
                        </tr>
                    </thead>

                    <tbody>
                        <tr
                            v-for="(member, index) in filteredMembers"
                            :key="member.id"
                        >
                            <td>{{ index + 1 }}</td>

                            <td class="employee-name">
                                {{ member.employeeName }}
                            </td>

                            <td>{{ member.employeeEmail }}</td>

                            <td>{{ member.projectName }}</td>

                            <td>
                                <span class="role-badge">
                                    {{ member.teamRole }}
                                </span>
                            </td>

                            <td>{{ member.joinedDate || '-' }}</td>

                            <td>
                                <div class="actions">
                                    <button
                                        class="edit-btn"
                                        @click="editMember(member)"
                                    >
                                        Edit
                                    </button>

                                    <button
                                        class="delete-btn"
                                        @click="deleteMember(member)"
                                    >
                                        Remove
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>

            </div>
        </div>

    </div>
</template>

<style scoped>
.team-page {
    padding: 25px;
    color: #1f2937;
}

.page-header,
.form-header,
.table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 15px;
}

.page-header {
    margin-bottom: 25px;
}

.page-header h1 {
    margin: 0 0 7px;
    font-size: 26px;
}

.page-header p {
    margin: 0;
    color: #6b7280;
}

.primary-btn,
.secondary-btn,
.edit-btn,
.delete-btn {
    border: none;
    border-radius: 6px;
    padding: 10px 16px;
    cursor: pointer;
    font-size: 14px;
}

.primary-btn {
    background: #2563eb;
    color: white;
}

.primary-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.secondary-btn {
    background: #e5e7eb;
    color: #374151;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 18px;
    margin-bottom: 22px;
}

.stat-card,
.form-card,
.filter-card,
.table-card {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 9px;
    padding: 20px;
}

.stat-card span {
    display: block;
    color: #6b7280;
    margin-bottom: 10px;
}

.stat-card strong {
    font-size: 28px;
}

.form-card {
    margin-bottom: 22px;
}

.form-header {
    margin-bottom: 20px;
}

.form-header h2,
.table-header h2 {
    margin: 0;
    font-size: 18px;
}

.close-btn {
    border: none;
    background: transparent;
    font-size: 25px;
    cursor: pointer;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
}

.form-group,
.filter-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.form-group label,
.filter-group label {
    font-weight: 600;
    font-size: 14px;
}

.form-group input,
.form-group select,
.filter-group select {
    padding: 11px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    outline: none;
    width: 100%;
    box-sizing: border-box;
    background: white;
}

.form-group input:focus,
.form-group select:focus,
.filter-group select:focus {
    border-color: #2563eb;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 22px;
}

.filter-card {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 15px;
    margin-bottom: 22px;
}

.filter-group {
    width: 300px;
}

.table-header {
    margin-bottom: 18px;
}

.table-header span {
    color: #6b7280;
    font-size: 14px;
}

.table-responsive {
    overflow-x: auto;
}

table {
    width: 100%;
    border-collapse: collapse;
    white-space: nowrap;
}

th,
td {
    text-align: left;
    padding: 13px;
    border-bottom: 1px solid #e5e7eb;
    font-size: 14px;
}

th {
    background: #f9fafb;
    color: #4b5563;
    font-weight: 600;
}

.employee-name {
    font-weight: 600;
}

.role-badge {
    display: inline-block;
    padding: 5px 10px;
    background: #dbeafe;
    color: #1d4ed8;
    border-radius: 20px;
    font-size: 12px;
}

.actions {
    display: flex;
    gap: 7px;
}

.edit-btn {
    background: #e0e7ff;
    color: #3730a3;
}

.delete-btn {
    background: #fee2e2;
    color: #b91c1c;
}

.empty-state {
    text-align: center;
    padding: 35px;
    color: #6b7280;
}

.alert {
    padding: 12px 16px;
    border-radius: 6px;
    margin-bottom: 18px;
    display: flex;
    justify-content: space-between;
}

.alert button {
    border: none;
    background: transparent;
    cursor: pointer;
    font-size: 18px;
}

.success {
    background: #dcfce7;
    color: #166534;
}

.error {
    background: #fee2e2;
    color: #991b1b;
}

@media (max-width: 768px) {
    .stats-grid,
    .form-grid {
        grid-template-columns: 1fr;
    }

    .page-header,
    .filter-card {
        align-items: stretch;
        flex-direction: column;
    }

    .filter-group {
        width: 100%;
    }
}
</style>
