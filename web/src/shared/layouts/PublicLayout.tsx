import { ArrowLeft, Plane } from 'lucide-react'
import { Link, Outlet } from 'react-router'

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-5xl items-center px-4 sm:px-6">
          <Link
            to="/"
            className="flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-600"
          >
            <span className="grid size-9 place-items-center rounded-md bg-blue-700 text-white">
              <Plane aria-hidden="true" size={19} />
            </span>
            <span>
              <span className="block text-sm font-semibold text-slate-950">
                Airport Ops
              </span>
              <span className="block text-xs text-slate-500">Operations lab</span>
            </span>
          </Link>
          <Link
            to="/"
            className="ml-auto inline-flex h-9 items-center gap-2 rounded-md px-3 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-950"
          >
            <ArrowLeft aria-hidden="true" size={17} />
            Home
          </Link>
        </div>
      </header>
      <main className="mx-auto w-full max-w-3xl px-4 py-10 sm:px-6 sm:py-16">
        <Outlet />
      </main>
    </div>
  )
}
