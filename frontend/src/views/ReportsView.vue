
<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '../api/api';

const projects = ref([]);
const tasks = ref([]);
const employees = ref([]);
const milestones = ref([]);

const loading = ref(true);
const errorMessage = ref('');

const startDate = ref('');
const endDate = ref('');

async function loadReports() {
    loading.value = true;
    errorMessage.value = '';

    const requests = await Promise.allSettled([
        api.get('/projects'),
        api.get('/tasks'),
        api.get('/employees'),
        api.get('/milestones')
    ]);

    const targets = [projects, tasks, employees, milestones];
    const names = ['Projects', 'Tasks', 'Employees', 'Milestones'];

    requests.forEach((result, index) => {
        if (result.status === 'fulfilled') {
            const data = result.value.data;

            targets[index].value = Array.isArray(data)
                ? data
                : Array.isArray(data?.content)
                    ? data.content
                    : [];
        } else {
            console.error(`Could not load ${names[index]}:`, result.reason);
        }
    });

    if (requests.every(result => result.status === 'rejected')) {
        errorMessage.value =
            'Could not load reports. Check your backend connection and login session.';
    } else if (requests.some(result => result.status === 'rejected')) {
        errorMessage.value =
            'Some report data could not be loaded. Check the browser console for details.';
    }

    loading.value = false;
}

function isWithinDateRange(dateValue) {
    if (!dateValue) return true;

    const date = String(dateValue).slice(0, 10);

    if (startDate.value && date < startDate.value) return false;
    if (endDate.value && date > endDate.value) return false;

    return true;
}

const filteredProjects = computed(() => {
    return projects.value.filter(project =>
        isWithinDateRange(project.startDate)
    );
});

const filteredTasks = computed(() => {
    return tasks.value.filter(task =>
        isWithinDateRange(task.createdAt || task.startDate || task.dueDate)
    );
});

const filteredMilestones = computed(() => {
    return milestones.value.filter(milestone =>
        isWithinDateRange(milestone.createdAt || milestone.dueDate)
    );
});

const completedTasks = computed(() =>
    filteredTasks.value.filter(
        task => task.status?.toUpperCase() === 'COMPLETED'
    )
);

const pendingTasks = computed(() =>
    filteredTasks.value.filter(
        task => task.status?.toUpperCase() === 'TODO'
    )
);

const inProgressTasks = computed(() =>
    filteredTasks.value.filter(
        task => task.status?.toUpperCase() === 'IN_PROGRESS'
    )
);

const overdueTasks = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return filteredTasks.value.filter(task => {
        if (!task.dueDate) return false;
        if (task.status?.toUpperCase() === 'COMPLETED') return false;

        const dueDate = new Date(`${task.dueDate}T00:00:00`);
        return dueDate < today;
    });
});

const completionRate = computed(() => {
    if (!filteredTasks.value.length) return 0;

    return Math.round(
        (completedTasks.value.length / filteredTasks.value.length) * 100
    );
});

const projectStatusReport = computed(() => {
    const statuses = [
        { key: 'PLANNING', label: 'Planning', color: '#64748b' },
        { key: 'IN_PROGRESS', label: 'In Progress', color: '#3b82f6' },
        { key: 'ON_HOLD', label: 'On Hold', color: '#f59e0b' },
        { key: 'COMPLETED', label: 'Completed', color: '#10b981' },
        { key: 'CANCELLED', label: 'Cancelled', color: '#ef4444' }
    ];

    return statuses.map(status => {
        const count = filteredProjects.value.filter(
            project => project.status?.toUpperCase() === status.key
        ).length;

        return {
            ...status,
            count,
            percentage: filteredProjects.value.length
                ? Math.round(count / filteredProjects.value.length * 100)
                : 0
        };
    });
});

const taskStatusReport = computed(() => {
    const statuses = [
        { key: 'TODO', label: 'To Do', color: '#64748b' },
        { key: 'IN_PROGRESS', label: 'In Progress', color: '#3b82f6' },
        { key: 'REVIEW', label: 'Review', color: '#f59e0b' },
        { key: 'COMPLETED', label: 'Completed', color: '#10b981' }
    ];

    return statuses.map(status => {
        const count = filteredTasks.value.filter(
            task => task.status?.toUpperCase() === status.key
        ).length;

        return {
            ...status,
            count,
            percentage: filteredTasks.value.length
                ? Math.round(count / filteredTasks.value.length * 100)
                : 0
        };
    });
});

