import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { apiClient } from '@/shared/api/apiClient'
import { useUiStore } from '@/shared/stores/uiStore'
import {
  dualWorkspaceUser,
  noWorkspaceUser,
  platformUser,
  tenantUser,
} from '@/test/authFixtures'
import type { AuthMeResponse } from '@/shared/api/schemas'

function renderRoute(path: string) {
  const testRouter = createMemoryRouter(routeDefinitions, {
    initialEntries: [path],
  })
  return render(
    <AppProviders>
      <RouterProvider router={testRouter} />
    </AppProviders>,
  )
}

function mockBackend(currentUser: AuthMeResponse | null) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    if (url.endsWith('/auth/me')) {
      return currentUser
        ? Response.json(currentUser)
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
    if (url.endsWith('/auth/session/logout')) {
      return new Response(null, { status: 204 })
    }
    if (url.endsWith('/actuator/health')) {
      return Response.json({ status: 'UP' })
    }
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('authenticated application routes', () => {
  beforeEach(() => {
    apiClient.resetSessionState()
    useUiStore.getState().reset()
    vi.unstubAllGlobals()
  })

  it.each(['/', '/dashboard'])(
    'redirects unauthenticated %s requests to login',
    async (path) => {
      mockBackend(null)
      renderRoute(path)
      expect(
        await screen.findByRole('heading', { name: 'Log in', level: 1 }),
      ).toBeInTheDocument()
    },
  )

  it('redirects a platform user to the platform dashboard', async () => {
    mockBackend(platformUser)
    renderRoute('/')
    expect(
      await screen.findByRole('heading', {
        name: 'Platform console overview',
        level: 1,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('platform.admin@demo.com')).toBeInTheDocument()
  })

  it('redirects a tenant user to the tenant dashboard', async () => {
    mockBackend(tenantUser)
    renderRoute('/')
    expect(
      await screen.findByRole('heading', {
        name: 'Example Airlines',
        level: 1,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('AIRLINE_ADMIN')).toBeInTheDocument()
  })

  it('defaults dual-workspace users to the platform', async () => {
    mockBackend(dualWorkspaceUser)
    renderRoute('/')
    expect(
      await screen.findByRole('heading', {
        name: 'Platform console overview',
      }),
    ).toBeInTheDocument()
  })

  it('redirects tenant-only users away from platform routes', async () => {
    mockBackend(tenantUser)
    renderRoute('/platform/dashboard')
    expect(
      await screen.findByRole('heading', { name: 'Example Airlines' }),
    ).toBeInTheDocument()
  })

  it('redirects platform-only users away from tenant routes', async () => {
    mockBackend(platformUser)
    renderRoute('/app/dashboard')
    expect(
      await screen.findByRole('heading', {
        name: 'Platform console overview',
      }),
    ).toBeInTheDocument()
  })

  it('shows a stable unavailable state when no workspace exists', async () => {
    mockBackend(noWorkspaceUser)
    renderRoute('/')
    expect(
      await screen.findByRole('heading', {
        name: 'Workspace access unavailable',
      }),
    ).toBeInTheDocument()
  })

  it('keeps invitation onboarding public and skips auth lookup', async () => {
    const fetchMock = mockBackend(null)
    renderRoute('/invitations/accept')
    expect(
      await screen.findByRole('heading', { name: 'Airline admin onboarding' }),
    ).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('keeps platform navigation limited to platform workflows', async () => {
    mockBackend(platformUser)
    renderRoute('/platform/dashboard')
    const navigation = await screen.findByRole('navigation', {
      name: 'Platform console navigation',
    })
    expect(navigation).toHaveTextContent('Platform dashboard')
    expect(navigation).toHaveTextContent('Tenant invitations')
    expect(navigation).toHaveTextContent('Tenant directory')
    expect(navigation).not.toHaveTextContent('Tenant dashboard')
  })

  it('opens and closes mobile navigation through the UI store', async () => {
    const user = userEvent.setup()
    mockBackend(platformUser)
    renderRoute('/platform/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Open navigation' }))
    expect(useUiStore.getState().isMobileNavOpen).toBe(true)
    await user.click(screen.getByRole('button', { name: 'Close navigation' }))
    expect(useUiStore.getState().isMobileNavOpen).toBe(false)
  })

  it('logs out, clears the auth query, and returns to login', async () => {
    const user = userEvent.setup()
    const fetchMock = mockBackend(platformUser)
    renderRoute('/platform/dashboard')

    await user.click(await screen.findByRole('button', { name: 'Sign out' }))

    expect(
      await screen.findByRole('heading', { name: 'Log in', level: 1 }),
    ).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) =>
      String(url).endsWith('/auth/session/logout'),
    )).toBe(true)
  })
})
