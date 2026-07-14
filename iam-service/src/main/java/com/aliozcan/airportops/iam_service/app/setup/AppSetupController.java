package com.aliozcan.airportops.iam_service.app.setup;

import com.aliozcan.airportops.iam_service.app.setup.dto.AppSetupOverviewResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/setup")
public class AppSetupController {

    private final AppSetupOverviewService setupOverviewService;

    public AppSetupController(AppSetupOverviewService setupOverviewService) {
        this.setupOverviewService = setupOverviewService;
    }

    @GetMapping("/overview")
    public AppSetupOverviewResponse overview(Authentication authentication) {
        return setupOverviewService.overview(authentication);
    }
}
