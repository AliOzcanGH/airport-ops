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
  },
}
