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
import { AuditLogListPage } from '@/features/audit/pages/AuditLogListPage'
import { PlatformAuditLogPage } from '@/features/audit/pages/PlatformAuditLogPage'
import { FlightCreatePage } from '@/features/flights/pages/FlightCreatePage'
import { FlightDetailPage } from '@/features/flights/pages/FlightDetailPage'
import { FlightListPage } from '@/features/flights/pages/FlightListPage'
import { TenantMembersPage } from '@/features/members/pages/TenantMembersPage'
import { NotFoundPage } from '@/features/not-found/NotFoundPage'
import { StationCreatePage } from '@/features/stations/pages/StationCreatePage'
import { StationDetailPage } from '@/features/stations/pages/StationDetailPage'
import { StationListPage } from '@/features/stations/pages/StationListPage'
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
          {
            path: '/platform/audit-logs',
            element: <PlatformAuditLogPage />,
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
              { path: '/app/stations', element: <StationListPage /> },
              { path: '/app/stations/new', element: <StationCreatePage /> },
              { path: '/app/stations/:stationId', element: <StationDetailPage /> },
              { path: '/app/flights', element: <FlightListPage /> },
              { path: '/app/flights/new', element: <FlightCreatePage /> },
              { path: '/app/flights/:flightId', element: <FlightDetailPage /> },
              {
                element: <RequireTenantRole role="AIRLINE_ADMIN" />,
                children: [
                  { path: '/app/members', element: <TenantMembersPage /> },
                  { path: '/app/audit-logs', element: <AuditLogListPage /> },
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
