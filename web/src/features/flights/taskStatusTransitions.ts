import type { TaskStatus } from '@/shared/api/schemas'

// Mirrors flight-service's TaskStatus allowed-transitions map (backend is
// still authoritative and returns 409 INVALID_STATUS_TRANSITION either way);
// this only narrows the dropdown so the UI doesn't offer doomed transitions.
const ALLOWED_TRANSITIONS: Record<TaskStatus, TaskStatus[]> = {
  OPEN: ['IN_PROGRESS'],
  IN_PROGRESS: ['DONE', 'BLOCKED'],
  BLOCKED: ['IN_PROGRESS'],
  DONE: [],
}

export function allowedNextTaskStatuses(current: TaskStatus): TaskStatus[] {
  return ALLOWED_TRANSITIONS[current]
}
