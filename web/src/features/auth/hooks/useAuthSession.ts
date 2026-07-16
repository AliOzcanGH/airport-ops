import { useCallback, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/features/auth/api/authApi'
import { authMeQueryOptions } from '@/features/auth/api/authQueries'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import type {
  AuthMeResponse,
  LoginRequest,
  LoginResponse,
  VerifyMfaRequest,
} from '@/shared/api/schemas'

type MutationCallbacks<T> = {
  onSuccess?: (data: T) => void
  onError?: (error: unknown) => void
}

export function useCurrentUser() {
  return useQuery(authMeQueryOptions)
}

export function useLogin() {
  const [isPending, setIsPending] = useState(false)
  const [error, setError] = useState<unknown>(null)

  const mutate = useCallback(
    (request: LoginRequest, callbacks: MutationCallbacks<LoginResponse> = {}) => {
      setIsPending(true)
      setError(null)
      void authApi.login(request).then(
        (challenge) => {
          setIsPending(false)
          callbacks.onSuccess?.(challenge)
        },
        (loginError: unknown) => {
          setIsPending(false)
          setError(loginError)
          callbacks.onError?.(loginError)
        },
      )
    },
    [],
  )

  const reset = useCallback(() => setError(null), [])

  return { mutate, reset, isPending, isError: error !== null, error }
}

export function useVerifyMfa() {
  const queryClient = useQueryClient()
  const [isPending, setIsPending] = useState(false)

  const mutate = useCallback(
    (
      request: VerifyMfaRequest,
      callbacks: MutationCallbacks<AuthMeResponse> = {},
    ) => {
      setIsPending(true)
      void (async () => {
        await authApi.verifyMfa(request)
        await queryClient.removeQueries({ queryKey: queryKeys.auth.me })
        const currentUser = await queryClient.fetchQuery(authMeQueryOptions)
        if (!currentUser) {
          throw new ApiError('The authenticated session could not be loaded.', 401)
        }
        return currentUser
      })().then(
        (currentUser) => {
          setIsPending(false)
          callbacks.onSuccess?.(currentUser)
        },
        (verificationError: unknown) => {
          setIsPending(false)
          callbacks.onError?.(verificationError)
        },
      )
    },
    [queryClient],
  )

  return { mutate, reset: () => undefined, isPending }
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
