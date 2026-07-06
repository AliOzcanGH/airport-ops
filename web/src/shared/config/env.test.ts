import { describe, expect, it } from 'vitest'
import { parsePublicEnvironment } from '@/shared/config/env'

describe('public environment configuration', () => {
  it('uses safe local defaults', () => {
    expect(parsePublicEnvironment({})).toEqual({
      VITE_IAM_API_BASE_URL: '/api',
      VITE_KEYCLOAK_URL: 'http://127.0.0.1:8085',
      VITE_KEYCLOAK_REALM: 'airport-ops',
      VITE_KEYCLOAK_CLIENT_ID: 'airport-ops-local',
    })
  })

  it('rejects an invalid public Keycloak URL', () => {
    expect(() =>
      parsePublicEnvironment({ VITE_KEYCLOAK_URL: 'not-a-url' }),
    ).toThrow()
  })
})
