import { z } from 'zod'
import { ApiError } from '@/shared/api/ApiError'
import { backendErrorResponseSchema } from '@/shared/api/schemas'
import { environment } from '@/shared/config/env'

type ApiRequestOptions<T> = Omit<RequestInit, 'body'> & {
  body?: unknown
  schema?: z.ZodType<T>
}

export class ApiClient {
  constructor(private readonly baseUrl: string) {}

  async request<T>(
    path: string,
    options: ApiRequestOptions<T> = {},
  ): Promise<T> {
    const { body, schema, ...requestInit } = options
    const headers = new Headers(requestInit.headers)
    headers.set('Accept', 'application/json')

    if (body !== undefined) {
      headers.set('Content-Type', 'application/json')
    }

    const response = await fetch(this.url(path), {
      ...requestInit,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    const payload = await this.readPayload(response)

    if (!response.ok) {
      const parsedError = backendErrorResponseSchema.safeParse(payload)
      const errorBody = parsedError.success ? parsedError.data : null
      throw new ApiError(
        errorBody?.message ?? `Request failed with status ${response.status}`,
        response.status,
        errorBody,
      )
    }

    return schema ? schema.parse(payload) : (payload as T)
  }

  get<T>(
    path: string,
    options: Omit<ApiRequestOptions<T>, 'method' | 'body'> = {},
  ): Promise<T> {
    return this.request(path, { ...options, method: 'GET' })
  }

  private url(path: string): string {
    const normalizedBaseUrl = this.baseUrl.replace(/\/$/, '')
    const normalizedPath = path.startsWith('/') ? path : `/${path}`
    return `${normalizedBaseUrl}${normalizedPath}`
  }

  private async readPayload(response: Response): Promise<unknown> {
    if (response.status === 204) {
      return undefined
    }

    const text = await response.text()
    if (!text) {
      return undefined
    }

    try {
      return JSON.parse(text) as unknown
    } catch {
      return text
    }
  }
}

export const apiClient = new ApiClient(environment.VITE_IAM_API_BASE_URL)
