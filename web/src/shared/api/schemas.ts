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
export const invitationEmailDeliveryStatusSchema = z.enum([
  'NOT_SENT',
  'SENT',
  'FAILED',
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

const mfaLoginChallengeBaseSchema = z.object({
  challengeId: z.uuid(),
  expiresAt: z.string().min(1),
  attemptsRemaining: z.number().int().nonnegative(),
})

export const mfaRequiredLoginResponseSchema =
  mfaLoginChallengeBaseSchema.extend({
    outcome: z.literal('MFA_REQUIRED'),
  })

export const mfaEnrollmentRequiredLoginResponseSchema =
  mfaLoginChallengeBaseSchema.extend({
    outcome: z.literal('MFA_ENROLLMENT_REQUIRED'),
    otpauthUri: z.string().startsWith('otpauth://'),
    manualEntryKey: z.string().min(1),
  })

export const loginResponseSchema = z.discriminatedUnion('outcome', [
  mfaRequiredLoginResponseSchema,
  mfaEnrollmentRequiredLoginResponseSchema,
])

export const verifyMfaRequestSchema = z.object({
  challengeId: z.uuid(),
  code: z.string().regex(/^\d{6}$/, 'Enter a 6-digit code'),
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
  emailDeliveryStatus: invitationEmailDeliveryStatusSchema,
  emailSentAt: z.string().nullable(),
  devAcceptLink: z.string().nullable(),
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

const optionalUppercaseCode = (pattern: RegExp, message: string) =>
  z
    .string()
    .transform((value) => value.trim().toUpperCase() || null)
    .refine((value) => value === null || pattern.test(value), message)

const optionalTrimmedString = (maxLength: number, message: string) =>
  z
    .string()
    .transform((value) => value.trim() || null)
    .refine((value) => value === null || value.length <= maxLength, message)

const optionalEmail = z
  .string()
  .transform((value) => value.trim().toLowerCase() || null)
  .refine(
    (value) => value === null || value.length <= 254,
    'setup.validation.operationsContactEmailMax',
  )
  .refine(
    (value) => value === null || z.email().safeParse(value).success,
    'setup.validation.operationsContactEmailInvalid',
  )

export const setupProfileFormSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, 'setup.validation.displayNameMin')
    .max(160, 'setup.validation.displayNameMax'),
  iataCode: optionalUppercaseCode(
    /^[A-Z0-9]{2}$/,
    'setup.validation.iataCode',
  ),
  icaoCode: optionalUppercaseCode(
    /^[A-Z]{3}$/,
    'setup.validation.icaoCode',
  ),
  countryCode: optionalUppercaseCode(
    /^[A-Z]{2}$/,
    'setup.validation.countryCode',
  ),
  timezone: optionalTrimmedString(80, 'setup.validation.timezoneMax'),
  baseAirportIata: optionalUppercaseCode(
    /^[A-Z]{3}$/,
    'setup.validation.baseAirportIata',
  ),
  operationsContactEmail: optionalEmail,
})

export const setupProfileRequestSchema = setupProfileFormSchema

export const setupProfileResponseSchema = z.object({
  organizationId: z.uuid(),
  displayName: z.string().min(1),
  iataCode: z.string().nullable(),
  icaoCode: z.string().nullable(),
  countryCode: z.string().nullable(),
  timezone: z.string().nullable(),
  baseAirportIata: z.string().nullable(),
  operationsContactEmail: z.string().nullable(),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
})

export const setupOverviewResponseSchema = z.object({
  organizationId: z.uuid(),
  organizationName: z.string().min(1),
  organizationStatus: organizationStatusSchema,
  preferredLanguage: preferredLanguageSchema,
  steps: z.array(setupStepSchema),
  profile: setupProfileResponseSchema.nullable(),
})

export const setupCompletionResponseSchema = z.object({
  organizationId: z.uuid(),
  organizationStatus: z.literal('ACTIVE'),
  completedAt: z.iso.datetime(),
})

export const dashboardOverviewResponseSchema = tenantContextSchema

export const organizationMemberRoleSchema = z.enum(['OPS_USER', 'VIEWER'])

export const inviteOrganizationMemberRequestSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'Email is required')
    .pipe(z.email()),
  fullName: z
    .string()
    .trim()
    .min(1, 'Full name is required')
    .max(150, 'Full name is too long'),
  intendedRole: organizationMemberRoleSchema,
})

export const organizationMemberInvitationResponseSchema = z.object({
  id: z.uuid(),
  email: z.email(),
  fullName: z.string().nullable(),
  intendedRole: organizationMemberRoleSchema,
  status: invitationStatusSchema,
  expiresAt: z.string().min(1),
  emailDeliveryStatus: invitationEmailDeliveryStatusSchema,
  emailSentAt: z.string().nullable(),
  devAcceptLink: z.string().nullable(),
})

export const organizationMembersResponseSchema = z.array(platformTenantMemberSchema)

export const createStationRequestSchema = z.object({
  stationName: z
    .string()
    .trim()
    .min(1, 'Station name is required')
    .max(200, 'Station name is too long'),
  airportCode: z
    .string()
    .trim()
    .min(1, 'Airport code is required')
    .max(10, 'Airport code is too long')
    .transform((value) => value.toUpperCase()),
  gateCount: z.coerce
    .number()
    .int('Gate count must be a whole number')
    .min(0, 'Gate count cannot be negative'),
})

export const stationResponseSchema = z.object({
  id: z.uuid(),
  organizationId: z.uuid(),
  stationName: z.string().min(1),
  airportCode: z.string().min(1),
  gateCount: z.number().int(),
  createdAt: z.string().min(1),
})

export const stationsResponseSchema = z.array(stationResponseSchema)

