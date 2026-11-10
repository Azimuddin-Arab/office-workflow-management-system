
<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

function loadUser() {
    try {
        const savedUser = localStorage.getItem('user')
        return savedUser ? JSON.parse(savedUser) : null
    } catch (error) {
        console.error('Invalid user data in localStorage:', error)
        return null
    }
}

const user = ref(loadUser())

const roleName = computed(() =>
    String(user.value?.role || '').toUpperCase()
)

const isAdmin = computed(() =>
    ['ADMIN', 'SUPERADMIN'].includes(roleName.value)
)

// Supports permission codes stored as strings or permission objects.
const permissions = computed(() => {
    const list = user.value?.permissions || []

    return list.map(permission =>
        typeof permission === 'string'
            ? permission
            : permission.code
    ).filter(Boolean)
})

function hasPermission(permission) {
    return isAdmin.value || permissions.value.includes(permission)
}

function hasAnyPermission(permissionList) {
    return isAdmin.value ||
        permissionList.some(permission => hasPermission(permission))
}

function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
}
</script>

<template>
    <div class="app-layout">

        <header class="top-header">
            <div class="brand">
                Project Management System
            </div>

            <div class="user-section">
                <span>{{ user?.fullName || user?.email || 'User' }}</span>
                <span class="role">{{ user?.role || '' }}</span>

                <button class="logout-btn" @click="logout">
                    Logout
                </button>
            </div>
        </header>

        <nav class="main-nav">

            <RouterLink
                v-if="hasPermission('DASHBOARD_VIEW')"
                to="/dashboard"
                class="nav-link"
            >
                Dashboard
            </RouterLink>

            <!-- System Management -->
            <div
                v-if="hasAnyPermission([
                    'USER_VIEW', 'USER_CREATE',
                    'ROLE_VIEW', 'ROLE_CREATE', 'ROLE_UPDATE'
                ])"
                class="nav-dropdown"
            >
                <button class="nav-dropdown-btn">
                    System Management ▾
                </button>

                <div class="dropdown-content">
                    <RouterLink v-if="hasPermission('USER_VIEW')" to="/system/users">
                        Users List
                    </RouterLink>

                    <RouterLink v-if="hasPermission('USER_CREATE')" to="/system/users/create">
                        Create User
                    </RouterLink>

                    <RouterLink v-if="hasPermission('ROLE_VIEW')" to="/system/roles">
                        Roles Management
                    </RouterLink>

                    <RouterLink v-if="hasPermission('ROLE_CREATE')" to="/system/roles/create">
                        Create Role
                    </RouterLink>
                </div>
            </div>

            <!-- Employee Management -->
            <div
                v-if="hasAnyPermission(['EMPLOYEE_VIEW', 'DEPARTMENT_VIEW'])"
                class="nav-dropdown"
            >
                <button class="nav-dropdown-btn">
                    Employee Management ▾
                </button>

                <div class="dropdown-content">
                    <RouterLink v-if="hasPermission('EMPLOYEE_VIEW')" to="/employees">
                        Employees
                    </RouterLink>

                    <RouterLink v-if="hasPermission('DEPARTMENT_VIEW')" to="/departments">
                        Departments
                    </RouterLink>
                </div>
            </div>

            <!-- Project Management -->
            <div
                v-if="hasAnyPermission([
                    'PROJECT_VIEW', 'PROJECT_TEAM_VIEW',
                    'TASK_VIEW', 'MILESTONE_VIEW',
                    'CALENDAR_VIEW', 'REPORT_VIEW'
                ])"
                class="nav-dropdown"
            >
                <button class="nav-dropdown-btn">
                    Project Management ▾
                </button>

                <div class="dropdown-content">
                    <RouterLink v-if="hasPermission('PROJECT_VIEW')" to="/projects">
                        Projects
                    </RouterLink>

                    <RouterLink v-if="hasPermission('PROJECT_TEAM_VIEW')" to="/project-teams">
                        Project Teams
                    </RouterLink>

                    <RouterLink v-if="hasPermission('TASK_VIEW')" to="/tasks">
                        Tasks
                    </RouterLink>

                    <RouterLink v-if="hasPermission('MILESTONE_VIEW')" to="/milestones">
                        Milestones
                    </RouterLink>

                    <RouterLink v-if="hasPermission('CALENDAR_VIEW')" to="/calendar">
                        Project Calendar
                    </RouterLink>

                    <RouterLink v-if="hasPermission('REPORT_VIEW')" to="/reports">
                        Reports & Analytics
                    </RouterLink>
                </div>
            </div>

        </nav>

        <main class="page-content">
            <RouterView />
        </main>

    </div>
</template>

<style scoped>
* {
    box-sizing: border-box;
}

.app-layout {
    min-height: 100vh;
    background: #f1f5f9;
    font-family: Arial, sans-serif;
}

.top-header {
    background: #17365d;
    color: white;
    padding: 18px 30px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.brand {
    font-size: 21px;
    font-weight: bold;
}

.user-section {
    display: flex;
    align-items: center;
    gap: 15px;
}

.role {
    background: #315783;
    padding: 5px 10px;
    border-radius: 15px;
    font-size: 12px;
}

.logout-btn {
    background: #dc3545;
    color: white;
    border: none;
    padding: 9px 15px;
    border-radius: 5px;
    cursor: pointer;
}

.main-nav {
    background: white;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 30px;
    border-bottom: 1px solid #ddd;
}

.nav-link,
.nav-dropdown-btn {
    display: block;
    padding: 17px 15px;
    color: #334155;
    text-decoration: none;
    font-size: 14px;
    background: none;
    border: none;
    cursor: pointer;
    font-weight: 600;
}

.nav-link.router-link-active {
    color: #2563eb;
    border-bottom: 3px solid #2563eb;
}

.nav-dropdown {
    position: relative;
}

.nav-dropdown:hover .dropdown-content,
.nav-dropdown:focus-within .dropdown-content {
    display: block;
}

.nav-dropdown-btn:hover {
    color: #2563eb;
}

.dropdown-content {
    display: none;
    position: absolute;
    top: 100%;
    left: 0;
    min-width: 190px;
    background: white;
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.12);
    z-index: 10;
    border-radius: 5px;
}

.dropdown-content a {
    display: block;
    padding: 13px 16px;
    color: #334155;
    text-decoration: none;
}

.dropdown-content a:hover {
    background: #eff6ff;
    color: #2563eb;
}

.page-content {
    padding: 30px;
}

@media (max-width: 650px) {
    .top-header {
        align-items: flex-start;
        gap: 15px;
        flex-direction: column;
    }

    .main-nav {
        padding: 0 8px;
        gap: 0;
        flex-wrap: wrap;
    }

    .nav-link,
    .nav-dropdown-btn {
        padding: 14px 9px;
        font-size: 12px;
    }

    .page-content {
        padding: 15px;
    }
}
</style>
