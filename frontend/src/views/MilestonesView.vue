
<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '../api/api';

// ==========================================
// DATA
// ==========================================

const milestones = ref([]);
const projects = ref([]);

const loading = ref(false);
const loadingProjects = ref(false);
const saving = ref(false);

const errorMessage = ref('');
const successMessage = ref('');

const showForm = ref(false);
const editingId = ref(null);

// ==========================================
// FILTERS
// ==========================================

const searchQuery = ref('');
const selectedProject = ref('');
const selectedStatus = ref('');

// ==========================================
// FORM
// ==========================================

const emptyForm = () => ({
    title: '',
    description: '',
    projectId: '',
    dueDate: '',
    completedDate: '',
    status: 'PENDING'
});

const form = ref(emptyForm());

// ==========================================
// LOAD PROJECTS
// ==========================================

const loadProjects = async () => {
    loadingProjects.value = true;

    try {
        const response = await api.get('/projects');

        console.log('Projects API response:', response.data);

        const data = response.data;

        // Support normal arrays and paginated responses
        if (Array.isArray(data)) {
            projects.value = data;
        } else if (Array.isArray(data.content)) {
            projects.value = data.content;
        } else {
            projects.value = [];
        }

        if (projects.value.length === 0) {
            console.warn('No projects found.');
        }

    } catch (error) {
        console.error('Failed to load projects:', error);

        projects.value = [];

        errorMessage.value =
            typeof error.response?.data === 'string'
                ? error.response.data
                : 'Failed to load projects. Please check the Projects API.';

    } finally {
        loadingProjects.value = false;
    }
};

// ==========================================
// PROJECT NAME
// ==========================================

const getProjectName = (project) => {
    return project.projectName ||
        project.name ||
        project.title ||
        `Project #${project.id}`;
};

// ==========================================
// LOAD MILESTONES
// ==========================================

const loadMilestones = async () => {
    loading.value = true;
    errorMessage.value = '';

    try {
        const response = await api.get('/milestones');

        milestones.value = Array.isArray(response.data)
            ? response.data
            : [];

    } catch (error) {
        console.error('Failed to load milestones:', error);

        errorMessage.value =
            typeof error.response?.data === 'string'
                ? error.response.data
                : 'Failed to load milestones.';

    } finally {
        loading.value = false;
    }
};

// ==========================================
// FILTER MILESTONES
// ==========================================

const filteredMilestones = computed(() => {

    return milestones.value.filter(milestone => {

        const title = (milestone.title || '').toLowerCase();

        const matchesSearch = title.includes(
            searchQuery.value.toLowerCase()
        );

        const matchesProject =
            !selectedProject.value ||
            String(milestone.projectId) ===
            String(selectedProject.value);

        const matchesStatus =
            !selectedStatus.value ||
            milestone.status === selectedStatus.value;

        return matchesSearch && matchesProject && matchesStatus;
    });
});

// ==========================================
// STATISTICS
// ==========================================

const totalMilestones = computed(() =>
    milestones.value.length
);

const completedMilestones = computed(() =>
    milestones.value.filter(
        m => m.status === 'COMPLETED'
    ).length
);

const pendingMilestones = computed(() =>
    milestones.value.filter(
        m => m.status === 'PENDING'
    ).length
);

const overdueMilestones = computed(() =>
    milestones.value.filter(m => isOverdue(m)).length
);

// ==========================================
// DATE HELPERS
// ==========================================

const getToday = () => {
    const date = new Date();

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
};

const isOverdue = (milestone) => {
    return Boolean(
        milestone.dueDate &&
        milestone.dueDate < getToday() &&
        milestone.status !== 'COMPLETED'
    );
};

// ==========================================
// OPEN CREATE FORM
// ==========================================

const openCreateForm = () => {

    editingId.value = null;

    form.value = emptyForm();

    errorMessage.value = '';
    successMessage.value = '';

    showForm.value = true;
};

// ==========================================
// OPEN EDIT FORM
// ==========================================

const editMilestone = (milestone) => {

    editingId.value = milestone.id;

    form.value = {
        title: milestone.title || '',
        description: milestone.description || '',
        projectId: milestone.projectId || '',
        dueDate: milestone.dueDate || '',
        completedDate: milestone.completedDate || '',
        status: milestone.status || 'PENDING'
    };

    errorMessage.value = '';
    successMessage.value = '';

    showForm.value = true;
};

