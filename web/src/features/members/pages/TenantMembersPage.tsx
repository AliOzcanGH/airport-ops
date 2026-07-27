import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, Clipboard, MailPlus, Users } from 'lucide-react'
import { useCurrentUser } from '@/features/auth/hooks/useAuthSession'
import { membersApi } from '@/features/members/api/membersApi'
import { organizationMembersQueryOptions } from '@/features/members/api/membersQueries'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import {
  inviteOrganizationMemberRequestSchema,
  type OrganizationMemberInvitationResponse,
  type OrganizationMemberRole,
} from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

type FieldErrors = Partial<Record<'email' | 'fullName' | 'intendedRole', string>>

const ROLE_OPTIONS: { value: OrganizationMemberRole; label: string }[] = [
  { value: 'OPS_USER', label: 'Operations user' },
  { value: 'VIEWER', label: 'Viewer' },
]

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function createErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'PENDING_INVITATION_EXISTS') {
      return 'A pending invitation already exists for this email.'
    }
    if (error.errorCode === 'DUPLICATE_RESOURCE') {
      return 'This email already belongs to an active member of your organization.'
    }
    if (error.errorCode === 'TENANT_MISMATCH') {
      return 'This organization does not match your authenticated tenant.'
    }
    if (error.errorCode === 'MISSING_PERMISSION') {
      return 'Your account is missing the member invitation permission.'
    }
    return error.message
  }
  return 'Invitation could not be created. Try again shortly.'
}

