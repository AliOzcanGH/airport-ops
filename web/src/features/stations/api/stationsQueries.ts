import { queryOptions } from '@tanstack/react-query'
import { stationsApi } from '@/features/stations/api/stationsApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const stationsQueryOptions = queryOptions({
  queryKey: queryKeys.app.stations,
  queryFn: () => stationsApi.listStations(),
})

export const stationGatesQueryOptions = (stationId: string) =>
  queryOptions({
    queryKey: queryKeys.app.stationGates(stationId),
    queryFn: () => stationsApi.listGates(stationId),
    enabled: Boolean(stationId),
  })
