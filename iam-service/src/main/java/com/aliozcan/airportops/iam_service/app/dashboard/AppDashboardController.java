package com.aliozcan.airportops.iam_service.app.dashboard;

import com.aliozcan.airportops.iam_service.app.dashboard.dto.AppDashboardOverviewResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/dashboard")
public class AppDashboardController {

    private final AppDashboardOverviewService dashboardOverviewService;

    public AppDashboardController(AppDashboardOverviewService dashboardOverviewService) {
        this.dashboardOverviewService = dashboardOverviewService;
    }

    @GetMapping("/overview")
    public AppDashboardOverviewResponse overview(Authentication authentication) {
        return dashboardOverviewService.overview(authentication);
    }
}
