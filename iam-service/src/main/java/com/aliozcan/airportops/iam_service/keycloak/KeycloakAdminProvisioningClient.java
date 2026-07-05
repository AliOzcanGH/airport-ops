package com.aliozcan.airportops.iam_service.keycloak;

import com.aliozcan.airportops.iam_service.config.KeycloakAdminProperties;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

@Component
public class KeycloakAdminProvisioningClient
        implements KeycloakProvisioningClient {

    private final Keycloak keycloak;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminProvisioningClient(
            Keycloak keycloak,
            KeycloakAdminProperties properties) {
        this.keycloak = keycloak;
        this.properties = properties;
    }

    @Override
    public String createUser(String email, String password) {
        UsersResource users = keycloak.realm(properties.realm()).users();
        String userId = createIdentity(users, email);
        setPassword(users, userId, password);
        return userId;
    }

    private String createIdentity(UsersResource users, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(false);

        try (Response response = users.create(user)) {
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new KeycloakProvisioningException(
                        "Keycloak identity already exists",
                        true);
            }
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new KeycloakProvisioningException(
                        "Keycloak user creation failed with status "
                                + response.getStatus(),
                        false);
            }
            return CreatedResponseUtil.getCreatedId(response);
        } catch (ProcessingException | WebApplicationException exception) {
            throw new KeycloakProvisioningException(
                    "Keycloak user creation request failed",
                    exception);
        }
    }

    private void setPassword(
            UsersResource users,
            String userId,
            String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        try {
            users.get(userId).resetPassword(credential);
        } catch (ProcessingException | WebApplicationException exception) {
            throw new KeycloakProvisioningException(
                    "Keycloak password setup request failed",
                    exception);
        }
    }
}
