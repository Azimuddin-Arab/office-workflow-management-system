
<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '../api/api';

const user = ref(null);

const loading = ref(true);
const errorMessage = ref('');

const employees = ref([]);
const departments = ref([]);
const projects = ref([]);
const tasks = ref([]);
const milestones = ref([]);

const stats = computed(() => [
    {
        title: 'Total Employees',
        value: employees.value.length,
        icon: '👥',
        color: 'blue',
        description: 'Registered employees'
    },
    {
        title: 'Departments',
        value: departments.value.length,
        icon: '🏢',
        color: 'purple',
        description: 'Organization departments'
    },
    {
        title: 'Total Projects',
        value: projects.value.length,
        icon: '📁',
        color: 'orange',
        description: 'All registered projects'
    },
    {
        title: 'Total Tasks',
        value: tasks.value.length,
        icon: '📋',
        color: 'green',
        description: 'All project tasks'
    },
    {
        title: 'Milestones',
        value: milestones.value.length,
        icon: '🎯',
        color: 'pink',
        description: 'Project milestones'
    }
]);

const projectStatuses = computed(() => {
    const statuses = [
        { name: 'PLANNING', label: 'Planning', color: '#64748b' },
        { name: 'IN_PROGRESS', label: 'In Progress', color: '#3b82f6' },
        { name: 'ON_HOLD', label: 'On Hold', color: '#f59e0b' },
        { name: 'COMPLETED', label: 'Completed', color: '#10b981' },
        { name: 'CANCELLED', label: 'Cancelled', color: '#ef4444' }
    ];

    return statuses.map(status => {
        const count = projects.value.filter(
            project => project.status?.toUpperCase() === status.name
        ).length;

        return {
            ...status,
            count,
            percentage: projects.value.length
                ? Math.round((count / projects.value.length) * 100)
                : 0
        };
    });
});

const taskStatuses = computed(() => {
    const statuses = [
        { name: 'TODO', label: 'To Do', color: '#64748b' },
        { name: 'IN_PROGRESS', label: 'In Progress', color: '#3b82f6' },
        { name: 'REVIEW', label: 'Review', color: '#f59e0b' },
        { name: 'COMPLETED', label: 'Completed', color: '#10b981' }
    ];

    return statuses.map(status => ({
        ...status,
        count: tasks.value.filter(
            task => task.status?.toUpperCase() === status.name
        ).length
    }));
});

const upcomingTasks = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return tasks.value
        .filter(task => {
            if (!task.dueDate) return false;

            const dueDate = new Date(`${task.dueDate}T00:00:00`);

            return dueDate >= today &&
                task.status?.toUpperCase() !== 'COMPLETED';
        })
        .sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate))
        .slice(0, 5);
});

const recentTasks = computed(() => {
    return [...tasks.value]
        .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
        .slice(0, 5);
});

const completionPercentage = computed(() => {
    if (!tasks.value.length) return 0;

    const completed = tasks.value.filter(
        task => task.status?.toUpperCase() === 'COMPLETED'
    ).length;

    return Math.round((completed / tasks.value.length) * 100);
});

function formatDate(date) {
    if (!date) return 'Not set';

    return new Date(`${date}T00:00:00`).toLocaleDateString();
}

function getStatusClass(status) {
    return (status || 'unknown').toLowerCase().replaceAll('_', '-');
}

async function loadDashboard() {
    loading.value = true;
    errorMessage.value = '';

    const results = await Promise.allSettled([
        api.get('/employees'),
        api.get('/departments'),
        api.get('/projects'),
        api.get('/tasks'),
        api.get('/milestones')
    ]);

    const refs = [
        employees,
        departments,
        projects,
        tasks,
        milestones
    ];

    const names = [
        'Employees',
        'Departments',
        'Projects',
        'Tasks',
        'Milestones'
    ];

    results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
            refs[index].value = Array.isArray(result.value.data)
                ? result.value.data
                : result.value.data.content || [];
        } else {
            console.error(`Failed to load ${names[index]}:`, result.reason);
        }
    });

    if (results.every(result => result.status === 'rejected')) {
        errorMessage.value =
            'Unable to load dashboard data. Please check your login session and backend connection.';
    }

    loading.value = false;
}

