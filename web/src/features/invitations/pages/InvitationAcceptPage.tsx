import { UserRoundCheck } from 'lucide-react'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'

export function InvitationAcceptPage() {
  return (
    <div className="space-y-8">
      <PageHeader
        title="Airline admin onboarding"
        description="Invited airline administrators complete tenant onboarding here."
      />
      <EmptyState
        icon={UserRoundCheck}
        title="No tenant invitation loaded"
        description="A valid airline tenant invitation link is required to begin onboarding."
      />
    </div>
  )
}