// ==========================================
// SAVE MILESTONE
// ==========================================

const saveMilestone = async () => {

    errorMessage.value = '';
    successMessage.value = '';

    if (!form.value.title.trim()) {
        errorMessage.value = 'Milestone title is required.';
        return;
    }

    if (!form.value.projectId) {
        errorMessage.value = 'Please select a project.';
        return;
    }

    if (
        !projects.value.some(
            p => String(p.id) === String(form.value.projectId)
        )
    ) {
        errorMessage.value = 'Please select a valid project.';
        return;
    }

    saving.value = true;

    try {

        const payload = {
            title: form.value.title.trim(),
            description: form.value.description,
            projectId: Number(form.value.projectId),
            dueDate: form.value.dueDate || null,
            completedDate:
                form.value.status === 'COMPLETED'
                    ? (form.value.completedDate || null)
                    : null,
            status: form.value.status
        };

        if (editingId.value) {

            await api.put(
                `/milestones/${editingId.value}`,
                payload
            );

            successMessage.value =
                'Milestone updated successfully.';

        } else {

            await api.post('/milestones', payload);

            successMessage.value =
                'Milestone created successfully.';
        }

        showForm.value = false;

        form.value = emptyForm();

        await loadMilestones();

    } catch (error) {

        console.error('Save milestone error:', error);

        errorMessage.value =
            typeof error.response?.data === 'string'
                ? error.response.data
                : 'Failed to save milestone.';

    } finally {
        saving.value = false;
    }
};

// ==========================================
// DELETE MILESTONE
// ==========================================

const deleteMilestone = async (id) => {

    if (!confirm('Are you sure you want to delete this milestone?')) {
        return;
    }

    errorMessage.value = '';
    successMessage.value = '';

    try {

        await api.delete(`/milestones/${id}`);

        successMessage.value =
            'Milestone deleted successfully.';

        await loadMilestones();

    } catch (error) {

        console.error('Delete milestone error:', error);

        errorMessage.value =
            typeof error.response?.data === 'string'
                ? error.response.data
                : 'Failed to delete milestone.';
    }
};

// ==========================================
// STATUS STYLE
// ==========================================

const statusClass = (status) => {

    return {
        PENDING: 'status-pending',
        IN_PROGRESS: 'status-progress',
        COMPLETED: 'status-completed',
        DELAYED: 'status-delayed'
    }[status] || '';
};

// ==========================================
// CLOSE FORM
// ==========================================

const closeForm = () => {
    showForm.value = false;
    editingId.value = null;
    form.value = emptyForm();
};

// ==========================================
// INITIALIZE
// ==========================================

onMounted(async () => {

    await Promise.all([
        loadProjects(),
        loadMilestones()
    ]);

});
</script>

