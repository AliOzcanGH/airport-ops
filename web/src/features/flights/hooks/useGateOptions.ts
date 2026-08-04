import { useQueries, useQuery } from '@tanstack/react-query'
import { stationGatesQueryOptions, stationsQueryOptions } from '@/features/stations/api/stationsQueries'
import type { GateStatus } from '@/shared/api/schemas'

export type GateOption = {
  gateId: string
  stationName: string
  code: string
  status: GateStatus
}

export function useGateOptions() {
  const stations = useQuery(stationsQueryOptions)
  const stationIds = stations.data?.map((station) => station.id) ?? []

  const gateQueries = useQueries({
    queries: stationIds.map((stationId) => stationGatesQueryOptions(stationId)),
  })

  const isPending = stations.isPending || gateQueries.some((query) => query.isPending)
  const isError = stations.isError || gateQueries.some((query) => query.isError)

  const options: GateOption[] = stationIds.flatMap((stationId, index) => {
    const station = stations.data?.find((candidate) => candidate.id === stationId)
    const gates = gateQueries[index]?.data ?? []
    return gates.map((gate) => ({
      gateId: gate.id,
      stationName: station?.stationName ?? 'Unknown station',
      code: gate.code,
      status: gate.status,
    }))
  })

  return { options, isPending, isError }
}
