import { createBrowserRouter, type RouteObject } from 'react-router'
import {
  HomeRedirect,
  RequireAuthentication,
  RequireWorkspace,
} from '@/features/auth/components/AuthGuards'
import { AccessUnavailablePage } from '@/features/auth/pages/AccessUnavailablePage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { PlatformDashboardPage } from '@/features/dashboard/pages/PlatformDashboardPage'
import { TenantDashboardPage } from '@/features/dashboard/pages/TenantDashboardPage'
import { InvitationAcceptPage } from '@/features/invitations/pages/InvitationAcceptPage'
import { PlatformInvitationsPage } from '@/features/invitations/pages/PlatformInvitationsPage'
import { NotFoundPage } from '@/features/not-found/NotFoundPage'
import { PlatformTenantDetailPage } from '@/features/tenants/pages/PlatformTenantDetailPage'
import { PlatformTenantDirectoryPage } from '@/features/tenants/pages/PlatformTenantDirectoryPage'
import { PlatformShell } from '@/shared/layouts/PlatformShell'
import { PublicLayout } from '@/shared/layouts/PublicLayout'
import { TenantShell } from '@/shared/layouts/TenantShell'

export const routeDefinitions: RouteObject[] = [
  { path: '/', element: <HomeRedirect /> },
  { path: '/dashboard', element: <HomeRedirect /> },
  {
    element: <RequireWorkspace workspace="PLATFORM" />,
    children: [
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
          {
            path: '/platform/tenants',
            element: <PlatformTenantDirectoryPage />,
          },
          {
            path: '/platform/tenants/:organizationId',
            element: <PlatformTenantDetailPage />,
          },
        ],
      },
    ],
  },
  {
    element: <RequireWorkspace workspace="TENANT" />,
    children: [
      {
        element: <TenantShell />,
        children: [
          { path: '/app/dashboard', element: <TenantDashboardPage /> },
        ],
      },
    ],
  },
  {
    element: <RequireAuthentication />,
    children: [
      {
        element: <PublicLayout />,
        children: [
          {
            path: '/access-unavailable',
            element: <AccessUnavailablePage />,
          },
        ],
      },
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
