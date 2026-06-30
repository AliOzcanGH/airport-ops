package com.aliozcan.airportops.iam_service.platform;

import com.aliozcan.airportops.iam_service.platform.dto.AuthorizationProbeResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/authorization")
public class PlatformAuthorizationProbeController {

    private static final String REQUIRED_PERMISSION = "platform:invitation:create";

    @PreAuthorize("hasAuthority('platform:invitation:create')")
    @GetMapping("/probe")
    public AuthorizationProbeResponse probe() {
        return new AuthorizationProbeResponse(
                "Permission granted",
                REQUIRED_PERMISSION
        );
    }
}
