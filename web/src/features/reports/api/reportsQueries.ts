import { queryOptions } from '@tanstack/react-query'
import { reportsApi } from '@/features/reports/api/reportsApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const dailyFlightsQueryOptions = (date: string) =>
  queryOptions({
    queryKey: queryKeys.app.dailyFlights(date),
    queryFn: () => reportsApi.getDailyFlights(date),
  })

export const gateUtilizationQueryOptions = (date: string) =>
  queryOptions({
    queryKey: queryKeys.app.gateUtilization(date),
    queryFn: () => reportsApi.getGateUtilization(date),
  })
