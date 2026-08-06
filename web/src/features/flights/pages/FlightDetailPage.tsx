import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, ClipboardList } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { flightsApi } from '@/features/flights/api/flightsApi'
import { flightTasksQueryOptions, flightsQueryOptions } from '@/features/flights/api/flightsQueries'
import { allowedNextTaskStatuses } from '@/features/flights/taskStatusTransitions'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import type { TaskResponse, TaskStatus } from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

const TASK_TYPE_LABEL: Record<string, string> = {
  CLEANING: 'Cleaning',
  CATERING: 'Catering',
  FUELING: 'Fueling',
  BAGGAGE_LOADING: 'Baggage loading',
  BOARDING_PREPARATION: 'Boarding preparation',
  SECURITY_CHECK: 'Security check',
}

const TASK_STATUS_BADGE: Record<TaskStatus, string> = {
  OPEN: 'bg-slate-100 text-slate-700 border-slate-200',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 border-blue-200',
  DONE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  BLOCKED: 'bg-red-50 text-red-700 border-red-200',
}

function taskErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'INVALID_STATUS_TRANSITION') {
      return error.message
    }
    if (error.errorCode === 'TASK_NOT_FOUND') {
      return 'This task could not be found for this flight.'
    }
    if (error.errorCode === 'FLIGHT_NOT_FOUND') {
      return 'This flight could not be found for your organization.'
    }
    if (error.errorCode === 'MISSING_PERMISSION') {
      return 'Your account is missing the task completion permission.'
    }
    if (error.errorCode === 'TENANT_MISMATCH') {
      return 'This organization does not match your authenticated tenant.'
    }
    return error.message
  }
  return 'Could not update task status.'
}

export function FlightDetailPage() {
  const { flightId = '' } = useParams<{ flightId: string }>()
  const flights = useQuery(flightsQueryOptions)
  const tasks = useQuery(flightTasksQueryOptions(flightId))
  const flight = flights.data?.find((candidate) => candidate.id === flightId)

  if (flights.isPending) {
    return <p className="text-sm text-slate-600">Loading flight...</p>
  }

  if (flights.isError) {
    return (
      <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
        We couldn&apos;t load this flight. Please try again.
      </div>
    )
  }

  if (!flight) {
    return (
      <div className="space-y-4">
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
          This flight could not be found in your organization.
        </div>
        <Link to="/app/flights" className="text-sm font-medium text-teal-700 hover:underline">
          Back to flights
        </Link>
      </div>
    )
  }

  const allTasksDone = (tasks.data?.length ?? 0) > 0 && tasks.data?.every((task) => task.status === 'DONE')

  return (
    <div className="space-y-8">
      <PageHeader
        title={`${flight.flightNumber} · ${flight.origin} → ${flight.destination}`}
        description={`Status: ${flight.status}`}
      />

      <section aria-labelledby="turnaround-tasks-heading">
        <div className="mb-3 flex items-center justify-between">
          <h2 id="turnaround-tasks-heading" className="text-sm font-semibold text-slate-950">
            Turnaround tasks
          </h2>
          {allTasksDone ? (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
              <CheckCircle2 aria-hidden="true" size={14} />
              Turnaround complete
            </span>
          ) : null}
        </div>

        {tasks.isPending ? (
          <p className="text-sm text-slate-600">Loading tasks...</p>
        ) : tasks.isError ? (
          <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
            We couldn&apos;t load the task list. Please try again.
          </div>
        ) : (
          <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-3">Task</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Assigned to</th>
                  <th className="px-4 py-3">Change status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {tasks.data?.map((task) => (
                  <TaskRow key={task.id} flightId={flightId} task={task} />
                ))}
              </tbody>
            </table>
            {tasks.data?.length === 0 ? (
              <p className="p-4 text-sm text-slate-600">
                <ClipboardList aria-hidden="true" className="mr-1 inline size-4" />
                No turnaround tasks for this flight.
              </p>
            ) : null}
          </div>
        )}
      </section>
    </div>
  )
}

function TaskRow({ flightId, task }: { flightId: string; task: TaskResponse }) {
  const queryClient = useQueryClient()
  const nextStatuses = allowedNextTaskStatuses(task.status)
  const [pendingStatus, setPendingStatus] = useState<TaskStatus | ''>('')
  const [assignedTo, setAssignedTo] = useState(task.assignedTo ?? '')

  const updateStatus = useMutation({
    mutationFn: (status: TaskStatus) =>
      flightsApi.updateTaskStatus(flightId, task.id, {
        status,
        assignedTo: assignedTo.trim() || undefined,
      }),
    onSuccess: () => {
      setPendingStatus('')
      void queryClient.invalidateQueries({ queryKey: queryKeys.app.flightTasks(flightId) })
    },
  })

  return (
    <tr>
      <td className="px-4 py-3 font-medium text-slate-900">
        {TASK_TYPE_LABEL[task.taskType] ?? task.taskType}
      </td>
      <td className="px-4 py-3">
        <span
          className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${TASK_STATUS_BADGE[task.status]}`}
        >
          {task.status}
        </span>
      </td>
      <td className="px-4 py-3">
        <input
          type="text"
          aria-label={`Assigned to for ${TASK_TYPE_LABEL[task.taskType] ?? task.taskType}`}
          value={assignedTo}
          onChange={(event) => setAssignedTo(event.target.value)}
          placeholder="Unassigned"
          className="h-9 w-40 rounded-md border border-slate-300 px-2 text-sm text-slate-950 shadow-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
        />
      </td>
      <td className="px-4 py-3">
        {nextStatuses.length === 0 ? (
          <span className="text-xs text-slate-500">No further transitions</span>
        ) : (
          <div className="flex items-center gap-2">
            <select
              aria-label={`Change status for ${TASK_TYPE_LABEL[task.taskType] ?? task.taskType}`}
              value={pendingStatus}
              onChange={(event) => setPendingStatus(event.target.value as TaskStatus)}
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
            {taskErrorMessage(updateStatus.error)}
          </p>
        ) : null}
      </td>
    </tr>
  )
}
