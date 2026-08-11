package com.aliozcan.airportops.airport_service.station;

import com.aliozcan.airportops.airport_service.event.StationCreatedEvent;
import com.aliozcan.airportops.airport_service.security.IamPrincipal;
import com.aliozcan.airportops.airport_service.station.dto.CreateStationRequest;
import com.aliozcan.airportops.airport_service.station.dto.StationResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StationService(StationRepository stationRepository, ApplicationEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.eventPublisher = eventPublisher;
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

    @Transactional
    public StationResponse create(
            UUID pathOrganizationId, IamPrincipal principal, CreateStationRequest request) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }

        StationEntity entity = new StationEntity(
                pathOrganizationId, request.stationName(), request.airportCode(), request.gateCount());
        StationEntity saved = stationRepository.save(entity);

        eventPublisher.publishEvent(new StationCreatedEvent(
                saved.getId(), saved.getOrganizationId(), saved.getStationName(), saved.getGateCount()));

        return toResponse(saved);
    }
}
