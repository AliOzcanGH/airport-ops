import { apiClient } from '@/shared/api/apiClient'
import { auditLogsResponseSchema, type AuditLogsResponse } from '@/shared/api/schemas'

export const platformAuditLogsApi = {
  listAuditLogs(): Promise<AuditLogsResponse> {
    return apiClient.get('/platform/audit-logs', {
      schema: auditLogsResponseSchema,
    })
  },
}
