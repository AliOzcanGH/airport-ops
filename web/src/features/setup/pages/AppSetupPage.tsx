import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type HTMLInputTypeAttribute,
} from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, Circle, LockKeyhole, Rocket, Save } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { authMeQueryOptions } from '@/features/auth/api/authQueries'
import { setupApi } from '@/features/setup/api/setupApi'
import { setupOverviewQueryOptions } from '@/features/setup/api/setupQueries'
import { ApiError } from '@/shared/api/ApiError'
import { queryKeys } from '@/shared/api/queryKeys'
import {
  setupProfileFormSchema,
  type SetupOverviewResponse,
  type SetupProfileForm,
  type SetupProfileResponse,
} from '@/shared/api/schemas'
import { PageHeader } from '@/shared/components/PageHeader'

type ProfileField = keyof SetupProfileForm
type FieldErrors = Partial<Record<ProfileField, string>>

const profileFields: ProfileField[] = [
  'displayName',
  'iataCode',
  'icaoCode',
  'countryCode',
  'timezone',
  'baseAirportIata',
  'operationsContactEmail',
]

const emptyProfile: SetupProfileForm = {
  displayName: '',
  iataCode: '',
  icaoCode: '',
  countryCode: '',
  timezone: '',
  baseAirportIata: '',
  operationsContactEmail: '',
}

function toFormValues(profile: SetupProfileResponse | null): SetupProfileForm {
  if (!profile) return { ...emptyProfile }
  return {
    displayName: profile.displayName,
    iataCode: profile.iataCode ?? '',
    icaoCode: profile.icaoCode ?? '',
    countryCode: profile.countryCode ?? '',
    timezone: profile.timezone ?? '',
    baseAirportIata: profile.baseAirportIata ?? '',
    operationsContactEmail: profile.operationsContactEmail ?? '',
  }
}

export function AppSetupPage() {
  const { t } = useTranslation()
  const overview = useQuery(setupOverviewQueryOptions)
  const [profileUnsaved, setProfileUnsaved] = useState(false)

  if (overview.isPending) {
    return (
      <section className="rounded-md border border-slate-200 bg-white p-5 shadow-sm">
        <p className="text-sm font-medium text-slate-950">
          {t('setup.page.loadingTitle')}
        </p>
        <p className="mt-1 text-sm text-slate-600">
          {t('setup.page.loadingDescription')}
        </p>
      </section>
    )
  }

  if (overview.isError) {
    return (
      <section
        role="alert"
        className="rounded-md border border-red-200 bg-red-50 p-5 text-red-900"
      >
        <h1 className="text-sm font-semibold">{t('setup.page.errorTitle')}</h1>
        <p className="mt-1 text-sm">{t('setup.page.errorDescription')}</p>
      </section>
    )
  }

  const data = overview.data

  return (
    <div className="space-y-8">
      <PageHeader
        title={`${data.organizationName} ${t('setup.page.titleSuffix')}`}
        description={t('setup.page.description')}
      />

      <section className="grid gap-4 border-y border-slate-200 py-5 md:grid-cols-3">
        <div>
          <p className="text-xs font-semibold uppercase text-slate-500">
            {t('setup.page.organizationStatus')}
          </p>
          <p className="mt-2 text-sm font-medium text-slate-900">
            {t(`setup.organizationStatuses.${data.organizationStatus}`)}
          </p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-500">
            {t('setup.page.preferredLanguage')}
          </p>
          <p className="mt-2 text-sm font-medium text-slate-900">
            {t(`setup.languages.${data.preferredLanguage}`)}
          </p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-500">
            {t('setup.page.wizardStatus')}
          </p>
          <p className="mt-2 text-sm font-medium text-slate-900">
            {t('setup.page.profileInProgress')}
          </p>
        </div>
      </section>

      <SetupProfileForm
        initialProfile={data.profile}
        onUnsavedChange={setProfileUnsaved}
      />

      <SetupCompletionPanel profile={data.profile} profileUnsaved={profileUnsaved} />

      <section
        aria-labelledby="setup-steps-heading"
        className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
      >
        <h2 id="setup-steps-heading" className="text-sm font-semibold text-slate-950">
          {t('setup.page.stepsTitle')}
        </h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">
          {t('setup.page.remainingSteps')}
        </p>
        <ol className="mt-5 space-y-3">
          {data.steps.map((step) => {
            const locked = step.status === 'LOCKED'
            const Icon = locked ? LockKeyhole : Circle
            return (
              <li
                key={step.key}
                className="flex items-center justify-between rounded-md border border-slate-200 px-4 py-3"
              >
                <div className="flex items-center gap-3">
                  <span className="grid size-8 place-items-center rounded-md bg-teal-50 text-teal-700">
                    <Icon aria-hidden="true" size={16} />
                  </span>
                  <div>
                    <p className="text-sm font-medium text-slate-950">
                      {t(`setup.steps.${step.key}`)}
                    </p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {t(`setup.statuses.${step.status}`)}
                    </p>
                  </div>
                </div>
                {!locked ? (
                  <CheckCircle2 aria-hidden="true" className="text-slate-300" size={18} />
                ) : null}
              </li>
            )
          })}
        </ol>
      </section>
    </div>
  )
}

