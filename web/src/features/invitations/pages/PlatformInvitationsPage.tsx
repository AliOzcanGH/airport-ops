import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Check, Clipboard, MailPlus } from 'lucide-react'
import { invitationApi } from '@/features/invitations/api/invitationApi'
import { ApiError } from '@/shared/api/ApiError'
import {
  createPlatformInvitationRequestSchema,
  type PlatformInvitationResponse,
} from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

type FieldErrors = Partial<Record<'email' | 'organizationName', string>>

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
    if (error.errorCode === 'MISSING_PERMISSION') {
      return 'Your platform account is missing the invitation permission.'
    }
    if (error.errorCode === 'USER_NOT_PROVISIONED') {
      return 'Your authenticated user is not provisioned in IAM.'
    }
    return error.message
  }
  return 'Invitation could not be created. Try again shortly.'
}

export function PlatformInvitationsPage() {
  const [email, setEmail] = useState('')
  const [organizationName, setOrganizationName] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [copyState, setCopyState] = useState<'idle' | 'copied' | 'failed'>('idle')
  const createInvitation = useMutation({
    mutationFn: invitationApi.createPlatformInvitation,
    onSuccess: () => setCopyState('idle'),
  })
  const invitation = createInvitation.data
  const acceptLink = invitation?.devAcceptLink ?? ''

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    createInvitation.reset()
    setCopyState('idle')
    const parsed = createPlatformInvitationRequestSchema.safeParse({
      email,
      organizationName,
    })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({
        email: errors.email?.[0],
        organizationName: errors.organizationName?.[0],
      })
      return
    }
    setFieldErrors({})
    createInvitation.mutate(parsed.data)
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
        title="Tenant invitations"
        description="Invite a new airline or tenant administrator from the internal platform console."
      />

      <section
        aria-labelledby="create-invitation-heading"
        className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(360px,420px)]"
      >
        <form
          onSubmit={submit}
          noValidate
          className="space-y-5 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
        >
          <div>
            <h2
              id="create-invitation-heading"
              className="text-sm font-semibold text-slate-950"
            >
              Create airline tenant invitation
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-600">
              The invited administrator will complete onboarding through a public invitation page.
            </p>
          </div>

          <div>
            <label htmlFor="invitation-email" className="text-sm font-medium text-slate-800">
              Invited admin email
            </label>
            <input
              id="invitation-email"
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'invitation-email-error' : undefined}
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              placeholder="tenant.admin@example-airline.demo"
            />
            {fieldErrors.email ? (
              <p id="invitation-email-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.email}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="organization-name" className="text-sm font-medium text-slate-800">
              Airline / organization name
            </label>
            <input
              id="organization-name"
              name="organizationName"
              type="text"
              autoComplete="organization"
              value={organizationName}
              onChange={(event) => setOrganizationName(event.target.value)}
              aria-invalid={Boolean(fieldErrors.organizationName)}
              aria-describedby={
                fieldErrors.organizationName ? 'organization-name-error' : undefined
              }
              className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              placeholder="Qatar Airways"
            />
            {fieldErrors.organizationName ? (
              <p id="organization-name-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.organizationName}
              </p>
            ) : null}
          </div>

          {createInvitation.isError ? (
            <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
              {createErrorMessage(createInvitation.error)}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={createInvitation.isPending}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-blue-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-blue-800 disabled:cursor-wait disabled:opacity-70"
          >
            <MailPlus aria-hidden="true" size={17} />
            {createInvitation.isPending ? 'Creating invitation...' : 'Create invitation'}
          </button>
        </form>

        <InvitationResult
          invitation={invitation}
          acceptLink={acceptLink}
          copyState={copyState}
          onCopy={copyLink}
        />
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
  invitation?: PlatformInvitationResponse
  acceptLink: string
  copyState: 'idle' | 'copied' | 'failed'
  onCopy: () => void
}) {
  if (!invitation) {
    return (
      <aside className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid size-11 place-items-center rounded-md bg-slate-100 text-slate-600">
          <MailPlus aria-hidden="true" size={21} />
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
              ? 'Invitation email sent to the tenant administrator.'
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
        <div>
          <dt className="font-medium text-emerald-950">Organization</dt>
          <dd className="mt-0.5 text-emerald-800">{invitation.organizationName}</dd>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <dt className="font-medium text-emerald-950">Status</dt>
            <dd className="mt-0.5 text-emerald-800">{invitation.status}</dd>
          </div>
          <div>
            <dt className="font-medium text-emerald-950">Expires</dt>
            <dd className="mt-0.5 text-emerald-800">{formatDate(invitation.expiresAt)}</dd>
          </div>
        </div>
        <div>
          <dt className="font-medium text-emerald-950">Email delivery</dt>
          <dd className="mt-0.5 text-emerald-800">
            {invitation.emailDeliveryStatus}
            {invitation.emailSentAt ? ` - sent ${formatDate(invitation.emailSentAt)}` : ''}
          </dd>
        </div>
      </dl>

      {acceptLink ? (
        <div>
          <label htmlFor="accept-link" className="text-xs font-semibold uppercase text-emerald-900">
            Local/dev fallback accept link
          </label>
          <textarea
            id="accept-link"
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
