import { apiClient } from '@/shared/api/apiClient'
import {
  platformTenantDetailResponseSchema,
  platformTenantDirectoryResponseSchema,
  type PlatformTenantDetailResponse,
  type PlatformTenantDirectoryResponse,
} from '@/shared/api/schemas'

export const platformTenantApi = {
  listPlatformTenants(
    options: { signal?: AbortSignal } = {},
  ): Promise<PlatformTenantDirectoryResponse> {
    return apiClient.get('/platform/tenants', {
      schema: platformTenantDirectoryResponseSchema,
      signal: options.signal,
    })
  },

  getPlatformTenantDetail(
    organizationId: string,
    options: { signal?: AbortSignal } = {},
  ): Promise<PlatformTenantDetailResponse> {
    return apiClient.get(`/platform/tenants/${organizationId}`, {
      schema: platformTenantDetailResponseSchema,
      signal: options.signal,
    })
  },
}
