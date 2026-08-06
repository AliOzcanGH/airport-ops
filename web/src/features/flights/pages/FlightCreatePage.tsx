import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Check, Plane } from 'lucide-react'
import { Link } from 'react-router'
import { flightsApi } from '@/features/flights/api/flightsApi'
import { useGateOptions } from '@/features/flights/hooks/useGateOptions'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import { createFlightRequestSchema } from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

type FieldErrors = Partial<
  Record<
    | 'flightNumber'
    | 'origin'
    | 'destination'
    | 'scheduledDeparture'
    | 'scheduledArrival'
    | 'assignedGateId',
    string
  >
>

function createErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'GATE_NOT_ACTIVE') {
      return 'The selected gate is not ACTIVE. Choose an ACTIVE gate or change the gate status first.'
    }
    if (error.errorCode === 'GATE_CONFLICT') {
      return 'Another flight already occupies this gate during the requested time range.'
    }
    if (error.errorCode === 'GATE_VERIFICATION_UNAVAILABLE') {
      return 'Could not verify the gate because the station service is unreachable. Try again shortly.'
    }
    if (error.errorCode === 'GATE_NOT_FOUND') {
      return 'The selected gate could not be found for your organization.'
    }
    if (error.errorCode === 'FLIGHT_NUMBER_CONFLICT') {
      return 'A flight with this number and departure time already exists.'
    }
    if (error.errorCode === 'TENANT_MISMATCH') {
      return 'This organization does not match your authenticated tenant.'
    }
    if (error.errorCode === 'MISSING_PERMISSION') {
      return 'Your account is missing the flight creation permission.'
    }
    if (error.errorCode === 'VALIDATION_ERROR') {
      return 'Check the fields below and try again.'
    }
    return error.message
  }
  return 'Flight could not be created. Try again shortly.'
}

function toIsoOrEmpty(localDateTime: string): string {
  if (!localDateTime) return ''
  const parsed = new Date(localDateTime)
  return Number.isNaN(parsed.getTime()) ? '' : parsed.toISOString()
}