const employeeWorkload = computed(() => {
    return employees.value.map(employee => {
        const employeeTasks = filteredTasks.value.filter(
            task => Number(task.assignedEmployeeId) === Number(employee.id)
        );

        const completed = employeeTasks.filter(
            task => task.status?.toUpperCase() === 'COMPLETED'
        ).length;

        return {
            id: employee.id,
            name: employee.fullName,
            total: employeeTasks.length,
            completed,
            pending: employeeTasks.length - completed
        };
    })
    .filter(employee => employee.total > 0)
    .sort((a, b) => b.total - a.total);
});

const maxEmployeeTasks = computed(() =>
    Math.max(1, ...employeeWorkload.value.map(employee => employee.total))
);

const milestoneStatusReport = computed(() => {
    const statuses = [
        { key: 'PENDING', label: 'Pending' },
        { key: 'IN_PROGRESS', label: 'In Progress' },
        { key: 'COMPLETED', label: 'Completed' },
        { key: 'DELAYED', label: 'Delayed' }
    ];

    return statuses.map(status => ({
        ...status,
        count: filteredMilestones.value.filter(
            milestone => milestone.status?.toUpperCase() === status.key
        ).length
    }));
});

const projectPerformance = computed(() => {
    return filteredProjects.value.map(project => {
        const projectTasks = filteredTasks.value.filter(
            task => Number(task.projectId) === Number(project.id)
        );

        const completed = projectTasks.filter(
            task => task.status?.toUpperCase() === 'COMPLETED'
        ).length;

        return {
            id: project.id,
            name: project.projectName,
            code: project.projectCode,
            status: project.status,
            totalTasks: projectTasks.length,
            completedTasks: completed,
            progress: projectTasks.length
                ? Math.round(completed / projectTasks.length * 100)
                : 0
        };
    });
});

const summaryCards = computed(() => [
    {
        title: 'Total Projects',
        value: filteredProjects.value.length,
        icon: '📁',
        color: 'blue'
    },
    {
        title: 'Total Tasks',
        value: filteredTasks.value.length,
        icon: '📋',
        color: 'purple'
    },
    {
        title: 'Completed Tasks',
        value: completedTasks.value.length,
        icon: '✓',
        color: 'green'
    },
    {
        title: 'Overdue Tasks',
        value: overdueTasks.value.length,
        icon: '⚠',
        color: 'red'
    }
]);

function formatDate(date) {
    if (!date) return '—';

    return new Date(`${String(date).slice(0, 10)}T00:00:00`)
        .toLocaleDateString();
}

function statusClass(status) {
    return String(status || 'unknown')
        .toLowerCase()
        .replaceAll('_', '-');
}

function clearDateFilter() {
    startDate.value = '';
    endDate.value = '';
}

