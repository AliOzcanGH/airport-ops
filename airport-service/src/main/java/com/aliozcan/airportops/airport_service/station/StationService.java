package com.aliozcan.airportops.airport_service.station;

import com.aliozcan.airportops.airport_service.security.IamPrincipal;
import com.aliozcan.airportops.airport_service.station.dto.CreateStationRequest;
import com.aliozcan.airportops.airport_service.station.dto.StationResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public List<StationResponse> list(UUID pathOrganizationId, IamPrincipal principal) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }
        return stationRepository.findByOrganizationIdOrderByStationName(pathOrganizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    private StationResponse toResponse(StationEntity entity) {
        return new StationResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getStationName(),
                entity.getAirportCode(),
                entity.getGateCount(),
                entity.getCreatedAt());
    }

    public StationResponse create(
            UUID pathOrganizationId, IamPrincipal principal, CreateStationRequest request) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }

        StationEntity entity = new StationEntity(
                pathOrganizationId, request.stationName(), request.airportCode(), request.gateCount());
        return toResponse(stationRepository.save(entity));
    }
}
