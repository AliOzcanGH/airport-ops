import { Building2, LayoutDashboard, MailPlus, ShieldCheck } from 'lucide-react'
import {
  WorkspaceShell,
  type WorkspaceShellConfig,
} from '@/shared/layouts/WorkspaceShell'

const platformShellConfig: WorkspaceShellConfig = {
  name: 'Platform console',
  subtitle: 'Internal administration',
  environmentLabel: 'Platform console · Local',
  navigationLabel: 'Platform console navigation',
  brandIcon: ShieldCheck,
  brandClassName: 'bg-blue-700',
  activeNavigationClassName:
    'bg-blue-50 text-blue-700 hover:bg-blue-50 hover:text-blue-700',
  statusClassName: 'bg-blue-600',
  navigationItems: [
    {
      to: '/platform/dashboard',
      label: 'Platform dashboard',
      icon: LayoutDashboard,
    },
    {
      to: '/platform/invitations/new',
      label: 'Tenant invitations',
      icon: MailPlus,
    },
    {
      to: '/platform/tenants',
      label: 'Tenant directory',
      icon: Building2,
    },
  ],
  pageTitles: {
    '/platform/dashboard': 'Platform dashboard',
    '/platform/invitations/new': 'Tenant invitations',
    '/platform/tenants': 'Tenant directory',
  },
}

export function PlatformShell() {
  return <WorkspaceShell config={platformShellConfig} />
}
