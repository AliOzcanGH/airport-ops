import {
  LogOut,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  X,
  type LucideIcon,
} from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { useLogout } from '@/features/auth/hooks/useAuthSession'
import { IconButton } from '@/shared/components/IconButton'
import { useUiStore } from '@/shared/stores/uiStore'
import { cn } from '@/shared/utils/cn'

export type WorkspaceNavigationItem = {
  to: string
  label: string
  icon: LucideIcon
}

export type WorkspaceShellConfig = {
  name: string
  subtitle: string
  environmentLabel: string
  navigationLabel: string
  brandIcon: LucideIcon
  brandClassName: string
  activeNavigationClassName: string
  statusClassName: string
  navigationItems: readonly WorkspaceNavigationItem[]
  pageTitles: Record<string, string>
}

type NavigationProps = {
  config: WorkspaceShellConfig
  collapsed?: boolean
  onNavigate?: () => void
}

function Navigation({
  config,
  collapsed = false,
  onNavigate,
}: NavigationProps) {
  return (
    <nav aria-label={config.navigationLabel} className="space-y-1 px-3">
      {config.navigationItems.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          onClick={onNavigate}
          title={collapsed ? label : undefined}
          className={({ isActive }) =>
            cn(
              'flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950',
              collapsed && 'justify-center px-0',
              isActive && config.activeNavigationClassName,
            )
          }
        >
          <Icon aria-hidden="true" className="shrink-0" size={19} />
          {collapsed ? (
            <span className="sr-only">{label}</span>
          ) : (
            <span>{label}</span>
          )}
        </NavLink>
      ))}
    </nav>
  )
}

type BrandProps = {
  config: WorkspaceShellConfig
  collapsed?: boolean
}

function Brand({ config, collapsed = false }: BrandProps) {
  const BrandIcon = config.brandIcon

  return (
    <>
      <div
        className={cn(
          'grid size-9 shrink-0 place-items-center rounded-md text-white',
          config.brandClassName,
        )}
      >
        <BrandIcon aria-hidden="true" size={19} />
      </div>
      {collapsed ? null : (
        <div className="ml-3 min-w-0">
          <p className="truncate text-sm font-semibold">{config.name}</p>
          <p className="truncate text-xs text-slate-500">{config.subtitle}</p>
        </div>
      )}
    </>
  )
}

export function WorkspaceShell({ config }: { config: WorkspaceShellConfig }) {
  const location = useLocation()
  const navigate = useNavigate()
  const logout = useLogout()
  const isSidebarCollapsed = useUiStore(
    (state) => state.isSidebarCollapsed,
  )
  const isMobileNavOpen = useUiStore((state) => state.isMobileNavOpen)
  const toggleSidebar = useUiStore((state) => state.toggleSidebar)
  const toggleMobileNav = useUiStore((state) => state.toggleMobileNav)
  const closeMobileNav = useUiStore((state) => state.closeMobileNav)
  const currentTitle = config.pageTitles[location.pathname] ?? config.name
  const signOut = () => {
    logout.mutate(undefined, {
      onSuccess: () => navigate('/login', { replace: true }),
    })
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <div className="flex min-h-screen">
        <aside
          className={cn(
            'sticky top-0 hidden h-screen shrink-0 flex-col border-r border-slate-200 bg-white transition-[width] duration-200 lg:flex',
            isSidebarCollapsed ? 'w-[76px]' : 'w-64',
          )}
        >
          <div
            className={cn(
              'flex h-16 items-center border-b border-slate-200 px-5',
              isSidebarCollapsed && 'justify-center px-0',
            )}
          >
            <Brand config={config} collapsed={isSidebarCollapsed} />
          </div>

          <div className="flex-1 py-5">
            <Navigation config={config} collapsed={isSidebarCollapsed} />
          </div>

          <div className="border-t border-slate-200 p-3">
            <button
              type="button"
              onClick={signOut}
              disabled={logout.isPending}
              title={isSidebarCollapsed ? 'Sign out' : undefined}
              className={cn(
                'flex h-10 w-full items-center gap-3 rounded-md px-3 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-950 disabled:opacity-60',
                isSidebarCollapsed && 'justify-center px-0',
              )}
            >
              <LogOut aria-hidden="true" size={19} />
              {isSidebarCollapsed ? (
                <span className="sr-only">Sign out</span>
              ) : (
                <span>{logout.isPending ? 'Signing out...' : 'Sign out'}</span>
              )}
            </button>
            <IconButton
              label={isSidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              onClick={toggleSidebar}
              className="mt-2 w-full"
            >
              {isSidebarCollapsed ? (
                <PanelLeftOpen size={19} />
              ) : (
                <PanelLeftClose size={19} />
              )}
            </IconButton>
          </div>
        </aside>

        <div className="min-w-0 flex-1">
          <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-slate-200 bg-white/95 px-4 backdrop-blur lg:px-6">
            <IconButton
              label="Open navigation"
              onClick={toggleMobileNav}
              className="lg:hidden"
            >
              <Menu size={20} />
            </IconButton>
            <p className="truncate text-sm font-semibold">{currentTitle}</p>
            <div className="ml-auto flex items-center gap-2">
              <span className="hidden text-xs font-medium text-slate-500 sm:inline">
                {config.environmentLabel}
              </span>
              <span
                className={cn('size-2 rounded-full', config.statusClassName)}
                aria-hidden="true"
              />
            </div>
          </header>

          <main className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
            <Outlet />
          </main>
        </div>
      </div>

      {isMobileNavOpen ? (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button
            type="button"
            aria-label="Dismiss navigation"
            className="absolute inset-0 bg-slate-950/35"
            onClick={closeMobileNav}
          />
          <aside className="relative flex h-full w-[min(84vw,320px)] flex-col border-r border-slate-200 bg-white shadow-xl">
            <div className="flex h-16 items-center border-b border-slate-200 px-4">
              <Brand config={config} />
              <IconButton
                label="Close navigation"
                onClick={closeMobileNav}
                className="ml-auto"
              >
                <X size={20} />
              </IconButton>
            </div>
            <div className="flex-1 py-5">
              <Navigation config={config} onNavigate={closeMobileNav} />
            </div>
            <div className="border-t border-slate-200 p-3">
              <button
                type="button"
                disabled={logout.isPending}
                onClick={() => {
                  closeMobileNav()
                  signOut()
                }}
                className="flex h-10 w-full items-center gap-3 rounded-md px-3 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-950 disabled:opacity-60"
              >
                <LogOut aria-hidden="true" size={19} />
                {logout.isPending ? 'Signing out...' : 'Sign out'}
              </button>
            </div>
          </aside>
        </div>
      ) : null}
    </div>
  )
}
