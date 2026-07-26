import { apiClient } from '@/shared/api/apiClient'
import {
  dashboardOverviewResponseSchema,
  type DashboardOverviewResponse,
} from '@/shared/api/schemas'

export const dashboardApi = {
  getOverview(): Promise<DashboardOverviewResponse> {
    return apiClient.get('/app/dashboard/overview', {
      schema: dashboardOverviewResponseSchema,
    })
  },
}
