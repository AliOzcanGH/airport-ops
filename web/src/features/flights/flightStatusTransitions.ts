import type { FlightStatus } from '@/shared/api/schemas'

// Mirrors flight-service's FlightStatus allowed-transitions map (backend is
// still authoritative and returns 409 INVALID_STATUS_TRANSITION either way);
// this only narrows the dropdown so the UI doesn't offer doomed transitions.
const ALLOWED_TRANSITIONS: Record<FlightStatus, FlightStatus[]> = {
  SCHEDULED: ['BOARDING', 'DELAYED', 'CANCELLED'],
  BOARDING: ['DEPARTED'],
  DELAYED: ['BOARDING', 'CANCELLED'],
  DEPARTED: [],
  CANCELLED: [],
}

export function allowedNextStatuses(current: FlightStatus): FlightStatus[] {
  return ALLOWED_TRANSITIONS[current]
}
