import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { apiClient } from '@/shared/api/apiClient'
import { platformUser } from '@/test/authFixtures'

const rawToken = 'abcdefghijklmnopqrstuvwxyzABCDEFGH_12345678'

function backendError(status: number, errorCode: string, message: string, path: string) {
  return Response.json(
    {
      timestamp: '2026-07-07T10:00:00Z',
      status,
      error: status === 409 ? 'CONFLICT' : 'ERROR',
      errorCode,
      message,
      path,
    },
    { status },
  )
}

function renderPlatformInvitations(outcome: 'success' | 'duplicate' = 'success') {
  const requests: RequestInit[] = []
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (init) requests.push(init)
    if (url.endsWith('/auth/me')) return Response.json(platformUser)
    if (url.endsWith('/auth/session/csrf')) {
      return Response.json({
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf',
        token: 'csrf-token',
      })
    }
    if (url.endsWith('/platform/invitations')) {
      if (outcome === 'duplicate') {
        return backendError(
          409,
          'PENDING_INVITATION_EXISTS',
          'A pending invitation already exists for this email',
          '/platform/invitations',
        )
      }
      return Response.json(
        {
          id: '11111111-1111-4111-8111-111111111111',
          email: 'tenant.admin@thy.demo',
          organizationName: 'THY Airlines',
          status: 'PENDING',
          expiresAt: '2026-07-10T10:00:00Z',
          invitationToken: rawToken,
        },
        { status: 201 },
      )
    }
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  vi.stubGlobal('navigator', {
    ...navigator,
    clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
  })
  const router = createMemoryRouter(routeDefinitions, {
    initialEntries: ['/platform/invitations/new'],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return { fetchMock, requests }
}

describe('PlatformInvitationsPage', () => {
  beforeEach(() => {
    apiClient.resetSessionState()
    vi.unstubAllGlobals()
  })

  it('creates a platform invitation with exactly email and organizationName', async () => {
    const user = userEvent.setup()
    const { fetchMock } = renderPlatformInvitations()

    await user.type(await screen.findByLabelText('Invited admin email'), 'tenant.admin@thy.demo')
    await user.type(screen.getByLabelText('Airline / organization name'), 'THY Airlines')
    await user.click(screen.getByRole('button', { name: 'Create invitation' }))

    await screen.findByText('Invitation created')
    const invitationCall = fetchMock.mock.calls.find(([url]) =>
      String(url).endsWith('/platform/invitations'),
    )
    expect(invitationCall).toBeDefined()
    const init = invitationCall?.[1] as RequestInit
    expect(JSON.parse(String(init.body))).toEqual({
      email: 'tenant.admin@thy.demo',
      organizationName: 'THY Airlines',
    })
    expect(init.credentials).toBe('include')
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('csrf-token')
  })

  it('shows a copyable local development accept link on success', async () => {
    const user = userEvent.setup()
    renderPlatformInvitations()

    await user.type(await screen.findByLabelText('Invited admin email'), 'tenant.admin@thy.demo')
    await user.type(screen.getByLabelText('Airline / organization name'), 'THY Airlines')
    await user.click(screen.getByRole('button', { name: 'Create invitation' }))

    const link = await screen.findByLabelText('Local/dev accept link')
    expect(link).toHaveValue(`${window.location.origin}/invitations/accept?token=${rawToken}`)
    expect(screen.getByText(/local development and manual testing/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Copy link' }))
    await waitFor(() =>
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        `${window.location.origin}/invitations/accept?token=${rawToken}`,
      ),
    )
  })

  it('maps duplicate pending invitation errors to conflict UI', async () => {
    const user = userEvent.setup()
    renderPlatformInvitations('duplicate')

    await user.type(await screen.findByLabelText('Invited admin email'), 'tenant.admin@thy.demo')
    await user.type(screen.getByLabelText('Airline / organization name'), 'THY Airlines')
    await user.click(screen.getByRole('button', { name: 'Create invitation' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'A pending invitation already exists for this email.',
    )
  })
})