function loadUser() {
    try {
        const savedUser = localStorage.getItem('user');
        user.value = savedUser ? JSON.parse(savedUser) : null;
    } catch (error) {
        console.error('Unable to read logged-in user:', error);
        user.value = null;
    }
}

onMounted(() => {
    loadUser();
    loadDashboard();
});
</script>

<template>
    <div class="dashboard">

        <!-- Welcome Header -->
        <section class="welcome-header">
            <div>
                <span class="welcome-label">OVERVIEW</span>
                <h1>Welcome back, {{ user?.fullName || 'User' }}!</h1>
                <p>Here is what's happening with your organization today.</p>
            </div>

            <button class="refresh-btn" @click="loadDashboard">
                ↻ Refresh Dashboard
            </button>
        </section>

        <!-- Loading -->
        <div v-if="loading" class="loading">
            Loading dashboard information...
        </div>

        <div v-else>

            <!-- Error -->
            <div v-if="errorMessage" class="error-message">
                {{ errorMessage }}
            </div>

            <!-- Statistics -->
            <section class="stats-grid">
                <div
                    v-for="stat in stats"
                    :key="stat.title"
                    class="stat-card"
                >
                    <div class="stat-top">
                        <div class="stat-icon" :class="stat.color">
                            {{ stat.icon }}
                        </div>
                    </div>

                    <h2>{{ stat.value }}</h2>
                    <h3>{{ stat.title }}</h3>
                    <p>{{ stat.description }}</p>
                </div>
            </section>

            <!-- Main Dashboard Grid -->
            <section class="dashboard-grid">

                <!-- Project Overview -->
                <div class="panel">
                    <div class="panel-header">
                        <div>
                            <h2>Project Overview</h2>
                            <p>Project distribution by status</p>
                        </div>
                        <span class="panel-icon">📁</span>
                    </div>

                    <div
                        v-for="status in projectStatuses"
                        :key="status.name"
                        class="progress-item"
                    >
                        <div class="progress-info">
                            <span>{{ status.label }}</span>
                            <strong>{{ status.count }}</strong>
                        </div>

                        <div class="progress-track">
                            <div
                                class="progress-fill"
                                :style="{
                                    width: status.percentage + '%',
                                    backgroundColor: status.color
                                }"
                            ></div>
                        </div>
                    </div>

                    <div v-if="!projects.length" class="empty-state">
                        No projects available.
                    </div>
                </div>

                <!-- Task Overview -->
                <div class="panel">
                    <div class="panel-header">
                        <div>
                            <h2>Task Overview</h2>
                            <p>Current task progress</p>
                        </div>
                        <span class="panel-icon">📋</span>
                    </div>

                    <div class="completion-section">
                        <div class="completion-circle">
                            <div>
                                <strong>{{ completionPercentage }}%</strong>
                                <span>Completed</span>
                            </div>
                        </div>
                    </div>

                    <div
                        v-for="status in taskStatuses"
                        :key="status.name"
                        class="task-status-row"
                    >
                        <span class="status-dot"
                            :style="{ backgroundColor: status.color }">
                        </span>

                        <span>{{ status.label }}</span>
                        <strong>{{ status.count }}</strong>
                    </div>
                </div>

            </section>

            <!-- Tables -->
            <section class="dashboard-grid table-grid">

                <!-- Upcoming Deadlines -->
                <div class="panel">
                    <div class="panel-header">
                        <div>
                            <h2>Upcoming Deadlines</h2>
                            <p>Tasks that need attention</p>
                        </div>
                        <span class="panel-icon">📅</span>
                    </div>

                    <div class="table-wrapper">
                        <table>
                            <thead>
                                <tr>
                                    <th>Task</th>
                                    <th>Project</th>
                                    <th>Due Date</th>
                                    <th>Status</th>
                                </tr>
                            </thead>

                            <tbody>
                                <tr v-for="task in upcomingTasks" :key="task.id">
                                    <td>{{ task.title }}</td>
                                    <td>{{ task.projectName || '—' }}</td>
                                    <td>{{ formatDate(task.dueDate) }}</td>
                                    <td>
                                        <span class="status-badge"
                                            :class="getStatusClass(task.status)">
                                            {{ task.status }}
                                        </span>
                                    </td>
                                </tr>

                                <tr v-if="!upcomingTasks.length">
                                    <td colspan="4" class="empty-state">
                                        No upcoming deadlines.
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Recent Tasks -->
                <div class="panel">
                    <div class="panel-header">
                        <div>
                            <h2>Recent Tasks</h2>
                            <p>Recently created tasks</p>
                        </div>
                        <span class="panel-icon">🕒</span>
                    </div>

                    <div class="table-wrapper">
                        <table>
                            <thead>
                                <tr>
                                    <th>Task</th>
                                    <th>Assigned To</th>
                                    <th>Priority</th>
                                    <th>Status</th>
                                </tr>
                            </thead>

                            <tbody>
                                <tr v-for="task in recentTasks" :key="task.id">
                                    <td>{{ task.title }}</td>
                                    <td>{{ task.assignedEmployeeName || 'Unassigned' }}</td>
                                    <td>
                                        <span class="priority-badge"
                                            :class="getStatusClass(task.priority)">
                                            {{ task.priority || '—' }}
                                        </span>
                                    </td>
                                    <td>
                                        <span class="status-badge"
                                            :class="getStatusClass(task.status)">
                                            {{ task.status }}
                                        </span>
                                    </td>
                                </tr>

                                <tr v-if="!recentTasks.length">
                                    <td colspan="4" class="empty-state">
                                        No tasks available.
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

            </section>

            <!-- User Information -->
            <section class="panel user-panel">
                <div class="panel-header">
                    <div>
                        <h2>Account Information</h2>
                        <p>Your logged-in account details</p>
                    </div>
                    <span class="panel-icon">👤</span>
                </div>

                <div class="user-details">
                    <div>
                        <span>Full Name</span>
                        <strong>{{ user?.fullName || 'Not available' }}</strong>
                    </div>

                    <div>
                        <span>Email Address</span>
                        <strong>{{ user?.email || 'Not available' }}</strong>
                    </div>

                    <div>
                        <span>Role</span>
                        <strong>{{ user?.role || 'Not available' }}</strong>
                    </div>
                </div>
            </section>

        </div>
    </div>
