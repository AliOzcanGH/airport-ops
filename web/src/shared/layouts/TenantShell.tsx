import { Building2, ClipboardList, LayoutDashboard, MapPin, Plane, Users } from 'lucide-react'
import { useCurrentUser } from '@/features/auth/hooks/useAuthSession'
import {
  WorkspaceShell,
  type WorkspaceShellConfig,
} from '@/shared/layouts/WorkspaceShell'

const baseTenantShellConfig: Omit<
  WorkspaceShellConfig,
  'navigationItems' | 'pageTitles'
> = {
  name: 'Airline tenant',
  subtitle: 'Organization workspace',
  environmentLabel: 'Tenant app · Local',
  navigationLabel: 'Airline tenant navigation',
  brandIcon: Building2,
  brandClassName: 'bg-teal-700',
  activeNavigationClassName:
    'bg-teal-50 text-teal-700 hover:bg-teal-50 hover:text-teal-700',
  statusClassName: 'bg-teal-600',
}

export function TenantShell() {
  const currentUser = useCurrentUser()
  const isAirlineAdmin = Boolean(
    currentUser.data?.tenantContext?.roles.includes('AIRLINE_ADMIN'),
  )

  const config: WorkspaceShellConfig = {
    ...baseTenantShellConfig,
    navigationItems: [
      {
        to: '/app/dashboard',
        label: 'Tenant dashboard',
        icon: LayoutDashboard,
      },
      { to: '/app/stations', label: 'Stations', icon: MapPin },
      { to: '/app/flights', label: 'Flights', icon: Plane },
      ...(isAirlineAdmin
        ? [
            { to: '/app/members', label: 'Members', icon: Users },
            { to: '/app/audit-logs', label: 'Audit log', icon: ClipboardList },
          ]
        : []),
    ],
    pageTitles: {
      '/app/dashboard': 'Tenant dashboard',
      '/app/stations': 'Stations',
      '/app/stations/new': 'New station',
      '/app/flights': 'Flights',
      '/app/flights/new': 'New flight',
      ...(isAirlineAdmin
        ? { '/app/members': 'Members', '/app/audit-logs': 'Audit log' }
        : {}),
    },
  }

  return <WorkspaceShell config={config} />
}
