import { apiClient } from '@/shared/api/apiClient'
import {
  createFlightRequestSchema,
  flightResponseSchema,
  flightsResponseSchema,
  taskResponseSchema,
  tasksResponseSchema,
  updateFlightStatusRequestSchema,
  updateTaskStatusRequestSchema,
  type CreateFlightRequest,
  type FlightResponse,
  type FlightsResponse,
  type TaskResponse,
  type TasksResponse,
  type UpdateFlightStatusRequest,
  type UpdateTaskStatusRequest,
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

  listTasks(flightId: string): Promise<TasksResponse> {
    return apiClient.get(`/app/flights/${flightId}/tasks`, {
      schema: tasksResponseSchema,
    })
  },

  updateTaskStatus(
    flightId: string,
    taskId: string,
    request: UpdateTaskStatusRequest,
  ): Promise<TaskResponse> {
    return apiClient.request(`/app/flights/${flightId}/tasks/${taskId}/status`, {
      method: 'PUT',
      body: updateTaskStatusRequestSchema.parse(request),
      schema: taskResponseSchema,
    })
  },
}
