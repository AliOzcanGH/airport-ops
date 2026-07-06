import type { LucideIcon } from 'lucide-react'

type EmptyStateProps = {
  icon: LucideIcon
  title: string
  description: string
}

export function EmptyState({ icon: Icon, title, description }: EmptyStateProps) {
  return (
    <div className="flex min-h-64 flex-col items-center justify-center border-y border-slate-200 px-6 py-12 text-center">
      <div className="grid size-11 place-items-center rounded-md bg-slate-100 text-slate-600">
        <Icon aria-hidden="true" size={21} />
      </div>
      <h2 className="mt-4 text-base font-semibold text-slate-950">{title}</h2>
      <p className="mt-1 max-w-md text-sm leading-6 text-slate-600">
        {description}
      </p>
    </div>
  )
}
