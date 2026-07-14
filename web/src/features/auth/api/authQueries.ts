import { queryOptions } from '@tanstack/react-query'
import { authApi } from '@/features/auth/api/authApi'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import { i18n } from '@/shared/i18n/i18n'
import { preferredLanguageToI18nLanguage } from '@/shared/i18n/language'

export const authMeQueryOptions = queryOptions({
  queryKey: queryKeys.auth.me,
  queryFn: async () => {
    try {
      const currentUser = await authApi.getCurrentUser()
      const language = preferredLanguageToI18nLanguage(
        currentUser.preferredLanguage,
      )
      if (i18n.language !== language) {
        await i18n.changeLanguage(language)
      }
      return currentUser
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) return null
      throw error
    }
  },
  retry: false,
  staleTime: 30_000,
})
