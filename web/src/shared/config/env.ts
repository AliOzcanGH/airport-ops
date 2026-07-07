import { z } from 'zod'

export const publicEnvironmentSchema = z.object({
  VITE_IAM_API_BASE_URL: z.string().min(1).default('/api'),
})

export type PublicEnvironment = z.infer<typeof publicEnvironmentSchema>

export function parsePublicEnvironment(
  input: Record<string, unknown>,
): PublicEnvironment {
  return publicEnvironmentSchema.parse(input)
}

export const environment = parsePublicEnvironment(import.meta.env)
