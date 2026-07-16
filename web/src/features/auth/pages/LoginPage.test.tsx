import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { apiClient } from '@/shared/api/apiClient'
import { platformUser } from '@/test/authFixtures'

type LoginOutcome = 'enrollment' | 'verification' | 'invalid' | 'unavailable'
type VerifyOutcome = 'success' | 'invalid' | 'expired' | 'locked' | 'unavailable'

const challengeId = '2f8ea3d6-5978-4b79-99ab-57a5493c8147'
const manualEntryKey = 'JBSWY3DPEHPK3PXP'
const otpauthUri =
  'otpauth://totp/Airport%20Ops%3Auser%40demo.com?secret=JBSWY3DPEHPK3PXP&issuer=Airport%20Ops'

function errorResponse(
  status: number,
  errorCode: string,
  message: string,
  path: string,
) {
  return Response.json(
    {
      timestamp: '2026-07-06T12:00:00Z',
      status,
      error: status === 401 ? 'UNAUTHORIZED' : 'SERVICE_UNAVAILABLE',
      errorCode,
      message,
      path,
    },
    { status },
  )
}

function renderLogin({
  loginOutcome = 'verification',
  verifyOutcome = 'success',
  initialEntry = '/login',
  initiallyAuthenticated = false,
}: {
  loginOutcome?: LoginOutcome
  verifyOutcome?: VerifyOutcome
  initialEntry?: string
  initiallyAuthenticated?: boolean
} = {}) {
  let authenticated = initiallyAuthenticated
  const calls: string[] = []
  const requests: Array<{ url: string; init?: RequestInit }> = []
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    calls.push(url)
    requests.push({ url, init })
    if (url.endsWith('/auth/me')) {
      return authenticated
        ? Response.json(platformUser)
        : new Response(null, { status: 401 })
    }
    if (url.endsWith('/auth/session/csrf')) {
      return Response.json({
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf',
        token: 'csrf-token',
      })
    }
    if (url.endsWith('/auth/session/refresh')) {
      return new Response(null, { status: 401 })
    }
    if (url.endsWith('/auth/session/login')) {
      if (loginOutcome === 'invalid') {
        return errorResponse(
          401,
          'INVALID_CREDENTIALS',
          'Invalid email or password',
          '/auth/session/login',
        )
      }
      if (loginOutcome === 'unavailable') {
        return errorResponse(
          503,
          'AUTH_PROVIDER_UNAVAILABLE',
          'Authentication provider is temporarily unavailable',
          '/auth/session/login',
        )
      }
      if (loginOutcome === 'enrollment') {
        return Response.json({
          outcome: 'MFA_ENROLLMENT_REQUIRED',
          challengeId,
          expiresAt: '2026-07-06T12:05:00Z',
          attemptsRemaining: 5,
          otpauthUri,
          manualEntryKey,
        })
      }
      return Response.json({
        outcome: 'MFA_REQUIRED',
        challengeId,
        expiresAt: '2026-07-06T12:05:00Z',
        attemptsRemaining: 5,
      })
    }
    if (url.endsWith('/auth/session/mfa/verify')) {
      if (verifyOutcome === 'invalid') {
        return errorResponse(
          401,
          'MFA_CODE_INVALID',
          'MFA code is invalid',
          '/auth/session/mfa/verify',
        )
      }
      if (verifyOutcome === 'expired') {
        return errorResponse(
          401,
          'MFA_CHALLENGE_EXPIRED',
          'MFA challenge has expired',
          '/auth/session/mfa/verify',
        )
      }
      if (verifyOutcome === 'locked') {
        return errorResponse(
          401,
          'MFA_CHALLENGE_LOCKED',
          'MFA challenge is locked',
          '/auth/session/mfa/verify',
        )
      }
      if (verifyOutcome === 'unavailable') {
        return errorResponse(
          503,
          'MFA_CONFIGURATION_ERROR',
          'MFA is temporarily unavailable',
          '/auth/session/mfa/verify',
        )
      }
      authenticated = true
      return new Response(null, { status: 204 })
    }
    if (url.endsWith('/auth/session/logout')) {
      authenticated = false
      return new Response(null, { status: 204 })
    }
    if (url.endsWith('/actuator/health')) {
      return Response.json({ status: 'UP' })
    }
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  const router = createMemoryRouter(routeDefinitions, {
    initialEntries: [initialEntry],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return { fetchMock, calls, requests, router }
}

async function submitPassword() {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Email'), 'user@demo.com')
  await user.type(screen.getByLabelText('Password'), 'ValidPassword123!')
  await user.click(screen.getByRole('button', { name: 'Log in' }))
  return user
}

async function submitMfaCode(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('6-digit code'), '123456')
  await user.click(screen.getByRole('button', { name: 'Verify code' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
    window.localStorage.clear()
    window.sessionStorage.clear()
    apiClient.resetSessionState()
  })

  it('validates email and password before calling login', async () => {
    const user = userEvent.setup()
    const { calls } = renderLogin()
    await user.click(await screen.findByRole('button', { name: 'Log in' }))

    expect(screen.getByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
    expect(calls.filter((url) => url.endsWith('/auth/session/login'))).toHaveLength(0)
  })

  it('toggles password visibility', async () => {
    const user = userEvent.setup()
    renderLogin()
    const password = await screen.findByLabelText('Password')
    expect(password).toHaveAttribute('type', 'password')
    await user.click(screen.getByRole('button', { name: 'Show password' }))
    expect(password).toHaveAttribute('type', 'text')
  })

  it('shows enrollment without fetching auth me after password submission', async () => {
    const { calls } = renderLogin({ loginOutcome: 'enrollment' })
    await screen.findByRole('button', { name: 'Log in' })
    const authMeCallsBeforeLogin = calls.filter((url) => url.endsWith('/auth/me')).length

    await submitPassword()

    expect(
      await screen.findByRole('heading', { name: 'Set up authenticator app' }),
    ).toBeInTheDocument()
    expect(calls.filter((url) => url.endsWith('/auth/me'))).toHaveLength(
      authMeCallsBeforeLogin,
    )
  })

  it('renders enrollment instructions, QR code, manual key, and code input', async () => {
    renderLogin({ loginOutcome: 'enrollment' })

    await submitPassword()

    expect(
      await screen.findByText(
        /Scan this QR code with Google Authenticator, Microsoft Authenticator, Authy/,
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', { name: 'Authenticator setup QR code' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Manual entry key')).toBeInTheDocument()
    expect(screen.getByText(manualEntryKey)).toBeInTheDocument()
    expect(screen.getByLabelText('6-digit code')).toHaveAttribute(
      'autocomplete',
      'one-time-code',
    )
  })

  it('shows the authenticator verification screen for an existing credential', async () => {
    renderLogin({ loginOutcome: 'verification' })

    await submitPassword()

    expect(
      await screen.findByRole('heading', { name: 'Enter authenticator code' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        'Open your authenticator app and enter the current 6-digit code.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByText('Manual entry key')).not.toBeInTheDocument()
  })

  it('verifies MFA, fetches auth me, and redirects with existing workspace logic', async () => {
    const { calls, requests } = renderLogin({ loginOutcome: 'verification' })
    const user = await submitPassword()

    await submitMfaCode(user)

    expect(
      await screen.findByRole('heading', { name: 'Platform console overview' }),
    ).toBeInTheDocument()
    expect(calls).toContain('/api/auth/session/mfa/verify')
    const verifyRequest = requests.find((request) =>
      request.url.endsWith('/auth/session/mfa/verify'),
    )
    expect(JSON.parse(String(verifyRequest?.init?.body))).toEqual({
      challengeId,
      code: '123456',
    })
    const verifyCallIndex = calls.findIndex((url) =>
      url.endsWith('/auth/session/mfa/verify'),
    )
    expect(
      calls.findIndex(
        (url, index) => index > verifyCallIndex && url.endsWith('/auth/me'),
      ),
    ).toBeGreaterThan(verifyCallIndex)
  })

  it('keeps the MFA screen, shows a safe invalid-code error, and clears the code', async () => {
    renderLogin({ loginOutcome: 'verification', verifyOutcome: 'invalid' })
    const user = await submitPassword()

    await submitMfaCode(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The code was not accepted. Enter the current 6-digit code and try again.',
    )
    expect(
      screen.getByRole('heading', { name: 'Enter authenticator code' }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('6-digit code')).toHaveValue('')
  })

  it('returns an expired challenge to password login with a clear message', async () => {
    renderLogin({ loginOutcome: 'verification', verifyOutcome: 'expired' })
    const user = await submitPassword()

    await submitMfaCode(user)

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Your verification session expired. Please log in again.',
    )
    expect(screen.queryByLabelText('6-digit code')).not.toBeInTheDocument()
  })

  it('returns a locked challenge to password login with a clear message', async () => {
    renderLogin({ loginOutcome: 'verification', verifyOutcome: 'locked' })
    const user = await submitPassword()

    await submitMfaCode(user)

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Too many incorrect codes. Please log in again.',
    )
  })

  it('shows a safe generic verification error without leaving the MFA screen', async () => {
    renderLogin({ loginOutcome: 'verification', verifyOutcome: 'unavailable' })
    const user = await submitPassword()

    await submitMfaCode(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Verification is temporarily unavailable. Please try again.',
    )
    expect(
      screen.getByRole('heading', { name: 'Enter authenticator code' }),
    ).toBeInTheDocument()
  })

  it('back to login clears the enrollment challenge and code state', async () => {
    renderLogin({ loginOutcome: 'enrollment' })
    const user = await submitPassword()
    await user.type(await screen.findByLabelText('6-digit code'), '123')

    await user.click(screen.getByRole('button', { name: 'Back to login' }))

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.queryByText(manualEntryKey)).not.toBeInTheDocument()
    expect(
      screen.queryByRole('img', { name: 'Authenticator setup QR code' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByLabelText('6-digit code')).not.toBeInTheDocument()
  })

  it('never persists MFA challenge data in browser storage', async () => {
    const storageWrite = vi.spyOn(Storage.prototype, 'setItem')
    renderLogin({ loginOutcome: 'enrollment', verifyOutcome: 'invalid' })
    const user = await submitPassword()
    await submitMfaCode(user)
    await screen.findByRole('alert')

    expect(storageWrite).not.toHaveBeenCalled()
    expect(window.localStorage).toHaveLength(0)
    expect(window.sessionStorage).toHaveLength(0)
  })

  it('shows a generic invalid credentials message', async () => {
    renderLogin({ loginOutcome: 'invalid' })
    await submitPassword()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Invalid email or password.',
    )
  })

  it('shows a clean provider unavailable message', async () => {
    renderLogin({ loginOutcome: 'unavailable' })
    await submitPassword()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Login is temporarily unavailable. Try again shortly.',
    )
  })

  it('keeps normal login redirect behavior for authenticated users', async () => {
    renderLogin({ initiallyAuthenticated: true })

    expect(
      await screen.findByRole('heading', { name: 'Platform console overview' }),
    ).toBeInTheDocument()
  })

  it('switch-account login logs out an existing session once and shows the form', async () => {
    const { calls, router } = renderLogin({
      initialEntry: '/login?switchAccount=true',
      initiallyAuthenticated: true,
    })

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    await waitFor(() =>
      expect(calls.filter((url) => url.endsWith('/auth/session/logout'))).toHaveLength(1),
    )
    expect(
      screen.queryByRole('heading', { name: 'Platform console overview' }),
    ).not.toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.search).toBe('')
  })

  it('switch-account login without a session shows the form without logout', async () => {
    const { calls, router } = renderLogin({
      initialEntry: '/login?switchAccount=true',
    })

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.search).toBe('')
    expect(calls.filter((url) => url.endsWith('/auth/session/logout'))).toHaveLength(0)
  })
})
