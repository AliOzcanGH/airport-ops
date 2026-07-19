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

function renderSetupPage({
  profile = null,
  saveFails = false,
}: {
  profile?: SetupProfileResponse | null
  saveFails?: boolean
} = {}) {
  let storedProfile = profile
  let overviewCalls = 0
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.endsWith('/auth/me')) return Response.json(englishTenantUser)
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
        organizationStatus: 'ONBOARDING_INCOMPLETE',
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

    expect(await screen.findByRole('status')).toHaveTextContent('Setup profile saved.')
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