export const gateStatusSchema = z.enum(['ACTIVE', 'MAINTENANCE', 'CLOSED'])

export const createGateRequestSchema = z.object({
  code: z
    .string()
    .trim()
    .min(1, 'Gate code is required')
    .max(10, 'Gate code is too long'),
  terminal: z
    .string()
    .trim()
    .max(50, 'Terminal is too long')
    .optional()
    .transform((value) => value || null),
})

export const updateGateStatusRequestSchema = z.object({
  status: gateStatusSchema,
})

export const gateResponseSchema = z.object({
  id: z.uuid(),
  stationId: z.uuid(),
  code: z.string().min(1),
  terminal: z.string().nullable(),
  status: gateStatusSchema,
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
})

export const gatesResponseSchema = z.array(gateResponseSchema)

export const flightStatusSchema = z.enum([
  'SCHEDULED',
  'BOARDING',
  'DEPARTED',
  'DELAYED',
  'CANCELLED',
])

export const createFlightRequestSchema = z.object({
  flightNumber: z
    .string()
    .trim()
    .min(1, 'Flight number is required')
    .max(10, 'Flight number is too long'),
  origin: z
    .string()
    .trim()
    .min(1, 'Origin is required')
    .max(10, 'Origin is too long')
    .transform((value) => value.toUpperCase()),
  destination: z
    .string()
    .trim()
    .min(1, 'Destination is required')
    .max(10, 'Destination is too long')
    .transform((value) => value.toUpperCase()),
  scheduledDeparture: z
    .string()
    .min(1, 'Scheduled departure is required')
    .pipe(z.iso.datetime({ offset: true, precision: null })),
  scheduledArrival: z
    .string()
    .min(1, 'Scheduled arrival is required')
    .pipe(z.iso.datetime({ offset: true, precision: null })),
  assignedGateId: z.uuid('Select a gate'),
})

export const updateFlightStatusRequestSchema = z.object({
  status: flightStatusSchema,
})

export const flightResponseSchema = z.object({
  id: z.uuid(),
  organizationId: z.uuid(),
  flightNumber: z.string().min(1),
  origin: z.string().min(1),
  destination: z.string().min(1),
  scheduledDeparture: z.string().min(1),
  scheduledArrival: z.string().min(1),
  status: flightStatusSchema,
  assignedGateId: z.uuid().nullable(),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
})

export const flightsResponseSchema = z.array(flightResponseSchema)

export type HealthResponse = z.infer<typeof healthResponseSchema>
export type BackendErrorResponse = z.infer<typeof backendErrorResponseSchema>
export type CsrfMetadata = z.infer<typeof csrfMetadataSchema>
export type WorkspaceType = z.infer<typeof workspaceTypeSchema>
export type TenantContext = z.infer<typeof tenantContextSchema>
export type AuthMeResponse = z.infer<typeof authMeResponseSchema>
export type LoginRequest = z.infer<typeof loginRequestSchema>
export type MfaRequiredLoginResponse = z.infer<
  typeof mfaRequiredLoginResponseSchema
>
export type MfaEnrollmentRequiredLoginResponse = z.infer<
  typeof mfaEnrollmentRequiredLoginResponseSchema
>
export type LoginResponse = z.infer<typeof loginResponseSchema>
export type VerifyMfaRequest = z.infer<typeof verifyMfaRequestSchema>
export type InvitationStatus = z.infer<typeof invitationStatusSchema>
export type InvitationEmailDeliveryStatus = z.infer<
  typeof invitationEmailDeliveryStatusSchema
>
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
export type SetupProfileForm = z.input<typeof setupProfileFormSchema>
export type SetupProfileRequest = z.output<typeof setupProfileRequestSchema>
export type SetupProfileResponse = z.infer<typeof setupProfileResponseSchema>
export type SetupOverviewResponse = z.infer<typeof setupOverviewResponseSchema>
export type SetupCompletionResponse = z.infer<
  typeof setupCompletionResponseSchema
>
export type DashboardOverviewResponse = z.infer<
  typeof dashboardOverviewResponseSchema
>
export type OrganizationMemberRole = z.infer<
  typeof organizationMemberRoleSchema
>
export type InviteOrganizationMemberRequest = z.infer<
  typeof inviteOrganizationMemberRequestSchema
>
export type OrganizationMemberInvitationResponse = z.infer<
  typeof organizationMemberInvitationResponseSchema
>
export type OrganizationMembersResponse = z.infer<
  typeof organizationMembersResponseSchema
>
export type CreateStationForm = z.input<typeof createStationRequestSchema>
export type CreateStationRequest = z.output<typeof createStationRequestSchema>
export type StationResponse = z.infer<typeof stationResponseSchema>
export type StationsResponse = z.infer<typeof stationsResponseSchema>
export type GateStatus = z.infer<typeof gateStatusSchema>
export type CreateGateForm = z.input<typeof createGateRequestSchema>
export type CreateGateRequest = z.output<typeof createGateRequestSchema>
export type UpdateGateStatusRequest = z.infer<typeof updateGateStatusRequestSchema>
export type GateResponse = z.infer<typeof gateResponseSchema>
export type GatesResponse = z.infer<typeof gatesResponseSchema>
export type FlightStatus = z.infer<typeof flightStatusSchema>
export type CreateFlightForm = z.input<typeof createFlightRequestSchema>
export type CreateFlightRequest = z.output<typeof createFlightRequestSchema>
export type UpdateFlightStatusRequest = z.infer<
  typeof updateFlightStatusRequestSchema
>
export type FlightResponse = z.infer<typeof flightResponseSchema>
export type FlightsResponse = z.infer<typeof flightsResponseSchema>
