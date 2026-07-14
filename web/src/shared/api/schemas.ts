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
export const organizationMemberStatusSchema = z.enum([
  'ACTIVE',
  'INACTIVE',
  'INVITED',
])
export const invitationStatusSchema = z.enum([
  'PENDING',
  'ACCEPTED',
  'CANCELLED',
  'EXPIRED',
])
export const provisioningStatusSchema = z.enum([
  'READY',
  'LOGIN_SETUP_PENDING',
])
export const preferredLanguageSchema = z.enum(['TR', 'EN'])
export const setupStepStatusSchema = z.enum(['NOT_STARTED', 'LOCKED'])
export const setupStepKeySchema = z.enum(['PROFILE', 'STATION', 'REVIEW'])

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
  fullName: z.string().min(1),
  preferredLanguage: preferredLanguageSchema,
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

export const invitationTokenSchema = z
  .string()
  .regex(/^[A-Za-z0-9_-]{43}$/, 'Invitation token is invalid')

export const createPlatformInvitationRequestSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'Email is required')
    .max(320, 'Email is too long')
    .pipe(z.email('Enter a valid email address')),
  organizationName: z
    .string()
    .trim()
    .min(1, 'Organization name is required')
    .max(200, 'Organization name is too long'),
})

export const platformInvitationResponseSchema = z.object({
  id: z.uuid(),
  email: z.email(),
  organizationName: z.string().min(1),
  status: invitationStatusSchema,
  expiresAt: z.string().min(1),
  invitationToken: invitationTokenSchema,
})

export const validateInvitationRequestSchema = z.object({
  token: invitationTokenSchema,
})

export const invitationValidationResponseSchema = z.object({
  organizationName: z.string().min(1),
  invitedEmail: z.string().min(1),
  expiresAt: z.string().min(1),
})

export const acceptInvitationFormSchema = z
  .object({
    fullName: z
      .string()
      .trim()
      .min(1, 'Full name is required')
      .max(150, 'Full name is too long'),
    password: z
      .string()
      .min(12, 'Password must be at least 12 characters')
      .max(128, 'Password is too long'),
    preferredLanguage: preferredLanguageSchema,
    confirmPassword: z.string().min(1, 'Confirm your password'),
  })
  .refine((value) => value.password === value.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  })

export const acceptInvitationRequestSchema = z.object({
  token: invitationTokenSchema,
  fullName: z
    .string()
    .trim()
    .min(1, 'Full name is required')
    .max(150, 'Full name is too long'),
  password: z
    .string()
    .min(12, 'Password must be at least 12 characters')
    .max(128, 'Password is too long'),
  preferredLanguage: preferredLanguageSchema,
})

export const invitationAcceptanceResponseSchema = z.object({
  email: z.email(),
  organizationName: z.string().min(1),
  organizationStatus: organizationStatusSchema,
  userStatus: userStatusSchema,
  provisioningStatus: provisioningStatusSchema,
  message: z.string().min(1),
})

export const platformTenantSummarySchema = z.object({
  organizationId: z.uuid(),
  organizationName: z.string().min(1),
  organizationStatus: organizationStatusSchema,
  createdAt: z.string().min(1),
  memberCount: z.number().int().nonnegative(),
  primaryAdminEmail: z.email().nullable(),
})

export const platformTenantDirectoryResponseSchema = z.object({
  tenants: z.array(platformTenantSummarySchema),
})

export const platformTenantMemberSchema = z.object({
  memberId: z.uuid(),
  userId: z.uuid(),
  email: z.email(),
  fullName: z.string().min(1),
  memberStatus: organizationMemberStatusSchema,
  roles: z.array(z.string()),
  joinedAt: z.string().nullable(),
})

export const platformTenantDetailResponseSchema =
  platformTenantSummarySchema.extend({
    members: z.array(platformTenantMemberSchema),
  })

export const setupStepSchema = z.object({
  key: setupStepKeySchema,
  status: setupStepStatusSchema,
})

export const setupOverviewResponseSchema = z.object({
  organizationId: z.uuid(),
  organizationName: z.string().min(1),
  organizationStatus: organizationStatusSchema,
  preferredLanguage: preferredLanguageSchema,
  steps: z.array(setupStepSchema),
})

export type HealthResponse = z.infer<typeof healthResponseSchema>
export type BackendErrorResponse = z.infer<typeof backendErrorResponseSchema>
export type CsrfMetadata = z.infer<typeof csrfMetadataSchema>
export type WorkspaceType = z.infer<typeof workspaceTypeSchema>
export type TenantContext = z.infer<typeof tenantContextSchema>
export type AuthMeResponse = z.infer<typeof authMeResponseSchema>
export type LoginRequest = z.infer<typeof loginRequestSchema>
export type InvitationStatus = z.infer<typeof invitationStatusSchema>
export type OrganizationMemberStatus = z.infer<
  typeof organizationMemberStatusSchema
>
export type ProvisioningStatus = z.infer<typeof provisioningStatusSchema>
export type PreferredLanguage = z.infer<typeof preferredLanguageSchema>
export type CreatePlatformInvitationRequest = z.infer<
  typeof createPlatformInvitationRequestSchema
>
export type PlatformInvitationResponse = z.infer<
  typeof platformInvitationResponseSchema
>
export type ValidateInvitationRequest = z.infer<
  typeof validateInvitationRequestSchema
>
export type InvitationValidationResponse = z.infer<
  typeof invitationValidationResponseSchema
>
export type AcceptInvitationForm = z.infer<typeof acceptInvitationFormSchema>
export type AcceptInvitationRequest = z.infer<
  typeof acceptInvitationRequestSchema
>
export type InvitationAcceptanceResponse = z.infer<
  typeof invitationAcceptanceResponseSchema
>
export type PlatformTenantSummary = z.infer<typeof platformTenantSummarySchema>
export type PlatformTenantDirectoryResponse = z.infer<
  typeof platformTenantDirectoryResponseSchema
>
export type PlatformTenantMember = z.infer<typeof platformTenantMemberSchema>
export type PlatformTenantDetailResponse = z.infer<
  typeof platformTenantDetailResponseSchema
>
export type SetupStep = z.infer<typeof setupStepSchema>
export type SetupOverviewResponse = z.infer<typeof setupOverviewResponseSchema>
