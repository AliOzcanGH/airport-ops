import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Eye, EyeOff, KeyRound } from 'lucide-react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '@/shared/api/ApiError'
import { loginRequestSchema } from '@/shared/api/schemas'
import {
  useCurrentUser,
  useLogin,
  useLogout,
} from '@/features/auth/hooks/useAuthSession'
import { defaultWorkspacePath } from '@/features/auth/routing/workspaceRouting'
import {
  SessionErrorView,
  SessionLoadingView,
} from '@/features/auth/components/AuthStateViews'
import { AccessUnavailablePage } from '@/features/auth/pages/AccessUnavailablePage'

type FieldErrors = Partial<Record<'email' | 'password', string>>

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const currentUser = useCurrentUser()
  const login = useLogin()
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

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFieldErrors({})
    const parsed = loginRequestSchema.safeParse({ email, password })
    if (!parsed.success) {
      const errors = parsed.error.flatten().fieldErrors
      setFieldErrors({ email: errors.email?.[0], password: errors.password?.[0] })
      return
    }
    login.mutate(parsed.data, {
      onSuccess: (user) => navigate(defaultWorkspacePath(user), { replace: true }),
    })
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

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="flex items-center gap-3">
        <span className="grid size-11 place-items-center rounded-md bg-blue-700 text-white">
          <KeyRound aria-hidden="true" size={21} />
        </span>
        <div>
          <h1 className="text-xl font-semibold text-slate-950">Log in</h1>
          <p className="mt-1 text-sm text-slate-600">
            Log in to continue to your Airport Ops workspace.
          </p>
        </div>
      </div>

      <form
        onSubmit={submit}
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

        {loginError ? (
          <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-800">
            {loginError}
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
          {logout.isPending ? 'Preparing login...' : login.isPending ? 'Logging in...' : 'Log in'}
        </button>
      </form>
    </div>
  )
}
