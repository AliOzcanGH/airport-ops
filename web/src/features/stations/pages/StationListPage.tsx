import { useQuery } from '@tanstack/react-query'
import { MapPin, Plus } from 'lucide-react'
import { Link } from 'react-router'
import { stationsQueryOptions } from '@/features/stations/api/stationsQueries'
import { PageHeader } from '@/shared/components/PageHeader'

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function StationListPage() {
  const stations = useQuery(stationsQueryOptions)

  return (
    <div className="space-y-8">
      <PageHeader
        title="Stations"
        description="Stations belonging to your organization. Select one to manage its gates."
        action={
          <Link
            to="/app/stations/new"
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-teal-800"
          >
            <Plus aria-hidden="true" size={17} />
            New station
          </Link>
        }
      />

      {stations.isPending ? (
        <p className="text-sm text-slate-600">Loading stations...</p>
      ) : stations.isError ? (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
          We couldn&apos;t load the station list. Please try again.
        </div>
      ) : (
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Station name</th>
                <th className="px-4 py-3">Airport code</th>
                <th className="px-4 py-3">Gate count</th>
                <th className="px-4 py-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {stations.data?.map((station) => (
                <tr key={station.id}>
                  <td className="px-4 py-3">
                    <Link
                      to={`/app/stations/${station.id}`}
                      className="flex items-center gap-2 font-medium text-teal-700 hover:underline"
                    >
                      <MapPin aria-hidden="true" size={15} />
                      {station.stationName}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-slate-700">{station.airportCode}</td>
                  <td className="px-4 py-3 text-slate-700">{station.gateCount}</td>
                  <td className="px-4 py-3 text-slate-700">{formatDate(station.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {stations.data?.length === 0 ? (
            <p className="p-4 text-sm text-slate-600">No stations yet. Create one to get started.</p>
          ) : null}
        </div>
      )}
    </div>
  )
}
