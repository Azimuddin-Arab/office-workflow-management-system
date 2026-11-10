
import { createRouter, createWebHistory } from 'vue-router'

import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import AppLayout from '../layouts/AppLayout.vue'

import EmployeesView from '../views/EmployeesView.vue'
import DepartmentsView from '../views/DepartmentsView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import ProjectTeamsView from '../views/ProjectTeamsView.vue'
import TasksView from '../views/TasksView.vue'
import MilestonesView from '../views/MilestonesView.vue'
import CalendarView from '../views/CalendarView.vue'
import ReportsView from '../views/ReportsView.vue'

import UsersView from '../views/UsersView.vue'
import CreateUserView from '../views/CreateUserView.vue'

import RolesView from '../views/RolesView.vue'
import CreateRoleView from '../views/CreateRoleView.vue'
import EditRoleView from '../views/EditRoleView.vue'

const routes = [
    {
        path: '/login',
        component: LoginView
    },
    {
        path: '/register',
        component: RegisterView
    },
    {
        path: '/',
        component: AppLayout,
        children: [
            {
                path: '',
                redirect: '/dashboard'
            },
            {
                path: 'dashboard',
                component: () => import('../views/DashboardView.vue'),
                meta: { permission: 'DASHBOARD_VIEW' }
            },

            // Employee Management
            {
                path: 'employees',
                component: EmployeesView,
                meta: { permission: 'EMPLOYEE_VIEW' }
            },
            {
                path: 'departments',
                component: DepartmentsView,
                meta: { permission: 'DEPARTMENT_VIEW' }
            },

            // Project Management
            {
                path: 'projects',
                component: ProjectsView,
                meta: { permission: 'PROJECT_VIEW' }
            },
            {
                path: 'project-teams',
                component: ProjectTeamsView,
                meta: { permission: 'PROJECT_TEAM_VIEW' }
            },
            {
                path: 'tasks',
                component: TasksView,
                meta: { permission: 'TASK_VIEW' }
            },
            {
                path: 'milestones',
                component: MilestonesView,
                meta: { permission: 'MILESTONE_VIEW' }
            },
            {
                path: 'calendar',
                name: 'Calendar',
                component: CalendarView,
                meta: { permission: 'CALENDAR_VIEW' }
            },
            {
                path: 'reports',
                name: 'Reports',
                component: ReportsView,
                meta: { permission: 'REPORT_VIEW' }
            },

            // User Management
            {
                path: 'system/users',
                name: 'Users',
                component: UsersView,
                meta: { permission: 'USER_VIEW' }
            },
            {
                path: 'system/users/create',
                name: 'CreateUser',
                component: CreateUserView,
                meta: { permission: 'USER_CREATE' }
            },

            // Role Management
            {
                path: 'system/roles',
                name: 'Roles',
                component: RolesView,
                meta: { permission: 'ROLE_VIEW' }
            },
            {
                path: 'system/roles/create',
                name: 'CreateRole',
                component: CreateRoleView,
                meta: { permission: 'ROLE_CREATE' }
            },
            {
                path: 'system/roles/edit/:id',
                name: 'EditRole',
                component: EditRoleView,
                meta: { permission: 'ROLE_UPDATE' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to) => {
    const token = localStorage.getItem('token')

    let user = null

    try {
        user = JSON.parse(localStorage.getItem('user') || 'null')
    } catch {
        localStorage.removeItem('user')
    }

    const isAuthenticated = Boolean(token && user)
    const isPublicPage = ['/login', '/register'].includes(to.path)

    // Not logged in: allow only login and register
    if (!isAuthenticated && !isPublicPage) {
        return '/login'
    }

    // Already logged in: do not show login/register
    if (isAuthenticated && isPublicPage) {
        return '/dashboard'
    }

    // Public pages
    if (isPublicPage) {
        return true
    }

    // Admin has access to every frontend route
    const role = String(user.role || '').toUpperCase()

    if (['ADMIN', 'SUPERADMIN'].includes(role)) {
        return true
    }

    // Check required permission
    const requiredPermission = to.meta.permission

    if (!requiredPermission) {
        return true
    }

    const permissions = (user.permissions || []).map(permission =>
        typeof permission === 'string'
            ? permission
            : permission.code
    )

    if (permissions.includes(requiredPermission)) {
        return true
    }

    // Access denied
    return '/dashboard'
})

export default router
