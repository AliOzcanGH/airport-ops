import { Building2, MapPinned, PlaneTakeoff } from 'lucide-react'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'

export function TenantDashboardPage() {
  return (
    <div className="space-y-8">
      <PageHeader
        title="Airline tenant dashboard"
        description="Organization workspace for airline users managing their own tenant operations."
      />

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

      <EmptyState
        icon={Building2}
        title="No tenant organization loaded"
        description="Organization data will appear here after an airline user signs in."
      />
    </div>
  )
}
