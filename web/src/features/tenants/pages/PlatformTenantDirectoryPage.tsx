import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Building2, RefreshCw, Users } from 'lucide-react'
import { Link } from 'react-router'
import { platformTenantApi } from '@/features/tenants/api/platformTenantApi'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import type { PlatformTenantSummary } from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'
import { cn } from '@/shared/utils/cn'

function formatDate(value: string): string {
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

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message
  return 'Tenant directory could not be loaded.'
}

export function PlatformTenantDirectoryPage() {
  const tenantQuery = useQuery({
    queryKey: queryKeys.platform.tenants,
    queryFn: ({ signal }) =>
      platformTenantApi.listPlatformTenants({ signal }),
    retry: false,
  })
  const tenants = tenantQuery.data?.tenants ?? []

  return (
    <div className="space-y-8">
      <PageHeader
        title="Tenant directory"
        description="Read-only platform view of airline tenant organizations created through the invitation onboarding flow."
        action={
          <button
            type="button"
            onClick={() => void tenantQuery.refetch()}
            disabled={tenantQuery.isFetching}
            className="inline-flex h-9 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
          >
            <RefreshCw
              aria-hidden="true"
              size={16}
              className={cn(tenantQuery.isFetching && 'animate-spin')}
            />
            Refresh
          </button>
        }
      />

      {tenantQuery.isPending ? <TenantDirectoryLoading /> : null}

      {tenantQuery.isError ? (
        <div
          role="alert"
          className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          {errorMessage(tenantQuery.error)}
        </div>
      ) : null}

      {tenantQuery.isSuccess && tenants.length === 0 ? (
        <TenantDirectoryEmpty />
      ) : null}

      {tenantQuery.isSuccess && tenants.length > 0 ? (
        <TenantDirectoryTable tenants={tenants} />
      ) : null}
    </div>
  )
}

function TenantDirectoryLoading() {
  return (
    <div
      aria-label="Loading tenants"
      className="grid gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm"
    >
      {['name', 'status', 'members', 'created'].map((item) => (
        <div
          key={item}
          className="h-10 animate-pulse rounded bg-slate-100"
        />
      ))}
    </div>
  )
}

function TenantDirectoryEmpty() {
  return (
    <section className="rounded-md border border-slate-200 bg-white px-6 py-10 text-center shadow-sm">
      <div className="mx-auto grid size-12 place-items-center rounded-md bg-blue-50 text-blue-700">
        <Building2 aria-hidden="true" size={23} />
      </div>
      <h2 className="mt-4 text-base font-semibold text-slate-950">
        No tenant organizations yet
      </h2>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-600">
        Create an invitation for an airline tenant admin to start onboarding a
        new organization.
      </p>
      <Link
        to="/platform/invitations/new"
        className="mt-5 inline-flex h-9 items-center gap-2 rounded-md bg-blue-700 px-3 text-sm font-semibold text-white hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-700"
      >
        Create tenant invitation
        <ArrowRight aria-hidden="true" size={15} />
      </Link>
    </section>
  )
}

function TenantDirectoryTable({
  tenants,
}: {
  tenants: PlatformTenantSummary[]
}) {
  return (
    <section
      aria-labelledby="tenant-directory-table-heading"
      className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm"
    >
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h2
          id="tenant-directory-table-heading"
          className="text-sm font-semibold text-slate-950"
        >
          Tenant organizations
        </h2>
        <span className="inline-flex items-center gap-2 text-xs text-slate-500">
          <Users aria-hidden="true" size={15} />
          {tenants.length} total
        </span>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
            <tr>
              <th scope="col" className="px-4 py-3">
                Organization
              </th>
              <th scope="col" className="px-4 py-3">
                Status
              </th>
              <th scope="col" className="px-4 py-3">
                Members
              </th>
              <th scope="col" className="px-4 py-3">
                Primary admin
              </th>
              <th scope="col" className="px-4 py-3">
                Created
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {tenants.map((tenant) => (
              <tr key={tenant.organizationId}>
                <td className="whitespace-nowrap px-4 py-3 font-medium text-slate-950">
                  <Link
                    to={`/platform/tenants/${tenant.organizationId}`}
                    className="text-blue-700 hover:text-blue-800 hover:underline"
                  >
                    {tenant.organizationName}
                  </Link>
                </td>
                <td className="whitespace-nowrap px-4 py-3">
                  <span
                    className={cn(
                      'inline-flex rounded-full px-2.5 py-1 text-xs font-semibold',
                      statusClassName(tenant.organizationStatus),
                    )}
                  >
                    {statusLabel(tenant.organizationStatus)}
                  </span>
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-slate-700">
                  {tenant.memberCount}
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-slate-700">
                  {tenant.primaryAdminEmail ?? 'Not assigned'}
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-slate-600">
                  {formatDate(tenant.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
