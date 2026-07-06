import { KeyRound } from 'lucide-react'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'

export function LoginPage() {
  return (
    <div className="space-y-8">
      <PageHeader
        title="Sign in"
        description="Access the internal platform console or your airline tenant workspace."
      />
      <EmptyState
        icon={KeyRound}
        title="No authentication session"
        description="Identity sign-in is not connected in the current frontend shell."
      />
    </div>
  )
}