</template>

<style scoped>
.dashboard {
    padding: 28px;
    background: #f5f7fb;
    min-height: 100vh;
    color: #1e293b;
}

.welcome-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 20px;
    margin-bottom: 28px;
}

.welcome-label {
    color: #2563eb;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 1.5px;
}

.welcome-header h1 {
    font-size: 28px;
    margin: 8px 0;
    color: #172554;
}

.welcome-header p,
.panel-header p {
    color: #64748b;
    margin: 0;
    font-size: 14px;
}

.refresh-btn {
    border: none;
    background: #2563eb;
    color: white;
    padding: 11px 18px;
    border-radius: 8px;
    cursor: pointer;
    font-weight: 600;
}

.refresh-btn:hover {
    background: #1d4ed8;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 18px;
    margin-bottom: 22px;
}

.stat-card,
.panel {
    background: white;
    border: 1px solid #e8edf4;
    border-radius: 12px;
    padding: 22px;
    box-shadow: 0 3px 12px rgba(15, 23, 42, 0.035);
}

.stat-icon {
    width: 45px;
    height: 45px;
    display: grid;
    place-items: center;
    border-radius: 10px;
    font-size: 22px;
}

.stat-icon.blue { background: #eff6ff; }
.stat-icon.purple { background: #f5f3ff; }
.stat-icon.orange { background: #fff7ed; }
.stat-icon.green { background: #ecfdf5; }
.stat-icon.pink { background: #fdf2f8; }

.stat-card h2 {
    font-size: 29px;
    margin: 18px 0 5px;
    color: #172554;
}

.stat-card h3 {
    font-size: 14px;
    margin: 0 0 7px;
    color: #334155;
}

.stat-card p {
    color: #94a3b8;
    font-size: 12px;
    margin: 0;
}

.dashboard-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
    margin-bottom: 22px;
}

.panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 25px;
}

.panel-header h2 {
    font-size: 17px;
    margin: 0 0 6px;
    color: #172554;
}

.panel-icon {
    font-size: 23px;
}

.progress-item {
    margin-bottom: 19px;
}

.progress-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 13px;
    color: #475569;
}

.progress-track {
    height: 8px;
    background: #f1f5f9;
    border-radius: 20px;
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    border-radius: 20px;
    transition: width 0.3s ease;
}

.completion-section {
    display: flex;
    justify-content: center;
    padding: 8px 0 20px;
}

.completion-circle {
    width: 145px;
    height: 145px;
    border-radius: 50%;
    background: conic-gradient(
        #2563eb 0%,
        #2563eb calc(var(--progress, 0) * 1%),
        #e2e8f0 0
    );
    display: grid;
    place-items: center;
    position: relative;
}

.completion-circle::before {
    content: '';
    position: absolute;
    inset: 11px;
    background: white;
    border-radius: 50%;
}

.completion-circle > div {
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.completion-circle strong {
    font-size: 27px;
    color: #172554;
}

.completion-circle span {
    font-size: 12px;
    color: #64748b;
}

.task-status-row {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 0;
    border-top: 1px solid #f1f5f9;
    font-size: 13px;
    color: #475569;
}

.task-status-row strong {
    margin-left: auto;
    color: #172554;
}

.status-dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
}

.table-grid {
    align-items: start;
}

.table-wrapper {
    overflow-x: auto;
}

table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
    white-space: nowrap;
}

th {
    color: #64748b;
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: .4px;
    background: #f8fafc;
}

th, td {
    padding: 13px 10px;
    border-bottom: 1px solid #f1f5f9;
}

td {
    font-size: 12px;
    color: #475569;
}

.status-badge,
.priority-badge {
    display: inline-block;
    padding: 5px 9px;
    border-radius: 20px;
    font-size: 10px;
    font-weight: 700;
    background: #f1f5f9;
    color: #475569;
}

.status-badge.in-progress {
    background: #dbeafe;
    color: #1d4ed8;
}

.status-badge.completed {
    background: #dcfce7;
    color: #15803d;
}

.status-badge.on-hold,
.status-badge.review,
.status-badge.delayed {
    background: #fef3c7;
    color: #b45309;
}

.status-badge.cancelled {
    background: #fee2e2;
    color: #b91c1c;
}

.priority-badge.high,
.priority-badge.urgent {
    background: #fee2e2;
    color: #b91c1c;
}

.priority-badge.medium {
    background: #fef3c7;
    color: #b45309;
}

.priority-badge.low {
    background: #dcfce7;
    color: #15803d;
}

.user-panel {
    margin-bottom: 25px;
}

.user-details {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
}

.user-details div {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.user-details span {
    color: #94a3b8;
    font-size: 12px;
}

.user-details strong {
    font-size: 14px;
    color: #334155;
}

.loading,
.empty-state {
    text-align: center;
    color: #94a3b8;
    padding: 25px;
}

.error-message {
    background: #fee2e2;
    color: #991b1b;
    padding: 14px;
    border-radius: 8px;
    margin-bottom: 20px;
}

@media (max-width: 1200px) {
    .stats-grid {
        grid-template-columns: repeat(3, 1fr);
    }
}

@media (max-width: 800px) {
    .dashboard-grid {
        grid-template-columns: 1fr;
    }

    .stats-grid {
        grid-template-columns: repeat(2, 1fr);
    }

    .user-details {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 550px) {
    .dashboard {
        padding: 16px;
    }

    .welcome-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .welcome-header h1 {
        font-size: 22px;
    }

    .stats-grid {
        grid-template-columns: 1fr;
    }
}
</style>
