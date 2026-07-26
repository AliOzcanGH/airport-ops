import { queryOptions } from '@tanstack/react-query'
import { dashboardApi } from '@/features/dashboard/api/dashboardApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const dashboardOverviewQueryOptions = queryOptions({
  queryKey: queryKeys.app.dashboardOverview,
  queryFn: dashboardApi.getOverview,
  retry: false,
})
