package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupOverviewResponse;
import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupCompletionResponse;
import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupProfileRequest;
import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/setup")
public class AppSetupController {

    private final AppSetupOverviewService setupOverviewService;
    private final AppSetupProfileService setupProfileService;
    private final AppSetupCompletionService setupCompletionService;

    public AppSetupController(
            AppSetupOverviewService setupOverviewService,
            AppSetupProfileService setupProfileService,
            AppSetupCompletionService setupCompletionService) {
        this.setupOverviewService = setupOverviewService;
        this.setupProfileService = setupProfileService;
        this.setupCompletionService = setupCompletionService;
    }

    @GetMapping("/overview")
    public AppSetupOverviewResponse overview(Authentication authentication) {
        return setupOverviewService.overview(authentication);
    }

    @PutMapping("/profile")
    public AppSetupProfileResponse saveProfile(
            Authentication authentication,
            @Valid @RequestBody AppSetupProfileRequest request) {
        return setupProfileService.save(authentication, request);
    }

    @PostMapping("/complete")
    public AppSetupCompletionResponse complete(Authentication authentication) {
        return setupCompletionService.complete(authentication);
    }
}
