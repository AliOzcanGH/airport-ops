import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  Building2,
  Plane,
  RefreshCw,
  TowerControl,
  Users,
} from 'lucide-react'
import { Link, useParams } from 'react-router'
import { platformTenantApi } from '@/features/tenants/api/platformTenantApi'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import type {
  PlatformTenantDetailResponse,
  PlatformTenantSummary,
} from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'
import { cn } from '@/shared/utils/cn'

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function formatDate(value: string | null): string {
  if (!value) return 'Not recorded'
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function statusLabel(status: PlatformTenantSummary['organizationStatus']) {
  return status.replaceAll('_', ' ')
}

function statusClassName(status: PlatformTenantSummary['organizationStatus']) {
  if (status === 'ACTIVE') return 'bg-emerald-50 text-emerald-700'
  if (status === 'INACTIVE') return 'bg-slate-100 text-slate-600'
  return 'bg-amber-50 text-amber-800'
}

function isTenantNotFound(error: unknown) {
  return error instanceof ApiError && error.status === 404
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message
  return 'Tenant detail could not be loaded.'
}

export function PlatformTenantDetailPage() {
  const { organizationId } = useParams()
  const isValidOrganizationId =
    organizationId !== undefined && uuidPattern.test(organizationId)

  const tenantQuery = useQuery({
    queryKey: queryKeys.platform.tenantDetail(organizationId ?? 'invalid'),
    queryFn: ({ signal }) =>
      platformTenantApi.getPlatformTenantDetail(organizationId ?? '', {
        signal,
      }),
    enabled: isValidOrganizationId,
    retry: false,
  })

  if (!isValidOrganizationId) {
    return <TenantNotFoundState />
  }

  if (tenantQuery.isError && isTenantNotFound(tenantQuery.error)) {
    return <TenantNotFoundState />
  }

  return (
    <div className="space-y-8">
      <PageHeader
        title={tenantQuery.data?.organizationName ?? 'Tenant detail'}
        description="Read-only platform view of tenant organization status, active members, and assigned organization roles."
        action={
          <Link
            to="/platform/tenants"
            className="inline-flex h-9 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50"
          >
            <ArrowLeft aria-hidden="true" size={16} />
            Tenant directory
          </Link>
        }
      />

      {tenantQuery.isPending ? <TenantDetailLoading /> : null}

      {tenantQuery.isError ? (
        <div
          role="alert"
          className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          {errorMessage(tenantQuery.error)}
        </div>
      ) : null}

      {tenantQuery.isSuccess ? (
        <TenantDetailContent
          tenant={tenantQuery.data}
          onRefresh={() => void tenantQuery.refetch()}
          isRefreshing={tenantQuery.isFetching}
        />
      ) : null}
    </div>
  )
}

function TenantNotFoundState() {
  return (
    <section className="rounded-md border border-slate-200 bg-white px-6 py-10 text-center shadow-sm">
      <div className="mx-auto grid size-12 place-items-center rounded-md bg-slate-100 text-slate-600">
        <Building2 aria-hidden="true" size={23} />
      </div>
      <h1 className="mt-4 text-xl font-semibold text-slate-950">
        Tenant organization not found
      </h1>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-600">
        The tenant may not exist, may have been removed, or the link may be
        invalid.
      </p>
      <Link
        to="/platform/tenants"
        className="mt-5 inline-flex h-9 items-center gap-2 rounded-md bg-blue-700 px-3 text-sm font-semibold text-white hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-700"
      >
        <ArrowLeft aria-hidden="true" size={15} />
        Back to Tenant directory
      </Link>
    </section>
  )
}

function TenantDetailLoading() {
  return (
    <div
      aria-label="Loading tenant detail"
      className="grid gap-4"
    >
      <div className="h-32 animate-pulse rounded-md border border-slate-200 bg-white" />
      <div className="h-56 animate-pulse rounded-md border border-slate-200 bg-white" />
    </div>
  )
}

function TenantDetailContent({
  tenant,
  onRefresh,
  isRefreshing,
}: {
  tenant: PlatformTenantDetailResponse
  onRefresh: () => void
  isRefreshing: boolean
}) {
  return (
    <>
      <section
        aria-labelledby="tenant-summary-heading"
        className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
      >
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h2
              id="tenant-summary-heading"
              className="text-sm font-semibold text-slate-950"
            >
              Organization summary
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              {tenant.organizationId}
            </p>
          </div>
          <button
            type="button"
            onClick={onRefresh}
            disabled={isRefreshing}
            className="inline-flex h-9 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
          >
            <RefreshCw
              aria-hidden="true"
              size={16}
              className={cn(isRefreshing && 'animate-spin')}
            />
            Refresh
          </button>
        </div>

        <dl className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <dt className="text-xs font-semibold uppercase text-slate-500">
              Status
            </dt>
            <dd className="mt-2">
              <span
                className={cn(
                  'inline-flex rounded-full px-2.5 py-1 text-xs font-semibold',
                  statusClassName(tenant.organizationStatus),
                )}
              >
                {statusLabel(tenant.organizationStatus)}
              </span>
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase text-slate-500">
              Created
            </dt>
            <dd className="mt-2 text-sm text-slate-800">
              {formatDate(tenant.createdAt)}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase text-slate-500">
              Active members
            </dt>
            <dd className="mt-2 text-sm text-slate-800">
              {tenant.memberCount}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase text-slate-500">
              Primary admin
            </dt>
            <dd className="mt-2 text-sm text-slate-800">
              {tenant.primaryAdminEmail ?? 'Not assigned'}
            </dd>
          </div>
        </dl>
      </section>

      <OperationalSummaryCard tenant={tenant} />

      <TenantMembersTable tenant={tenant} />
    </>
  )
}

function OperationalSummaryCard({
  tenant,
}: {
  tenant: PlatformTenantDetailResponse
}) {
  return (
    <section
      aria-labelledby="tenant-operational-summary-heading"
      className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
    >
      <h2
        id="tenant-operational-summary-heading"
        className="text-sm font-semibold text-slate-950"
      >
        Operational summary
      </h2>

      {tenant.operationalSummary ? (
        <dl className="mt-5 grid gap-4 sm:grid-cols-3">
          <div>
            <dt className="flex items-center gap-1.5 text-xs font-semibold uppercase text-slate-500">
              <TowerControl aria-hidden="true" size={14} />
              Stations
            </dt>
            <dd className="mt-2 text-sm text-slate-800">
              {tenant.operationalSummary.stationCount}
            </dd>
          </div>
          <div>
            <dt className="flex items-center gap-1.5 text-xs font-semibold uppercase text-slate-500">
              <Plane aria-hidden="true" size={14} />
              Flights, last 30 days
            </dt>
            <dd className="mt-2 text-sm text-slate-800">
              {tenant.operationalSummary.totalFlightsLast30Days}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase text-slate-500">
              Last flight activity
            </dt>
            <dd className="mt-2 text-sm text-slate-800">
              {formatDate(tenant.operationalSummary.lastFlightActivityAt)}
            </dd>
          </div>
        </dl>
      ) : (
        <p className="mt-3 text-sm text-slate-500">
          Operational data unavailable.
        </p>
      )}
    </section>
  )
}

function TenantMembersTable({
  tenant,
}: {
  tenant: PlatformTenantDetailResponse
}) {
  return (
    <section
      aria-labelledby="tenant-members-heading"
      className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm"
    >
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h2
          id="tenant-members-heading"
          className="text-sm font-semibold text-slate-950"
        >
          Active members
        </h2>
        <span className="inline-flex items-center gap-2 text-xs text-slate-500">
          <Users aria-hidden="true" size={15} />
          {tenant.members.length} listed
        </span>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
            <tr>
              <th scope="col" className="px-4 py-3">
                Member
              </th>
              <th scope="col" className="px-4 py-3">
                Email
              </th>
              <th scope="col" className="px-4 py-3">
                Status
              </th>
              <th scope="col" className="px-4 py-3">
                Roles
              </th>
              <th scope="col" className="px-4 py-3">
                Joined
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {tenant.members.map((member) => (
              <tr key={member.memberId}>
                <td className="whitespace-nowrap px-4 py-3 font-medium text-slate-950">
                  {member.fullName}
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-slate-700">
                  {member.email}
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-slate-700">
                  {member.memberStatus}
                </td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1.5">
                    {member.roles.length > 0 ? (
                      member.roles.map((role) => (
                        <span
                          key={role}
                          className="inline-flex rounded bg-blue-50 px-2 py-1 font-mono text-xs font-semibold text-blue-700"
                        >
                          {role}
                        </span>
                      ))
                    ) : (
                      <span className="text-slate-500">No roles</span>
                    )}
                  </div>
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-slate-600">
                  {formatDate(member.joinedAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
