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

export const csrfMetadataSchema = z.object({
  headerName: z.string().min(1),
  parameterName: z.string().min(1),
  token: z.string().min(1),
})

export const workspaceTypeSchema = z.enum(['PLATFORM', 'TENANT'])
export const userStatusSchema = z.enum([
  'PROVISIONING',
  'ACTIVE',
  'KEYCLOAK_SYNC_FAILED',
  'INACTIVE',
])
export const organizationStatusSchema = z.enum([
  'ONBOARDING_INCOMPLETE',
  'ACTIVE',
  'INACTIVE',
])

export const tenantContextSchema = z.object({
  organizationId: z.uuid(),
  organizationName: z.string().min(1),
  organizationStatus: organizationStatusSchema,
  roles: z.array(z.string()),
  permissions: z.array(z.string()),
})

export const authMeResponseSchema = z.object({
  keycloakSubject: z.string().nullable(),
  issuer: z.string().nullable(),
  email: z.email(),
  preferredUsername: z.string().nullable(),
  iamUserId: z.uuid(),
  iamUserStatus: userStatusSchema,
  keycloakRoles: z.array(z.string()),
  iamRoles: z.array(z.string()),
  permissions: z.array(z.string()),
  availableWorkspaces: z.array(workspaceTypeSchema),
  defaultWorkspace: workspaceTypeSchema.nullable(),
  tenantContext: tenantContextSchema.nullable(),
})

export const loginRequestSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').pipe(z.email()),
  password: z.string().min(1, 'Password is required'),
})

export type HealthResponse = z.infer<typeof healthResponseSchema>
export type BackendErrorResponse = z.infer<typeof backendErrorResponseSchema>
export type CsrfMetadata = z.infer<typeof csrfMetadataSchema>
export type WorkspaceType = z.infer<typeof workspaceTypeSchema>
export type TenantContext = z.infer<typeof tenantContextSchema>
export type AuthMeResponse = z.infer<typeof authMeResponseSchema>
export type LoginRequest = z.infer<typeof loginRequestSchema>
