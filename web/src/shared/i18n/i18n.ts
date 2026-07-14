import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

void i18n.use(initReactI18next).init({
  lng: 'en',
  fallbackLng: 'en',
  interpolation: {
    escapeValue: false,
  },
  resources: {
    en: {
      translation: {
        setup: {
          layout: {
            productName: 'Airport Ops setup',
            subtitle: 'Tenant onboarding',
            environment: 'Setup workspace - Local',
            logout: 'Log out',
            loggingOut: 'Logging out...',
          },
          page: {
            loadingTitle: 'Loading setup...',
            loadingDescription:
              'Airport Ops is loading your organization onboarding state.',
            errorTitle: 'Setup overview unavailable',
            errorDescription:
              'Your organization setup cannot be loaded right now. Try again shortly.',
            titleSuffix: 'setup',
            description:
              'Complete the first tenant onboarding steps before opening the main airline workspace.',
            organizationStatus: 'Organization status',
            preferredLanguage: 'Preferred language',
            wizardStatus: 'Wizard status',
            placeholderOnly: 'Placeholder only',
            stepsTitle: 'Tenant setup steps',
            notImplemented:
              'The full setup wizard is not implemented yet. These W5A steps reserve the gate behavior only.',
          },
          steps: {
            PROFILE: 'Basic profile',
            STATION: 'Main station',
            REVIEW: 'Review and finish',
          },
          statuses: {
            NOT_STARTED: 'Not started',
            LOCKED: 'Locked',
          },
          organizationStatuses: {
            ONBOARDING_INCOMPLETE: 'Onboarding incomplete',
            ACTIVE: 'Active',
            INACTIVE: 'Inactive',
          },
          languages: {
            TR: 'Turkish',
            EN: 'English',
          },
        },
      },
    },
    tr: {
      translation: {
        setup: {
          layout: {
            productName: 'Airport Ops kurulum',
            subtitle: 'Tenant onboarding',
            environment: 'Kurulum \u00e7al\u0131\u015fma alan\u0131 - Local',
            logout: '\u00c7\u0131k\u0131\u015f yap',
            loggingOut: '\u00c7\u0131k\u0131\u015f yap\u0131l\u0131yor...',
          },
          page: {
            loadingTitle: 'Kurulum y\u00fckleniyor...',
            loadingDescription:
              'Airport Ops organizasyon onboarding durumunu y\u00fckl\u00fcyor.',
            errorTitle: 'Kurulum \u00f6zeti al\u0131namad\u0131',
            errorDescription:
              'Organizasyon kurulumunuz \u015fu anda y\u00fcklenemiyor. K\u0131sa s\u00fcre sonra tekrar deneyin.',
            titleSuffix: 'kurulumu',
            description:
              'Ana havayolu \u00e7al\u0131\u015fma alan\u0131na ge\u00e7meden \u00f6nce ilk tenant onboarding ad\u0131mlar\u0131n\u0131 tamamlay\u0131n.',
            organizationStatus: 'Organizasyon durumu',
            preferredLanguage: 'Tercih edilen dil',
            wizardStatus: 'Sihirbaz durumu',
            placeholderOnly: 'Ge\u00e7ici yer tutucu',
            stepsTitle: 'Tenant kurulum ad\u0131mlar\u0131',
            notImplemented:
              'Tam kurulum sihirbaz\u0131 hen\u00fcz uygulanmad\u0131. Bu W5A ad\u0131mlar\u0131 yaln\u0131zca ge\u00e7i\u015f davran\u0131\u015f\u0131n\u0131 ay\u0131r\u0131r.',
          },
          steps: {
            PROFILE: 'Temel profil',
            STATION: 'Ana istasyon',
            REVIEW: 'Kontrol ve tamamlama',
          },
          statuses: {
            NOT_STARTED: 'Ba\u015flanmad\u0131',
            LOCKED: 'Kilitli',
          },
          organizationStatuses: {
            ONBOARDING_INCOMPLETE: 'Onboarding tamamlanmad\u0131',
            ACTIVE: 'Aktif',
            INACTIVE: 'Pasif',
          },
          languages: {
            TR: 'T\u00fcrk\u00e7e',
            EN: '\u0130ngilizce',
          },
        },
      },
    },
  },
})

export { i18n }
