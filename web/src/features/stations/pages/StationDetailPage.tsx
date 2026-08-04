import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { DoorOpen, Plus } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { stationsApi } from '@/features/stations/api/stationsApi'
import { stationGatesQueryOptions, stationsQueryOptions } from '@/features/stations/api/stationsQueries'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import {
  createGateRequestSchema,
  type CreateGateRequest,
  type GateResponse,
  type GateStatus,
} from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

type FieldErrors = Partial<Record<'code' | 'terminal', string>>

const GATE_STATUS_OPTIONS: GateStatus[] = ['ACTIVE', 'MAINTENANCE', 'CLOSED']

const GATE_STATUS_BADGE: Record<GateStatus, string> = {
  ACTIVE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  MAINTENANCE: 'bg-amber-50 text-amber-800 border-amber-200',
  CLOSED: 'bg-slate-100 text-slate-600 border-slate-200',
}

function createGateErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'GATE_CODE_CONFLICT') {
      return 'A gate with this code already exists at this station.'
    }
    if (error.errorCode === 'STATION_NOT_FOUND') {
      return 'This station could not be found for your organization.'
    }
    if (error.errorCode === 'MISSING_PERMISSION') {
      return 'Your account is missing the gate management permission.'
    }
    if (error.errorCode === 'TENANT_MISMATCH') {
      return 'This organization does not match your authenticated tenant.'
    }
    if (error.errorCode === 'VALIDATION_ERROR') {
      return 'Check the fields below and try again.'
    }
    return error.message
  }
  return 'Gate could not be created. Try again shortly.'
}

