import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { CheckCircle2, Eye, EyeOff, ShieldAlert, UserRoundCheck } from 'lucide-react'
import { Link, useLocation } from 'react-router'
import { invitationApi } from '@/features/invitations/api/invitationApi'
import { ApiError } from '@/shared/api/ApiError'
import {
  acceptInvitationFormSchema,
  invitationTokenSchema,
  type InvitationValidationResponse,
} from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

type FieldErrors = Partial<
  Record<'fullName' | 'password' | 'confirmPassword', string>
>
type ValidationState =
  | { status: 'idle' }
  | { status: 'pending' }
  | { status: 'success'; data: InvitationValidationResponse }
  | { status: 'error'; error: unknown }

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function invitationErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.errorCode === 'INVITATION_NOT_FOUND') {
      return 'This invitation link is invalid or has been cancelled.'
    }
    if (error.errorCode === 'INVITATION_ALREADY_USED') {
      return 'This invitation has already been used.'
    }
    if (error.errorCode === 'INVITATION_EXPIRED') {
      return 'This invitation has expired. Ask a platform administrator for a new invitation.'
    }
    if (error.errorCode === 'VALIDATION_ERROR') {
      return 'The invitation token is malformed.'
    }
    if (error.errorCode === 'IAM_USER_ALREADY_EXISTS') {
      return 'An account already exists for this invitation email.'
    }
    if (error.errorCode === 'ORGANIZATION_ALREADY_EXISTS') {
      return 'An organization already exists with this name.'
    }
    return error.message
  }
  return 'Invitation onboarding is temporarily unavailable. Try again shortly.'
}

export function InvitationAcceptPage() {
  const location = useLocation()
  const [rawToken] = useState(() => new URLSearchParams(location.search).get('token') ?? '')
  const parsedToken = rawToken ? invitationTokenSchema.safeParse(rawToken) : null
  const validToken = parsedToken?.success ? parsedToken.data : null
  const tokenProblem: 'missing' | 'malformed' | null = !rawToken
    ? 'missing'
    : validToken
      ? null
      : 'malformed'
  const validationStartedFor = useRef<string | null>(null)
  const [validation, setValidation] = useState<ValidationState>(() =>
    validToken ? { status: 'pending' } : { status: 'idle' },
  )
  const acceptInvitation = useMutation({
    mutationFn: invitationApi.acceptInvitation,
  })

  useEffect(() => {
    if (rawToken && location.search) {
      window.history.replaceState(window.history.state, '', '/invitations/accept')
    }
  }, [location.search, rawToken])

  useEffect(() => {
    if (!validToken || validationStartedFor.current === validToken) return
    validationStartedFor.current = validToken
    invitationApi
      .validateInvitation({ token: validToken })
      .then((data) => setValidation({ status: 'success', data }))
      .catch((error: unknown) => setValidation({ status: 'error', error }))
  }, [validToken])

  return (
    <div className="space-y-8">
      <PageHeader
        title="Airline admin onboarding"
        description="Invited airline administrators complete tenant onboarding here."
      />

      {!rawToken || tokenProblem ? (
        <InvitationProblem
          title={tokenProblem === 'malformed' ? 'Invalid invitation token' : 'No invitation token'}
          description={
            tokenProblem === 'malformed'
              ? 'The invitation link is malformed. Ask a platform administrator for a new link.'
              : 'A valid airline tenant invitation link is required to begin onboarding.'
          }
        />
      ) : null}

      {rawToken && !tokenProblem && validation.status === 'pending' ? (
        <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-sm font-medium text-slate-950">Checking invitation...</p>
          <p className="mt-1 text-sm text-slate-600">
            This page is validating the invitation without starting a login session.
          </p>
        </section>
      ) : null}

      {rawToken && !tokenProblem && validation.status === 'error' ? (
        <InvitationProblem
          title="Invitation cannot be used"
          description={invitationErrorMessage(validation.error)}
        />
      ) : null}

      {rawToken && validation.status === 'success' && !acceptInvitation.data ? (
        <AcceptInvitationForm
          token={rawToken}
          invitation={validation.data}
          isPending={acceptInvitation.isPending}
          error={acceptInvitation.error}
          onSubmit={(request) => acceptInvitation.mutate(request)}
        />
      ) : null}

      {acceptInvitation.data ? (
        <AcceptanceResult response={acceptInvitation.data} />
      ) : null}
    </div>
  )
}

function InvitationProblem({
  title,
  description,
}: {
  title: string
  description: string
}) {
  return (
    <section
      role="alert"
      className="rounded-md border border-amber-200 bg-amber-50 p-5 text-amber-950"
    >
      <div className="flex gap-3">
        <ShieldAlert aria-hidden="true" className="mt-0.5 shrink-0" size={20} />
        <div>
          <h2 className="text-sm font-semibold">{title}</h2>
          <p className="mt-1 text-sm leading-6 text-amber-800">{description}</p>
        </div>
      </div>
    </section>
  )
}

