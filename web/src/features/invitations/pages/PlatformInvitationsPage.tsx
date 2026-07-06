import { MailPlus } from 'lucide-react'
import { EmptyState } from '@/shared/components/EmptyState'
import { PageHeader } from '@/shared/components/PageHeader'

export function PlatformInvitationsPage() {
  return (
    <div className="space-y-8">
      <PageHeader
        title="Tenant invitations"
        description="Invite a new airline or tenant administrator from the internal platform console."
      />
      <EmptyState
        icon={MailPlus}
        title="No tenant invitation draft"
        description="A new invitation will onboard the first administrator for an airline tenant."
      />
    </div>
  )
}
