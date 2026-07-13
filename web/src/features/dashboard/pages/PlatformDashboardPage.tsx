import { useQuery } from '@tanstack/react-query'
import {
  Activity,
  ArrowRight,
  Building2,
  MailPlus,
  RefreshCw,
  Server,
} from 'lucide-react'
import { Link } from 'react-router'
import { useCurrentUser } from '@/features/auth/hooks/useAuthSession'
import { ApiError } from '@/shared/api/ApiError'
import { apiClient } from '@/shared/api/apiClient'
import { queryKeys } from '@/shared/api/queryKeys'
import {
  healthResponseSchema,
  type HealthResponse,
} from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'
import { cn } from '@/shared/utils/cn'

function healthLabel(
  isPending: boolean,
  isError: boolean,
  status?: string,
) {
  if (isPending) return 'Checking'
  if (isError) return 'Unavailable'
  return status ?? 'Unknown'
}

export function PlatformDashboardPage() {
  const currentUser = useCurrentUser()
  const healthQuery = useQuery({
    queryKey: queryKeys.iam.health,
    queryFn: ({ signal }) =>
      apiClient.get<HealthResponse>('/actuator/health', {
        signal,
        schema: healthResponseSchema,
      }),
  })
  const isHealthy = healthQuery.data?.status === 'UP'
  const status = healthLabel(
    healthQuery.isPending,
    healthQuery.isError,
    healthQuery.data?.status,
  )
  const errorMessage =
    healthQuery.error instanceof ApiError
      ? healthQuery.error.message
      : 'IAM service could not be reached.'

  return (
    <div className="space-y-8">
      <PageHeader
        title="Platform console overview"
        description="Internal control surface for platform administrators managing tenant onboarding and IAM readiness."
        action={
          <button
            type="button"
            onClick={() => void healthQuery.refetch()}
            disabled={healthQuery.isFetching}
            className="inline-flex h-9 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
          >
            <RefreshCw
              aria-hidden="true"
              size={16}
              className={cn(healthQuery.isFetching && 'animate-spin')}
            />
            Refresh
          </button>
        }
      />

      {currentUser.data ? (
        <section
          aria-labelledby="platform-access-heading"
          className="grid gap-5 border-y border-slate-200 py-5 md:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]"
        >
          <div>
            <h2
              id="platform-access-heading"
              className="text-sm font-semibold text-slate-950"
            >
              Current platform access
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              {currentUser.data.email}
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">
                IAM roles
              </p>
              <p className="mt-2 text-sm text-slate-800">
                {currentUser.data.iamRoles.join(', ') || 'No platform roles'}
              </p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase text-slate-500">
                Permissions
              </p>
              <ul className="mt-2 space-y-1 text-sm text-slate-700">
                {currentUser.data.permissions.map((permission) => (
                  <li key={permission} className="font-mono text-xs">
                    {permission}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </section>
      ) : null}

      <section aria-labelledby="platform-health-heading">
        <div className="mb-3 flex items-center justify-between">
          <h2
            id="platform-health-heading"
            className="text-sm font-semibold text-slate-950"
          >
            Platform service health
          </h2>
          <span className="text-xs text-slate-500">IAM API</span>
        </div>
        <div className="grid min-h-32 grid-cols-[auto_1fr_auto] items-center gap-4 rounded-md border border-slate-200 bg-white px-5 py-4 shadow-sm">
          <div className="grid size-11 place-items-center rounded-md bg-slate-100 text-slate-600">
            <Server aria-hidden="true" size={21} />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-950">
              iam-service
            </p>
            <p className="mt-1 text-xs text-slate-500">
              http://127.0.0.1:8081
            </p>
            {healthQuery.isError ? (
              <p className="mt-2 text-xs text-red-700">{errorMessage}</p>
            ) : null}
          </div>
          <div
            aria-live="polite"
            className={cn(
              'inline-flex h-7 items-center gap-2 rounded-full px-3 text-xs font-semibold',
              healthQuery.isPending && 'bg-slate-100 text-slate-600',
              !healthQuery.isPending &&
                isHealthy &&
                'bg-emerald-50 text-emerald-700',
              healthQuery.isError && 'bg-red-50 text-red-700',
              !healthQuery.isPending &&
                !healthQuery.isError &&
                !isHealthy &&
                'bg-amber-50 text-amber-800',
            )}
          >
            <span
              className={cn(
                'size-1.5 rounded-full bg-current',
                healthQuery.isPending && 'animate-pulse',
              )}
            />
            {status}
          </div>
        </div>
      </section>

      <section aria-labelledby="platform-workspace-heading">
        <div className="mb-3 flex items-center justify-between">
          <h2 id="platform-workspace-heading" className="text-sm font-semibold">
            Platform workflows
          </h2>
          <Activity aria-hidden="true" className="text-slate-400" size={17} />
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <Link
            to="/platform/invitations/new"
            className="group flex min-h-36 items-start gap-4 rounded-md border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-blue-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
          >
            <span className="grid size-10 shrink-0 place-items-center rounded-md bg-blue-50 text-blue-700">
              <MailPlus aria-hidden="true" size={20} />
            </span>
            <span className="min-w-0">
              <span className="block text-sm font-semibold text-slate-950">
                Tenant invitations
              </span>
              <span className="mt-1 block text-sm leading-6 text-slate-600">
                Invite a new airline tenant administrator.
              </span>
              <span className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-blue-700">
                Open workspace
                <ArrowRight
                  aria-hidden="true"
                  size={14}
                  className="transition-transform group-hover:translate-x-0.5"
                />
              </span>
            </span>
          </Link>
          <Link
            to="/platform/tenants"
            className="group flex min-h-36 items-start gap-4 rounded-md border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-blue-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
          >
            <span className="grid size-10 shrink-0 place-items-center rounded-md bg-slate-100 text-slate-600">
              <Building2 aria-hidden="true" size={20} />
            </span>
            <span className="min-w-0">
              <span className="block text-sm font-semibold text-slate-950">
                Tenant directory
              </span>
              <span className="mt-1 block text-sm leading-6 text-slate-600">
                Review airline organization readiness after invitation acceptance.
              </span>
              <span className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-blue-700">
                Open directory
                <ArrowRight
                  aria-hidden="true"
                  size={14}
                  className="transition-transform group-hover:translate-x-0.5"
                />
              </span>
            </span>
          </Link>
        </div>
      </section>
    </div>
  )
}
