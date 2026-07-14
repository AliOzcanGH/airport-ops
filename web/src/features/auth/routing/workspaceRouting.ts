import type {
  AuthMeResponse,
  WorkspaceType,
} from '@/shared/api/schemas'

export const workspacePaths: Record<WorkspaceType, string> = {
  PLATFORM: '/platform/dashboard',
  TENANT: '/app/dashboard',
}

export function requiresTenantSetup(currentUser: AuthMeResponse): boolean {
  return currentUser.tenantContext?.organizationStatus === 'ONBOARDING_INCOMPLETE'
}

export function isTenantActive(currentUser: AuthMeResponse): boolean {
  return currentUser.tenantContext?.organizationStatus === 'ACTIVE'
}

export function hasWorkspace(
  currentUser: AuthMeResponse,
  workspace: WorkspaceType,
): boolean {
  if (!currentUser.availableWorkspaces.includes(workspace)) return false
  return workspace !== 'TENANT' || currentUser.tenantContext !== null
}

export function defaultWorkspacePath(currentUser: AuthMeResponse): string {
  if (
    currentUser.defaultWorkspace &&
    hasWorkspace(currentUser, currentUser.defaultWorkspace)
  ) {
    if (
      currentUser.defaultWorkspace === 'TENANT' &&
      requiresTenantSetup(currentUser)
    ) {
      return '/app/setup'
    }
    return workspacePaths[currentUser.defaultWorkspace]
  }
  if (hasWorkspace(currentUser, 'PLATFORM')) return workspacePaths.PLATFORM
  if (hasWorkspace(currentUser, 'TENANT')) {
    return requiresTenantSetup(currentUser) ? '/app/setup' : workspacePaths.TENANT
  }
  return '/access-unavailable'
}

export function workspaceFallbackPath(
  currentUser: AuthMeResponse,
  requestedWorkspace: WorkspaceType,
): string {
  if (hasWorkspace(currentUser, requestedWorkspace)) {
    return workspacePaths[requestedWorkspace]
  }
  const alternative = requestedWorkspace === 'PLATFORM' ? 'TENANT' : 'PLATFORM'
  if (!hasWorkspace(currentUser, alternative)) return '/access-unavailable'
  if (alternative === 'TENANT' && requiresTenantSetup(currentUser)) {
    return '/app/setup'
  }
  return workspacePaths[alternative]
}
