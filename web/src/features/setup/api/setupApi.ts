import { apiClient } from '@/shared/api/apiClient'
import {
  setupOverviewResponseSchema,
  type SetupOverviewResponse,
} from '@/shared/api/schemas'

export const setupApi = {
  getOverview(): Promise<SetupOverviewResponse> {
    return apiClient.get('/app/setup/overview', {
      schema: setupOverviewResponseSchema,
    })
  },
}
