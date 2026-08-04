import { apiClient } from '@/shared/api/apiClient'
import {
  createGateRequestSchema,
  createStationRequestSchema,
  gateResponseSchema,
  gatesResponseSchema,
  stationResponseSchema,
  stationsResponseSchema,
  updateGateStatusRequestSchema,
  type CreateGateRequest,
  type CreateStationRequest,
  type GateResponse,
  type GatesResponse,
  type StationResponse,
  type StationsResponse,
  type UpdateGateStatusRequest,
} from '@/shared/api/schemas'

export const stationsApi = {
  listStations(): Promise<StationsResponse> {
    return apiClient.get('/app/stations', {
      schema: stationsResponseSchema,
    })
  },

  createStation(request: CreateStationRequest): Promise<StationResponse> {
    return apiClient.post('/app/stations', {
      body: createStationRequestSchema.parse(request),
      schema: stationResponseSchema,
    })
  },

  listGates(stationId: string): Promise<GatesResponse> {
    return apiClient.get(`/app/stations/${stationId}/gates`, {
      schema: gatesResponseSchema,
    })
  },

  createGate(stationId: string, request: CreateGateRequest): Promise<GateResponse> {
    return apiClient.post(`/app/stations/${stationId}/gates`, {
      body: createGateRequestSchema.parse(request),
      schema: gateResponseSchema,
    })
  },

  updateGateStatus(
    stationId: string,
    gateId: string,
    request: UpdateGateStatusRequest,
  ): Promise<GateResponse> {
    return apiClient.request(`/app/stations/${stationId}/gates/${gateId}/status`, {
      method: 'PUT',
      body: updateGateStatusRequestSchema.parse(request),
      schema: gateResponseSchema,
    })
  },
}
