import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { platformTenantApi } from '@/features/tenants/api/platformTenantApi'
import { apiClient } from '@/shared/api/apiClient'
import { platformUser } from '@/test/authFixtures'

const tenantResponse = {
  tenants: [
    {
      organizationId: '11111111-1111-4111-8111-111111111111',
      organizationName: 'Qatar Airways Cargo',
      organizationStatus: 'ONBOARDING_INCOMPLETE',
      createdAt: '2026-07-13T10:00:00Z',
      memberCount: 2,
      primaryAdminEmail: 'admin@qatar.example.com',
    },
    {
      organizationId: '22222222-2222-4222-8222-222222222222',
      organizationName: 'Lufthansa Regional',
      organizationStatus: 'ACTIVE',
      createdAt: '2026-07-12T10:00:00Z',
      memberCount: 1,
      primaryAdminEmail: null,
    },
  ],
}

function renderTenantDirectory(
  tenants: typeof tenantResponse.tenants = tenantResponse.tenants,
) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    if (url.endsWith('/auth/me')) return Response.json(platformUser)
    if (url.endsWith('/platform/tenants')) {
      return Response.json({ tenants })
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
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)

  const router = createMemoryRouter(routeDefinitions, {
    initialEntries: ['/platform/tenants'],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return fetchMock
}

describe('PlatformTenantDirectoryPage', () => {
  beforeEach(() => {
    apiClient.resetSessionState()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('renders tenant summaries under the platform shell', async () => {
    const fetchMock = renderTenantDirectory()

    expect(
      await screen.findByRole('heading', { name: 'Tenant directory', level: 1 }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('navigation', { name: 'Platform console navigation' }),
    ).toHaveTextContent('Tenant directory')

    const table = await screen.findByRole('table')
    expect(
      within(table).getByRole('link', { name: 'Qatar Airways Cargo' }),
    ).toHaveAttribute(
      'href',
      '/platform/tenants/11111111-1111-4111-8111-111111111111',
    )
    expect(within(table).getByText('ONBOARDING INCOMPLETE')).toBeInTheDocument()
    expect(within(table).getByText('admin@qatar.example.com')).toBeInTheDocument()
    expect(within(table).getByText('Lufthansa Regional')).toBeInTheDocument()
    expect(within(table).getByText('Not assigned')).toBeInTheDocument()

    expect(fetchMock.mock.calls.some(([url]) =>
      String(url).endsWith('/platform/tenants'),
    )).toBe(true)
    expect(fetchMock.mock.calls.every(([url]) =>
      !String(url).includes('/api/api/'),
    )).toBe(true)
  })

  it('shows an empty state with an invitation link', async () => {
    renderTenantDirectory([])

    expect(
      await screen.findByRole('heading', { name: 'No tenant organizations yet' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /create tenant invitation/i }),
    ).toHaveAttribute('href', '/platform/invitations/new')
  })

  it('shows a stable error state', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith('/auth/me')) return Response.json(platformUser)
      if (url.endsWith('/platform/tenants')) {
        return Response.json(
          {
            timestamp: '2026-07-13T10:00:00Z',
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            errorCode: 'UNEXPECTED_ERROR',
            message: 'Tenant directory failed',
            path: '/platform/tenants',
          },
          { status: 500 },
        )
      }
      return new Response(null, { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    const router = createMemoryRouter(routeDefinitions, {
      initialEntries: ['/platform/tenants'],
    })
    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Tenant directory failed',
    )
  })

  it('uses the platform tenants API path without an embedded api prefix', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValue({ tenants: [] })

    await platformTenantApi.listPlatformTenants()

    expect(getSpy).toHaveBeenCalledWith(
      '/platform/tenants',
      expect.objectContaining({ schema: expect.any(Object) }),
    )
  })
})
