import { apiClient } from '@/shared/api/apiClient'
import {
  acceptInvitationRequestSchema,
  createPlatformInvitationRequestSchema,
  invitationAcceptanceResponseSchema,
  invitationValidationResponseSchema,
  platformInvitationResponseSchema,
  validateInvitationRequestSchema,
  type AcceptInvitationRequest,
  type CreatePlatformInvitationRequest,
  type InvitationAcceptanceResponse,
  type InvitationValidationResponse,
  type PlatformInvitationResponse,
  type ValidateInvitationRequest,
} from '@/shared/api/schemas'

export const invitationApi = {
  createPlatformInvitation(
    request: CreatePlatformInvitationRequest,
  ): Promise<PlatformInvitationResponse> {
    return apiClient.post('/platform/invitations', {
      body: createPlatformInvitationRequestSchema.parse(request),
      schema: platformInvitationResponseSchema,
    })
  },

  validateInvitation(
    request: ValidateInvitationRequest,
  ): Promise<InvitationValidationResponse> {
    return apiClient.post('/invitations/validate', {
      body: validateInvitationRequestSchema.parse(request),
      schema: invitationValidationResponseSchema,
      credentials: 'omit',
      csrf: false,
      retryUnauthorized: false,
    })
  },

  acceptInvitation(
    request: AcceptInvitationRequest,
  ): Promise<InvitationAcceptanceResponse> {
    return apiClient.post('/invitations/accept', {
      body: acceptInvitationRequestSchema.parse(request),
      schema: invitationAcceptanceResponseSchema,
      credentials: 'omit',
      csrf: false,
      retryUnauthorized: false,
    })
  },
}
