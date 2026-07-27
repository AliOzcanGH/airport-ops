import { apiClient } from '@/shared/api/apiClient'
import {
  inviteOrganizationMemberRequestSchema,
  organizationMemberInvitationResponseSchema,
  organizationMembersResponseSchema,
  type InviteOrganizationMemberRequest,
  type OrganizationMemberInvitationResponse,
  type OrganizationMembersResponse,
} from '@/shared/api/schemas'

export const membersApi = {
  inviteMember(
    organizationId: string,
    request: InviteOrganizationMemberRequest,
  ): Promise<OrganizationMemberInvitationResponse> {
    return apiClient.post(`/organizations/${organizationId}/invitations`, {
      body: inviteOrganizationMemberRequestSchema.parse(request),
      schema: organizationMemberInvitationResponseSchema,
    })
  },

  listMembers(organizationId: string): Promise<OrganizationMembersResponse> {
    return apiClient.get(`/organizations/${organizationId}/members`, {
      schema: organizationMembersResponseSchema,
    })
  },
}
