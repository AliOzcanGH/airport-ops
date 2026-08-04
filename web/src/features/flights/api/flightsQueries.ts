import { queryOptions } from '@tanstack/react-query'
import { flightsApi } from '@/features/flights/api/flightsApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const flightsQueryOptions = queryOptions({
  queryKey: queryKeys.app.flights,
  queryFn: () => flightsApi.listFlights(),
})