export function FlightCreatePage() {
  const queryClient = useQueryClient()
  const gateOptions = useGateOptions()

  const [flightNumber, setFlightNumber] = useState('')
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [scheduledDeparture, setScheduledDeparture] = useState('')
  const [scheduledArrival, setScheduledArrival] = useState('')
  const [assignedGateId, setAssignedGateId] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const createFlight = useMutation({
    mutationFn: flightsApi.createFlight,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.app.flights })
    },
  })

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    createFlight.reset()
    const parsed = createFlightRequestSchema.safeParse({
      flightNumber,
      origin,
      destination,
      scheduledDeparture: toIsoOrEmpty(scheduledDeparture),
      scheduledArrival: toIsoOrEmpty(scheduledArrival),
      assignedGateId,
    })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({
        flightNumber: errors.flightNumber?.[0],
        origin: errors.origin?.[0],
        destination: errors.destination?.[0],
        scheduledDeparture: errors.scheduledDeparture?.[0],
        scheduledArrival: errors.scheduledArrival?.[0],
        assignedGateId: errors.assignedGateId?.[0],
      })
      return
    }
    setFieldErrors({})
    createFlight.mutate(parsed.data)
  }

  const flight = createFlight.data

  return (
    <div className="space-y-8">
      <PageHeader
        title="New flight"
        description="Schedule a flight against an existing gate. The gate must be ACTIVE and free for the requested time range."
      />

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(320px,380px)]">
        <form
          onSubmit={submit}
          noValidate
          className="space-y-5 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label htmlFor="flight-number" className="text-sm font-medium text-slate-800">
                Flight number
              </label>
              <input
                id="flight-number"
                type="text"
                value={flightNumber}
                onChange={(event) => setFlightNumber(event.target.value)}
                aria-invalid={Boolean(fieldErrors.flightNumber)}
                className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                placeholder="PC123"
              />
              {fieldErrors.flightNumber ? (
                <p className="mt-1.5 text-xs text-red-700">{fieldErrors.flightNumber}</p>
              ) : null}
            </div>
            <div>
              <label htmlFor="flight-origin" className="text-sm font-medium text-slate-800">
                Origin
              </label>
              <input
                id="flight-origin"
                type="text"
                value={origin}
                onChange={(event) => setOrigin(event.target.value)}
                aria-invalid={Boolean(fieldErrors.origin)}
                className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm uppercase text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                placeholder="SAW"
              />
              {fieldErrors.origin ? (
                <p className="mt-1.5 text-xs text-red-700">{fieldErrors.origin}</p>
              ) : null}
            </div>
            <div>
              <label htmlFor="flight-destination" className="text-sm font-medium text-slate-800">
                Destination
              </label>
              <input
                id="flight-destination"
                type="text"
                value={destination}
                onChange={(event) => setDestination(event.target.value)}
                aria-invalid={Boolean(fieldErrors.destination)}
                className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm uppercase text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                placeholder="IST"
              />
              {fieldErrors.destination ? (
                <p className="mt-1.5 text-xs text-red-700">{fieldErrors.destination}</p>
              ) : null}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="flight-departure" className="text-sm font-medium text-slate-800">
                Scheduled departure
              </label>
              <input
                id="flight-departure"
                type="datetime-local"
                value={scheduledDeparture}
                onChange={(event) => setScheduledDeparture(event.target.value)}
                aria-invalid={Boolean(fieldErrors.scheduledDeparture)}
                className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              />
              {fieldErrors.scheduledDeparture ? (
                <p className="mt-1.5 text-xs text-red-700">{fieldErrors.scheduledDeparture}</p>
              ) : null}
            </div>
            <div>
              <label htmlFor="flight-arrival" className="text-sm font-medium text-slate-800">
                Scheduled arrival
              </label>
              <input
                id="flight-arrival"
                type="datetime-local"
                value={scheduledArrival}
                onChange={(event) => setScheduledArrival(event.target.value)}
                aria-invalid={Boolean(fieldErrors.scheduledArrival)}
                className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              />
              {fieldErrors.scheduledArrival ? (
                <p className="mt-1.5 text-xs text-red-700">{fieldErrors.scheduledArrival}</p>
              ) : null}
            </div>
          </div>

          <div>
            <label htmlFor="flight-gate" className="text-sm font-medium text-slate-800">
              Assigned gate
            </label>
            <select
              id="flight-gate"
              value={assignedGateId}
              onChange={(event) => setAssignedGateId(event.target.value)}
              aria-invalid={Boolean(fieldErrors.assignedGateId)}
              disabled={gateOptions.isPending}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
            >
              <option value="">
                {gateOptions.isPending ? 'Loading gates...' : 'Select a gate'}
              </option>
              {gateOptions.options.map((gate) => (
                <option key={gate.gateId} value={gate.gateId}>
                  {gate.stationName} · {gate.code} · {gate.status}
                </option>
              ))}
            </select>
            {gateOptions.isError ? (
              <p className="mt-1.5 text-xs text-red-700">
                Could not load gates. Create a station and gate first.
              </p>
            ) : null}
            {fieldErrors.assignedGateId ? (
              <p className="mt-1.5 text-xs text-red-700">{fieldErrors.assignedGateId}</p>
            ) : null}
          </div>

          {createFlight.isError ? (
            <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
              {createErrorMessage(createFlight.error)}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={createFlight.isPending}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-teal-800 disabled:cursor-wait disabled:opacity-70"
          >
            <Plane aria-hidden="true" size={17} />
            {createFlight.isPending ? 'Creating flight...' : 'Create flight'}
          </button>
        </form>

        {flight ? (
          <aside className="space-y-4 rounded-md border border-emerald-200 bg-emerald-50 p-5 shadow-sm">
            <div className="flex items-start gap-3">
              <span className="grid size-9 shrink-0 place-items-center rounded-md bg-emerald-600 text-white">
                <Check aria-hidden="true" size={18} />
              </span>
              <div>
                <h2 className="text-sm font-semibold text-emerald-950">Flight created</h2>
                <p className="mt-1 text-sm leading-6 text-emerald-800">
                  {flight.flightNumber} ({flight.origin} → {flight.destination}) is scheduled.
                </p>
              </div>
            </div>
            <Link
              to="/app/flights"
              className="inline-flex h-9 items-center gap-2 rounded-md border border-emerald-300 bg-white px-3 text-sm font-semibold text-emerald-800 hover:bg-emerald-100"
            >
              View flights
            </Link>
          </aside>
        ) : (
          <aside className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
            <div className="grid size-11 place-items-center rounded-md bg-slate-100 text-slate-600">
              <Plane aria-hidden="true" size={21} />
            </div>
            <h2 className="mt-4 text-sm font-semibold text-slate-950">Flight summary</h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              After creation, the new flight&apos;s details appear here.
            </p>
          </aside>
        )}
      </section>
    </div>
  )
}
