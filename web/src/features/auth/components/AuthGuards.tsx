import { Navigate, Outlet } from 'react-router'
import { useCurrentUser } from '@/features/auth/hooks/useAuthSession'
import {
  defaultWorkspacePath,
  hasWorkspace,
  isTenantActive,
  requiresTenantSetup,
  workspaceFallbackPath,
} from '@/features/auth/routing/workspaceRouting'
import {
  SessionErrorView,
  SessionLoadingView,
} from '@/features/auth/components/AuthStateViews'
import type { WorkspaceType } from '@/shared/api/schemas'

function queryState(query: ReturnType<typeof useCurrentUser>) {
  if (query.isPending) return <SessionLoadingView />
  if (query.isError) {
    return <SessionErrorView retry={() => void query.refetch()} />
  }
  return null
}

export function HomeRedirect() {
  const query = useCurrentUser()
  const state = queryState(query)
  if (state) return state
  if (!query.data) return <Navigate to="/login" replace />
  return <Navigate to={defaultWorkspacePath(query.data)} replace />
}

export function RequireAuthentication() {
  const query = useCurrentUser()
  const state = queryState(query)
  if (state) return state
  if (!query.data) return <Navigate to="/login" replace />
  return <Outlet />
}

export function RequireWorkspace({ workspace }: { workspace: WorkspaceType }) {
  const query = useCurrentUser()
  const state = queryState(query)
  if (state) return state
  if (!query.data) return <Navigate to="/login" replace />
  if (!hasWorkspace(query.data, workspace)) {
    return (
      <Navigate
        to={workspaceFallbackPath(query.data, workspace)}
        replace
      />
    )
  }
  return <Outlet />
}

export function RequireTenantSetupComplete() {
  const query = useCurrentUser()
  const state = queryState(query)
  if (state) return state
  if (!query.data) return <Navigate to="/login" replace />
  if (!hasWorkspace(query.data, 'TENANT')) {
    return (
      <Navigate
        to={workspaceFallbackPath(query.data, 'TENANT')}
        replace
      />
    )
  }
  if (requiresTenantSetup(query.data)) {
    return <Navigate to="/app/setup" replace />
  }
  return <Outlet />
}

export function RequireTenantSetupPage() {
  const query = useCurrentUser()
  const state = queryState(query)
  if (state) return state
  if (!query.data) return <Navigate to="/login" replace />
  if (!hasWorkspace(query.data, 'TENANT')) {
    return (
      <Navigate
        to={workspaceFallbackPath(query.data, 'TENANT')}
        replace
      />
    )
  }
  if (isTenantActive(query.data)) {
    return <Navigate to="/app/dashboard" replace />
  }
  return <Outlet />
}
