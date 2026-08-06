import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plane, Plus } from 'lucide-react'
import { Link } from 'react-router'
import { flightsApi } from '@/features/flights/api/flightsApi'
import { flightsQueryOptions } from '@/features/flights/api/flightsQueries'
import { allowedNextStatuses } from '@/features/flights/flightStatusTransitions'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import type { FlightResponse, FlightStatus } from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

const FLIGHT_STATUS_BADGE: Record<FlightStatus, string> = {
  SCHEDULED: 'bg-slate-100 text-slate-700 border-slate-200',
  BOARDING: 'bg-blue-50 text-blue-700 border-blue-200',
  DEPARTED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  DELAYED: 'bg-amber-50 text-amber-800 border-amber-200',
  CANCELLED: 'bg-red-50 text-red-700 border-red-200',
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function statusErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'INVALID_STATUS_TRANSITION') {
      return error.message
    }
    if (error.errorCode === 'FLIGHT_NOT_FOUND') {
      return 'This flight could not be found for your organization.'
    }
    if (error.errorCode === 'MISSING_PERMISSION') {
      return 'Your account is missing the flight update permission.'
    }
    if (error.errorCode === 'TENANT_MISMATCH') {
      return 'This organization does not match your authenticated tenant.'
    }
    return error.message
  }
  return 'Could not update flight status.'
}

export function FlightListPage() {
  const flights = useQuery(flightsQueryOptions)

  return (
    <div className="space-y-8">
      <PageHeader
        title="Flights"
        description="Flights scheduled for your organization. Change status as operations progress."
        action={
          <Link
            to="/app/flights/new"
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-teal-800"
          >
            <Plus aria-hidden="true" size={17} />
            New flight
          </Link>
        }
      />

      {flights.isPending ? (
        <p className="text-sm text-slate-600">Loading flights...</p>
      ) : flights.isError ? (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
          We couldn&apos;t load the flight list. Please try again.
        </div>
      ) : (
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Flight</th>
                <th className="px-4 py-3">Route</th>
                <th className="px-4 py-3">Scheduled departure</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Change status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {flights.data?.map((flight) => (
                <FlightRow key={flight.id} flight={flight} />
              ))}
            </tbody>
          </table>
          {flights.data?.length === 0 ? (
            <p className="p-4 text-sm text-slate-600">
              <Plane aria-hidden="true" className="mr-1 inline size-4" />
              No flights yet. Create one to get started.
            </p>
          ) : null}
        </div>
      )}
    </div>
  )
}

function FlightRow({ flight }: { flight: FlightResponse }) {
  const queryClient = useQueryClient()
  const nextStatuses = allowedNextStatuses(flight.status)
  const [pendingStatus, setPendingStatus] = useState<FlightStatus | ''>('')

  const updateStatus = useMutation({
    mutationFn: (status: FlightStatus) => flightsApi.updateFlightStatus(flight.id, { status }),
    onSuccess: () => {
      setPendingStatus('')
      void queryClient.invalidateQueries({ queryKey: queryKeys.app.flights })
    },
  })

  return (
    <tr>
      <td className="px-4 py-3 font-medium text-slate-900">{flight.flightNumber}</td>
      <td className="px-4 py-3 text-slate-700">
        {flight.origin} → {flight.destination}
      </td>
      <td className="px-4 py-3 text-slate-700">{formatDate(flight.scheduledDeparture)}</td>
      <td className="px-4 py-3">
        <span
          className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${FLIGHT_STATUS_BADGE[flight.status]}`}
        >
          {flight.status}
        </span>
      </td>
      <td className="px-4 py-3">
        {nextStatuses.length === 0 ? (
          <span className="text-xs text-slate-500">No further transitions</span>
        ) : (
          <div className="flex items-center gap-2">
            <select
              aria-label={`Change status for flight ${flight.flightNumber}`}
              value={pendingStatus}
              onChange={(event) => setPendingStatus(event.target.value as FlightStatus)}
              className="h-9 rounded-md border border-slate-300 px-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
            >
              <option value="">Select status</option>
              {nextStatuses.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
            <button
              type="button"
              disabled={!pendingStatus || updateStatus.isPending}
              onClick={() => pendingStatus && updateStatus.mutate(pendingStatus)}
              className="inline-flex h-9 items-center rounded-md border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-800 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {updateStatus.isPending ? 'Saving...' : 'Apply'}
            </button>
          </div>
        )}
        {updateStatus.isError ? (
          <p role="alert" className="mt-1.5 text-xs text-red-700">
            {statusErrorMessage(updateStatus.error)}
          </p>
        ) : null}
      </td>
    </tr>
  )
}
