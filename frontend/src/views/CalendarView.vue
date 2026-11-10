
<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '../api/api';

// ==========================================
// STATE
// ==========================================

const projects = ref([]);
const events = ref([]);
const tasks = ref([]);
const milestones = ref([]);

const loading = ref(false);
const saving = ref(false);

const errorMessage = ref('');
const successMessage = ref('');

const showForm = ref(false);
const selectedEventId = ref(null);
const selectedProject = ref('');

const selectedDate = ref(getDateString(new Date()));

const currentDate = ref(new Date());

const form = ref(emptyForm());

// ==========================================
// FORM
// ==========================================

function emptyForm() {
    return {
        title: '',
        description: '',
        projectId: '',
        startDateTime: '',
        endDateTime: '',
        eventType: 'MEETING',
        status: 'SCHEDULED'
    };
}

// ==========================================
// DATE HELPERS
// ==========================================

function getDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

function formatDateTime(dateTime) {
    if (!dateTime) return '';

    return dateTime.replace('T', ' ').substring(0, 16);
}

function toLocalDateTime(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// ==========================================
// CALENDAR MONTH
// ==========================================

const currentMonth = computed(() =>
    currentDate.value.getMonth()
);

const currentYear = computed(() =>
    currentDate.value.getFullYear()
);

const monthTitle = computed(() =>
    currentDate.value.toLocaleDateString('en-US', {
        month: 'long',
        year: 'numeric'
    })
);

const calendarDays = computed(() => {

    const firstDay = new Date(
        currentYear.value,
        currentMonth.value,
        1
    );

    const lastDay = new Date(
        currentYear.value,
        currentMonth.value + 1,
        0
    );

    const days = [];

    const startingDay = firstDay.getDay();

    // Previous month
    for (let i = 0; i < startingDay; i++) {

        const date = new Date(
            currentYear.value,
            currentMonth.value,
            -startingDay + i + 1
        );

        days.push({
            date: getDateString(date),
            day: date.getDate(),
            currentMonth: false
        });
    }

    // Current month
    for (let day = 1; day <= lastDay.getDate(); day++) {

        const date = new Date(
            currentYear.value,
            currentMonth.value,
            day
        );

        days.push({
            date: getDateString(date),
            day,
            currentMonth: true
        });
    }

    // Next month
    while (days.length < 42) {

        const date = new Date(
            currentYear.value,
            currentMonth.value + 1,
            days.length - startingDay - lastDay.getDate() + 1
        );

        days.push({
            date: getDateString(date),
            day: date.getDate(),
            currentMonth: false
        });
    }

    return days;
});

// ==========================================
// NAVIGATION
// ==========================================

function previousMonth() {
    currentDate.value = new Date(
        currentYear.value,
        currentMonth.value - 1,
        1
    );
}

function nextMonth() {
    currentDate.value = new Date(
        currentYear.value,
        currentMonth.value + 1,
        1
    );
}

function goToToday() {
    const today = new Date();

    currentDate.value = today;
    selectedDate.value = getDateString(today);
}

// ==========================================
// PROJECTS
// ==========================================

const getProjectName = (project) => {
    return project.projectName ||
        project.name ||
        project.title ||
        `Project #${project.id}`;
};

async function loadProjects() {

    try {

        const response = await api.get('/projects');

        const data = response.data;

        projects.value = Array.isArray(data)
            ? data
            : Array.isArray(data.content)
                ? data.content
                : [];

    } catch (error) {

        console.error('Failed to load projects:', error);

        errorMessage.value =
            'Unable to load projects. Check your Projects API.';
    }
}

// ==========================================
// LOAD CALENDAR DATA
// ==========================================

async function loadCalendarData() {

    loading.value = true;
    errorMessage.value = '';

    const results = await Promise.allSettled([
        api.get('/project-events'),
        api.get('/tasks'),
        api.get('/milestones')
    ]);

    if (results[0].status === 'fulfilled') {
        events.value = Array.isArray(results[0].value.data)
            ? results[0].value.data
            : [];
    } else {
        console.error('Events loading failed:', results[0].reason);
        errorMessage.value = 'Unable to load project events.';
    }

    if (results[1].status === 'fulfilled') {
        tasks.value = Array.isArray(results[1].value.data)
            ? results[1].value.data
            : [];
    } else {
        console.error('Tasks loading failed:', results[1].reason);
    }

    if (results[2].status === 'fulfilled') {
        milestones.value = Array.isArray(results[2].value.data)
            ? results[2].value.data
            : [];
    } else {
        console.error('Milestones loading failed:', results[2].reason);
    }

    loading.value = false;
}

// ==========================================
// NORMALIZE CALENDAR ITEMS
// ==========================================

const calendarItems = computed(() => {

    const items = [];

    // Project events
    events.value.forEach(event => {

        const date = event.startDateTime?.substring(0, 10);

        if (!date) return;

        items.push({
            id: `event-${event.id}`,
            originalId: event.id,
            title: event.title,
            date,
            time: event.startDateTime?.substring(11, 16) || '',
            type: 'EVENT',
            status: event.status,
            projectId: event.projectId,
            projectName: event.projectName,
            description: event.description
        });
    });

    // Tasks: display their due dates
    tasks.value.forEach(task => {

        const date = task.dueDate;

        if (!date) return;

        items.push({
            id: `task-${task.id}`,
            originalId: task.id,
            title: task.title,
            date: date.substring(0, 10),
            time: '',
            type: 'TASK',
            status: task.status,
            projectId: task.projectId,
            projectName: task.projectName,
            description: task.description
        });
    });

    // Milestones: display their due dates
    milestones.value.forEach(milestone => {

        const date = milestone.dueDate;

        if (!date) return;

        items.push({
            id: `milestone-${milestone.id}`,
            originalId: milestone.id,
            title: milestone.title,
            date: date.substring(0, 10),
            time: '',
            type: 'MILESTONE',
            status: milestone.status,
            projectId: milestone.projectId,
            projectName: milestone.projectName,
            description: milestone.description
        });
    });

    return items.filter(item =>
        !selectedProject.value ||
        String(item.projectId) === String(selectedProject.value)
    );
});

// ==========================================
// CALENDAR DAY ITEMS
// ==========================================

function getItemsForDate(date) {

    return calendarItems.value
        .filter(item => item.date === date)
        .sort((a, b) => a.time.localeCompare(b.time));
}

const selectedDayItems = computed(() =>
    getItemsForDate(selectedDate.value)
);

// ==========================================
// STATISTICS
// ==========================================

const totalEvents = computed(() =>
    events.value.length
);

const totalTasks = computed(() =>
    tasks.value.length
);

const totalMilestones = computed(() =>
    milestones.value.length
);

// ==========================================
// EVENT STYLE
// ==========================================

function itemClass(type) {

    return {
        EVENT: 'event-item',
        TASK: 'task-item',
        MILESTONE: 'milestone-item'
    }[type] || '';
}

function itemTypeLabel(type) {

    return {
        EVENT: 'Event',
        TASK: 'Task',
        MILESTONE: 'Milestone'
    }[type] || type;
}

// ==========================================
// OPEN CREATE FORM
// ==========================================

function openCreateForm() {

    selectedEventId.value = null;

    form.value = emptyForm();

    if (selectedProject.value) {
        form.value.projectId = Number(selectedProject.value);
    }

    const date = selectedDate.value || getDateString(new Date());

    form.value.startDateTime = `${date}T09:00`;
    form.value.endDateTime = `${date}T10:00`;

    errorMessage.value = '';
    showForm.value = true;
}

// ==========================================
// EDIT EVENT
// ==========================================

function editEvent(item) {

    if (item.type !== 'EVENT') return;

    const event = events.value.find(
        e => e.id === item.originalId
    );

    if (!event) return;

    selectedEventId.value = event.id;

    form.value = {
        title: event.title || '',
        description: event.description || '',
        projectId: event.projectId || '',
        startDateTime: event.startDateTime || '',
        endDateTime: event.endDateTime || '',
        eventType: event.eventType || 'OTHER',
        status: event.status || 'SCHEDULED'
    };

    showForm.value = true;
}

// ==========================================
// SAVE EVENT
// ==========================================

async function saveEvent() {

    errorMessage.value = '';

    if (!form.value.title.trim()) {
        errorMessage.value = 'Event title is required.';
        return;
    }

    if (!form.value.projectId) {
        errorMessage.value = 'Please select a project.';
        return;
    }

    if (!form.value.startDateTime || !form.value.endDateTime) {
        errorMessage.value = 'Start and end dates are required.';
        return;
    }

    if (
        new Date(form.value.endDateTime) <=
        new Date(form.value.startDateTime)
    ) {
        errorMessage.value =
            'End date and time must be after the start date and time.';
        return;
    }

    saving.value = true;

    try {

        const payload = {
            title: form.value.title.trim(),
            description: form.value.description,
            projectId: Number(form.value.projectId),
            startDateTime: form.value.startDateTime,
            endDateTime: form.value.endDateTime,
            eventType: form.value.eventType,
            status: form.value.status
        };

        if (selectedEventId.value) {

            await api.put(
                `/project-events/${selectedEventId.value}`,
                payload
            );

            successMessage.value = 'Event updated successfully.';

        } else {

            await api.post('/project-events', payload);

            successMessage.value = 'Event created successfully.';
        }

        showForm.value = false;

        selectedEventId.value = null;
        form.value = emptyForm();

        await loadCalendarData();

    } catch (error) {

        console.error('Save event error:', error);

        errorMessage.value =
            typeof error.response?.data === 'string'
                ? error.response.data
                : 'Unable to save event.';

    } finally {

        saving.value = false;
    }
}

// ==========================================
// DELETE EVENT
// ==========================================

async function deleteEvent(id) {

    if (!confirm('Are you sure you want to delete this event?')) {
        return;
    }

    try {

        await api.delete(`/project-events/${id}`);

        successMessage.value = 'Event deleted successfully.';

        await loadCalendarData();

    } catch (error) {

        console.error('Delete event error:', error);

        errorMessage.value = 'Unable to delete event.';
    }
}

// ==========================================
// SELECT CALENDAR DAY
// ==========================================

function selectDay(day) {

    selectedDate.value = day.date;
}

// ==========================================
// INITIALIZE
// ==========================================

onMounted(async () => {

    await Promise.all([
        loadProjects(),
        loadCalendarData()
    ]);

});
</script>

<template>
    <div class="calendar-page">

        <!-- PAGE HEADER -->

        <div class="page-header">

            <div>
                <h1>Project Calendar</h1>
                <p>Manage project events, tasks and milestones.</p>
            </div>

            <button
                class="primary-btn"
                @click="openCreateForm"
            >
                + Create Event
            </button>

        </div>

        <!-- ALERTS -->

        <div
            v-if="errorMessage"
            class="alert error-alert"
        >
            {{ errorMessage }}
        </div>

        <div
            v-if="successMessage"
            class="alert success-alert"
        >
            {{ successMessage }}
        </div>

        <!-- STATISTICS -->

        <div class="stats-grid">

            <div class="stat-card">
                <span>Project Events</span>
                <h2>{{ totalEvents }}</h2>
            </div>

            <div class="stat-card">
                <span>Tasks</span>
                <h2>{{ totalTasks }}</h2>
            </div>

            <div class="stat-card">
                <span>Milestones</span>
                <h2>{{ totalMilestones }}</h2>
            </div>

        </div>

        <!-- EVENT FORM -->

        <div
            v-if="showForm"
            class="form-card"
        >

            <div class="form-header">

                <h2>
                    {{ selectedEventId ? 'Edit Event' : 'Create Event' }}
                </h2>

                <button
                    class="close-btn"
                    type="button"
                    @click="showForm = false"
                >
                    ✕
                </button>

            </div>

            <form @submit.prevent="saveEvent">

                <div class="form-grid">

                    <div class="form-group">

                        <label>Event Title *</label>

                        <input
                            v-model="form.title"
                            type="text"
                            maxlength="200"
                            placeholder="Enter event title"
                            required
                        />

                    </div>

                    <div class="form-group">

                        <label>Project *</label>

                        <select
                            v-model="form.projectId"
                            required
                        >

                            <option value="">
                                Select Project
                            </option>

                            <option
                                v-for="project in projects"
                                :key="project.id"
                                :value="project.id"
                            >
                                {{ getProjectName(project) }}
                            </option>

                        </select>

                    </div>

                    <div class="form-group">

                        <label>Start Date and Time *</label>

                        <input
                            v-model="form.startDateTime"
                            type="datetime-local"
                            required
                        />

                    </div>

                    <div class="form-group">

                        <label>End Date and Time *</label>

                        <input
                            v-model="form.endDateTime"
                            type="datetime-local"
                            required
                        />

                    </div>

                    <div class="form-group">

                        <label>Event Type</label>

                        <select v-model="form.eventType">

                            <option value="MEETING">Meeting</option>
                            <option value="REVIEW">Review</option>
                            <option value="PRESENTATION">Presentation</option>
                            <option value="DEADLINE">Deadline</option>
                            <option value="OTHER">Other</option>

                        </select>

                    </div>

                    <div class="form-group">

                        <label>Status</label>

                        <select v-model="form.status">

                            <option value="SCHEDULED">Scheduled</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="CANCELLED">Cancelled</option>

                        </select>

                    </div>

                    <div class="form-group full-width">

                        <label>Description</label>

                        <textarea
                            v-model="form.description"
                            rows="3"
                            maxlength="2000"
                            placeholder="Enter event description"
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
                        {{ saving ? 'Saving...' : 'Save Event' }}
                    </button>

                </div>

            </form>

        </div>

        <!-- CALENDAR TOOLBAR -->

        <div class="calendar-toolbar">

            <div class="month-navigation">

                <button
                    class="navigation-btn"
                    @click="previousMonth"
                >
                    ‹
                </button>

                <h2>{{ monthTitle }}</h2>

                <button
                    class="navigation-btn"
                    @click="nextMonth"
                >
                    ›
                </button>

                <button
                    class="today-btn"
                    @click="goToToday"
                >
                    Today
                </button>

            </div>

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

        </div>

        <!-- CALENDAR LEGEND -->

        <div class="calendar-legend">

            <span>
                <i class="legend-dot event-dot"></i>
                Events
            </span>

            <span>
                <i class="legend-dot task-dot"></i>
                Tasks
            </span>

            <span>
                <i class="legend-dot milestone-dot"></i>
                Milestones
            </span>

        </div>

        <!-- CALENDAR -->

        <div class="calendar-card">

            <div
                v-if="loading"
                class="loading-message"
            >
                Loading calendar...
            </div>

            <div v-else>

                <div class="calendar-grid">

                    <!-- WEEKDAYS -->

                    <div
                        v-for="weekday in [
                            'Sun', 'Mon', 'Tue',
                            'Wed', 'Thu', 'Fri', 'Sat'
                        ]"
                        :key="weekday"
                        class="weekday"
                    >
                        {{ weekday }}
                    </div>

                    <!-- DAYS -->

                    <div
                        v-for="day in calendarDays"
                        :key="day.date"
                        :class="[
                            'calendar-day',
                            {
                                'other-month': !day.currentMonth,
                                'selected-day': selectedDate === day.date,
                                'today-day': day.date === getDateString(new Date())
                            }
                        ]"
                        @click="selectDay(day)"
                    >

                        <div class="day-number">
                            {{ day.day }}
                        </div>

                        <div class="day-items">

                            <div
                                v-for="item in getItemsForDate(day.date).slice(0, 3)"
                                :key="item.id"
                                :class="[
                                    'calendar-item',
                                    itemClass(item.type)
                                ]"
                                :title="item.title"
                            >
                                {{ item.time ? item.time + ' ' : '' }}{{ item.title }}
                            </div>

                            <div
                                v-if="getItemsForDate(day.date).length > 3"
                                class="more-items"
                            >
                                +{{ getItemsForDate(day.date).length - 3 }} more
                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </div>

        <!-- SELECTED DAY DETAILS -->

        <div class="selected-day-card">

            <div class="selected-day-header">

                <div>
                    <h2>Calendar Details</h2>

                    <p>
                        {{ selectedDate }}
                    </p>
                </div>

                <button
                    class="primary-btn"
                    @click="openCreateForm"
                >
                    + Add Event
                </button>

            </div>

            <div
                v-if="selectedDayItems.length === 0"
                class="empty-message"
            >
                No activities scheduled for this day.
            </div>

            <div
                v-else
                class="activity-list"
            >

                <div
                    v-for="item in selectedDayItems"
                    :key="item.id"
                    class="activity-item"
                >

                    <div
                        :class="[
                            'activity-indicator',
                            itemClass(item.type)
                        ]"
                    ></div>

                    <div class="activity-content">

                        <h3>{{ item.title }}</h3>

                        <p>
                            {{ item.projectName || 'No project name' }}
                        </p>

                        <div class="activity-meta">

                            <span>
                                {{ itemTypeLabel(item.type) }}
                            </span>

                            <span v-if="item.time">
                                {{ item.time }}
                            </span>

                            <span>
                                {{ item.status || '-' }}
                            </span>

                        </div>

                        <p
                            v-if="item.description"
                            class="activity-description"
                        >
                            {{ item.description }}
                        </p>

                    </div>

                    <div
                        v-if="item.type === 'EVENT'"
                        class="activity-actions"
                    >

                        <button
                            class="edit-btn"
                            @click="editEvent(item)"
                        >
                            Edit
                        </button>

                        <button
                            class="delete-btn"
                            @click="deleteEvent(item.originalId)"
                        >
                            Delete
                        </button>

                    </div>

                </div>

            </div>

        </div>

    </div>
