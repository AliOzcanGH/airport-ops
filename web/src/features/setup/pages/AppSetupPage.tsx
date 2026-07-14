import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, Circle, LockKeyhole } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { setupOverviewQueryOptions } from '@/features/setup/api/setupQueries'
import { PageHeader } from '@/shared/components/PageHeader'

export function AppSetupPage() {
  const { t } = useTranslation()
  const overview = useQuery(setupOverviewQueryOptions)

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
        <p className="mt-1 text-sm">
          {t('setup.page.errorDescription')}
        </p>
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
            {t('setup.page.placeholderOnly')}
          </p>
        </div>
      </section>

      <section
        aria-labelledby="setup-steps-heading"
        className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
      >
        <h2 id="setup-steps-heading" className="text-sm font-semibold text-slate-950">
          {t('setup.page.stepsTitle')}
        </h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">
          {t('setup.page.notImplemented')}
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
