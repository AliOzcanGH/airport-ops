import { z } from 'zod'

export const healthResponseSchema = z.object({
  status: z.string().min(1),
})

export const backendErrorResponseSchema = z.object({
  timestamp: z.string(),
  status: z.number().int(),
  error: z.string(),
  errorCode: z.string(),
  message: z.string(),
  path: z.string(),
})

export type HealthResponse = z.infer<typeof healthResponseSchema>
export type BackendErrorResponse = z.infer<typeof backendErrorResponseSchema>
