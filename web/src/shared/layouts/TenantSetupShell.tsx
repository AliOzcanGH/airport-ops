import { Building2, LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Outlet, useNavigate } from 'react-router'
import { useLogout } from '@/features/auth/hooks/useAuthSession'

export function TenantSetupShell() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const logout = useLogout()

  const signOut = () => {
    logout.mutate(undefined, {
      onSuccess: () => navigate('/login', { replace: true }),
    })
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex min-h-16 w-full max-w-5xl items-center gap-3 px-4 sm:px-6">
          <span className="grid size-10 place-items-center rounded-md bg-teal-700 text-white">
            <Building2 aria-hidden="true" size={20} />
          </span>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold">
              {t('setup.layout.productName')}
            </p>
            <p className="truncate text-xs text-slate-500">
              {t('setup.layout.subtitle')}
            </p>
          </div>
          <div className="ml-auto flex items-center gap-4">
            <span className="hidden text-xs font-medium text-slate-500 sm:inline">
              {t('setup.layout.environment')}
            </span>
            <button
              type="button"
              onClick={signOut}
              disabled={logout.isPending}
              className="inline-flex h-9 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60"
            >
              <LogOut aria-hidden="true" size={16} />
              {logout.isPending
                ? t('setup.layout.loggingOut')
                : t('setup.layout.logout')}
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:py-10">
        <Outlet />
      </main>
    </div>
  )
}
