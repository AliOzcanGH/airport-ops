import type { PreferredLanguage } from '@/shared/api/schemas'

export const defaultPreferredLanguage: PreferredLanguage = 'EN'

export function preferredLanguageToI18nLanguage(
  preferredLanguage: PreferredLanguage,
): 'tr' | 'en' {
  return preferredLanguage === 'TR' ? 'tr' : 'en'
}
