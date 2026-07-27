import { createBrowserRouter, type RouteObject } from 'react-router'
import {
  HomeRedirect,
  RequireAuthentication,
  RequireTenantRole,
  RequireTenantSetupComplete,
  RequireTenantSetupPage,
  RequireWorkspace,
} from '@/features/auth/components/AuthGuards'
import { AccessUnavailablePage } from '@/features/auth/pages/AccessUnavailablePage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { PlatformDashboardPage } from '@/features/dashboard/pages/PlatformDashboardPage'
import { TenantDashboardPage } from '@/features/dashboard/pages/TenantDashboardPage'
import { InvitationAcceptPage } from '@/features/invitations/pages/InvitationAcceptPage'
import { PlatformInvitationsPage } from '@/features/invitations/pages/PlatformInvitationsPage'
import { TenantMembersPage } from '@/features/members/pages/TenantMembersPage'
import { NotFoundPage } from '@/features/not-found/NotFoundPage'
import { PlatformTenantDetailPage } from '@/features/tenants/pages/PlatformTenantDetailPage'
import { PlatformTenantDirectoryPage } from '@/features/tenants/pages/PlatformTenantDirectoryPage'
import { AppSetupPage } from '@/features/setup/pages/AppSetupPage'
import { PlatformShell } from '@/shared/layouts/PlatformShell'
import { PublicLayout } from '@/shared/layouts/PublicLayout'
import { TenantSetupShell } from '@/shared/layouts/TenantSetupShell'
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
          {
            element: <RequireTenantSetupComplete />,
            children: [
              { path: '/app/dashboard', element: <TenantDashboardPage /> },
              {
                element: <RequireTenantRole role="AIRLINE_ADMIN" />,
                children: [
                  { path: '/app/members', element: <TenantMembersPage /> },
                ],
              },
            ],
          },
        ],
      },
      {
        element: <RequireTenantSetupPage />,
        children: [
          {
            element: <TenantSetupShell />,
            children: [
              { path: '/app/setup', element: <AppSetupPage /> },
            ],
          },
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
