import type {
  AuthMeResponse,
  WorkspaceType,
} from '@/shared/api/schemas'

export const workspacePaths: Record<WorkspaceType, string> = {
  PLATFORM: '/platform/dashboard',
  TENANT: '/app/dashboard',
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
    return workspacePaths[currentUser.defaultWorkspace]
  }
  if (hasWorkspace(currentUser, 'PLATFORM')) return workspacePaths.PLATFORM
  if (hasWorkspace(currentUser, 'TENANT')) return workspacePaths.TENANT
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
  return hasWorkspace(currentUser, alternative)
    ? workspacePaths[alternative]
    : '/access-unavailable'
}
