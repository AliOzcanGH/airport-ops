import { queryOptions } from '@tanstack/react-query'
import { authApi } from '@/features/auth/api/authApi'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'

export const authMeQueryOptions = queryOptions({
  queryKey: queryKeys.auth.me,
  queryFn: async () => {
    try {
      return await authApi.getCurrentUser()
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) return null
      throw error
    }
  },
  retry: false,
  staleTime: 30_000,
})