</template>

<style scoped>
.calendar-page {
    padding: 24px;
    color: #1e293b;
}

.page-header,
.selected-day-header,
.calendar-toolbar,
.month-navigation,
.form-header,
.form-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.page-header {
    margin-bottom: 22px;
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
.today-btn,
.navigation-btn,
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
    opacity: .6;
    cursor: not-allowed;
}

.secondary-btn {
    background: #e2e8f0;
    color: #334155;
}

.alert {
    padding: 12px 15px;
    margin-bottom: 15px;
    border-radius: 6px;
}

.error-alert {
    background: #fee2e2;
    color: #991b1b;
}

.success-alert {
    background: #dcfce7;
    color: #166534;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
    margin-bottom: 20px;
}

.stat-card,
.form-card,
.calendar-card,
.selected-day-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 18px;
}

.stat-card span {
    color: #64748b;
}

.stat-card h2 {
    margin: 10px 0 0;
    font-size: 27px;
}

.form-card {
    margin-bottom: 20px;
}

.form-header {
    margin-bottom: 18px;
}

.form-header h2,
.selected-day-header h2 {
    margin: 0;
    font-size: 19px;
}

.close-btn {
    background: transparent;
    border: none;
    font-size: 20px;
    cursor: pointer;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
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
.calendar-toolbar select {
    box-sizing: border-box;
    width: 100%;
    padding: 10px;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    font: inherit;
}

.full-width {
    grid-column: 1 / -1;
}

.form-actions {
    justify-content: flex-end;
    margin-top: 20px;
}

.calendar-toolbar {
    margin: 20px 0 15px;
}

.month-navigation {
    justify-content: flex-start;
}

.month-navigation h2 {
    min-width: 170px;
    text-align: center;
    font-size: 20px;
}

.navigation-btn {
    background: #e2e8f0;
    font-size: 22px;
    padding: 5px 14px;
}

.today-btn {
    background: #eff6ff;
    color: #1d4ed8;
}

.calendar-toolbar select {
    max-width: 260px;
}

.calendar-legend {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    margin-bottom: 12px;
    font-size: 13px;
}

.calendar-legend span {
    display: flex;
    align-items: center;
    gap: 7px;
}

.legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
}

