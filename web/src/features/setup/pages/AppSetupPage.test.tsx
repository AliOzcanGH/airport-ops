import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { apiClient } from '@/shared/api/apiClient'
import { i18n } from '@/shared/i18n/i18n'
import type { SetupProfileResponse } from '@/shared/api/schemas'
import { tenantUser } from '@/test/authFixtures'

const englishTenantUser = {
  ...tenantUser,
  preferredLanguage: 'EN' as const,
}

const existingProfile: SetupProfileResponse = {
  organizationId: '316b7ca9-02b7-4ec7-a69f-f70b8725625a',
  displayName: 'Example Airlines',
  iataCode: 'EX',
  icaoCode: 'EXA',
  countryCode: 'TR',
  timezone: 'Europe/Istanbul',
  baseAirportIata: 'IST',
  operationsContactEmail: 'ops@example.com',
  createdAt: '2026-07-18T10:00:00Z',
  updatedAt: '2026-07-18T10:00:00Z',
}

const canonicalProfile: SetupProfileResponse = {
  ...existingProfile,
  displayName: 'Canonical Airline Operations',
  iataCode: 'B6',
  icaoCode: 'THY',
  countryCode: null,
  timezone: 'Europe/Istanbul',
  baseAirportIata: null,
  operationsContactEmail: 'ops@example.com',
  updatedAt: '2026-07-19T10:00:00Z',
}

function backendError() {
  return Response.json(
    {
      timestamp: '2026-07-19T10:00:00Z',
      status: 400,
      error: 'BAD_REQUEST',
      errorCode: 'VALIDATION_ERROR',
      message: 'Setup profile validation failed.',
      path: '/app/setup/profile',
    },
    { status: 400 },
  )
}

type CompletionConflict =
  | 'SETUP_PROFILE_REQUIRED'
  | 'SETUP_PROFILE_INCOMPLETE'
  | 'SETUP_ALREADY_COMPLETED'

function completionError(errorCode: CompletionConflict) {
  return Response.json(
    {
      timestamp: '2026-07-20T12:00:00Z',
      status: 409,
      error: 'CONFLICT',
      errorCode,
      message: 'Setup completion conflict.',
      path: '/app/setup/complete',
    },
    { status: 409 },
  )
}

function renderSetupPage({
  profile = null,
  saveFails = false,
  completionConflict,
}: {
  profile?: SetupProfileResponse | null
  saveFails?: boolean
  completionConflict?: CompletionConflict
} = {}) {
  let storedProfile = profile
  let currentUser = englishTenantUser
  let overviewCalls = 0
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.endsWith('/auth/me')) return Response.json(currentUser)
    if (url.endsWith('/auth/session/csrf')) {
      return Response.json({
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf',
        token: 'csrf-token',
      })
    }
    if (url.endsWith('/app/setup/overview')) {
      overviewCalls += 1
      return Response.json({
        organizationId: englishTenantUser.tenantContext?.organizationId,
        organizationName: englishTenantUser.tenantContext?.organizationName,
        organizationStatus: currentUser.tenantContext?.organizationStatus,
        preferredLanguage: 'EN',
        steps: [
          { key: 'PROFILE', status: 'NOT_STARTED' },
          { key: 'STATION', status: 'LOCKED' },
          { key: 'REVIEW', status: 'LOCKED' },
        ],
        profile: storedProfile,
      })
    }
    if (url.endsWith('/app/setup/profile') && init?.method === 'PUT') {
      if (saveFails) return backendError()
      storedProfile = canonicalProfile
      return Response.json(canonicalProfile)
    }
    if (url.endsWith('/app/setup/complete') && init?.method === 'POST') {
      if (completionConflict) {
        if (completionConflict === 'SETUP_ALREADY_COMPLETED') {
          currentUser = {
            ...englishTenantUser,
            tenantContext: {
              ...englishTenantUser.tenantContext!,
              organizationStatus: 'ACTIVE',
            },
          }
        }
        return completionError(completionConflict)
      }
      currentUser = {
        ...englishTenantUser,
        tenantContext: {
          ...englishTenantUser.tenantContext!,
          organizationStatus: 'ACTIVE',
        },
      }
      return Response.json({
        organizationId: currentUser.tenantContext!.organizationId,
        organizationStatus: 'ACTIVE',
        completedAt: '2026-07-20T12:00:00Z',
      })
    }
    return new Response(null, { status: 404 })
  })
  vi.stubGlobal('fetch', fetchMock)

  const router = createMemoryRouter(routeDefinitions, {
    initialEntries: ['/app/setup'],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )

  return {
    fetchMock,
    getOverviewCalls: () => overviewCalls,
  }
}

