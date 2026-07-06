import { z } from 'zod'

export const publicEnvironmentSchema = z.object({
  VITE_IAM_API_BASE_URL: z.string().min(1).default('/api'),
  VITE_KEYCLOAK_URL: z
    .string()
    .url()
    .default('http://127.0.0.1:8085'),
  VITE_KEYCLOAK_REALM: z.string().min(1).default('airport-ops'),
  VITE_KEYCLOAK_CLIENT_ID: z.string().min(1).default('airport-ops-local'),
})

export type PublicEnvironment = z.infer<typeof publicEnvironmentSchema>

export function parsePublicEnvironment(
  input: Record<string, unknown>,
): PublicEnvironment {
  return publicEnvironmentSchema.parse(input)
}

export const environment = parsePublicEnvironment(import.meta.env)
