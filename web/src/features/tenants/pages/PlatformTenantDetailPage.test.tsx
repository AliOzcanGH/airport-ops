import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { platformTenantApi } from '@/features/tenants/api/platformTenantApi'
import { apiClient } from '@/shared/api/apiClient'
import { platformUser } from '@/test/authFixtures'

const organizationId = '11111111-1111-4111-8111-111111111111'

const tenantDetail = {
  organizationId,
  organizationName: 'Qatar Airways Cargo',
  organizationStatus: 'ONBOARDING_INCOMPLETE',
  createdAt: '2026-07-13T10:00:00Z',
  memberCount: 2,
  primaryAdminEmail: 'admin@qatar.example.com',
  members: [
    {
      memberId: '22222222-2222-4222-8222-222222222221',
      userId: '33333333-3333-4333-8333-333333333331',
      email: 'admin@qatar.example.com',
      fullName: 'Qatar Admin',
      memberStatus: 'ACTIVE',
      roles: ['AIRLINE_ADMIN', 'OPS_USER'],
      joinedAt: '2026-07-13T10:05:00Z',
    },
    {
      memberId: '22222222-2222-4222-8222-222222222222',
      userId: '33333333-3333-4333-8333-333333333332',
      email: 'ops@qatar.example.com',
      fullName: 'Qatar Ops',
      memberStatus: 'ACTIVE',
      roles: ['OPS_USER'],
      joinedAt: '2026-07-13T10:08:00Z',
    },
  ],
}

function backendError(status: number, errorCode: string, message: string) {
  return Response.json(
    {
      timestamp: '2026-07-13T10:00:00Z',
      status,
      error: status === 404 ? 'NOT_FOUND' : 'INTERNAL_SERVER_ERROR',
      errorCode,
      message,
      path: `/platform/tenants/${organizationId}`,
    },
    { status },
  )
}

function renderTenantDetail(
  path = `/platform/tenants/${organizationId}`,
  outcome: 'success' | 'not-found' | 'error' = 'success',
) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    if (url.endsWith('/auth/me')) return Response.json(platformUser)
    if (url.endsWith(`/platform/tenants/${organizationId}`)) {
      if (outcome === 'not-found') {
        return backendError(404, 'TENANT_NOT_FOUND', 'Tenant organization not found')
      }
      if (outcome === 'error') {
        return backendError(500, 'UNEXPECTED_ERROR', 'Tenant detail failed')
      }
      return Response.json(tenantDetail)
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
    initialEntries: [path],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return fetchMock
}

describe('PlatformTenantDetailPage', () => {
  beforeEach(() => {
    apiClient.resetSessionState()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('renders tenant detail under the platform shell', async () => {
    const fetchMock = renderTenantDetail()

    expect(
      await screen.findByRole('heading', { name: 'Qatar Airways Cargo' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('navigation', { name: 'Platform console navigation' }),
    ).toHaveTextContent('Tenant directory')
    expect(screen.getByText('ONBOARDING INCOMPLETE')).toBeInTheDocument()
    expect(screen.getAllByText('admin@qatar.example.com')).toHaveLength(2)
    expect(screen.getByText('2')).toBeInTheDocument()

    const table = screen.getByRole('table')
    expect(within(table).getByText('Qatar Admin')).toBeInTheDocument()
    expect(within(table).getByText('admin@qatar.example.com')).toBeInTheDocument()
    expect(within(table).getByText('AIRLINE_ADMIN')).toBeInTheDocument()
    expect(within(table).getAllByText('OPS_USER')).toHaveLength(2)
    expect(within(table).getByText('Qatar Ops')).toBeInTheDocument()

    expect(fetchMock.mock.calls.some(([url]) =>
      String(url).endsWith(`/platform/tenants/${organizationId}`),
    )).toBe(true)
    expect(fetchMock.mock.calls.every(([url]) =>
      !String(url).includes('/api/api/'),
    )).toBe(true)
  })

  it('shows not found state for backend 404', async () => {
    renderTenantDetail(`/platform/tenants/${organizationId}`, 'not-found')

    expect(
      await screen.findByRole('heading', {
        name: 'Tenant organization not found',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /back to tenant directory/i }),
    ).toHaveAttribute('href', '/platform/tenants')
  })

  it('does not call the API for invalid route params', async () => {
    const fetchMock = renderTenantDetail('/platform/tenants/not-a-uuid')

    expect(
      await screen.findByRole('heading', {
        name: 'Tenant organization not found',
      }),
    ).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) =>
      String(url).includes('/platform/tenants/not-a-uuid'),
    )).toBe(false)
  })

  it('shows a stable generic error state', async () => {
    renderTenantDetail(`/platform/tenants/${organizationId}`, 'error')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Tenant detail failed',
    )
  })

  it('uses the tenant detail API path without an embedded api prefix', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValue(tenantDetail)

    await platformTenantApi.getPlatformTenantDetail(organizationId)

    expect(getSpy).toHaveBeenCalledWith(
      `/platform/tenants/${organizationId}`,
      expect.objectContaining({ schema: expect.any(Object) }),
    )
  })
})
