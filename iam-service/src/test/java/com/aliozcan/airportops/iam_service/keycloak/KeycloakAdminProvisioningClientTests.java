package com.aliozcan.airportops.iam_service.keycloak;

import com.aliozcan.airportops.iam_service.config.KeycloakAdminProperties;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KeycloakAdminProvisioningClientTests {

    private Keycloak keycloak;
    private UsersResource users;
    private UserResource userResource;
    private Response response;
    private KeycloakAdminProvisioningClient client;

    @BeforeEach
    void setUp() {
        keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        users = mock(UsersResource.class);
        userResource = mock(UserResource.class);
        response = mock(Response.class);
        when(keycloak.realm("airport-ops")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-subject")).thenReturn(userResource);
        client = new KeycloakAdminProvisioningClient(
                keycloak,
                new KeycloakAdminProperties(
                        "http://127.0.0.1:8085",
                        "airport-ops",
                        "iam-service-admin",
                        "local-secret"));
    }

    @Test
    void createsLoginReadyUserAndSetsPassword() {
        when(users.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create(
                "http://127.0.0.1:8085/admin/realms/airport-ops/users/keycloak-subject"));

        String subject = client.createUser(
                "admin@pegasus.demo",
                "Airline Test",
                "StrongPassword123!");

        assertThat(subject).isEqualTo("keycloak-subject");
        ArgumentCaptor<UserRepresentation> userCaptor =
                ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername())
                .isEqualTo("admin@pegasus.demo");
        assertThat(userCaptor.getValue().getEmail())
                .isEqualTo("admin@pegasus.demo");
        assertThat(userCaptor.getValue().isEnabled()).isTrue();
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Airline");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("Test");
        assertThat(userCaptor.getValue().getRequiredActions()).isEmpty();

        ArgumentCaptor<CredentialRepresentation> credentialCaptor =
                ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getType())
                .isEqualTo(CredentialRepresentation.PASSWORD);
        assertThat(credentialCaptor.getValue().getValue())
                .isEqualTo("StrongPassword123!");
        assertThat(credentialCaptor.getValue().isTemporary()).isFalse();
        verify(response).close();
    }

    @Test
    void usesDashLastNameForSingleTokenName() {
        when(users.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create(
                "http://127.0.0.1:8085/admin/realms/airport-ops/users/keycloak-subject"));

        client.createUser(
                "admin@airline.demo",
                "Airline",
                "StrongPassword123!");

        ArgumentCaptor<UserRepresentation> userCaptor =
                ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Airline");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("-");
    }

    @Test
    void normalizesRepeatedWhitespaceInProfileName() {
        when(users.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create(
                "http://127.0.0.1:8085/admin/realms/airport-ops/users/keycloak-subject"));

        client.createUser(
                "admin@airline.demo",
                "  Airline   Test  Admin  ",
                "StrongPassword123!");

        ArgumentCaptor<UserRepresentation> userCaptor =
                ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Airline");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("Test Admin");
    }

    @Test
    void reportsDuplicateWithoutLookingUpOrLinkingExistingUser() {
        when(users.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);
        when(response.getStatus()).thenReturn(409);

        assertThatThrownBy(() -> client.createUser(
                "admin@pegasus.demo",
                "Airline Admin",
                "StrongPassword123!"))
                .isInstanceOfSatisfying(
                        KeycloakProvisioningException.class,
                        exception -> assertThat(exception.isDuplicateIdentity()).isTrue());

        verifyNoInteractions(userResource);
        verify(response).close();
    }
}