function AcceptInvitationForm({
  token,
  invitation,
  isPending,
  error,
  onSubmit,
}: {
  token: string
  invitation: InvitationValidationResponse
  isPending: boolean
  error: unknown
  onSubmit: (request: { token: string; fullName: string; password: string }) => void
}) {
  const [fullName, setFullName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const parsed = acceptInvitationFormSchema.safeParse({
      fullName,
      password,
      confirmPassword,
    })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({
        fullName: errors.fullName?.[0],
        password: errors.password?.[0],
        confirmPassword: errors.confirmPassword?.[0],
      })
      return
    }
    setFieldErrors({})
    onSubmit({
      token,
      fullName: parsed.data.fullName,
      password: parsed.data.password,
    })
  }

  return (
    <section
      aria-labelledby="accept-invitation-heading"
      className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]"
    >
      <aside className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid size-11 place-items-center rounded-md bg-blue-50 text-blue-700">
          <UserRoundCheck aria-hidden="true" size={21} />
        </div>
        <h2 id="accept-invitation-heading" className="mt-4 text-sm font-semibold text-slate-950">
          Invitation details
        </h2>
        <dl className="mt-4 space-y-3 text-sm">
          <div>
            <dt className="font-medium text-slate-950">Organization</dt>
            <dd className="mt-0.5 text-slate-700">{invitation.organizationName}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-950">Invited email</dt>
            <dd className="mt-0.5 break-words text-slate-700">{invitation.invitedEmail}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-950">Expires</dt>
            <dd className="mt-0.5 text-slate-700">{formatDate(invitation.expiresAt)}</dd>
          </div>
        </dl>
      </aside>

      <form onSubmit={submit} noValidate className="space-y-5 rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <div>
          <h2 className="text-sm font-semibold text-slate-950">Create your login</h2>
          <p className="mt-1 text-sm leading-6 text-slate-600">
            Your email and organization come from the invitation. Only your name and password are submitted.
          </p>
        </div>

        <div>
          <label htmlFor="full-name" className="text-sm font-medium text-slate-800">
            Full name
          </label>
          <input
            id="full-name"
            name="fullName"
            type="text"
            autoComplete="name"
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            aria-invalid={Boolean(fieldErrors.fullName)}
            aria-describedby={fieldErrors.fullName ? 'full-name-error' : undefined}
            className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />
          {fieldErrors.fullName ? (
            <p id="full-name-error" className="mt-1.5 text-xs text-red-700">
              {fieldErrors.fullName}
            </p>
          ) : null}
        </div>

        <div>
          <label htmlFor="new-password" className="text-sm font-medium text-slate-800">
            Password
          </label>
          <div className="relative mt-1.5">
            <input
              id="new-password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? 'new-password-error' : undefined}
              className="h-10 w-full rounded-md border border-slate-300 px-3 pr-11 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
            />
            <button
              type="button"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              className="absolute right-1 top-1 inline-grid size-8 place-items-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-800"
            >
              {showPassword ? <EyeOff aria-hidden="true" size={17} /> : <Eye aria-hidden="true" size={17} />}
            </button>
          </div>
          {fieldErrors.password ? (
            <p id="new-password-error" className="mt-1.5 text-xs text-red-700">
              {fieldErrors.password}
            </p>
          ) : null}
        </div>

        <div>
          <label htmlFor="confirm-password" className="text-sm font-medium text-slate-800">
            Confirm password
          </label>
          <input
            id="confirm-password"
            name="confirmPassword"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            aria-invalid={Boolean(fieldErrors.confirmPassword)}
            aria-describedby={
              fieldErrors.confirmPassword ? 'confirm-password-error' : undefined
            }
            className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />
          {fieldErrors.confirmPassword ? (
            <p id="confirm-password-error" className="mt-1.5 text-xs text-red-700">
              {fieldErrors.confirmPassword}
            </p>
          ) : null}
        </div>

        {error ? (
          <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
            {invitationErrorMessage(error)}
          </div>
        ) : null}

        <button
          type="submit"
          disabled={isPending}
          className="inline-flex h-10 items-center justify-center rounded-md bg-blue-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-blue-800 disabled:cursor-wait disabled:opacity-70"
        >
          {isPending ? 'Accepting invitation...' : 'Accept invitation'}
        </button>
      </form>
    </section>
  )
}

function AcceptanceResult({
  response,
}: {
  response: {
    email: string
    organizationName: string
    organizationStatus: string
    userStatus: string
    provisioningStatus: string
    message: string
  }
}) {
  const ready = response.provisioningStatus === 'READY'

  return (
    <section
      role="status"
      className="rounded-md border border-emerald-200 bg-emerald-50 p-5 text-emerald-950 shadow-sm"
    >
      <div className="flex gap-3">
        <CheckCircle2 aria-hidden="true" className="mt-0.5 shrink-0" size={21} />
        <div>
          <h2 className="text-sm font-semibold">
            {ready ? 'Invitation accepted' : 'Login setup pending'}
          </h2>
          <p className="mt-1 text-sm leading-6 text-emerald-800">{response.message}</p>
          <dl className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="font-medium">Email</dt>
              <dd className="mt-0.5 break-words text-emerald-800">{response.email}</dd>
            </div>
            <div>
              <dt className="font-medium">Organization</dt>
              <dd className="mt-0.5 text-emerald-800">{response.organizationName}</dd>
            </div>
            <div>
              <dt className="font-medium">User status</dt>
              <dd className="mt-0.5 text-emerald-800">{response.userStatus}</dd>
            </div>
            <div>
              <dt className="font-medium">Organization status</dt>
              <dd className="mt-0.5 text-emerald-800">{response.organizationStatus}</dd>
            </div>
          </dl>
          {ready ? (
            <Link
              to="/login?switchAccount=true"
              className="mt-5 inline-flex h-10 items-center rounded-md bg-emerald-700 px-4 text-sm font-semibold text-white hover:bg-emerald-800"
            >
              Log in
            </Link>
          ) : (
            <p className="mt-5 text-sm font-medium text-emerald-900">
              Do not try to log in yet. Platform support needs to complete login setup.
            </p>
          )}
        </div>
      </div>
    </section>
  )
}
