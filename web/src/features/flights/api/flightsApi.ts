import { apiClient } from '@/shared/api/apiClient'
import {
  createFlightRequestSchema,
  flightResponseSchema,
  flightsResponseSchema,
  updateFlightStatusRequestSchema,
  type CreateFlightRequest,
  type FlightResponse,
  type FlightsResponse,
  type UpdateFlightStatusRequest,
} from '@/shared/api/schemas'

export const flightsApi = {
  listFlights(): Promise<FlightsResponse> {
    return apiClient.get('/app/flights', {
      schema: flightsResponseSchema,
    })
  },

  createFlight(request: CreateFlightRequest): Promise<FlightResponse> {
    return apiClient.post('/app/flights', {
      body: createFlightRequestSchema.parse(request),
      schema: flightResponseSchema,
    })
  },

  updateFlightStatus(
    flightId: string,
    request: UpdateFlightStatusRequest,
  ): Promise<FlightResponse> {
    return apiClient.request(`/app/flights/${flightId}/status`, {
      method: 'PUT',
      body: updateFlightStatusRequestSchema.parse(request),
      schema: flightResponseSchema,
    })
  },
}
