import { z } from 'zod'
import { ApiError } from '@/shared/api/ApiError'
import {
  backendErrorResponseSchema,
  csrfMetadataSchema,
  type CsrfMetadata,
} from '@/shared/api/schemas'
import { environment } from '@/shared/config/env'

type ApiRequestOptions<T> = Omit<RequestInit, 'body'> & {
  body?: unknown
  schema?: z.ZodType<T>
  csrf?: boolean
  retryUnauthorized?: boolean
}

type SessionExpiredHandler = () => void

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE'])

export class ApiClient {
  private csrfMetadata: CsrfMetadata | null = null
  private csrfPromise: Promise<CsrfMetadata> | null = null
  private refreshPromise: Promise<boolean> | null = null
  private sessionExpiredHandler: SessionExpiredHandler | null = null

  constructor(private readonly baseUrl: string) {}

  setSessionExpiredHandler(handler: SessionExpiredHandler | null): void {
    this.sessionExpiredHandler = handler
  }

  resetSessionState(): void {
    this.csrfMetadata = null
    this.csrfPromise = null
    this.refreshPromise = null
  }

  request<T>(
    path: string,
    options: ApiRequestOptions<T> = {},
  ): Promise<T> {
    return this.executeRequest(path, options, false)
  }

  get<T>(
    path: string,
    options: Omit<ApiRequestOptions<T>, 'method' | 'body'> = {},
  ): Promise<T> {
    return this.request(path, { ...options, method: 'GET' })
  }

  post<T>(
    path: string,
    options: Omit<ApiRequestOptions<T>, 'method'> = {},
  ): Promise<T> {
    return this.request(path, { ...options, method: 'POST' })
  }

  private async executeRequest<T>(
    path: string,
    options: ApiRequestOptions<T>,
    retried: boolean,
  ): Promise<T> {
    const {
      body,
      schema,
      csrf = this.isUnsafe(options.method),
      retryUnauthorized = true,
      ...requestInit
    } = options
    const headers = new Headers(requestInit.headers)
    headers.set('Accept', 'application/json')

    if (body !== undefined) {
      headers.set('Content-Type', 'application/json')
    }

    if (csrf) {
      const csrfMetadata = await this.ensureCsrf()
      headers.set(csrfMetadata.headerName, csrfMetadata.token)
    }

    const response = await fetch(this.url(path), {
      ...requestInit,
      headers,
      credentials: requestInit.credentials ?? 'include',
      body: body === undefined ? undefined : JSON.stringify(body),
    })

    if (response.status === 401 && retryUnauthorized && !retried) {
      if (await this.refreshSession()) {
        return this.executeRequest(path, options, true)
      }
      this.sessionExpiredHandler?.()
    }

    const payload = await this.readPayload(response)
    if (!response.ok) {
      throw this.toApiError(response.status, payload)
    }

    return schema ? schema.parse(payload) : (payload as T)
  }

  private async ensureCsrf(): Promise<CsrfMetadata> {
    if (this.csrfMetadata) return this.csrfMetadata
    if (this.csrfPromise) return this.csrfPromise

    this.csrfPromise = this.fetchCsrf()
    try {
      this.csrfMetadata = await this.csrfPromise
      return this.csrfMetadata
    } finally {
      this.csrfPromise = null
    }
  }

  private async fetchCsrf(): Promise<CsrfMetadata> {
    const response = await fetch(this.url('/auth/session/csrf'), {
      method: 'GET',
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const payload = await this.readPayload(response)
    if (!response.ok) throw this.toApiError(response.status, payload)
    return csrfMetadataSchema.parse(payload)
  }

  private async refreshSession(): Promise<boolean> {
    if (this.refreshPromise) return this.refreshPromise
    this.refreshPromise = this.performRefresh()
    try {
      return await this.refreshPromise
    } finally {
      this.refreshPromise = null
    }
  }

  private async performRefresh(): Promise<boolean> {
    try {
      const csrfMetadata = await this.ensureCsrf()
      const response = await fetch(this.url('/auth/session/refresh'), {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          [csrfMetadata.headerName]: csrfMetadata.token,
        },
        credentials: 'include',
      })
      return response.ok
    } catch {
      return false
    }
  }

  private isUnsafe(method?: string): boolean {
    return !SAFE_METHODS.has((method ?? 'GET').toUpperCase())
  }

  private toApiError(status: number, payload: unknown): ApiError {
    const parsedError = backendErrorResponseSchema.safeParse(payload)
    const errorBody = parsedError.success ? parsedError.data : null
    return new ApiError(
      errorBody?.message ?? `Request failed with status ${status}`,
      status,
      errorBody,
    )
  }

  private url(path: string): string {
    const normalizedBaseUrl = this.baseUrl.replace(/\/$/, '')
    const normalizedPath = path.startsWith('/') ? path : `/${path}`
    return `${normalizedBaseUrl}${normalizedPath}`
  }

  private async readPayload(response: Response): Promise<unknown> {
    if (response.status === 204) return undefined

    const text = await response.text()
    if (!text) return undefined

    try {
      return JSON.parse(text) as unknown
    } catch {
      return text
    }
  }
}

export const apiClient = new ApiClient(environment.VITE_IAM_API_BASE_URL)
