import { describe, expect, it } from 'vitest'
import { parsePublicEnvironment } from '@/shared/config/env'

describe('public environment configuration', () => {
  it('uses safe local defaults', () => {
    expect(parsePublicEnvironment({})).toEqual({
      VITE_IAM_API_BASE_URL: '/api',
    })
  })

  it('accepts an explicit IAM proxy base path', () => {
    expect(parsePublicEnvironment({ VITE_IAM_API_BASE_URL: '/backend' }))
      .toEqual({ VITE_IAM_API_BASE_URL: '/backend' })
  })
})
