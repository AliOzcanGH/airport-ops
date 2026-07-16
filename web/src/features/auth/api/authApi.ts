import { apiClient } from '@/shared/api/apiClient'
import {
  authMeResponseSchema,
  loginRequestSchema,
  loginResponseSchema,
  verifyMfaRequestSchema,
  type AuthMeResponse,
  type LoginRequest,
  type LoginResponse,
  type VerifyMfaRequest,
} from '@/shared/api/schemas'

export const authApi = {
  login(request: LoginRequest): Promise<LoginResponse> {
    return apiClient.post('/auth/session/login', {
      body: loginRequestSchema.parse(request),
      schema: loginResponseSchema,
      retryUnauthorized: false,
    })
  },

  async verifyMfa(request: VerifyMfaRequest): Promise<void> {
    await apiClient.post<void>('/auth/session/mfa/verify', {
      body: verifyMfaRequestSchema.parse(request),
      retryUnauthorized: false,
    })
  },

  async logout(): Promise<void> {
    await apiClient.post<void>('/auth/session/logout', {
      retryUnauthorized: false,
    })
  },

  getCurrentUser(): Promise<AuthMeResponse> {
    return apiClient.get('/auth/me', { schema: authMeResponseSchema })
  },
}
