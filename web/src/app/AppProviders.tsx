import { useEffect, useState, type ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { createAppQueryClient } from '@/app/queryClient'
import { apiClient } from '@/shared/api/apiClient'
import { queryKeys } from '@/shared/api/queryKeys'

type AppProvidersProps = {
  children: ReactNode
}

export function AppProviders({ children }: AppProvidersProps) {
  const [queryClient] = useState(createAppQueryClient)

  useEffect(() => {
    apiClient.setSessionExpiredHandler(() => {
      queryClient.setQueryData(queryKeys.auth.me, null)
    })
    return () => apiClient.setSessionExpiredHandler(null)
  }, [queryClient])

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {import.meta.env.DEV ? (
        <ReactQueryDevtools initialIsOpen={false} />
      ) : null}
    </QueryClientProvider>
  )
}