function exportCsv(filename, rows) {
    if (!rows.length) {
        alert('There is no data to export.');
        return;
    }

    const headers = Object.keys(rows[0]);

    const escapeCsv = value => {
        const text = String(value ?? '');
        return `"${text.replaceAll('"', '""')}"`;
    };

    const csv = [
        headers.map(escapeCsv).join(','),
        ...rows.map(row =>
            headers.map(header => escapeCsv(row[header])).join(',')
        )
    ].join('\n');

    const blob = new Blob(['\uFEFF' + csv], {
        type: 'text/csv;charset=utf-8;'
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = filename;
    link.click();

    URL.revokeObjectURL(url);
}

function exportProjectReport() {
    exportCsv('project-report.csv', projectPerformance.value);
}

function exportTaskReport() {
    const rows = filteredTasks.value.map(task => ({
        Task: task.title,
        Project: task.projectName,
        AssignedEmployee: task.assignedEmployeeName,
        Priority: task.priority,
        Status: task.status,
        StartDate: task.startDate,
        DueDate: task.dueDate
    }));

    exportCsv('task-report.csv', rows);
}

function exportEmployeeReport() {
    exportCsv('employee-workload-report.csv', employeeWorkload.value);
}

onMounted(loadReports);
</script>

<template>
    <div class="reports-page">

        <!-- Page Header -->
        <header class="page-header">
            <div>
                <span class="eyebrow">BUSINESS INTELLIGENCE</span>
                <h1>Reports & Analytics</h1>
                <p>Monitor project performance, tasks, and employee workload.</p>
            </div>

            <button class="refresh-button" @click="loadReports">
                ↻ Refresh Data
            </button>
        </header>

        <!-- Date Filters -->
        <section class="filter-panel">
            <div class="filter-heading">
                <div>
                    <h3>Report Period</h3>
                    <p>Filter report data by date</p>
                </div>
            </div>

            <div class="filter-controls">
                <label>
                    Start Date
                    <input type="date" v-model="startDate">
                </label>

                <label>
                    End Date
                    <input
                        type="date"
                        v-model="endDate"
                        :min="startDate || undefined"
                    >
                </label>

                <button class="clear-button" @click="clearDateFilter">
                    Clear Filter
                </button>
            </div>
        </section>

        <div v-if="loading" class="state-message">
            Loading reports and analytics...
        </div>

        <template v-else>

            <div v-if="errorMessage" class="error-message">
                {{ errorMessage }}
            </div>

            <!-- Summary Cards -->
            <section class="summary-grid">
                <div
                    v-for="card in summaryCards"
                    :key="card.title"
                    class="summary-card"
                >
                    <div class="summary-icon" :class="card.color">
                        {{ card.icon }}
                    </div>

                    <div>
                        <p>{{ card.title }}</p>
                        <h2>{{ card.value }}</h2>
                    </div>
                </div>
            </section>

            <!-- Completion Rate -->
            <section class="completion-banner">
                <div>
                    <h2>Overall Task Completion</h2>
                    <p>
                        {{ completedTasks.length }} of
                        {{ filteredTasks.length }} tasks completed
                    </p>
                </div>

                <div class="completion-value">
                    {{ completionRate }}%
                </div>

                <div class="completion-track">
                    <div
                        class="completion-fill"
                        :style="{ width: completionRate + '%' }"
                    ></div>
                </div>
            </section>

            <!-- Analytics Charts -->
            <section class="charts-grid">

                <!-- Project Status -->
                <div class="report-panel">
                    <div class="panel-heading">
                        <div>
                            <h2>Project Status</h2>
                            <p>Distribution of projects by status</p>
                        </div>
                    </div>

                    <div
                        v-for="item in projectStatusReport"
                        :key="item.key"
                        class="chart-row"
                    >
                        <div class="chart-label">
                            <span>{{ item.label }}</span>
                            <strong>{{ item.count }}</strong>
                        </div>

                        <div class="bar-background">
                            <div
                                class="bar-fill"
                                :style="{
                                    width: item.percentage + '%',
                                    background: item.color
                                }"
                            ></div>
                        </div>
                    </div>

                    <p v-if="!filteredProjects.length" class="empty-message">
                        No project data available.
                    </p>
                </div>

                <!-- Task Status -->
                <div class="report-panel">
                    <div class="panel-heading">
                        <div>
                            <h2>Task Status</h2>
                            <p>Distribution of tasks by status</p>
                        </div>
                    </div>

                    <div
                        v-for="item in taskStatusReport"
                        :key="item.key"
                        class="chart-row"
                    >
                        <div class="chart-label">
                            <span>{{ item.label }}</span>
                            <strong>{{ item.count }}</strong>
                        </div>

                        <div class="bar-background">
                            <div
                                class="bar-fill"
                                :style="{
                                    width: item.percentage + '%',
                                    background: item.color
                                }"
                            ></div>
                        </div>
                    </div>

                    <p v-if="!filteredTasks.length" class="empty-message">
                        No task data available.
                    </p>
                </div>

            </section>

            <!-- Milestones -->
            <section class="report-panel milestone-panel">
                <div class="panel-heading">
                    <div>
                        <h2>Milestone Report</h2>
                        <p>Milestone status summary</p>
                    </div>
                </div>

                <div class="milestone-grid">
                    <div
                        v-for="item in milestoneStatusReport"
                        :key="item.key"
                        class="milestone-item"
                    >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.count }}</strong>
                    </div>
                </div>
            </section>

            <!-- Employee Workload -->
            <section class="report-panel">
                <div class="panel-heading">
                    <div>
                        <h2>Employee Workload</h2>
                        <p>Tasks assigned to each employee</p>
                    </div>

                    <button class="export-button" @click="exportEmployeeReport">
                        ↓ Export CSV
                    </button>
                </div>

                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>Employee</th>
                                <th>Total Tasks</th>
                                <th>Completed</th>
                                <th>Remaining</th>
                                <th>Workload</th>
                            </tr>
                        </thead>

                        <tbody>
                            <tr
                                v-for="employee in employeeWorkload"
                                :key="employee.id"
                            >
                                <td>{{ employee.name }}</td>
                                <td>{{ employee.total }}</td>
                                <td class="success-text">
                                    {{ employee.completed }}
                                </td>
                                <td>{{ employee.pending }}</td>
                                <td class="workload-cell">
                                    <div class="bar-background">
                                        <div
                                            class="bar-fill workload-fill"
                                            :style="{
                                                width:
                                                    (employee.total / maxEmployeeTasks * 100) + '%'
                                            }"
                                        ></div>
                                    </div>
                                </td>
                            </tr>

                            <tr v-if="!employeeWorkload.length">
                                <td colspan="5" class="empty-message">
                                    No assigned employee tasks found.
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            <!-- Project Performance -->
            <section class="report-panel project-panel">
                <div class="panel-heading">
                    <div>
                        <h2>Project Performance</h2>
                        <p>Task completion progress for each project</p>
                    </div>

                    <button class="export-button" @click="exportProjectReport">
                        ↓ Export CSV
                    </button>
                </div>

                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>Project</th>
                                <th>Project Code</th>
                                <th>Status</th>
                                <th>Total Tasks</th>
                                <th>Completed</th>
                                <th>Progress</th>
                            </tr>
                        </thead>

                        <tbody>
                            <tr
                                v-for="project in projectPerformance"
                                :key="project.id"
                            >
                                <td>{{ project.name }}</td>
                                <td>{{ project.code || '—' }}</td>
                                <td>
                                    <span
                                        class="status-badge"
                                        :class="statusClass(project.status)"
                                    >
                                        {{ project.status }}
                                    </span>
                                </td>
                                <td>{{ project.totalTasks }}</td>
                                <td>{{ project.completedTasks }}</td>
                                <td class="progress-cell">
                                    <strong>{{ project.progress }}%</strong>
                                    <div class="bar-background">
                                        <div
                                            class="bar-fill workload-fill"
                                            :style="{ width: project.progress + '%' }"
                                        ></div>
                                    </div>
                                </td>
                            </tr>

                            <tr v-if="!projectPerformance.length">
                                <td colspan="6" class="empty-message">
                                    No projects available.
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            <!-- Export Tasks -->
            <section class="export-panel">
                <div>
                    <h2>Download Reports</h2>
                    <p>Export report data for further analysis.</p>
                </div>

                <div class="export-actions">
                    <button @click="exportTaskReport">
                        ↓ Task Report
                    </button>

                    <button @click="exportProjectReport">
                        ↓ Project Report
                    </button>

                    <button @click="exportEmployeeReport">
                        ↓ Employee Report
                    </button>
                </div>
            </section>

        </template>
    </div>
