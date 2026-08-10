import { apiClient } from '@/shared/api/apiClient'
import {
  dailyFlightSummaryResponseSchema,
  gateUtilizationResponseSchema,
  type DailyFlightSummaryResponse,
  type GateUtilizationResponse,
} from '@/shared/api/schemas'

export const reportsApi = {
  getDailyFlights(date: string): Promise<DailyFlightSummaryResponse> {
    return apiClient.get(`/app/reports/daily-flights?date=${encodeURIComponent(date)}`, {
      schema: dailyFlightSummaryResponseSchema,
    })
  },
  getGateUtilization(date: string): Promise<GateUtilizationResponse> {
    return apiClient.get(`/app/reports/gate-utilization?date=${encodeURIComponent(date)}`, {
      schema: gateUtilizationResponseSchema,
    })
  },
}
