import { afterEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'
import { ApiClient } from '@/shared/api/apiClient'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ApiClient', () => {
  it('validates successful responses with Zod', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: 'UP' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    const client = new ApiClient('/api')

    const response = await client.get('/actuator/health', {
      schema: z.object({ status: z.literal('UP') }),
    })

    expect(response).toEqual({ status: 'UP' })
    expect(fetch).toHaveBeenCalledWith(
      '/api/actuator/health',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('maps the standard backend error response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            timestamp: '2026-07-05T10:00:00Z',
            status: 403,
            error: 'FORBIDDEN',
            errorCode: 'MISSING_PERMISSION',
            message: 'Required permission is missing',
            path: '/platform/invitations',
          }),
          { status: 403, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )
    const client = new ApiClient('/api')

    await expect(client.get('/platform/invitations')).rejects.toMatchObject({
      status: 403,
      errorCode: 'MISSING_PERMISSION',
      message: 'Required permission is missing',
      path: '/platform/invitations',
    })
  })
})
