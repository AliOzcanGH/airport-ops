package com.aliozcan.airportops.report_service.api;

import com.aliozcan.airportops.report_service.cache.GateUtilizationCache;
import com.aliozcan.airportops.report_service.error.TenantMismatchException;
import com.aliozcan.airportops.report_service.readmodel.DailyFlightSummaryRepository;
import com.aliozcan.airportops.report_service.readmodel.GateUtilizationRepository;
import com.aliozcan.airportops.report_service.security.IamPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class ReportController {

    private static final String REPORT_READ_PERMISSION = "report:read";

    private final DailyFlightSummaryRepository dailyFlightSummaryRepository;
    private final GateUtilizationRepository gateUtilizationRepository;
    private final GateUtilizationCache gateUtilizationCache;
    private final ObjectMapper objectMapper;

    public ReportController(
            DailyFlightSummaryRepository dailyFlightSummaryRepository,
            GateUtilizationRepository gateUtilizationRepository,
            GateUtilizationCache gateUtilizationCache,
            ObjectMapper objectMapper) {
        this.dailyFlightSummaryRepository = dailyFlightSummaryRepository;
        this.gateUtilizationRepository = gateUtilizationRepository;
        this.gateUtilizationCache = gateUtilizationCache;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/organizations/{orgId}/reports/daily-flights")
    public DailyFlightSummaryResponse dailyFlights(
            @PathVariable UUID orgId, @RequestParam LocalDate date, Authentication authentication) {
        authorize(orgId, authentication);
        return DailyFlightSummaryResponse.from(
                date, dailyFlightSummaryRepository.findByOrganizationIdAndSummaryDate(orgId, date).orElse(null));
    }

    @GetMapping("/organizations/{orgId}/reports/gate-utilization")
    public List<GateUtilizationEntryResponse> gateUtilization(
            @PathVariable UUID orgId, @RequestParam LocalDate date, Authentication authentication) {
        authorize(orgId, authentication);

        String cached = gateUtilizationCache.get(orgId, date);
        if (cached != null) {
            return deserialize(cached);
        }

        List<GateUtilizationEntryResponse> response = gateUtilizationRepository
                .findByOrganizationIdAndSummaryDate(orgId, date).stream()
                .map(GateUtilizationEntryResponse::from)
                .toList();
        gateUtilizationCache.put(orgId, date, serialize(response));
        return response;
    }

    private void authorize(UUID orgId, Authentication authentication) {
        IamPrincipal principal = principal(authentication);
        if (!principal.permissions().contains(REPORT_READ_PERMISSION)) {
            throw new AccessDeniedException(REPORT_READ_PERMISSION + " permission required");
        }
        if (!orgId.equals(principal.organizationId())) {
            throw new TenantMismatchException();
        }
    }

    private IamPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof IamPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing IAM principal details");
    }

    private String serialize(List<GateUtilizationEntryResponse> response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize gate utilization report", exception);
        }
    }

    private List<GateUtilizationEntryResponse> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<GateUtilizationEntryResponse>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize cached gate utilization report", exception);
        }
    }
}
