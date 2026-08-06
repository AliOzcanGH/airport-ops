import { queryOptions } from '@tanstack/react-query'
import { auditLogsApi } from '@/features/audit/api/auditLogsApi'
import { platformAuditLogsApi } from '@/features/audit/api/platformAuditLogsApi'
import { queryKeys } from '@/shared/api/queryKeys'

export const auditLogsQueryOptions = queryOptions({
  queryKey: queryKeys.app.auditLogs,
  queryFn: () => auditLogsApi.listAuditLogs(),
})

export const platformAuditLogsQueryOptions = queryOptions({
  queryKey: queryKeys.platform.auditLogs,
  queryFn: () => platformAuditLogsApi.listAuditLogs(),
})
