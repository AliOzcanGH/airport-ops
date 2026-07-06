import {
  createBrowserRouter,
  Navigate,
  type RouteObject,
} from 'react-router'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { PlatformDashboardPage } from '@/features/dashboard/pages/PlatformDashboardPage'
import { TenantDashboardPage } from '@/features/dashboard/pages/TenantDashboardPage'
import { InvitationAcceptPage } from '@/features/invitations/pages/InvitationAcceptPage'
import { PlatformInvitationsPage } from '@/features/invitations/pages/PlatformInvitationsPage'
import { NotFoundPage } from '@/features/not-found/NotFoundPage'
import { PlatformShell } from '@/shared/layouts/PlatformShell'
import { PublicLayout } from '@/shared/layouts/PublicLayout'
import { TenantShell } from '@/shared/layouts/TenantShell'

export const routeDefinitions: RouteObject[] = [
  { path: '/', element: <Navigate to="/platform/dashboard" replace /> },
  {
    path: '/dashboard',
    element: <Navigate to="/platform/dashboard" replace />,
  },
  {
    element: <PlatformShell />,
    children: [
      {
        path: '/platform/dashboard',
        element: <PlatformDashboardPage />,
      },
      {
        path: '/platform/invitations/new',
        element: <PlatformInvitationsPage />,
      },
    ],
  },
  {
    element: <TenantShell />,
    children: [
      { path: '/app/dashboard', element: <TenantDashboardPage /> },
    ],
  },
  {
    element: <PublicLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/invitations/accept', element: <InvitationAcceptPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routeDefinitions)