export function StationDetailPage() {
  const { stationId = '' } = useParams<{ stationId: string }>()
  const queryClient = useQueryClient()
  const stations = useQuery(stationsQueryOptions)
  const gates = useQuery(stationGatesQueryOptions(stationId))
  const station = stations.data?.find((candidate) => candidate.id === stationId)

  const [code, setCode] = useState('')
  const [terminal, setTerminal] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const createGate = useMutation({
    mutationFn: (variables: CreateGateRequest) =>
      stationsApi.createGate(stationId, variables),
    onSuccess: () => {
      setCode('')
      setTerminal('')
      void queryClient.invalidateQueries({ queryKey: queryKeys.app.stationGates(stationId) })
    },
  })

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    createGate.reset()
    const parsed = createGateRequestSchema.safeParse({ code, terminal })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({ code: errors.code?.[0], terminal: errors.terminal?.[0] })
      return
    }
    setFieldErrors({})
    createGate.mutate(parsed.data)
  }

  if (stations.isPending) {
    return <p className="text-sm text-slate-600">Loading station...</p>
  }

  if (stations.isError) {
    return (
      <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
        We couldn&apos;t load this station. Please try again.
      </div>
    )
  }

  if (!station) {
    return (
      <div className="space-y-4">
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
          This station could not be found in your organization.
        </div>
        <Link to="/app/stations" className="text-sm font-medium text-teal-700 hover:underline">
          Back to stations
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-8">
      <PageHeader
        title={station.stationName}
        description={`${station.airportCode} · ${station.gateCount} planned gates`}
      />

      <section
        aria-labelledby="add-gate-heading"
        className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(320px,380px)]"
      >
        <form
          onSubmit={submit}
          noValidate
          className="space-y-5 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
        >
          <h2 id="add-gate-heading" className="text-sm font-semibold text-slate-950">
            Add gate
          </h2>

          <div>
            <label htmlFor="gate-code" className="text-sm font-medium text-slate-800">
              Gate code
            </label>
            <input
              id="gate-code"
              name="code"
              type="text"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              aria-invalid={Boolean(fieldErrors.code)}
              aria-describedby={fieldErrors.code ? 'gate-code-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              placeholder="A1"
            />
            {fieldErrors.code ? (
              <p id="gate-code-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.code}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="gate-terminal" className="text-sm font-medium text-slate-800">
              Terminal
              <span className="ml-1 text-xs font-normal text-slate-500">Optional</span>
            </label>
            <input
              id="gate-terminal"
              name="terminal"
              type="text"
              value={terminal}
              onChange={(event) => setTerminal(event.target.value)}
              aria-invalid={Boolean(fieldErrors.terminal)}
              aria-describedby={fieldErrors.terminal ? 'gate-terminal-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              placeholder="T1"
            />
            {fieldErrors.terminal ? (
              <p id="gate-terminal-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.terminal}
              </p>
            ) : null}
          </div>

          {createGate.isError ? (
            <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
              {createGateErrorMessage(createGate.error)}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={createGate.isPending}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-teal-800 disabled:cursor-wait disabled:opacity-70"
          >
            <Plus aria-hidden="true" size={17} />
            {createGate.isPending ? 'Adding gate...' : 'Add gate'}
          </button>
        </form>

        <aside className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid size-11 place-items-center rounded-md bg-slate-100 text-slate-600">
            <DoorOpen aria-hidden="true" size={21} />
          </div>
          <h2 className="mt-4 text-sm font-semibold text-slate-950">Gate status</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Change a gate&apos;s status directly from the table. Only ACTIVE gates can be assigned to flights.
          </p>
        </aside>
      </section>

      <section aria-labelledby="gate-list-heading">
        <h2 id="gate-list-heading" className="mb-3 text-sm font-semibold text-slate-950">
          Gates
        </h2>
        {gates.isPending ? (
          <p className="text-sm text-slate-600">Loading gates...</p>
        ) : gates.isError ? (
          <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
            We couldn&apos;t load the gate list. Please try again.
          </div>
        ) : (
          <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-3">Code</th>
                  <th className="px-4 py-3">Terminal</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Change status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {gates.data?.map((gate) => (
                  <GateRow key={gate.id} stationId={stationId} gate={gate} />
                ))}
              </tbody>
            </table>
            {gates.data?.length === 0 ? (
              <p className="p-4 text-sm text-slate-600">No gates yet. Add one above.</p>
            ) : null}
          </div>
        )}
      </section>
    </div>
  )
}

function GateRow({ stationId, gate }: { stationId: string; gate: GateResponse }) {
  const queryClient = useQueryClient()
  const [pendingStatus, setPendingStatus] = useState<GateStatus>(gate.status)

  const updateStatus = useMutation({
    mutationFn: (status: GateStatus) =>
      stationsApi.updateGateStatus(stationId, gate.id, { status }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.app.stationGates(stationId) })
    },
  })

  return (
    <tr>
      <td className="px-4 py-3 font-medium text-slate-900">{gate.code}</td>
      <td className="px-4 py-3 text-slate-700">{gate.terminal ?? '—'}</td>
      <td className="px-4 py-3">
        <span
          className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${GATE_STATUS_BADGE[gate.status]}`}
        >
          {gate.status}
        </span>
      </td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <select
            aria-label={`Change status for gate ${gate.code}`}
            value={pendingStatus}
            onChange={(event) => setPendingStatus(event.target.value as GateStatus)}
            className="h-9 rounded-md border border-slate-300 px-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
          >
            {GATE_STATUS_OPTIONS.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
          <button
            type="button"
            disabled={pendingStatus === gate.status || updateStatus.isPending}
            onClick={() => updateStatus.mutate(pendingStatus)}
            className="inline-flex h-9 items-center rounded-md border border-slate-300 bg-white px-3 text-sm font-semibold text-slate-800 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {updateStatus.isPending ? 'Saving...' : 'Apply'}
          </button>
        </div>
        {updateStatus.isError ? (
          <p role="alert" className="mt-1.5 text-xs text-red-700">
            {updateStatus.error instanceof ApiError
              ? updateStatus.error.message
              : 'Could not update gate status.'}
          </p>
        ) : null}
      </td>
    </tr>
  )
}
