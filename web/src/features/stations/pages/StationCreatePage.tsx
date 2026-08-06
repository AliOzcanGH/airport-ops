import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Check, MapPin } from 'lucide-react'
import { Link } from 'react-router'
import { stationsApi } from '@/features/stations/api/stationsApi'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import { createStationRequestSchema } from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

type FieldErrors = Partial<Record<'stationName' | 'airportCode' | 'gateCount', string>>

function createErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'TENANT_MISMATCH') {
      return 'This organization does not match your authenticated tenant.'
    }
    if (error.errorCode === 'MISSING_PERMISSION') {
      return 'Your account is missing the station creation permission.'
    }
    if (error.errorCode === 'VALIDATION_ERROR') {
      return 'Check the fields below and try again.'
    }
    return error.message
  }
  return 'Station could not be created. Try again shortly.'
}

export function StationCreatePage() {
  const queryClient = useQueryClient()
  const [stationName, setStationName] = useState('')
  const [airportCode, setAirportCode] = useState('')
  const [gateCount, setGateCount] = useState('0')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const createStation = useMutation({
    mutationFn: stationsApi.createStation,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.app.stations })
    },
  })

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    createStation.reset()
    const parsed = createStationRequestSchema.safeParse({
      stationName,
      airportCode,
      gateCount,
    })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({
        stationName: errors.stationName?.[0],
        airportCode: errors.airportCode?.[0],
        gateCount: errors.gateCount?.[0],
      })
      return
    }
    setFieldErrors({})
    createStation.mutate(parsed.data)
  }

  const station = createStation.data

  return (
    <div className="space-y-8">
      <PageHeader
        title="New station"
        description="Create a station for your organization. Gates are added from the station detail page."
      />

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(320px,380px)]">
        <form
          onSubmit={submit}
          noValidate
          className="space-y-5 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div>
            <label htmlFor="station-name" className="text-sm font-medium text-slate-800">
              Station name
            </label>
            <input
              id="station-name"
              name="stationName"
              type="text"
              value={stationName}
              onChange={(event) => setStationName(event.target.value)}
              aria-invalid={Boolean(fieldErrors.stationName)}
              aria-describedby={fieldErrors.stationName ? 'station-name-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              placeholder="Sabiha Gokcen"
            />
            {fieldErrors.stationName ? (
              <p id="station-name-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.stationName}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="station-airport-code" className="text-sm font-medium text-slate-800">
              Airport code
            </label>
            <input
              id="station-airport-code"
              name="airportCode"
              type="text"
              value={airportCode}
              onChange={(event) => setAirportCode(event.target.value)}
              aria-invalid={Boolean(fieldErrors.airportCode)}
              aria-describedby={fieldErrors.airportCode ? 'station-airport-code-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm uppercase text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              placeholder="SAW"
            />
            {fieldErrors.airportCode ? (
              <p id="station-airport-code-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.airportCode}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="station-gate-count" className="text-sm font-medium text-slate-800">
              Gate count
            </label>
            <input
              id="station-gate-count"
              name="gateCount"
              type="number"
              min={0}
              value={gateCount}
              onChange={(event) => setGateCount(event.target.value)}
              aria-invalid={Boolean(fieldErrors.gateCount)}
              aria-describedby={fieldErrors.gateCount ? 'station-gate-count-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
            />
            {fieldErrors.gateCount ? (
              <p id="station-gate-count-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.gateCount}
              </p>
            ) : null}
          </div>

          {createStation.isError ? (
            <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
              {createErrorMessage(createStation.error)}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={createStation.isPending}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-teal-800 disabled:cursor-wait disabled:opacity-70"
          >
            <MapPin aria-hidden="true" size={17} />
            {createStation.isPending ? 'Creating station...' : 'Create station'}
          </button>
        </form>

        {station ? (
          <aside className="space-y-4 rounded-md border border-emerald-200 bg-emerald-50 p-5 shadow-sm">
            <div className="flex items-start gap-3">
              <span className="grid size-9 shrink-0 place-items-center rounded-md bg-emerald-600 text-white">
                <Check aria-hidden="true" size={18} />
              </span>
              <div>
                <h2 className="text-sm font-semibold text-emerald-950">Station created</h2>
                <p className="mt-1 text-sm leading-6 text-emerald-800">
                  {station.stationName} ({station.airportCode}) is ready. Add gates from its detail page.
                </p>
              </div>
            </div>
            <Link
              to={`/app/stations/${station.id}`}
              className="inline-flex h-9 items-center gap-2 rounded-md border border-emerald-300 bg-white px-3 text-sm font-semibold text-emerald-800 hover:bg-emerald-100"
            >
              Go to station
            </Link>
          </aside>
        ) : (
          <aside className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
            <div className="grid size-11 place-items-center rounded-md bg-slate-100 text-slate-600">
              <MapPin aria-hidden="true" size={21} />
            </div>
            <h2 className="mt-4 text-sm font-semibold text-slate-950">Station summary</h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              After creation, the new station&apos;s details appear here with a link to add gates.
            </p>
          </aside>
        )}
      </section>
    </div>
  )
}