export function TenantMembersPage() {
  const currentUser = useCurrentUser()
  const organizationId = currentUser.data?.tenantContext?.organizationId ?? ''
  const queryClient = useQueryClient()
  const members = useQuery(organizationMembersQueryOptions(organizationId))

  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [intendedRole, setIntendedRole] = useState<OrganizationMemberRole>('OPS_USER')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [copyState, setCopyState] = useState<'idle' | 'copied' | 'failed'>('idle')

  const inviteMember = useMutation({
    mutationFn: (variables: {
      email: string
      fullName: string
      intendedRole: OrganizationMemberRole
    }) => membersApi.inviteMember(organizationId, variables),
    onSuccess: () => {
      setCopyState('idle')
      void queryClient.invalidateQueries({
        queryKey: queryKeys.app.members(organizationId),
      })
    },
  })
  const invitation = inviteMember.data
  const acceptLink = invitation?.devAcceptLink ?? ''

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    inviteMember.reset()
    setCopyState('idle')
    const parsed = inviteOrganizationMemberRequestSchema.safeParse({
      email,
      fullName,
      intendedRole,
    })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({
        email: errors.email?.[0],
        fullName: errors.fullName?.[0],
        intendedRole: errors.intendedRole?.[0],
      })
      return
    }
    setFieldErrors({})
    inviteMember.mutate(parsed.data)
  }

  const copyLink = async () => {
    if (!acceptLink || !navigator.clipboard) {
      setCopyState('failed')
      return
    }
    try {
      await navigator.clipboard.writeText(acceptLink)
      setCopyState('copied')
    } catch {
      setCopyState('failed')
    }
  }

  return (
    <div className="space-y-8">
      <PageHeader
        title="Organization members"
        description="Invite operations users and viewers into your organization, and review who already has access."
      />

      <section
        aria-labelledby="invite-member-heading"
        className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(360px,420px)]"
      >
        <form
          onSubmit={submit}
          noValidate
          className="space-y-5 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div>
            <h2
              id="invite-member-heading"
              className="text-sm font-semibold text-slate-950"
            >
              Invite member
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-600">
              The invited member joins this organization once they accept the invitation.
            </p>
          </div>

          <div>
            <label htmlFor="member-email" className="text-sm font-medium text-slate-800">
              Email
            </label>
            <input
              id="member-email"
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'member-email-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              placeholder="new.member@example-airline.demo"
            />
            {fieldErrors.email ? (
              <p id="member-email-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.email}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="member-full-name" className="text-sm font-medium text-slate-800">
              Full name
            </label>
            <input
              id="member-full-name"
              name="fullName"
              type="text"
              autoComplete="name"
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              aria-invalid={Boolean(fieldErrors.fullName)}
              aria-describedby={fieldErrors.fullName ? 'member-full-name-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              placeholder="New Member"
            />
            {fieldErrors.fullName ? (
              <p id="member-full-name-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.fullName}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="member-role" className="text-sm font-medium text-slate-800">
              Role
            </label>
            <select
              id="member-role"
              name="intendedRole"
              value={intendedRole}
              onChange={(event) =>
                setIntendedRole(event.target.value as OrganizationMemberRole)
              }
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
            >
              {ROLE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {fieldErrors.intendedRole ? (
              <p className="mt-1.5 text-xs text-red-700">{fieldErrors.intendedRole}</p>
            ) : null}
          </div>

          {inviteMember.isError ? (
            <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
              {createErrorMessage(inviteMember.error)}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={inviteMember.isPending || !organizationId}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-blue-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-blue-800 disabled:cursor-wait disabled:opacity-70"
          >
            <MailPlus aria-hidden="true" size={17} />
            {inviteMember.isPending ? 'Sending invitation...' : 'Send invitation'}
          </button>
        </form>

        <InvitationResult
          invitation={invitation}
          acceptLink={acceptLink}
          copyState={copyState}
          onCopy={copyLink}
        />
      </section>

      <section aria-labelledby="member-list-heading">
        <h2
          id="member-list-heading"
          className="mb-3 text-sm font-semibold text-slate-950"
        >
          Active members
        </h2>
        {members.isPending ? (
          <p className="text-sm text-slate-600">Loading members...</p>
        ) : members.isError ? (
          <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-900">
            We couldn&apos;t load the member list. Please try again.
          </div>
        ) : (
          <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Full name</th>
                  <th className="px-4 py-3">Roles</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Joined</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {members.data?.map((member) => (
                  <tr key={member.memberId}>
                    <td className="px-4 py-3 text-slate-900">{member.email}</td>
                    <td className="px-4 py-3 text-slate-700">{member.fullName}</td>
                    <td className="px-4 py-3 text-slate-700">
                      {member.roles.join(', ')}
                    </td>
                    <td className="px-4 py-3 text-slate-700">{member.memberStatus}</td>
                    <td className="px-4 py-3 text-slate-700">
                      {member.joinedAt ? formatDate(member.joinedAt) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {members.data?.length === 0 ? (
              <p className="p-4 text-sm text-slate-600">No active members yet.</p>
            ) : null}
          </div>
        )}
      </section>
    </div>
  )
}

function InvitationResult({
  invitation,
  acceptLink,
  copyState,
  onCopy,
}: {
  invitation?: OrganizationMemberInvitationResponse
  acceptLink: string
  copyState: 'idle' | 'copied' | 'failed'
  onCopy: () => void
}) {
  if (!invitation) {
    return (
      <aside className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid size-11 place-items-center rounded-md bg-slate-100 text-slate-600">
          <Users aria-hidden="true" size={21} />
        </div>
        <h2 className="mt-4 text-sm font-semibold text-slate-950">
          Local invitation link
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          After creation, email delivery status appears here. Local/dev fallback links are shown only when enabled by the backend.
        </p>
      </aside>
    )
  }

  return (
    <aside className="space-y-4 rounded-md border border-emerald-200 bg-emerald-50 p-5 shadow-sm">
      <div className="flex items-start gap-3">
        <span className="grid size-9 shrink-0 place-items-center rounded-md bg-emerald-600 text-white">
          <Check aria-hidden="true" size={18} />
        </span>
        <div>
          <h2 className="text-sm font-semibold text-emerald-950">
            Invitation created
          </h2>
          <p className="mt-1 text-sm leading-6 text-emerald-800">
            {invitation.emailDeliveryStatus === 'SENT'
              ? 'Invitation email sent to the new member.'
              : invitation.emailDeliveryStatus === 'FAILED'
                ? 'Invitation created, but email delivery failed. Use the local development fallback link if it is available.'
                : 'Invitation created. Email delivery has not been completed yet.'}
          </p>
        </div>
      </div>

      <dl className="grid gap-3 text-sm">
        <div>
          <dt className="font-medium text-emerald-950">Email</dt>
          <dd className="mt-0.5 break-words text-emerald-800">{invitation.email}</dd>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <dt className="font-medium text-emerald-950">Role</dt>
            <dd className="mt-0.5 text-emerald-800">{invitation.intendedRole}</dd>
          </div>
          <div>
            <dt className="font-medium text-emerald-950">Expires</dt>
            <dd className="mt-0.5 text-emerald-800">{formatDate(invitation.expiresAt)}</dd>
          </div>
        </div>
      </dl>

      {acceptLink ? (
        <div>
          <label htmlFor="member-accept-link" className="text-xs font-semibold uppercase text-emerald-900">
            Local/dev fallback accept link
          </label>
          <textarea
            id="member-accept-link"
            readOnly
            rows={3}
            value={acceptLink}
            className="mt-1.5 w-full resize-none rounded-md border border-emerald-300 bg-white px-3 py-2 font-mono text-xs text-slate-950"
          />
          <p className="mt-1.5 text-xs text-emerald-800">
            Development fallback only. Disable this outside local/dev environments.
          </p>
          <button
            type="button"
            onClick={onCopy}
            className="mt-2 inline-flex h-9 items-center gap-2 rounded-md border border-emerald-300 bg-white px-3 text-sm font-semibold text-emerald-800 hover:bg-emerald-100"
          >
            <Clipboard aria-hidden="true" size={16} />
            Copy link
          </button>
          {copyState === 'copied' ? (
            <p className="mt-2 text-xs font-medium text-emerald-800">Copied.</p>
          ) : null}
          {copyState === 'failed' ? (
            <p className="mt-2 text-xs font-medium text-red-700">
              Copy failed. Select the link manually.
            </p>
          ) : null}
        </div>
      ) : null}
    </aside>
  )
}