function SetupCompletionPanel({
  profile,
  profileUnsaved,
}: {
  profile: SetupProfileResponse | null
  profileUnsaved: boolean
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [completionSucceeded, setCompletionSucceeded] = useState(false)
  const profileComplete = Boolean(
    profile &&
      [
        profile.displayName,
        profile.countryCode,
        profile.timezone,
        profile.operationsContactEmail,
      ].every((value) => value?.trim()),
  )

  const refreshContextAndOpenDashboard = async () => {
    setCompletionSucceeded(true)
    queryClient.setQueryData<SetupOverviewResponse>(
      queryKeys.app.setupOverview,
      (current) =>
        current ? { ...current, organizationStatus: 'ACTIVE' } : current,
    )
    await queryClient.invalidateQueries({
      queryKey: queryKeys.app.setupOverview,
    })
    await queryClient.invalidateQueries({
      queryKey: queryKeys.auth.me,
      refetchType: 'none',
    })
    await queryClient.fetchQuery(authMeQueryOptions)
    navigate('/app/dashboard', { replace: true })
  }

  const completeSetup = useMutation({
    mutationFn: setupApi.completeSetup,
    onSuccess: refreshContextAndOpenDashboard,
    onError: async (error) => {
      if (error instanceof ApiError && error.errorCode === 'SETUP_ALREADY_COMPLETED') {
        await refreshContextAndOpenDashboard()
        return
      }
      if (
        error instanceof ApiError &&
        (error.errorCode === 'SETUP_PROFILE_REQUIRED' ||
          error.errorCode === 'SETUP_PROFILE_INCOMPLETE')
      ) {
        await queryClient.invalidateQueries({
          queryKey: queryKeys.app.setupOverview,
        })
      }
    },
  })

  const completionError = (() => {
    const error = completeSetup.error
    if (!(error instanceof ApiError)) return t('setup.completion.genericError')
    if (error.errorCode === 'SETUP_PROFILE_REQUIRED') {
      return t('setup.completion.profileRequiredError')
    }
    if (error.errorCode === 'SETUP_PROFILE_INCOMPLETE') {
      return t('setup.completion.profileIncompleteError')
    }
    return t('setup.completion.genericError')
  })()
  const showError =
    completeSetup.isError &&
    !(completeSetup.error instanceof ApiError &&
      completeSetup.error.errorCode === 'SETUP_ALREADY_COMPLETED')

  return (
    <section className="space-y-4 rounded-md border border-slate-200 bg-white p-5 shadow-sm">
      <div>
        <h2 className="text-base font-semibold text-slate-950">
          {t('setup.completion.title')}
        </h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">
          {t('setup.completion.description')}
        </p>
      </div>

      {!profileComplete ? (
        <div
          role="status"
          className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900"
        >
          {t('setup.completion.incompleteWarning')}
        </div>
      ) : null}

      {showError ? (
        <div
          role="alert"
          className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {completionError}
        </div>
      ) : null}

      {completionSucceeded ? (
        <div
          role="status"
          className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800"
        >
          {t('setup.completion.success')}
        </div>
      ) : null}

      <button
        type="button"
        disabled={!profileComplete || profileUnsaved || completeSetup.isPending}
        onClick={() => completeSetup.mutate()}
        className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60"
      >
        <Rocket aria-hidden="true" size={17} />
        {completeSetup.isPending
          ? t('setup.completion.loading')
          : t('setup.completion.button')}
      </button>
    </section>
  )
}

function SetupProfileForm({
  initialProfile,
  onUnsavedChange,
}: {
  initialProfile: SetupProfileResponse | null
  onUnsavedChange: (unsaved: boolean) => void
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [values, setValues] = useState<SetupProfileForm>(() =>
    toFormValues(initialProfile),
  )
  const [savedValues, setSavedValues] = useState<SetupProfileForm>(() =>
    toFormValues(initialProfile),
  )
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [saved, setSaved] = useState(false)
  const loadedVersion = useRef(initialProfile?.updatedAt ?? 'empty')

  useEffect(() => {
    const version = initialProfile?.updatedAt ?? 'empty'
    if (version !== loadedVersion.current) {
      loadedVersion.current = version
      setValues(toFormValues(initialProfile))
      setSavedValues(toFormValues(initialProfile))
      setFieldErrors({})
      setSaved(false)
    }
  }, [initialProfile])

  const saveProfile = useMutation({
    mutationFn: setupApi.saveProfile,
    onSuccess: (profile) => {
      loadedVersion.current = profile.updatedAt
      setValues(toFormValues(profile))
      setSavedValues(toFormValues(profile))
      setFieldErrors({})
      setSaved(true)
      queryClient.setQueryData<SetupOverviewResponse>(
        queryKeys.app.setupOverview,
        (current) => (current ? { ...current, profile } : current),
      )
      void queryClient.invalidateQueries({ queryKey: queryKeys.app.setupOverview })
    },
  })

  const isDirty =
    JSON.stringify(values) !== JSON.stringify(savedValues)

  useEffect(() => {
    onUnsavedChange(isDirty || saveProfile.isPending)
  }, [isDirty, saveProfile.isPending, onUnsavedChange])

  const updateField = (field: ProfileField, value: string) => {
    setValues((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSaved(false)
    if (saveProfile.isError) saveProfile.reset()
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaved(false)
    saveProfile.reset()
    const parsed = setupProfileFormSchema.safeParse(values)
    if (!parsed.success) {
      const flattened = parsed.error.flatten().fieldErrors
      const nextErrors: FieldErrors = {}
      for (const field of profileFields) {
        nextErrors[field] = flattened[field]?.[0]
      }
      setFieldErrors(nextErrors)
      return
    }
    setFieldErrors({})
    saveProfile.mutate(parsed.data)
  }

  const errorMessage =
    saveProfile.error instanceof ApiError
      ? saveProfile.error.message
      : t('setup.profile.genericError')

  return (
    <form
      onSubmit={submit}
      noValidate
      className="space-y-6 rounded-md border border-slate-200 bg-white p-5 shadow-sm"
    >
      <div>
        <h2 className="text-base font-semibold text-slate-950">
          {t('setup.profile.title')}
        </h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">
          {t('setup.profile.description')}
        </p>
      </div>

      <fieldset className="space-y-4">
        <legend className="text-sm font-semibold text-slate-950">
          {t('setup.profile.organizationDetails')}
        </legend>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <SetupTextField
              field="displayName"
              label={t('setup.profile.fields.displayName')}
              value={values.displayName}
              error={fieldErrors.displayName}
              required
              autoComplete="organization"
              onChange={updateField}
            />
          </div>
          <SetupTextField
            field="iataCode"
            label={t('setup.profile.fields.iataCode')}
            value={values.iataCode}
            error={fieldErrors.iataCode}
            placeholder="B6"
            onChange={updateField}
          />
          <SetupTextField
            field="icaoCode"
            label={t('setup.profile.fields.icaoCode')}
            value={values.icaoCode}
            error={fieldErrors.icaoCode}
            placeholder="THY"
            onChange={updateField}
          />
          <SetupTextField
            field="countryCode"
            label={t('setup.profile.fields.countryCode')}
            value={values.countryCode}
            error={fieldErrors.countryCode}
            placeholder="TR"
            onChange={updateField}
          />
          <SetupTextField
            field="timezone"
            label={t('setup.profile.fields.timezone')}
            value={values.timezone}
            error={fieldErrors.timezone}
            placeholder="Europe/Istanbul"
            onChange={updateField}
          />
        </div>
      </fieldset>

      <fieldset className="space-y-4 border-t border-slate-200 pt-5">
        <legend className="text-sm font-semibold text-slate-950">
          {t('setup.profile.operationsContact')}
        </legend>
        <div className="grid gap-4 sm:grid-cols-2">
          <SetupTextField
            field="baseAirportIata"
            label={t('setup.profile.fields.baseAirportIata')}
            value={values.baseAirportIata}
            error={fieldErrors.baseAirportIata}
            placeholder="IST"
            onChange={updateField}
          />
          <SetupTextField
            field="operationsContactEmail"
            label={t('setup.profile.fields.operationsContactEmail')}
            value={values.operationsContactEmail}
            error={fieldErrors.operationsContactEmail}
            type="email"
            autoComplete="email"
            placeholder="ops@example.com"
            onChange={updateField}
          />
        </div>
      </fieldset>

      {saveProfile.isError ? (
        <div
          role="alert"
          className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {errorMessage}
        </div>
      ) : null}

      {saved ? (
        <div
          role="status"
          className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800"
        >
          {t('setup.profile.success')}
        </div>
      ) : null}

      <button
        type="submit"
        disabled={saveProfile.isPending}
        className="inline-flex h-10 items-center justify-center gap-2 rounded-md bg-teal-700 px-4 text-sm font-semibold text-white shadow-sm hover:bg-teal-800 disabled:cursor-wait disabled:opacity-70"
      >
        <Save aria-hidden="true" size={17} />
        {saveProfile.isPending
          ? t('setup.profile.saving')
          : t('setup.profile.save')}
      </button>
    </form>
  )
}

function SetupTextField({
  field,
  label,
  value,
  error,
  required = false,
  type = 'text',
  autoComplete,
  placeholder,
  onChange,
}: {
  field: ProfileField
  label: string
  value: string
  error?: string
  required?: boolean
  type?: HTMLInputTypeAttribute
  autoComplete?: string
  placeholder?: string
  onChange: (field: ProfileField, value: string) => void
}) {
  const { t } = useTranslation()
  const inputId = `setup-profile-${field}`
  const errorId = `${inputId}-error`

  return (
    <div>
      <label htmlFor={inputId} className="text-sm font-medium text-slate-800">
        {label}
        {!required ? (
          <span className="ml-1 text-xs font-normal text-slate-500">
            {t('setup.profile.optional')}
          </span>
        ) : null}
      </label>
      <input
        id={inputId}
        name={field}
        type={type}
        autoComplete={autoComplete}
        value={value}
        onChange={(event) => onChange(field, event.target.value)}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
        placeholder={placeholder}
        className="mt-1.5 h-10 w-full rounded-md border border-slate-300 px-3 text-sm text-slate-950 shadow-sm outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
      />
      {error ? (
        <p id={errorId} className="mt-1.5 text-xs text-red-700">
          {t(error)}
        </p>
      ) : null}
    </div>
  )
}
