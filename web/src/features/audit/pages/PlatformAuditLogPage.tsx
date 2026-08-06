import { useQuery } from '@tanstack/react-query'
import { platformAuditLogsQueryOptions } from '@/features/audit/api/auditLogsQueries'
import { PageHeader } from '@/shared/components/PageHeader'

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function PlatformAuditLogPage() {
  const auditLogs = useQuery(platformAuditLogsQueryOptions)

  return (
    <div className="space-y-8">
      <PageHeader
        title="Audit log"
        description="Critical actions performed across every tenant organization."
      />

      {auditLogs.isPending ? (
        <p className="text-sm text-slate-600">Loading audit log...</p>
      ) : auditLogs.isError ? (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
          We couldn&apos;t load the audit log. Please try again.
        </div>
      ) : (
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Organization</th>
                <th className="px-4 py-3">Action</th>
                <th className="px-4 py-3">Resource</th>
                <th className="px-4 py-3">Actor</th>
                <th className="px-4 py-3">Occurred at</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {auditLogs.data?.map((entry) => (
                <tr key={entry.id}>
                  <td className="px-4 py-3 text-xs text-slate-500">
                    {entry.organizationId ?? 'Platform'}
                  </td>
                  <td className="px-4 py-3 font-medium text-slate-900">{entry.action}</td>
                  <td className="px-4 py-3 text-slate-700">
                    {entry.resourceType}
                    {entry.resourceId ? (
                      <span className="ml-1 text-xs text-slate-400">{entry.resourceId}</span>
                    ) : null}
                  </td>
                  <td className="px-4 py-3 text-slate-700">{entry.actorEmail ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-700">{formatDate(entry.occurredAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {auditLogs.data?.length === 0 ? (
            <p className="p-4 text-sm text-slate-600">No audit entries yet.</p>
          ) : null}
        </div>
      )}
    </div>
  )
}
