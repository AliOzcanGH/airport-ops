import { apiClient } from '@/shared/api/apiClient'
import { auditLogsResponseSchema, type AuditLogsResponse } from '@/shared/api/schemas'

export const auditLogsApi = {
  listAuditLogs(): Promise<AuditLogsResponse> {
    return apiClient.get('/app/audit-logs', {
      schema: auditLogsResponseSchema,
    })
  },
}
