import { ArrowLeft, SearchX } from 'lucide-react'
import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="py-12 text-center">
      <div className="mx-auto grid size-12 place-items-center rounded-md bg-slate-100 text-slate-600">
        <SearchX aria-hidden="true" size={23} />
      </div>
      <p className="mt-5 text-xs font-semibold uppercase text-blue-700">404</p>
      <h1 className="mt-2 text-2xl font-semibold text-slate-950">
        Page not found
      </h1>
      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-600">
        The requested workspace does not exist.
      </p>
      <Link
        to="/platform/dashboard"
        className="mt-6 inline-flex h-9 items-center gap-2 rounded-md bg-blue-700 px-4 text-sm font-semibold text-white hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
      >
        <ArrowLeft aria-hidden="true" size={16} />
        Return to platform console
      </Link>
    </div>
  )
}
