package com.aliozcan.airportops.airport_service.station;

import com.aliozcan.airportops.airport_service.security.IamPrincipal;
import com.aliozcan.airportops.airport_service.station.dto.CreateStationRequest;
import com.aliozcan.airportops.airport_service.station.dto.StationResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public StationResponse create(
            UUID pathOrganizationId, IamPrincipal principal, CreateStationRequest request) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }

        StationEntity entity = new StationEntity(
                pathOrganizationId, request.stationName(), request.airportCode(), request.gateCount());
        StationEntity saved = stationRepository.save(entity);

        return new StationResponse(
                saved.getId(),
                saved.getOrganizationId(),
                saved.getStationName(),
                saved.getAirportCode(),
                saved.getGateCount(),
                saved.getCreatedAt());
    }
}
