import { apiClient } from '@/shared/api/apiClient'
import {
  authMeResponseSchema,
  loginRequestSchema,
  type AuthMeResponse,
  type LoginRequest,
} from '@/shared/api/schemas'

export const authApi = {
  async login(request: LoginRequest): Promise<void> {
    await apiClient.post<void>('/auth/session/login', {
      body: loginRequestSchema.parse(request),
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
