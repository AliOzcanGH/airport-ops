import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { render } from '@testing-library/react'
import { AppProviders } from '@/app/AppProviders'
import { routeDefinitions } from '@/app/router'
import { useUiStore } from '@/shared/stores/uiStore'

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

describe('application routes', () => {
  beforeEach(() => {
    useUiStore.getState().reset()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: 'UP' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
  })

  it.each([
    ['/login', 'Sign in'],
    ['/platform/invitations/new', 'Tenant invitations'],
    ['/app/dashboard', 'Airline tenant dashboard'],
    ['/invitations/accept', 'Airline admin onboarding'],
    ['/unknown-workspace', 'Page not found'],
  ])('renders %s', async (path, heading) => {
    renderRoute(path)
    expect(
      await screen.findByRole('heading', { name: heading, level: 1 }),
    ).toBeInTheDocument()
  })

  it.each(['/', '/dashboard'])(
    'redirects %s to the platform dashboard and shows IAM health',
    async (path) => {
      renderRoute(path)

      expect(
        await screen.findByRole('heading', {
          name: 'Platform console overview',
          level: 1,
        }),
      ).toBeInTheDocument()
      expect(await screen.findByText('UP')).toBeInTheDocument()
      expect(fetch).toHaveBeenCalledWith(
        '/api/actuator/health',
        expect.objectContaining({ method: 'GET' }),
      )
    },
  )

  it('keeps platform navigation limited to platform workflows', async () => {
    renderRoute('/platform/dashboard')

    const navigation = await screen.findByRole('navigation', {
      name: 'Platform console navigation',
    })
    expect(navigation).toHaveTextContent('Platform dashboard')
    expect(navigation).toHaveTextContent('Tenant invitations')
    expect(navigation).not.toHaveTextContent('Tenant dashboard')
  })

  it('keeps tenant navigation free of platform actions and health queries', async () => {
    renderRoute('/app/dashboard')

    const navigation = await screen.findByRole('navigation', {
      name: 'Airline tenant navigation',
    })
    expect(navigation).toHaveTextContent('Tenant dashboard')
    expect(navigation).not.toHaveTextContent('Tenant invitations')
    expect(navigation).not.toHaveTextContent('Platform dashboard')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('keeps invitation onboarding outside workspace navigation', async () => {
    renderRoute('/invitations/accept')

    expect(
      await screen.findByRole('heading', {
        name: 'Airline admin onboarding',
      }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })

  it('opens and closes mobile navigation through the UI store', async () => {
    const user = userEvent.setup()
    renderRoute('/platform/dashboard')

    await user.click(screen.getByRole('button', { name: 'Open navigation' }))
    expect(useUiStore.getState().isMobileNavOpen).toBe(true)
    expect(
      screen.getByRole('button', { name: 'Close navigation' }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Close navigation' }))
    expect(useUiStore.getState().isMobileNavOpen).toBe(false)
  })
})