</template>

<style scoped>
.reports-page {
    min-height: 100vh;
    padding: 28px;
    background: #f5f7fb;
    color: #1e293b;
}

.page-header,
.panel-heading,
.filter-heading {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
}

.page-header {
    margin-bottom: 25px;
}

.eyebrow {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1.5px;
    color: #2563eb;
}

.page-header h1 {
    margin: 8px 0;
    font-size: 28px;
    color: #172554;
}

.page-header p,
.panel-heading p,
.filter-heading p,
.export-panel p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
}

.refresh-button,
.export-button,
.clear-button {
    padding: 10px 15px;
    border: 0;
    border-radius: 7px;
    cursor: pointer;
    font-weight: 600;
}

.refresh-button {
    color: white;
    background: #2563eb;
}

.refresh-button:hover {
    background: #1d4ed8;
}

.filter-panel,
.report-panel,
.summary-card,
.completion-banner,
.export-panel {
    background: white;
    border: 1px solid #e8edf4;
    border-radius: 11px;
    padding: 21px;
    box-shadow: 0 3px 12px rgba(15, 23, 42, .035);
}

.filter-panel {
    margin-bottom: 20px;
}

.filter-heading h3,
.export-panel h2 {
    margin: 0 0 5px;
    color: #172554;
    font-size: 16px;
}

.filter-controls {
    display: flex;
    align-items: end;
    flex-wrap: wrap;
    gap: 15px;
    margin-top: 18px;
}

.filter-controls label {
    display: flex;
    flex-direction: column;
    gap: 7px;
    font-size: 12px;
    color: #475569;
    font-weight: 600;
}

.filter-controls input {
    border: 1px solid #dbe2ea;
    padding: 10px;
    border-radius: 7px;
    color: #334155;
}

.clear-button {
    color: #475569;
    background: #f1f5f9;
}

.summary-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 17px;
    margin-bottom: 20px;
}

.summary-card {
    display: flex;
    align-items: center;
    gap: 15px;
}

