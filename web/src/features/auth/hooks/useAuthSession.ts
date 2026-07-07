import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/features/auth/api/authApi'
import { authMeQueryOptions } from '@/features/auth/api/authQueries'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'

export function useCurrentUser() {
  return useQuery(authMeQueryOptions)
}

export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: { email: string; password: string }) => {
      await authApi.login(request)
      await queryClient.removeQueries({ queryKey: queryKeys.auth.me })
      const currentUser = await queryClient.fetchQuery(authMeQueryOptions)
      if (!currentUser) {
        throw new ApiError('The authenticated session could not be loaded.', 401)
      }
      return currentUser
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => {
      queryClient.setQueryData(queryKeys.auth.me, null)
    },
  })
}
