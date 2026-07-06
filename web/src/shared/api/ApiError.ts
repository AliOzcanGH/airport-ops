import type { BackendErrorResponse } from '@/shared/api/schemas'

export class ApiError extends Error {
  readonly status: number
  readonly errorCode: string | null
  readonly path: string | null
  readonly response: BackendErrorResponse | null

  constructor(
    message: string,
    status: number,
    response: BackendErrorResponse | null = null,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = response?.errorCode ?? null
    this.path = response?.path ?? null
    this.response = response
  }
}