.summary-icon {
    width: 47px;
    height: 47px;
    border-radius: 10px;
    display: grid;
    place-items: center;
    font-size: 22px;
    flex-shrink: 0;
}

.summary-icon.blue { background: #eff6ff; }
.summary-icon.purple { background: #f5f3ff; }
.summary-icon.green { background: #ecfdf5; }
.summary-icon.red { background: #fef2f2; }

.summary-card p {
    margin: 0 0 5px;
    color: #64748b;
    font-size: 12px;
}

.summary-card h2 {
    margin: 0;
    font-size: 25px;
    color: #172554;
}

.completion-banner {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 15px;
    align-items: center;
    margin-bottom: 20px;
}

.completion-banner h2 {
    margin: 0 0 5px;
    font-size: 17px;
}

.completion-banner p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
}

.completion-value {
    font-size: 27px;
    font-weight: 700;
    color: #2563eb;
}

.completion-track {
    grid-column: 1 / -1;
    height: 11px;
    border-radius: 20px;
    background: #e2e8f0;
    overflow: hidden;
}

.completion-fill {
    height: 100%;
    border-radius: inherit;
    background: #2563eb;
    transition: width .3s ease;
}

.charts-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
    margin-bottom: 20px;
}

.report-panel {
    margin-bottom: 20px;
    min-width: 0;
}

.charts-grid .report-panel {
    margin-bottom: 0;
}

.panel-heading {
    margin-bottom: 23px;
}

.panel-heading h2 {
    margin: 0 0 6px;
    font-size: 16px;
    color: #172554;
}

.chart-row {
    margin-bottom: 19px;
}

.chart-label {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 13px;
    color: #475569;
}

.bar-background {
    width: 100%;
    height: 9px;
    overflow: hidden;
    background: #f1f5f9;
    border-radius: 20px;
}

.bar-fill {
    height: 100%;
    border-radius: 20px;
    transition: width .3s ease;
}

.milestone-panel {
    margin-bottom: 20px;
}

.milestone-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 13px;
}

.milestone-item {
    background: #f8fafc;
    border: 1px solid #edf1f6;
    border-radius: 8px;
    padding: 17px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    color: #64748b;
    font-size: 12px;
}

.milestone-item strong {
    font-size: 24px;
    color: #172554;
}

.export-button {
    color: #1d4ed8;
    background: #eff6ff;
    white-space: nowrap;
}

.table-wrapper {
    width: 100%;
    overflow-x: auto;
}

table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
    white-space: nowrap;
}

th {
    background: #f8fafc;
    color: #64748b;
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: .4px;
}

th,
td {
    padding: 13px 11px;
    border-bottom: 1px solid #edf1f6;
}

td {
    color: #475569;
    font-size: 12px;
}

.success-text {
    color: #15803d;
    font-weight: 700;
}

.workload-cell {
    min-width: 130px;
}

.workload-fill {
    background: #3b82f6;
}

.progress-cell {
    min-width: 130px;
}

.progress-cell strong {
    display: block;
    margin-bottom: 6px;
    color: #334155;
}

.status-badge {
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
.status-badge.delayed {
    background: #fef3c7;
    color: #b45309;
}

.status-badge.cancelled {
    background: #fee2e2;
    color: #b91c1c;
}

.export-panel {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 20px;
}

.export-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.export-actions button {
    padding: 10px 14px;
    background: #172554;
    color: white;
    border: 0;
    border-radius: 7px;
    cursor: pointer;
    font-size: 12px;
}

.export-actions button:hover {
    background: #1e40af;
}

.empty-message,
.state-message {
    text-align: center;
    padding: 24px;
    color: #94a3b8;
    font-size: 13px;
}

.error-message {
    margin-bottom: 20px;
    padding: 13px;
    background: #fee2e2;
    color: #991b1b;
    border-radius: 8px;
    font-size: 13px;
}

@media (max-width: 1050px) {
    .summary-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 750px) {
    .reports-page {
        padding: 16px;
    }

    .charts-grid {
        grid-template-columns: 1fr;
    }

    .milestone-grid {
        grid-template-columns: repeat(2, 1fr);
    }

    .page-header,
    .export-panel {
        align-items: flex-start;
        flex-direction: column;
    }
}

@media (max-width: 450px) {
    .summary-grid {
        grid-template-columns: 1fr;
    }

    .milestone-grid {
        grid-template-columns: 1fr 1fr;
    }

    .completion-banner {
        grid-template-columns: 1fr auto;
    }
}
</style>
