import { queryOptions } from '@tanstack/react-query'
import { membersApi } from '@/features/members/api/membersApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const organizationMembersQueryOptions = (organizationId: string) =>
  queryOptions({
    queryKey: queryKeys.app.members(organizationId),
    queryFn: () => membersApi.listMembers(organizationId),
    enabled: Boolean(organizationId),
  })
