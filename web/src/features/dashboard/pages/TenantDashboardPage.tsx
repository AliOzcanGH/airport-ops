import { useQuery } from '@tanstack/react-query'
import { Building2, MapPinned, PlaneTakeoff } from 'lucide-react'
import { dashboardOverviewQueryOptions } from '@/features/dashboard/api/dashboardQueries'
import { useCurrentUser } from '@/features/auth/hooks/useAuthSession'
import { PageHeader } from '@/shared/components/PageHeader'

export function TenantDashboardPage() {
  const currentUser = useCurrentUser()
  const overview = useQuery(dashboardOverviewQueryOptions)

  if (overview.isPending) {
    return (
      <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <p className="text-sm font-medium text-slate-950">Loading dashboard</p>
        <p className="mt-1 text-sm text-slate-600">
          Fetching your organization workspace.
        </p>
      </section>
    )
  }

  if (overview.isError) {
    return (
      <section
        role="alert"
        className="rounded-md border border-red-200 bg-red-50 p-5 text-red-900"
      >
        <h1 className="text-sm font-semibold">Dashboard unavailable</h1>
        <p className="mt-1 text-sm">
          We couldn&apos;t load your organization workspace. Please try again.
        </p>
      </section>
    )
  }

  const tenant = overview.data

  return (
    <div className="space-y-8">
      <PageHeader
        title={tenant.organizationName}
        description="Organization workspace for airline users managing their own tenant operations."
      />

      <section
        aria-labelledby="tenant-access-heading"
        className="grid gap-5 border-y border-slate-200 py-5 md:grid-cols-3"
      >
        <div>
          <h2
            id="tenant-access-heading"
            className="text-sm font-semibold text-slate-950"
          >
            Organization access
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {currentUser.data?.email}
          </p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-500">
            Status and roles
          </p>
          <p className="mt-2 text-sm font-medium text-slate-800">
            {tenant.organizationStatus.replaceAll('_', ' ')}
          </p>
          <p className="mt-1 text-sm text-slate-600">{tenant.roles.join(', ')}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-500">
            Permissions
          </p>
          <ul className="mt-2 max-h-32 space-y-1 overflow-auto text-slate-700">
            {tenant.permissions.map((permission) => (
              <li key={permission} className="font-mono text-xs">
                {permission}
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section aria-labelledby="tenant-workspace-heading">
        <h2
          id="tenant-workspace-heading"
          className="mb-3 text-sm font-semibold text-slate-950"
        >
          Organization workspace
        </h2>
        <div className="grid gap-4 sm:grid-cols-3">
          {[
            {
              title: 'Organization profile',
              description: 'Airline tenant details and onboarding state.',
              icon: Building2,
            },
            {
              title: 'Stations and gates',
              description: 'Organization-owned operational locations.',
              icon: MapPinned,
            },
            {
              title: 'Flight operations',
              description: 'Tenant flight and task activity.',
              icon: PlaneTakeoff,
            },
          ].map(({ title, description, icon: Icon }) => (
            <div
              key={title}
              className="min-h-36 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
            >
              <div className="grid size-10 place-items-center rounded-md bg-teal-50 text-teal-700">
                <Icon aria-hidden="true" size={20} />
              </div>
              <h3 className="mt-4 text-sm font-semibold text-slate-950">
                {title}
              </h3>
              <p className="mt-1 text-sm leading-6 text-slate-600">
                {description}
              </p>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
