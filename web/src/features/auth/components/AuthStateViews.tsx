import { AlertTriangle, LoaderCircle, RefreshCw } from 'lucide-react'

export function SessionLoadingView() {
  return (
    <div className="grid min-h-screen place-items-center bg-slate-50 px-4">
      <div className="flex items-center gap-3 text-sm font-medium text-slate-600">
        <LoaderCircle className="animate-spin" size={19} aria-hidden="true" />
        Loading your workspace
      </div>
    </div>
  )
}

export function SessionErrorView({ retry }: { retry: () => void }) {
  return (
    <div className="grid min-h-screen place-items-center bg-slate-50 px-4">
      <div className="w-full max-w-md rounded-md border border-red-200 bg-white p-6 shadow-sm">
        <AlertTriangle className="text-red-700" size={22} aria-hidden="true" />
        <h1 className="mt-4 text-lg font-semibold text-slate-950">
          Workspace session unavailable
        </h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          Airport Ops could not load your access context. Try again shortly.
        </p>
        <button
          type="button"
          onClick={retry}
          className="mt-5 inline-flex h-9 items-center gap-2 rounded-md bg-slate-950 px-3 text-sm font-semibold text-white hover:bg-slate-800"
        >
          <RefreshCw size={16} aria-hidden="true" />
          Try again
        </button>
      </div>
    </div>
  )
}
