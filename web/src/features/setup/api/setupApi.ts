import { apiClient } from '@/shared/api/apiClient'
import {
  setupOverviewResponseSchema,
  setupProfileResponseSchema,
  type SetupOverviewResponse,
  type SetupProfileRequest,
  type SetupProfileResponse,
} from '@/shared/api/schemas'

export const setupApi = {
  getOverview(): Promise<SetupOverviewResponse> {
    return apiClient.get('/app/setup/overview', {
      schema: setupOverviewResponseSchema,
    })
  },

  saveProfile(request: SetupProfileRequest): Promise<SetupProfileResponse> {
    return apiClient.request('/app/setup/profile', {
      method: 'PUT',
      body: request,
      schema: setupProfileResponseSchema,
    })
  },
}