.event-dot {
    background: #2563eb;
}

.task-dot {
    background: #16a34a;
}

.milestone-dot {
    background: #d97706;
}

.calendar-card {
    padding: 0;
    overflow: hidden;
}

.calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
}

.weekday {
    background: #f8fafc;
    text-align: center;
    padding: 13px 5px;
    font-weight: 700;
    border-bottom: 1px solid #e2e8f0;
}

.calendar-day {
    min-height: 115px;
    padding: 8px;
    border-right: 1px solid #e2e8f0;
    border-bottom: 1px solid #e2e8f0;
    cursor: pointer;
    overflow: hidden;
}

.calendar-day:hover {
    background: #f8fafc;
}

.other-month {
    background: #f8fafc;
    color: #94a3b8;
}

.selected-day {
    background: #eff6ff;
    outline: 2px solid #2563eb;
    outline-offset: -2px;
}

.today-day .day-number {
    background: #2563eb;
    color: white;
    border-radius: 50%;
}

.day-number {
    width: 27px;
    height: 27px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 7px;
    font-size: 13px;
    font-weight: 600;
}

.day-items {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.calendar-item {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 4px 6px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
}

.event-item {
    background: #dbeafe;
    color: #1d4ed8;
}

.task-item {
    background: #dcfce7;
    color: #166534;
}

.milestone-item {
    background: #fef3c7;
    color: #92400e;
}

.more-items {
    color: #64748b;
    font-size: 11px;
    padding: 2px 5px;
}

.selected-day-card {
    margin-top: 20px;
}

.selected-day-header {
    margin-bottom: 15px;
}

.selected-day-header p {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 13px;
}

.empty-message,
.loading-message {
    padding: 25px;
    text-align: center;
    color: #64748b;
}

.activity-list {
    display: flex;
    flex-direction: column;
}

.activity-item {
    display: flex;
    align-items: flex-start;
    gap: 13px;
    padding: 15px 0;
    border-top: 1px solid #e2e8f0;
}

.activity-indicator {
    width: 5px;
    min-width: 5px;
    align-self: stretch;
    border-radius: 5px;
}

.activity-content {
    flex: 1;
    min-width: 0;
}

.activity-content h3 {
    margin: 0 0 5px;
    font-size: 15px;
}

.activity-content p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
}

