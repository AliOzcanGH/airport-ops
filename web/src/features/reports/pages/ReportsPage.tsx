import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { dailyFlightsQueryOptions, gateUtilizationQueryOptions } from '@/features/reports/api/reportsQueries'
import { PageHeader } from '@/shared/components/PageHeader'

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

export function ReportsPage() {
  const [date, setDate] = useState(today)

  const dailyFlights = useQuery(dailyFlightsQueryOptions(date))
  const gateUtilization = useQuery(gateUtilizationQueryOptions(date))

  return (
    <div className="space-y-8">
      <PageHeader
        title="Reports"
        description="Daily flight activity and gate utilization for your organization."
      />

      <div className="max-w-xs">
        <label htmlFor="report-date" className="block text-sm font-medium text-slate-700">
          Date
        </label>
        <input
          id="report-date"
          type="date"
          value={date}
          onChange={(event) => setDate(event.target.value)}
          className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
        />
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-slate-900">Daily flight summary</h2>
          {dailyFlights.isPending ? (
            <p className="mt-3 text-sm text-slate-600">Loading...</p>
          ) : dailyFlights.isError ? (
            <div role="alert" className="mt-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-900">
              We couldn&apos;t load the daily flight summary. Please try again.
            </div>
          ) : (
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex items-center justify-between">
                <dt className="text-slate-600">Total flights</dt>
                <dd className="font-medium text-slate-900">{dailyFlights.data?.totalFlights}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-slate-600">Delayed flights</dt>
                <dd className="font-medium text-slate-900">{dailyFlights.data?.delayedFlights}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-slate-600">Cancelled flights</dt>
                <dd className="font-medium text-slate-900">{dailyFlights.data?.cancelledFlights}</dd>
              </div>
            </dl>
          )}
        </div>

        <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
          <h2 className="px-5 pt-5 text-sm font-semibold text-slate-900">Gate utilization</h2>
          {gateUtilization.isPending ? (
            <p className="p-5 text-sm text-slate-600">Loading...</p>
          ) : gateUtilization.isError ? (
            <div role="alert" className="m-5 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-900">
              We couldn&apos;t load gate utilization. Please try again.
            </div>
          ) : (
            <table className="mt-3 w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3">Gate</th>
                  <th className="px-5 py-3">Flights</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {gateUtilization.data?.map((entry) => (
                  <tr key={entry.gateId}>
                    <td className="px-5 py-3 text-slate-700">{entry.gateId}</td>
                    <td className="px-5 py-3 font-medium text-slate-900">{entry.flightCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {gateUtilization.data?.length === 0 ? (
            <p className="p-5 text-sm text-slate-600">No gate activity for this date.</p>
          ) : null}
        </div>
      </div>
    </div>
  )
}
