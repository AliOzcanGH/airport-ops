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
            profileInProgress: 'Profile setup in progress',
            stepsTitle: 'Tenant setup steps',
            notImplemented:
              'The full setup wizard is not implemented yet. These W5A steps reserve the gate behavior only.',
            remainingSteps:
              'Profile details can now be saved. The remaining setup steps are not available yet.',
          },
          profile: {
            title: 'Setup profile',
            description:
              'Add the organization and operations contact details used by your airline workspace.',
            organizationDetails: 'Organization details',
            operationsContact: 'Operations contact',
            optional: '(optional)',
            fields: {
              displayName: 'Display name',
              iataCode: 'IATA code',
              icaoCode: 'ICAO code',
              countryCode: 'Country code',
              timezone: 'Timezone',
              baseAirportIata: 'Base airport IATA',
              operationsContactEmail: 'Operations contact email',
            },
            save: 'Save setup profile',
            saving: 'Saving profile...',
            success: 'Setup profile saved.',
            genericError: 'Setup profile could not be saved. Try again shortly.',
          },
          validation: {
            displayNameMin: 'Display name must be at least 2 characters.',
            displayNameMax: 'Display name must be at most 160 characters.',
            iataCode: 'IATA code must contain exactly 2 letters or numbers.',
            icaoCode: 'ICAO code must contain exactly 3 letters.',
            countryCode: 'Country code must contain exactly 2 letters.',
            timezoneMax: 'Timezone must be at most 80 characters.',
            baseAirportIata:
              'Base airport IATA code must contain exactly 3 letters.',
            operationsContactEmailMax:
              'Operations contact email must be at most 254 characters.',
            operationsContactEmailInvalid:
              'Enter a valid operations contact email.',
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
            profileInProgress: 'Profil kurulumu devam ediyor',
            stepsTitle: 'Tenant kurulum ad\u0131mlar\u0131',
            notImplemented:
              'Tam kurulum sihirbaz\u0131 hen\u00fcz uygulanmad\u0131. Bu W5A ad\u0131mlar\u0131 yaln\u0131zca ge\u00e7i\u015f davran\u0131\u015f\u0131n\u0131 ay\u0131r\u0131r.',
            remainingSteps:
              'Profil bilgileri art\u0131k kaydedilebilir. Kalan kurulum ad\u0131mlar\u0131 hen\u00fcz kullan\u0131ma a\u00e7\u0131k de\u011fil.',
          },
          profile: {
            title: 'Kurulum profili',
            description:
              'Havayolu \u00e7al\u0131\u015fma alan\u0131nda kullan\u0131lacak organizasyon ve operasyon ileti\u015fim bilgilerini ekleyin.',
            organizationDetails: 'Organizasyon bilgileri',
            operationsContact: 'Operasyon ileti\u015fimi',
            optional: '(iste\u011fe ba\u011fl\u0131)',
            fields: {
              displayName: 'G\u00f6r\u00fcnen ad',
              iataCode: 'IATA kodu',
              icaoCode: 'ICAO kodu',
              countryCode: '\u00dclke kodu',
              timezone: 'Saat dilimi',
              baseAirportIata: 'Ana havaliman\u0131 IATA kodu',
              operationsContactEmail: 'Operasyon ileti\u015fim e-postas\u0131',
            },
            save: 'Kurulum profilini kaydet',
            saving: 'Profil kaydediliyor...',
            success: 'Kurulum profili kaydedildi.',
            genericError:
              'Kurulum profili kaydedilemedi. K\u0131sa s\u00fcre sonra tekrar deneyin.',
          },
          validation: {
            displayNameMin: 'G\u00f6r\u00fcnen ad en az 2 karakter olmal\u0131d\u0131r.',
            displayNameMax: 'G\u00f6r\u00fcnen ad en fazla 160 karakter olabilir.',
            iataCode: 'IATA kodu tam olarak 2 harf veya rakam i\u00e7ermelidir.',
            icaoCode: 'ICAO kodu tam olarak 3 harf i\u00e7ermelidir.',
            countryCode: '\u00dclke kodu tam olarak 2 harf i\u00e7ermelidir.',
            timezoneMax: 'Saat dilimi en fazla 80 karakter olabilir.',
            baseAirportIata:
              'Ana havaliman\u0131 IATA kodu tam olarak 3 harf i\u00e7ermelidir.',
            operationsContactEmailMax:
              'Operasyon ileti\u015fim e-postas\u0131 en fazla 254 karakter olabilir.',
            operationsContactEmailInvalid:
              'Ge\u00e7erli bir operasyon ileti\u015fim e-postas\u0131 girin.',
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
