import { afterEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'
import { ApiClient } from '@/shared/api/apiClient'

const csrfBody = {
  headerName: 'X-XSRF-TOKEN',
  parameterName: '_csrf',
  token: 'csrf-token',
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => vi.unstubAllGlobals())

describe('ApiClient', () => {
  it('validates responses and includes browser credentials', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ status: 'UP' })))
    const client = new ApiClient('/api')

    await expect(
      client.get('/actuator/health', {
        schema: z.object({ status: z.literal('UP') }),
      }),
    ).resolves.toEqual({ status: 'UP' })
    expect(fetch).toHaveBeenCalledWith(
      '/api/actuator/health',
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    )
  })

  it('maps the standard backend error response without refreshing a 403', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        json(
          {
            timestamp: '2026-07-05T10:00:00Z',
            status: 403,
            error: 'FORBIDDEN',
            errorCode: 'MISSING_PERMISSION',
            message: 'Required permission is missing',
            path: '/platform/invitations',
          },
          403,
        ),
      ),
    )

    await expect(
      new ApiClient('/api').get('/platform/invitations'),
    ).rejects.toMatchObject({ status: 403, errorCode: 'MISSING_PERMISSION' })
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('bootstraps CSRF for unsafe requests and supports 204 responses', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json(csrfBody))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      new ApiClient('/api').post('/auth/session/login', {
        body: { email: 'admin@demo.com', password: 'secret' },
        retryUnauthorized: false,
      }),
    ).resolves.toBeUndefined()

    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/auth/session/csrf')
    const loginInit = fetchMock.mock.calls[1]?.[1] as RequestInit
    const headers = new Headers(loginInit.headers)
    expect(loginInit.credentials).toBe('include')
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
    expect(headers.has('Authorization')).toBe(false)
  })

  it('refreshes once and retries the original request once', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json({ message: 'expired' }, 401))
      .mockResolvedValueOnce(json(csrfBody))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(json({ value: 'retried' }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      new ApiClient('/api').get('/protected', {
        schema: z.object({ value: z.string() }),
      }),
    ).resolves.toEqual({ value: 'retried' })

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/protected',
      '/api/auth/session/csrf',
      '/api/auth/session/refresh',
      '/api/protected',
    ])
  })

  it('deduplicates refresh across concurrent unauthorized requests', async () => {
    let protectedCalls = 0
    let refreshCalls = 0
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith('/auth/session/csrf')) return json(csrfBody)
      if (url.endsWith('/auth/session/refresh')) {
        refreshCalls += 1
        await Promise.resolve()
        return new Response(null, { status: 204 })
      }
      protectedCalls += 1
      return protectedCalls <= 2
        ? json({ message: 'expired' }, 401)
        : json({ ok: true })
    })
    vi.stubGlobal('fetch', fetchMock)
    const client = new ApiClient('/api')

    await Promise.all([
      client.get('/one'),
      client.get('/two'),
    ])

    expect(refreshCalls).toBe(1)
    expect(protectedCalls).toBe(4)
  })

  it('notifies session expiry when refresh fails', async () => {
    const expired = vi.fn()
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json({ message: 'expired' }, 401))
      .mockResolvedValueOnce(json(csrfBody))
      .mockResolvedValueOnce(json({ message: 'expired' }, 401))
    vi.stubGlobal('fetch', fetchMock)
    const client = new ApiClient('/api')
    client.setSessionExpiredHandler(expired)

    await expect(client.get('/protected')).rejects.toMatchObject({ status: 401 })
    expect(expired).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })
})