describe('AppSetupPage', () => {
  beforeEach(() => {
    apiClient.resetSessionState()
    vi.unstubAllGlobals()
    void i18n.changeLanguage('en')
  })

  it('renders an empty form when the overview profile is null', async () => {
    renderSetupPage()

    expect(await screen.findByLabelText('Display name')).toHaveValue('')
    expect(screen.getByLabelText(/IATA code/)).toHaveValue('')
    expect(screen.getByLabelText(/Operations contact email/)).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Complete setup' })).toBeDisabled()
    expect(screen.getByText(/Save the display name, country code/)).toBeVisible()
  })

  it('prefills the form from an existing backend profile', async () => {
    renderSetupPage({ profile: existingProfile })

    expect(await screen.findByLabelText('Display name')).toHaveValue('Example Airlines')
    expect(screen.getByLabelText(/IATA code/)).toHaveValue('EX')
    expect(screen.getByLabelText(/ICAO code/)).toHaveValue('EXA')
    expect(screen.getByLabelText(/Country code/)).toHaveValue('TR')
    expect(screen.getByLabelText(/Timezone/)).toHaveValue('Europe/Istanbul')
    expect(screen.getByLabelText(/Base airport IATA/)).toHaveValue('IST')
    expect(screen.getByLabelText(/Operations contact email/)).toHaveValue(
      'ops@example.com',
    )
    expect(screen.getByRole('button', { name: 'Complete setup' })).toBeEnabled()
  })

  it('does not treat unsaved required form values as a complete profile', async () => {
    const user = userEvent.setup()
    renderSetupPage()

    await user.type(await screen.findByLabelText('Display name'), 'Unsaved Airline')
    await user.type(screen.getByLabelText(/Country code/), 'TR')
    await user.type(screen.getByLabelText(/Timezone/), 'Europe/Istanbul')
    await user.type(
      screen.getByLabelText(/Operations contact email/),
      'ops@example.com',
    )

    expect(screen.getByRole('button', { name: 'Complete setup' })).toBeDisabled()
  })

  it('posts completion without a body or client-supplied organization context and opens the dashboard', async () => {
    const user = userEvent.setup()
    const { fetchMock, getOverviewCalls } = renderSetupPage({
      profile: existingProfile,
    })

    await user.click(await screen.findByRole('button', { name: 'Complete setup' }))

    expect(
      await screen.findByRole('heading', { name: 'Example Airlines', level: 1 }),
    ).toBeInTheDocument()
    const completionCall = fetchMock.mock.calls.find(([url]) =>
      String(url).endsWith('/app/setup/complete'),
    )
    expect(completionCall).toBeDefined()
    expect(completionCall?.[1]?.method).toBe('POST')
    expect(completionCall?.[1]?.body).toBeUndefined()
    expect(String(completionCall?.[0])).not.toContain('organizationId')

    const authMeCalls = fetchMock.mock.calls.filter(([url]) =>
      String(url).endsWith('/auth/me'),
    )
    expect(authMeCalls.length).toBeGreaterThanOrEqual(2)
    expect(getOverviewCalls()).toBeGreaterThanOrEqual(2)
  })

  it.each([
    [
      'SETUP_PROFILE_REQUIRED' as const,
      'Save the required setup profile fields before completing setup.',
    ],
    [
      'SETUP_PROFILE_INCOMPLETE' as const,
      'The saved profile is incomplete. Review the required fields and save it again.',
    ],
  ])('keeps setup open and shows a safe message for %s', async (errorCode, message) => {
    const user = userEvent.setup()
    renderSetupPage({ profile: existingProfile, completionConflict: errorCode })

    await user.click(await screen.findByRole('button', { name: 'Complete setup' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(screen.getByRole('button', { name: 'Complete setup' })).toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: 'Example Airlines', level: 1 }),
    ).not.toBeInTheDocument()
  })

  it('treats SETUP_ALREADY_COMPLETED as success and opens the dashboard', async () => {
    const user = userEvent.setup()
    renderSetupPage({
      profile: existingProfile,
      completionConflict: 'SETUP_ALREADY_COMPLETED',
    })

    await user.click(await screen.findByRole('button', { name: 'Complete setup' }))

    expect(
      await screen.findByRole('heading', { name: 'Example Airlines', level: 1 }),
    ).toBeInTheDocument()
  })

  it('shows client validation when display name is blank', async () => {
    const user = userEvent.setup()
    const { fetchMock } = renderSetupPage()

    await user.click(await screen.findByRole('button', { name: 'Save setup profile' }))

    expect(
      await screen.findByText('Display name must be at least 2 characters.'),
    ).toBeInTheDocument()
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).endsWith('/app/setup/profile')),
    ).toBe(false)
  })

  it.each([
    ['IATA code', 'ABC', 'IATA code must contain exactly 2 letters or numbers.'],
    ['ICAO code', 'A1B', 'ICAO code must contain exactly 3 letters.'],
    [
      'Base airport IATA',
      '12A',
      'Base airport IATA code must contain exactly 3 letters.',
    ],
  ])('blocks PUT when %s is invalid', async (label, value, message) => {
    const user = userEvent.setup()
    const { fetchMock } = renderSetupPage()

    await user.type(await screen.findByLabelText('Display name'), 'Airline Tenant')
    await user.type(screen.getByLabelText(new RegExp(label)), value)
    await user.click(screen.getByRole('button', { name: 'Save setup profile' }))

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).endsWith('/app/setup/profile')),
    ).toBe(false)
  })

  it('normalizes the exact PUT body, accepts B6, applies canonical data, and refetches overview', async () => {
    const user = userEvent.setup()
    const { fetchMock, getOverviewCalls } = renderSetupPage()

    await user.type(await screen.findByLabelText('Display name'), '  Airline Operations  ')
    await user.type(screen.getByLabelText(/IATA code/), 'b6')
    await user.type(screen.getByLabelText(/ICAO code/), 'thy')
    await user.type(screen.getByLabelText(/Country code/), '   ')
    await user.type(screen.getByLabelText(/Timezone/), '  Europe/Istanbul  ')
    await user.type(screen.getByLabelText(/Base airport IATA/), '   ')
    await user.type(
      screen.getByLabelText(/Operations contact email/),
      '  OPS@EXAMPLE.COM  ',
    )
    await user.click(screen.getByRole('button', { name: 'Save setup profile' }))

    expect(await screen.findByText('Setup profile saved.')).toBeInTheDocument()
    const profileCall = fetchMock.mock.calls.find(([url]) =>
      String(url).endsWith('/app/setup/profile'),
    )
    expect(profileCall).toBeDefined()
    const init = profileCall?.[1] as RequestInit
    expect(init.method).toBe('PUT')
    expect(JSON.parse(String(init.body))).toEqual({
      displayName: 'Airline Operations',
      iataCode: 'B6',
      icaoCode: 'THY',
      countryCode: null,
      timezone: 'Europe/Istanbul',
      baseAirportIata: null,
      operationsContactEmail: 'ops@example.com',
    })
    expect(JSON.parse(String(init.body))).not.toHaveProperty('organizationId')
    expect(JSON.parse(String(init.body))).not.toHaveProperty('createdAt')
    expect(JSON.parse(String(init.body))).not.toHaveProperty('updatedAt')

    expect(screen.getByLabelText('Display name')).toHaveValue(
      'Canonical Airline Operations',
    )
    expect(screen.getByLabelText(/IATA code/)).toHaveValue('B6')
    await waitFor(() => expect(getOverviewCalls()).toBeGreaterThanOrEqual(2))
  })

  it('shows backend validation errors to the user', async () => {
    const user = userEvent.setup()
    renderSetupPage({ saveFails: true })

    await user.type(await screen.findByLabelText('Display name'), 'Airline Tenant')
    await user.click(screen.getByRole('button', { name: 'Save setup profile' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Setup profile validation failed.',
    )
  })
})
