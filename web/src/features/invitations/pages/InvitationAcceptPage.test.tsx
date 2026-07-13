import { StrictMode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { apiClient } from '@/shared/api/apiClient'

const validToken = 'abcdefghijklmnopqrstuvwxyzABCDEFGH_12345678'

function backendError(status: number, errorCode: string, message: string, path: string) {
  return Response.json(
    {
      timestamp: '2026-07-07T10:00:00Z',
      status,
      error: status === 404 ? 'NOT_FOUND' : status === 410 ? 'GONE' : 'CONFLICT',
      errorCode,
      message,
      path,
    },
    { status },
  )
}

type ValidateOutcome = 'success' | 'not-found' | 'used' | 'expired'
type AcceptOutcome = 'ready' | 'pending'

function renderAccept({
  path = `/invitations/accept?token=${validToken}`,
  validateOutcome = 'success',
  acceptOutcome = 'ready',
  strict = false,
}: {
  path?: string
  validateOutcome?: ValidateOutcome
  acceptOutcome?: AcceptOutcome
  strict?: boolean
} = {}) {
  window.history.pushState(null, '', path)
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input)
    if (
      url.endsWith('/auth/me') ||
      url.endsWith('/auth/session/csrf') ||
      url.endsWith('/auth/session/refresh')
    ) {
      throw new Error(`Unexpected auth request: ${url}`)
    }
    if (url.endsWith('/invitations/validate')) {
      if (validateOutcome === 'not-found') {
        return backendError(
          404,
          'INVITATION_NOT_FOUND',
          'Invitation not found',
          '/invitations/validate',
        )
      }
      if (validateOutcome === 'used') {
        return backendError(
          409,
          'INVITATION_ALREADY_USED',
          'Invitation has already been used',
          '/invitations/validate',
        )
      }
      if (validateOutcome === 'expired') {
        return backendError(
          410,
          'INVITATION_EXPIRED',
          'Invitation has expired',
          '/invitations/validate',
        )
      }
      return Response.json({
        organizationName: 'Lufthansa Group',
        invitedEmail: 'lu***@lufthansa.demo',
        expiresAt: '2026-07-10T10:00:00Z',
      })
    }
    if (url.endsWith('/invitations/accept')) {
      return Response.json(
        {
          email: 'lufthansa.admin@lufthansa.demo',
          organizationName: 'Lufthansa Group',
          organizationStatus: 'ONBOARDING_INCOMPLETE',
          userStatus: acceptOutcome === 'ready' ? 'ACTIVE' : 'KEYCLOAK_SYNC_FAILED',
          provisioningStatus:
            acceptOutcome === 'ready' ? 'READY' : 'LOGIN_SETUP_PENDING',
          message:
            acceptOutcome === 'ready'
              ? 'Invitation accepted. You can now sign in.'
              : 'Invitation accepted, but login setup is not ready yet. Please contact platform support.',
        },
        { status: acceptOutcome === 'ready' ? 201 : 202 },
      )
    }
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)
  const router = createMemoryRouter(routeDefinitions, {
    initialEntries: [path],
  })
  const tree = (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  )
  render(strict ? <StrictMode>{tree}</StrictMode> : tree)
  return { fetchMock, router }
}

function validateCalls(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.filter(([url]) =>
    String(url).endsWith('/invitations/validate'),
  )
}

function acceptCalls(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.filter(([url]) =>
    String(url).endsWith('/invitations/accept'),
  )
}

