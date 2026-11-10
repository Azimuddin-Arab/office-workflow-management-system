
<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import api from '../api/api';

// Main data
const tasks = ref([]);
const projects = ref([]);
const teamMembers = ref([]);
const loading = ref(false);
const saving = ref(false);

// Messages
const errorMessage = ref('');
const successMessage = ref('');

// Search and filters
const searchText = ref('');
const selectedProject = ref('');
const selectedStatus = ref('');
const selectedPriority = ref('');

// Form visibility and editing
const showForm = ref(false);
const editingId = ref(null);

// Get today's date in local time
function getToday() {
    const date = new Date();
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

// Task form
const form = ref({
    title: '',
    description: '',
    projectId: '',
    assignedEmployeeId: '',
    priority: 'MEDIUM',
    status: 'TODO',
    startDate: getToday(),
    dueDate: ''
});

// Dropdown options
const priorities = [
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
];

const statuses = [
    'TODO',
    'IN_PROGRESS',
    'REVIEW',
    'COMPLETED'
];

// Filter tasks
const filteredTasks = computed(() => {
    return tasks.value.filter(task => {

        const matchesSearch =
            !searchText.value ||
            task.title.toLowerCase().includes(
                searchText.value.toLowerCase()
            );

        const matchesProject =
            !selectedProject.value ||
            task.projectId === Number(selectedProject.value);

        const matchesStatus =
            !selectedStatus.value ||
            task.status === selectedStatus.value;

        const matchesPriority =
            !selectedPriority.value ||
            task.priority === selectedPriority.value;

        return matchesSearch &&
            matchesProject &&
            matchesStatus &&
            matchesPriority;
    });
});

// Dashboard statistics
const totalTasks = computed(() => tasks.value.length);

const completedTasks = computed(() =>
    tasks.value.filter(task => task.status === 'COMPLETED').length
);

const pendingTasks = computed(() =>
    tasks.value.filter(task =>
        task.status === 'TODO' ||
        task.status === 'IN_PROGRESS' ||
        task.status === 'REVIEW'
    ).length
);

const overdueTasks = computed(() =>
    tasks.value.filter(task =>
        task.status !== 'COMPLETED' &&
        task.dueDate &&
        task.dueDate < getToday()
    ).length
);

// Load initial data
onMounted(async () => {
    await Promise.all([
        loadProjects(),
        loadTasks()
    ]);
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

// Load tasks
async function loadTasks() {
    loading.value = true;

    try {
        const response = await api.get('/tasks');
        tasks.value = response.data;
    } catch (error) {
        showError('Failed to load tasks.');
    } finally {
        loading.value = false;
    }
}

// Load members belonging to a project
async function loadProjectMembers(projectId) {
    teamMembers.value = [];

    if (!projectId) return;

    try {
        const response = await api.get(
            `/project-teams/project/${projectId}`
        );

        teamMembers.value = response.data;
    } catch (error) {
        showError('Failed to load project team members.');
    }
}

// Watch project selection in the form
watch(
    () => form.value.projectId,
    async (newProjectId, oldProjectId) => {
        if (String(newProjectId) !== String(oldProjectId)) {
            form.value.assignedEmployeeId = '';
        }

        await loadProjectMembers(newProjectId);
    }
);

// Reset form
function resetForm() {
    editingId.value = null;

    form.value = {
        title: '',
        description: '',
        projectId: selectedProject.value || '',
        assignedEmployeeId: '',
        priority: 'MEDIUM',
        status: 'TODO',
        startDate: getToday(),
        dueDate: ''
    };

    showForm.value = true;

    if (form.value.projectId) {
        loadProjectMembers(form.value.projectId);
    } else {
        teamMembers.value = [];
    }
}

// Edit task
async function editTask(task) {
    editingId.value = task.id;

    form.value = {
        title: task.title,
        description: task.description || '',
        projectId: String(task.projectId),
        assignedEmployeeId: task.assignedEmployeeId
            ? String(task.assignedEmployeeId)
            : '',
        priority: task.priority,
        status: task.status,
        startDate: task.startDate || '',
        dueDate: task.dueDate || ''
    };

    showForm.value = true;

    await loadProjectMembers(task.projectId);
}

// Save task
async function saveTask() {
    errorMessage.value = '';
    successMessage.value = '';

    if (
        !form.value.title.trim() ||
        !form.value.projectId
    ) {
        showError('Task title and project are required.');
        return;
    }

    if (
        form.value.startDate &&
        form.value.dueDate &&
        form.value.dueDate < form.value.startDate
    ) {
        showError('Due date cannot be before start date.');
        return;
    }

    const payload = {
        title: form.value.title.trim(),
        description: form.value.description,
        projectId: Number(form.value.projectId),
        assignedEmployeeId: form.value.assignedEmployeeId
            ? Number(form.value.assignedEmployeeId)
            : null,
        priority: form.value.priority,
        status: form.value.status,
        startDate: form.value.startDate || null,
        dueDate: form.value.dueDate || null
    };

    saving.value = true;

    try {
        if (editingId.value) {
            await api.put(
                `/tasks/${editingId.value}`,
                payload
            );

            successMessage.value = 'Task updated successfully.';
        } else {
            await api.post('/tasks', payload);

            successMessage.value = 'Task created successfully.';
        }

        showForm.value = false;
        editingId.value = null;

        await loadTasks();

    } catch (error) {
        const message = error.response?.data;

        showError(
            typeof message === 'string'
                ? message
                : 'Unable to save task. Please check the information.'
        );
    } finally {
        saving.value = false;
    }
}

// Delete task
async function deleteTask(task) {
    const confirmed = window.confirm(
        `Are you sure you want to delete "${task.title}"?`
    );

    if (!confirmed) return;

    errorMessage.value = '';
    successMessage.value = '';

    try {
        await api.delete(`/tasks/${task.id}`);

        successMessage.value = 'Task deleted successfully.';

        await loadTasks();

    } catch (error) {
        showError('Failed to delete task.');
    }
}

// Clear filters
function clearFilters() {
    searchText.value = '';
    selectedProject.value = '';
    selectedStatus.value = '';
    selectedPriority.value = '';
}

// Show error message
function showError(message) {
    errorMessage.value =
        typeof message === 'string'
            ? message
            : 'An unexpected error occurred.';
}

// Format status
function formatStatus(status) {
    return status.replaceAll('_', ' ');
}

// Priority CSS class
function priorityClass(priority) {
    return priority.toLowerCase();
}

// Status CSS class
function statusClass(status) {
    return status.toLowerCase();
}

// Check overdue
function isOverdue(task) {
    return task.status !== 'COMPLETED' &&
        task.dueDate &&
        task.dueDate < getToday();
}
</script>

<template>
    <div class="tasks-page">

        <!-- Page Header -->
        <div class="page-header">
            <div>
                <h1>Task Management</h1>
                <p>Create, assign and track project tasks.</p>
            </div>

            <button class="primary-btn" @click="resetForm">
                + Create Task
            </button>
        </div>

        <!-- Messages -->
        <div v-if="successMessage" class="alert success">
            <span>{{ successMessage }}</span>
            <button @click="successMessage = ''">×</button>
        </div>

        <div v-if="errorMessage" class="alert error">
            <span>{{ errorMessage }}</span>
            <button @click="errorMessage = ''">×</button>
        </div>

        <!-- Statistics -->
        <div class="stats-grid">

            <div class="stat-card">
                <span>Total Tasks</span>
                <strong>{{ totalTasks }}</strong>
            </div>

            <div class="stat-card">
                <span>Completed</span>
                <strong>{{ completedTasks }}</strong>
            </div>

            <div class="stat-card">
                <span>Pending</span>
                <strong>{{ pendingTasks }}</strong>
            </div>

            <div class="stat-card">
                <span>Overdue</span>
                <strong>{{ overdueTasks }}</strong>
            </div>

        </div>

        <!-- Task Form -->
        <div v-if="showForm" class="form-card">

            <div class="form-header">
                <h2>
                    {{ editingId ? 'Edit Task' : 'Create New Task' }}
                </h2>

                <button
                    class="close-btn"
                    type="button"
                    @click="showForm = false"
                >
                    ×
                </button>
            </div>

            <form @submit.prevent="saveTask">

                <div class="form-grid">

                    <!-- Title -->
                    <div class="form-group full-width">
                        <label>Task Title *</label>

                        <input
                            v-model="form.title"
                            type="text"
                            placeholder="Enter task title"
                            maxlength="200"
                            required
                        />
                    </div>

                    <!-- Project -->
                    <div class="form-group">
                        <label>Project *</label>

                        <select
                            v-model="form.projectId"
                            required
                        >
                            <option value="" disabled>
                                Select Project
                            </option>

                            <option
                                v-for="project in projects"
                                :key="project.id"
                                :value="String(project.id)"
                            >
                                {{ project.projectName }}
                            </option>
                        </select>
                    </div>

                    <!-- Employee -->
                    <div class="form-group">
                        <label>Assign Employee</label>

                        <select
                            v-model="form.assignedEmployeeId"
                            :disabled="!form.projectId"
                        >
                            <option value="">
                                Unassigned
                            </option>

                            <option
                                v-for="member in teamMembers"
                                :key="member.employeeId"
                                :value="String(member.employeeId)"
                            >
                                {{ member.employeeName }}
                                ({{ member.teamRole }})
                            </option>
                        </select>

                        <small>
                            Only members of the selected project are shown.
                        </small>
                    </div>

                    <!-- Priority -->
                    <div class="form-group">
                        <label>Priority *</label>

                        <select v-model="form.priority" required>
                            <option
                                v-for="priority in priorities"
                                :key="priority"
                                :value="priority"
                            >
                                {{ priority }}
                            </option>
                        </select>
                    </div>

                    <!-- Status -->
                    <div class="form-group">
                        <label>Status *</label>

                        <select v-model="form.status" required>
                            <option
                                v-for="status in statuses"
                                :key="status"
                                :value="status"
                            >
                                {{ formatStatus(status) }}
                            </option>
                        </select>
                    </div>

                    <!-- Start Date -->
                    <div class="form-group">
                        <label>Start Date</label>

                        <input
                            v-model="form.startDate"
                            type="date"
                        />
                    </div>

                    <!-- Due Date -->
                    <div class="form-group">
                        <label>Due Date</label>

                        <input
                            v-model="form.dueDate"
                            type="date"
                        />
                    </div>

                    <!-- Description -->
                    <div class="form-group full-width">
                        <label>Description</label>

                        <textarea
                            v-model="form.description"
                            rows="4"
                            maxlength="2000"
                            placeholder="Describe the task..."
                        ></textarea>
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
                        {{ saving ? 'Saving...' : 'Save Task' }}
                    </button>

                </div>

            </form>
        </div>

        <!-- Search and Filters -->
        <div class="filter-card">

            <div class="filter-group search-group">
                <label>Search Tasks</label>

                <input
                    v-model="searchText"
                    type="text"
                    placeholder="Search by task title..."
                />
            </div>

            <div class="filter-group">
                <label>Project</label>

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

            <div class="filter-group">
                <label>Status</label>

                <select v-model="selectedStatus">
                    <option value="">All Statuses</option>

                    <option
                        v-for="status in statuses"
                        :key="status"
                        :value="status"
                    >
                        {{ formatStatus(status) }}
                    </option>
                </select>
            </div>

            <div class="filter-group">
                <label>Priority</label>

                <select v-model="selectedPriority">
                    <option value="">All Priorities</option>

                    <option
                        v-for="priority in priorities"
                        :key="priority"
                        :value="priority"
                    >
                        {{ priority }}
                    </option>
                </select>
            </div>

            <button
                class="secondary-btn clear-btn"
                @click="clearFilters"
            >
                Clear Filters
            </button>

        </div>

        <!-- Tasks Table -->
        <div class="table-card">

            <div class="table-header">
                <h2>Tasks</h2>

                <span>
                    {{ filteredTasks.length }} task(s)
                </span>
            </div>

            <div v-if="loading" class="empty-state">
                Loading tasks...
            </div>

            <div
                v-else-if="filteredTasks.length === 0"
                class="empty-state"
            >
                No tasks found.
            </div>

            <div v-else class="table-responsive">

                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Task</th>
                            <th>Project</th>
                            <th>Assigned Employee</th>
                            <th>Priority</th>
                            <th>Status</th>
                            <th>Due Date</th>
                            <th>Actions</th>
                        </tr>
                    </thead>

                    <tbody>
                        <tr
                            v-for="(task, index) in filteredTasks"
                            :key="task.id"
                        >
                            <td>{{ index + 1 }}</td>

                            <td>
                                <strong>{{ task.title }}</strong>

                                <small
                                    v-if="task.description"
                                    class="task-description"
                                >
                                    {{ task.description }}
                                </small>
                            </td>

                            <td>{{ task.projectName }}</td>

                            <td>
                                {{ task.assignedEmployeeName || 'Unassigned' }}
                            </td>

                            <td>
                                <span
                                    class="priority-badge"
                                    :class="priorityClass(task.priority)"
                                >
                                    {{ task.priority }}
                                </span>
                            </td>

                            <td>
                                <span
                                    class="status-badge"
                                    :class="statusClass(task.status)"
                                >
                                    {{ formatStatus(task.status) }}
                                </span>
                            </td>

                            <td>
                                <span :class="{ overdue: isOverdue(task) }">
                                    {{ task.dueDate || '-' }}
                                    <small v-if="isOverdue(task)">
                                        Overdue
                                    </small>
                                </span>
                            </td>

                            <td>
                                <div class="actions">
                                    <button
                                        class="edit-btn"
                                        @click="editTask(task)"
                                    >
                                        Edit
                                    </button>

                                    <button
                                        class="delete-btn"
                                        @click="deleteTask(task)"
                                    >
                                        Delete
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
.tasks-page {
    padding: 25px;
    color: #1f2937;
}

.page-header,
.form-header,
.table-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
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
    padding: 10px 15px;
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
    grid-template-columns: repeat(4, 1fr);
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
    min-width: 0;
}

.form-group label,
.filter-group label {
    font-weight: 600;
    font-size: 14px;
}

.form-group small {
    color: #6b7280;
}

.full-width {
    grid-column: 1 / -1;
}

.form-group input,
.form-group select,
.form-group textarea,
.filter-group input,
.filter-group select {
    width: 100%;
    box-sizing: border-box;
    padding: 11px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    outline: none;
    background: white;
    font: inherit;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus,
.filter-group input:focus,
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
    display: grid;
    grid-template-columns: 2fr 1.3fr 1fr 1fr auto;
    align-items: end;
    gap: 14px;
    margin-bottom: 22px;
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

.task-description {
    display: block;
    max-width: 230px;
    margin-top: 5px;
    color: #6b7280;
    white-space: normal;
    overflow: hidden;
    text-overflow: ellipsis;
}

.priority-badge,
.status-badge {
    display: inline-block;
    padding: 5px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;
}

.priority-badge.low {
    background: #f3f4f6;
    color: #4b5563;
}

.priority-badge.medium {
    background: #dbeafe;
    color: #1d4ed8;
}

.priority-badge.high {
    background: #ffedd5;
    color: #c2410c;
}

.priority-badge.critical {
    background: #fee2e2;
    color: #b91c1c;
}

.status-badge.todo {
    background: #f3f4f6;
    color: #4b5563;
}

.status-badge.in_progress {
    background: #dbeafe;
    color: #1d4ed8;
}

.status-badge.review {
    background: #fef3c7;
    color: #92400e;
}

.status-badge.completed {
    background: #dcfce7;
    color: #166534;
}

.overdue {
    color: #dc2626;
    font-weight: 600;
}

.overdue small {
    display: block;
    font-size: 11px;
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
    align-items: center;
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

@media (max-width: 1100px) {
    .filter-card {
        grid-template-columns: repeat(2, 1fr);
    }

    .stats-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 650px) {
    .tasks-page {
        padding: 15px;
    }

    .stats-grid,
    .form-grid,
    .filter-card {
        grid-template-columns: 1fr;
    }

    .page-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .full-width {
        grid-column: auto;
    }
}
</style>
