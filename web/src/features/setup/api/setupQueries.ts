import { queryOptions } from '@tanstack/react-query'
import { setupApi } from '@/features/setup/api/setupApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const setupOverviewQueryOptions = queryOptions({
  queryKey: queryKeys.app.setupOverview,
  queryFn: setupApi.getOverview,
  retry: false,
})
