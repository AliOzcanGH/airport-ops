import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { apiClient } from '@/shared/api/apiClient'
import { platformUser } from '@/test/authFixtures'

type LoginOutcome = 'success' | 'invalid' | 'unavailable'

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

function renderLogin(outcome: LoginOutcome = 'success') {
  let authenticated = false
  const calls: string[] = []
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    calls.push(url)
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
      if (outcome === 'invalid') {
        return errorResponse(
          401,
          'INVALID_CREDENTIALS',
          'Invalid email or password',
          '/auth/session/login',
        )
      }
      if (outcome === 'unavailable') {
        return errorResponse(
          503,
          'AUTH_PROVIDER_UNAVAILABLE',
          'Authentication provider is temporarily unavailable',
          '/auth/session/login',
        )
      }
      authenticated = true
      return new Response(null, { status: 204 })
    }
    if (url.endsWith('/actuator/health')) {
      return Response.json({ status: 'UP' })
    }
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  const router = createMemoryRouter(routeDefinitions, {
    initialEntries: ['/login'],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return { fetchMock, calls }
}

describe('LoginPage', () => {
  beforeEach(() => {
    apiClient.resetSessionState()
    vi.unstubAllGlobals()
  })

  it('validates email and password before calling login', async () => {
    const user = userEvent.setup()
    const { calls } = renderLogin()
    await user.click(await screen.findByRole('button', { name: 'Sign in' }))

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

  it('logs in through CSRF and redirects using auth me', async () => {
    const user = userEvent.setup()
    const storageWrite = vi.spyOn(Storage.prototype, 'setItem')
    const { calls } = renderLogin()

    await user.type(await screen.findByLabelText('Email'), 'platform.admin@demo.com')
    await user.type(screen.getByLabelText('Password'), 'Admin123!')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(
      await screen.findByRole('heading', { name: 'Platform console overview' }),
    ).toBeInTheDocument()
    expect(calls).toContain('/api/auth/session/csrf')
    expect(calls).toContain('/api/auth/session/login')
    expect(calls.filter((url) => url.endsWith('/auth/me')).length).toBeGreaterThan(1)
    expect(storageWrite).not.toHaveBeenCalled()
  })

  it('shows a generic invalid credentials message', async () => {
    const user = userEvent.setup()
    renderLogin('invalid')
    await user.type(await screen.findByLabelText('Email'), 'user@demo.com')
    await user.type(screen.getByLabelText('Password'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Invalid email or password.',
    )
  })

  it('shows a clean provider unavailable message', async () => {
    const user = userEvent.setup()
    renderLogin('unavailable')
    await user.type(await screen.findByLabelText('Email'), 'user@demo.com')
    await user.type(screen.getByLabelText('Password'), 'valid-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Sign-in is temporarily unavailable. Try again shortly.',
    )
  })
})
