import { ShieldAlert } from 'lucide-react'
import { useNavigate } from 'react-router'
import { useLogout } from '@/features/auth/hooks/useAuthSession'
import { EmptyState } from '@/shared/components/EmptyState'

export function AccessUnavailablePage() {
  const navigate = useNavigate()
  const logout = useLogout()

  return (
    <div className="space-y-6">
      <EmptyState
        icon={ShieldAlert}
        title="Workspace access unavailable"
        description="Your account is authenticated, but no Airport Ops workspace is currently assigned. Contact platform support."
      />
      <div className="flex justify-center">
        <button
          type="button"
          disabled={logout.isPending}
          onClick={() =>
            logout.mutate(undefined, {
              onSuccess: () => navigate('/login', { replace: true }),
            })
          }
          className="inline-flex h-9 items-center rounded-md border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60"
        >
          {logout.isPending ? 'Signing out...' : 'Sign out'}
        </button>
      </div>
    </div>
  )
}
