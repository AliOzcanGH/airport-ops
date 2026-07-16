import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Eye, EyeOff, KeyRound } from 'lucide-react'
import { QRCodeSVG } from 'qrcode.react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '@/shared/api/ApiError'
import {
  loginRequestSchema,
  verifyMfaRequestSchema,
  type LoginResponse,
} from '@/shared/api/schemas'
import {
  useCurrentUser,
  useLogin,
  useLogout,
  useVerifyMfa,
} from '@/features/auth/hooks/useAuthSession'
import { defaultWorkspacePath } from '@/features/auth/routing/workspaceRouting'
import {
  SessionErrorView,
  SessionLoadingView,
} from '@/features/auth/components/AuthStateViews'
import { AccessUnavailablePage } from '@/features/auth/pages/AccessUnavailablePage'

type FieldErrors = Partial<Record<'email' | 'password', string>>

const MFA_CODE_INVALID_MESSAGE =
  'The code was not accepted. Enter the current 6-digit code and try again.'
const MFA_CHALLENGE_EXPIRED_MESSAGE =
  'Your verification session expired. Please log in again.'
const MFA_CHALLENGE_LOCKED_MESSAGE =
  'Too many incorrect codes. Please log in again.'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const currentUser = useCurrentUser()
  const login = useLogin()
  const verifyMfa = useVerifyMfa()
  const logout = useLogout()
  const switchAccountRequested =
    new URLSearchParams(location.search).get('switchAccount') === 'true'
  const [forceLoginForm] = useState(switchAccountRequested)
  const [switchAccountWarning, setSwitchAccountWarning] = useState<string | null>(null)
  const switchAccountCleanupStarted = useRef(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [mfaChallenge, setMfaChallenge] = useState<LoginResponse | null>(null)
  const [mfaCode, setMfaCode] = useState('')
  const [mfaError, setMfaError] = useState<string | null>(null)
  const [passwordMessage, setPasswordMessage] = useState<string | null>(null)

  useEffect(() => {
    if (
      !switchAccountRequested ||
      currentUser.isPending ||
      switchAccountCleanupStarted.current
    ) {
      return
    }

    switchAccountCleanupStarted.current = true

    if (currentUser.data) {
      logout.mutate(undefined, {
        onError: () => {
          setSwitchAccountWarning(
            'The previous session could not be cleared. Logging in will replace it.',
          )
        },
        onSettled: () => {
          navigate('/login', { replace: true })
        },
      })
      return
    }

    navigate('/login', { replace: true })
  }, [
    currentUser.data,
    currentUser.isPending,
    logout,
    navigate,
    switchAccountRequested,
  ])

  if (currentUser.isPending) return <SessionLoadingView />
  if (currentUser.isError) {
    if (currentUser.error instanceof ApiError && currentUser.error.status === 403) {
      return <AccessUnavailablePage />
    }
    return <SessionErrorView retry={() => void currentUser.refetch()} />
  }
  if (currentUser.data && !forceLoginForm) {
    return <Navigate to={defaultWorkspacePath(currentUser.data)} replace />
  }

  const submitPassword = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFieldErrors({})
    setPasswordMessage(null)
    login.reset()
    const parsed = loginRequestSchema.safeParse({ email, password })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({ email: errors.email?.[0], password: errors.password?.[0] })
      return
    }
    login.mutate(parsed.data, {
      onSuccess: (challenge) => {
        setMfaChallenge(challenge)
        setMfaCode('')
        setMfaError(null)
        setPassword('')
      },
    })
  }

  const submitMfa = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!mfaChallenge) return

    setMfaError(null)
    verifyMfa.reset()
    const parsed = verifyMfaRequestSchema.safeParse({
      challengeId: mfaChallenge.challengeId,
      code: mfaCode,
    })
    if (!parsed.success) {
      setMfaError('Enter a 6-digit code.')
      return
    }

    verifyMfa.mutate(parsed.data, {
      onSuccess: (user) => {
        setMfaCode('')
        setMfaChallenge(null)
        navigate(defaultWorkspacePath(user), { replace: true })
      },
      onError: (error) => {
        setMfaCode('')
        if (error instanceof ApiError && error.errorCode === 'MFA_CODE_INVALID') {
          setMfaError(MFA_CODE_INVALID_MESSAGE)
          return
        }
        if (error instanceof ApiError && error.errorCode === 'MFA_CHALLENGE_EXPIRED') {
          returnToPassword(MFA_CHALLENGE_EXPIRED_MESSAGE, false)
          return
        }
        if (error instanceof ApiError && error.errorCode === 'MFA_CHALLENGE_LOCKED') {
          returnToPassword(MFA_CHALLENGE_LOCKED_MESSAGE, false)
          return
        }
        setMfaError('Verification is temporarily unavailable. Please try again.')
      },
    })
  }

  const returnToPassword = (message: string | null = null, resetMutation = true) => {
    setMfaChallenge(null)
    setMfaCode('')
    setMfaError(null)
    setPasswordMessage(message)
    if (resetMutation) verifyMfa.reset()
    login.reset()
  }

  const loginError =
    login.error instanceof ApiError && login.error.errorCode === 'INVALID_CREDENTIALS'
      ? 'Invalid email or password.'
      : login.error instanceof ApiError &&
          login.error.errorCode === 'AUTH_PROVIDER_UNAVAILABLE'
        ? 'Login is temporarily unavailable. Try again shortly.'
        : login.isError
          ? 'Airport Ops could not start your session.'
          : null

  const isEnrollment = mfaChallenge?.outcome === 'MFA_ENROLLMENT_REQUIRED'
  const title = isEnrollment
    ? 'Set up authenticator app'
    : mfaChallenge
      ? 'Enter authenticator code'
      : 'Log in'

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="flex items-center gap-3">
        <span className="grid size-11 place-items-center rounded-md bg-blue-700 text-white">
          <KeyRound aria-hidden="true" size={21} />
        </span>
        <div>
          <h1 className="text-xl font-semibold text-slate-950">{title}</h1>
          {!mfaChallenge ? (
            <p className="mt-1 text-sm text-slate-600">
              Log in to continue to your Airport Ops workspace.
            </p>
          ) : null}
        </div>
      </div>

      {!mfaChallenge ? (
        <form
          onSubmit={submitPassword}
          noValidate
          className="mt-8 space-y-5 rounded-md border border-slate-200 bg-white p-6 shadow-sm"
        >
          <div>
            <label htmlFor="email" className="text-sm font-medium text-slate-800">
              Email
            </label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'email-error' : undefined}
              className="mt-2 h-10 w-full rounded-md border border-slate-300 px-3 text-sm outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
            />
            {fieldErrors.email ? (
              <p id="email-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.email}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="password" className="text-sm font-medium text-slate-800">
              Password
            </label>
            <div className="relative mt-2">
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                aria-invalid={Boolean(fieldErrors.password)}
                aria-describedby={fieldErrors.password ? 'password-error' : undefined}
                className="h-10 w-full rounded-md border border-slate-300 px-3 pr-11 text-sm outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
              />
              <button
                type="button"
                title={showPassword ? 'Hide password' : 'Show password'}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                onClick={() => setShowPassword((visible) => !visible)}
                className="absolute right-1 top-1 grid size-8 place-items-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-800"
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>
            {fieldErrors.password ? (
              <p id="password-error" className="mt-1.5 text-xs text-red-700">
                {fieldErrors.password}
              </p>
            ) : null}
          </div>

          {passwordMessage || loginError ? (
            <p
              role="alert"
              className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-800"
            >
              {passwordMessage ?? loginError}
            </p>
          ) : null}

          {switchAccountWarning ? (
            <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {switchAccountWarning}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={login.isPending || logout.isPending}
            className="inline-flex h-10 w-full items-center justify-center rounded-md bg-blue-700 px-4 text-sm font-semibold text-white hover:bg-blue-800 disabled:cursor-wait disabled:opacity-60"
          >
            {logout.isPending
              ? 'Preparing login...'
              : login.isPending
                ? 'Logging in...'
                : 'Log in'}
          </button>
        </form>
      ) : (
        <form
          onSubmit={submitMfa}
          noValidate
          className="mt-8 space-y-5 rounded-md border border-slate-200 bg-white p-6 shadow-sm"
        >
          {isEnrollment ? (
            <>
              <p className="text-sm leading-6 text-slate-600">
                Scan this QR code with Google Authenticator, Microsoft Authenticator,
                Authy, or another compatible app.
              </p>
              <div
                role="img"
                aria-label="Authenticator setup QR code"
                className="mx-auto w-fit rounded-md border border-slate-200 bg-white p-3"
              >
                <QRCodeSVG value={mfaChallenge.otpauthUri} size={184} level="M" />
              </div>
              <div className="rounded-md bg-slate-50 p-3">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                  Manual entry key
                </p>
                <code className="mt-1 block break-all text-sm font-semibold text-slate-900">
                  {mfaChallenge.manualEntryKey}
                </code>
              </div>
            </>
          ) : (
            <p className="text-sm leading-6 text-slate-600">
              Open your authenticator app and enter the current 6-digit code.
            </p>
          )}

          <div>
            <label htmlFor="mfa-code" className="text-sm font-medium text-slate-800">
              6-digit code
            </label>
            <input
              id="mfa-code"
              name="code"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={6}
              autoComplete="one-time-code"
              autoFocus
              value={mfaCode}
              onChange={(event) =>
                setMfaCode(event.target.value.replace(/\D/g, '').slice(0, 6))
              }
              aria-invalid={Boolean(mfaError)}
              aria-describedby={mfaError ? 'mfa-error' : undefined}
              className="mt-2 h-11 w-full rounded-md border border-slate-300 px-3 text-center font-mono text-lg tracking-[0.35em] outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
            />
          </div>

          {mfaError ? (
            <p
              id="mfa-error"
              role="alert"
              className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-800"
            >
              {mfaError}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={verifyMfa.isPending}
            className="inline-flex h-10 w-full items-center justify-center rounded-md bg-blue-700 px-4 text-sm font-semibold text-white hover:bg-blue-800 disabled:cursor-wait disabled:opacity-60"
          >
            {verifyMfa.isPending ? 'Verifying...' : 'Verify code'}
          </button>
          <button
            type="button"
            onClick={() => returnToPassword()}
            disabled={verifyMfa.isPending}
            className="inline-flex h-10 w-full items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
          >
            Back to login
          </button>
        </form>
      )}
    </div>
  )
}
