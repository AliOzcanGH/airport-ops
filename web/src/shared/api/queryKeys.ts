export const queryKeys = {
  auth: {
    me: ['auth', 'me'] as const,
  },
  iam: {
    health: ['iam', 'health'] as const,
  },
  platform: {
    tenants: ['platform', 'tenants'] as const,
    tenantDetail: (organizationId: string) =>
      ['platform', 'tenants', organizationId] as const,
    auditLogs: ['platform', 'auditLogs'] as const,
  },
  app: {
    setupOverview: ['app', 'setup', 'overview'] as const,
    dashboardOverview: ['app', 'dashboard', 'overview'] as const,
    members: (organizationId: string) =>
      ['app', 'members', organizationId] as const,
    stations: ['app', 'stations'] as const,
    stationGates: (stationId: string) =>
      ['app', 'stations', stationId, 'gates'] as const,
    flights: ['app', 'flights'] as const,
    flightTasks: (flightId: string) =>
      ['app', 'flights', flightId, 'tasks'] as const,
    auditLogs: ['app', 'auditLogs'] as const,
  },
}