.activity-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 8px;
    color: #475569;
    font-size: 12px;
}

.activity-description {
    margin-top: 8px !important;
}

.activity-actions {
    display: flex;
    gap: 7px;
}

.edit-btn,
.delete-btn {
    padding: 7px 10px;
    font-size: 12px;
}

.edit-btn {
    background: #dbeafe;
    color: #1d4ed8;
}

.delete-btn {
    background: #fee2e2;
    color: #b91c1c;
}

@media (max-width: 800px) {
    .calendar-day {
        min-height: 90px;
        padding: 4px;
    }

    .calendar-item {
        font-size: 9px;
    }
}

@media (max-width: 600px) {
    .calendar-page {
        padding: 12px;
    }

    .page-header {
        align-items: flex-start;
        flex-direction: column;
    }

    .stats-grid {
        grid-template-columns: 1fr;
    }

    .form-grid {
        grid-template-columns: 1fr;
    }

    .full-width {
        grid-column: auto;
    }

    .calendar-toolbar {
        align-items: stretch;
        flex-direction: column;
    }

    .calendar-toolbar select {
        max-width: none;
    }

    .calendar-day {
        min-height: 75px;
    }

    .calendar-item {
        font-size: 8px;
        padding: 3px;
    }

    .weekday {
        font-size: 11px;
    }

    .activity-item {
        flex-wrap: wrap;
    }

    .activity-actions {
        width: 100%;
        padding-left: 18px;
    }
}
</style>