<template>
    <div class="milestones-page">

        <!-- PAGE HEADER -->

        <div class="page-header">

            <div>
                <h1>Milestone Management</h1>

                <p>
                    Manage important project targets and achievements.
                </p>
            </div>

            <button
                class="primary-btn"
                @click="openCreateForm"
            >
                + Add Milestone
            </button>

        </div>

        <!-- ERROR MESSAGE -->

        <div
            v-if="errorMessage"
            class="alert error-alert"
        >
            {{ errorMessage }}
        </div>

        <!-- SUCCESS MESSAGE -->

        <div
            v-if="successMessage"
            class="alert success-alert"
        >
            {{ successMessage }}
        </div>

        <!-- STATISTICS -->

        <div class="stats-grid">

            <div class="stat-card">
                <span>Total Milestones</span>
                <h2>{{ totalMilestones }}</h2>
            </div>

            <div class="stat-card">
                <span>Completed</span>
                <h2>{{ completedMilestones }}</h2>
            </div>

            <div class="stat-card">
                <span>Pending</span>
                <h2>{{ pendingMilestones }}</h2>
            </div>

            <div class="stat-card overdue-card">
                <span>Overdue</span>
                <h2>{{ overdueMilestones }}</h2>
            </div>

        </div>

        <!-- CREATE / EDIT FORM -->

        <div
            v-if="showForm"
            class="form-card"
        >

            <div class="form-header">

                <h2>
                    {{ editingId ? 'Edit Milestone' : 'Create Milestone' }}
                </h2>

                <button
                    type="button"
                    class="close-btn"
                    @click="closeForm"
                >
                    ✕
                </button>

            </div>

            <form @submit.prevent="saveMilestone">

                <div class="form-grid">

                    <!-- TITLE -->

                    <div class="form-group">

                        <label>Milestone Title *</label>

                        <input
                            v-model="form.title"
                            type="text"
                            maxlength="200"
                            placeholder="Enter milestone title"
                            required
                        />

                    </div>

                    <!-- PROJECT DROPDOWN -->

                    <div class="form-group">

                        <label>Project *</label>

                        <select
                            v-model="form.projectId"
                            required
                            :disabled="loadingProjects"
                        >

                            <option value="">
                                {{
                                    loadingProjects
                                        ? 'Loading projects...'
                                        : 'Select Project'
                                }}
                            </option>

                            <option
                                v-for="project in projects"
                                :key="project.id"
                                :value="project.id"
                            >
                                {{ getProjectName(project) }}
                            </option>

                        </select>

                        <small v-if="!loadingProjects && projects.length === 0">
                            No projects available. Create a project first.
                        </small>

                    </div>

                    <!-- DUE DATE -->

                    <div class="form-group">

                        <label>Due Date</label>

                        <input
                            v-model="form.dueDate"
                            type="date"
                        />

                    </div>

                    <!-- STATUS -->

                    <div class="form-group">

                        <label>Status</label>

                        <select v-model="form.status">

                            <option value="PENDING">
                                Pending
                            </option>

                            <option value="IN_PROGRESS">
                                In Progress
                            </option>

                            <option value="COMPLETED">
                                Completed
                            </option>

                            <option value="DELAYED">
                                Delayed
                            </option>

                        </select>

                    </div>

                    <!-- COMPLETED DATE -->

                    <div
                        v-if="form.status === 'COMPLETED'"
                        class="form-group"
                    >

                        <label>Completed Date</label>

                        <input
                            v-model="form.completedDate"
                            type="date"
                        />

                    </div>

                    <!-- DESCRIPTION -->

                    <div class="form-group full-width">

                        <label>Description</label>

                        <textarea
                            v-model="form.description"
                            maxlength="2000"
                            rows="4"
                            placeholder="Enter milestone description"
                        ></textarea>

                    </div>

                </div>

                <!-- FORM BUTTONS -->

                <div class="form-actions">

                    <button
                        type="button"
                        class="secondary-btn"
                        @click="closeForm"
                    >
                        Cancel
                    </button>

                    <button
                        type="submit"
                        class="primary-btn"
                        :disabled="saving || loadingProjects || projects.length === 0"
                    >
                        {{
                            saving
                                ? 'Saving...'
                                : editingId
                                    ? 'Update Milestone'
                                    : 'Save Milestone'
                        }}
                    </button>

                </div>

            </form>

        </div>

        <!-- FILTERS -->

        <div class="filter-card">

            <input
                v-model="searchQuery"
                type="text"
                placeholder="Search milestones..."
            />

            <select v-model="selectedProject">

                <option value="">
                    All Projects
                </option>

                <option
                    v-for="project in projects"
                    :key="project.id"
                    :value="project.id"
                >
                    {{ getProjectName(project) }}
                </option>

            </select>

            <select v-model="selectedStatus">

                <option value="">
                    All Statuses
                </option>

                <option value="PENDING">
                    Pending
                </option>

                <option value="IN_PROGRESS">
                    In Progress
                </option>

                <option value="COMPLETED">
                    Completed
                </option>

                <option value="DELAYED">
                    Delayed
                </option>

            </select>

        </div>

        <!-- MILESTONE TABLE -->

        <div class="table-card">

            <div class="table-header">

                <h2>Milestones</h2>

                <span>
                    {{ filteredMilestones.length }} records
                </span>

            </div>

            <div
                v-if="loading"
                class="empty-message"
            >
                Loading milestones...
            </div>

            <div
                v-else-if="filteredMilestones.length === 0"
                class="empty-message"
            >
                No milestones found.
            </div>

            <div
                v-else
                class="table-responsive"
            >

                <table>

                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Project</th>
                            <th>Due Date</th>
                            <th>Completed Date</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>

                    <tbody>

                        <tr
                            v-for="milestone in filteredMilestones"
                            :key="milestone.id"
                        >

                            <td>
                                <strong>
                                    {{ milestone.title }}
                                </strong>

                                <small>
                                    {{ milestone.description }}
                                </small>
                            </td>

                            <td>
                                {{ milestone.projectName }}
                            </td>

                            <td>
                                {{ milestone.dueDate || '-' }}
                            </td>

                            <td>
                                {{ milestone.completedDate || '-' }}
                            </td>

                            <td>

                                <span
                                    :class="[
                                        'status-badge',
                                        statusClass(milestone.status)
                                    ]"
                                >
                                    {{ (milestone.status || '').replace('_', ' ') }}
                                </span>

                                <span
                                    v-if="isOverdue(milestone)"
                                    class="overdue-label"
                                >
                                    Overdue
                                </span>

                            </td>

                            <td class="action-cell">

                                <button
                                    class="edit-btn"
                                    @click="editMilestone(milestone)"
                                >
                                    Edit
                                </button>

                                <button
                                    class="delete-btn"
                                    @click="deleteMilestone(milestone.id)"
                                >
                                    Delete
                                </button>

                            </td>

                        </tr>

                    </tbody>

                </table>

            </div>

        </div>

    </div>
