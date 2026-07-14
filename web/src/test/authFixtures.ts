import type { AuthMeResponse } from '@/shared/api/schemas'

const baseUser: AuthMeResponse = {
  keycloakSubject: 'keycloak-user-id',
  issuer: 'http://127.0.0.1:8085/realms/airport-ops',
  email: 'user@airport-ops.test',
  fullName: 'Airport Ops User',
  preferredLanguage: 'EN',
  preferredUsername: 'user@airport-ops.test',
  iamUserId: '8d284ebc-8085-49f0-a41e-fc1a99dbdb49',
  iamUserStatus: 'ACTIVE',
  keycloakRoles: [],
  iamRoles: [],
  permissions: [],
  availableWorkspaces: [],
  defaultWorkspace: null,
  tenantContext: null,
}

export const noWorkspaceUser: AuthMeResponse = { ...baseUser }

export const platformUser: AuthMeResponse = {
  ...baseUser,
  email: 'platform.admin@demo.com',
  fullName: 'Platform Admin',
  preferredUsername: 'platform.admin@demo.com',
  keycloakRoles: ['PLATFORM_ADMIN'],
  iamRoles: ['PLATFORM_ADMIN'],
  permissions: [
    'platform:invitation:create',
    'tenant:manage',
    'tenant:read',
  ],
  availableWorkspaces: ['PLATFORM'],
  defaultWorkspace: 'PLATFORM',
}

export const tenantUser: AuthMeResponse = {
  ...baseUser,
  email: 'airline.admin@demo.com',
  fullName: 'Airline Admin',
  preferredLanguage: 'TR',
  preferredUsername: 'airline.admin@demo.com',
  availableWorkspaces: ['TENANT'],
  defaultWorkspace: 'TENANT',
  tenantContext: {
    organizationId: '316b7ca9-02b7-4ec7-a69f-f70b8725625a',
    organizationName: 'Example Airlines',
    organizationStatus: 'ONBOARDING_INCOMPLETE',
    roles: ['AIRLINE_ADMIN'],
    permissions: ['flight:create', 'member:invite', 'station:read'],
  },
}

export const activeTenantUser: AuthMeResponse = {
  ...tenantUser,
  preferredLanguage: 'EN',
  tenantContext: tenantUser.tenantContext
    ? {
        ...tenantUser.tenantContext,
        organizationStatus: 'ACTIVE',
      }
    : null,
}

export const dualWorkspaceUser: AuthMeResponse = {
  ...platformUser,
  availableWorkspaces: ['PLATFORM', 'TENANT'],
  defaultWorkspace: 'PLATFORM',
  tenantContext: tenantUser.tenantContext,
}