describe('InvitationAcceptPage', () => {
  beforeEach(() => {
    apiClient.resetSessionState()
    vi.unstubAllGlobals()
  })

  it('shows missing token state without validating', async () => {
    const { fetchMock } = renderAccept({ path: '/invitations/accept' })

    expect(
      await screen.findByRole('heading', { name: 'No invitation token' }),
    ).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('shows malformed token state without validating', async () => {
    const { fetchMock } = renderAccept({ path: '/invitations/accept?token=bad-token' })

    expect(
      await screen.findByRole('heading', { name: 'Invalid invitation token' }),
    ).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('validates with public credentials omitted and removes the token from the URL', async () => {
    const { fetchMock } = renderAccept()

    expect(await screen.findByText('Invitation details')).toBeInTheDocument()
    await waitFor(() => expect(window.location.search).toBe(''))
    expect(validateCalls(fetchMock)).toHaveLength(1)
    const init = validateCalls(fetchMock)[0]?.[1] as RequestInit
    const headers = new Headers(init.headers)
    expect(init.credentials).toBe('omit')
    expect(headers.has('Authorization')).toBe(false)
    expect(headers.has('X-XSRF-TOKEN')).toBe(false)
    expect(JSON.parse(String(init.body))).toEqual({ token: validToken })
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/auth/'))).toBe(false)
  })

  it('does not validate twice in Strict Mode', async () => {
    const { fetchMock } = renderAccept({ strict: true })

    expect(await screen.findByText('Invitation details')).toBeInTheDocument()
    expect(validateCalls(fetchMock)).toHaveLength(1)
  })

  it('shows valid invitation details', async () => {
    renderAccept()

    expect(await screen.findByText('Lufthansa Group')).toBeInTheDocument()
    expect(screen.getByText('lu***@lufthansa.demo')).toBeInTheDocument()
    expect(screen.getByText(/Jul 10, 2026|10 Jul 2026|2026/)).toBeInTheDocument()
  })

  it('validates the accept form before submission', async () => {
    const user = userEvent.setup()
    const { fetchMock } = renderAccept()

    await screen.findByText('Invitation details')
    await user.click(screen.getByRole('button', { name: 'Accept invitation' }))

    expect(screen.getByText('Full name is required')).toBeInTheDocument()
    expect(screen.getByText('Password must be at least 12 characters')).toBeInTheDocument()
    expect(screen.getByText('Confirm your password')).toBeInTheDocument()
    expect(acceptCalls(fetchMock)).toHaveLength(0)
  })

  it('sends only token, fullName, and password when accepting', async () => {
    const user = userEvent.setup()
    const storageWrite = vi.spyOn(Storage.prototype, 'setItem')
    const { fetchMock } = renderAccept()

    await screen.findByText('Invitation details')
    await user.type(screen.getByLabelText('Full name'), 'Lufthansa Admin')
    await user.type(screen.getByLabelText('Password'), 'StrongPassword123!')
    await user.type(screen.getByLabelText('Confirm password'), 'StrongPassword123!')
    await user.click(screen.getByRole('button', { name: 'Accept invitation' }))

    expect(await screen.findByText('Invitation accepted')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Log in' })).toHaveAttribute(
      'href',
      '/login?switchAccount=true',
    )
    const init = acceptCalls(fetchMock)[0]?.[1] as RequestInit
    expect(init.credentials).toBe('omit')
    const headers = new Headers(init.headers)
    expect(headers.has('Authorization')).toBe(false)
    expect(headers.has('X-XSRF-TOKEN')).toBe(false)
    expect(JSON.parse(String(init.body))).toEqual({
      token: validToken,
      fullName: 'Lufthansa Admin',
      password: 'StrongPassword123!',
    })
    expect(JSON.stringify(JSON.parse(String(init.body)))).not.toContain('confirmPassword')
    expect(JSON.stringify(JSON.parse(String(init.body)))).not.toContain('organizationName')
    expect(JSON.stringify(JSON.parse(String(init.body)))).not.toContain('email')
    expect(storageWrite).not.toHaveBeenCalled()
  })

  it('renders setup pending without redirecting to login', async () => {
    const user = userEvent.setup()
    renderAccept({ acceptOutcome: 'pending' })

    await screen.findByText('Invitation details')
    await user.type(screen.getByLabelText('Full name'), 'Lufthansa Admin')
    await user.type(screen.getByLabelText('Password'), 'StrongPassword123!')
    await user.type(screen.getByLabelText('Confirm password'), 'StrongPassword123!')
    await user.click(screen.getByRole('button', { name: 'Accept invitation' }))

    expect(await screen.findByText('Login setup pending')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Log in' })).not.toBeInTheDocument()
  })

  it.each([
    ['not-found' as const, 'This invitation link is invalid or has been cancelled.'],
    ['used' as const, 'This invitation has already been used.'],
    ['expired' as const, 'This invitation has expired. Ask a platform administrator for a new invitation.'],
  ])('maps %s validation errors to clear UI', async (validateOutcome, message) => {
    renderAccept({ validateOutcome })

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
  })
})