</template>

<style scoped>
.milestones-page {
    padding: 24px;
    color: #1e293b;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 15px;
    margin-bottom: 24px;
}

.page-header h1 {
    margin: 0 0 7px;
    font-size: 26px;
}

.page-header p {
    margin: 0;
    color: #64748b;
}

.primary-btn,
.secondary-btn,
.edit-btn,
.delete-btn {
    border: none;
    border-radius: 6px;
    padding: 10px 15px;
    cursor: pointer;
    font-weight: 600;
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
    background: #e2e8f0;
    color: #334155;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 22px;
}

.stat-card,
.form-card,
.filter-card,
.table-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 18px;
}

.stat-card span {
    color: #64748b;
}

.stat-card h2 {
    margin: 12px 0 0;
    font-size: 28px;
}

.overdue-card h2 {
    color: #dc2626;
}

.alert {
    padding: 12px 15px;
    border-radius: 6px;
    margin-bottom: 16px;
}

.error-alert {
    background: #fee2e2;
    color: #991b1b;
}

.success-alert {
    background: #dcfce7;
    color: #166534;
}

.form-card {
    margin-bottom: 22px;
}

.form-header,
.table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18px;
}

.form-header h2,
.table-header h2 {
    margin: 0;
    font-size: 19px;
}

.close-btn {
    border: none;
    background: transparent;
    cursor: pointer;
    font-size: 20px;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 7px;
}

.form-group label {
    font-weight: 600;
    font-size: 14px;
}

.form-group input,
.form-group select,
.form-group textarea,
.filter-card input,
.filter-card select {
    width: 100%;
    box-sizing: border-box;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    padding: 10px;
    font: inherit;
}

.form-group small {
    color: #dc2626;
    font-size: 12px;
}

.full-width {
    grid-column: 1 / -1;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 20px;
}

.filter-card {
    display: grid;
    grid-template-columns: 2fr 1fr 1fr;
    gap: 12px;
    margin-bottom: 20px;
}

.table-header span {
    color: #64748b;
    font-size: 14px;
}

.table-responsive {
    overflow-x: auto;
}

table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
}

th,
td {
    padding: 13px;
    border-bottom: 1px solid #e2e8f0;
    white-space: nowrap;
}

th {
    background: #f8fafc;
    color: #475569;
    font-size: 13px;
}

td small {
    display: block;
    max-width: 230px;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #64748b;
    margin-top: 5px;
}

.status-badge,
.overdue-label {
    display: inline-block;
    padding: 5px 9px;
    border-radius: 20px;
    font-size: 11px;
    font-weight: 700;
}

.status-pending {
    background: #fef3c7;
    color: #92400e;
}

.status-progress {
    background: #dbeafe;
    color: #1e40af;
}

.status-completed {
    background: #dcfce7;
    color: #166534;
}

.status-delayed {
    background: #fee2e2;
    color: #991b1b;
}

.overdue-label {
    background: #fee2e2;
    color: #991b1b;
    margin-top: 5px;
}

.action-cell {
    display: flex;
    gap: 7px;
}

.edit-btn {
    background: #dbeafe;
    color: #1d4ed8;
}

.delete-btn {
    background: #fee2e2;
    color: #b91c1c;
}

.empty-message {
    padding: 30px;
    text-align: center;
    color: #64748b;
}

@media (max-width: 900px) {
    .stats-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 600px) {
    .page-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .form-grid,
    .filter-card {
        grid-template-columns: 1fr;
    }

    .full-width {
        grid-column: auto;
    }
}
</style>
