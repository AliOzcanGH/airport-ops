import { Building2, LayoutDashboard } from 'lucide-react'
import {
  WorkspaceShell,
  type WorkspaceShellConfig,
} from '@/shared/layouts/WorkspaceShell'

const tenantShellConfig: WorkspaceShellConfig = {
  name: 'Airline tenant',
  subtitle: 'Organization workspace',
  environmentLabel: 'Tenant app · Local',
  navigationLabel: 'Airline tenant navigation',
  brandIcon: Building2,
  brandClassName: 'bg-teal-700',
  activeNavigationClassName:
    'bg-teal-50 text-teal-700 hover:bg-teal-50 hover:text-teal-700',
  statusClassName: 'bg-teal-600',
  navigationItems: [
    {
      to: '/app/dashboard',
      label: 'Tenant dashboard',
      icon: LayoutDashboard,
    },
  ],
  pageTitles: {
    '/app/dashboard': 'Tenant dashboard',
  },
}

export function TenantShell() {
  return <WorkspaceShell config={tenantShellConfig} />
}
