import { queryOptions } from '@tanstack/react-query'
import { flightsApi } from '@/features/flights/api/flightsApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const flightsQueryOptions = queryOptions({
  queryKey: queryKeys.app.flights,
  queryFn: () => flightsApi.listFlights(),
})

export const flightTasksQueryOptions = (flightId: string) =>
  queryOptions({
    queryKey: queryKeys.app.flightTasks(flightId),
    queryFn: () => flightsApi.listTasks(flightId),
    enabled: Boolean(flightId),
  })
