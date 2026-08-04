package com.aliozcan.airportops.flight_service.flight;

import com.aliozcan.airportops.flight_service.flight.dto.CreateFlightRequest;
import com.aliozcan.airportops.flight_service.flight.dto.FlightResponse;
import com.aliozcan.airportops.flight_service.flight.dto.UpdateFlightStatusRequest;
import com.aliozcan.airportops.flight_service.security.IamPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FlightService {

    private static final String ACTIVE_GATE_STATUS = "ACTIVE";

    private final FlightRepository flightRepository;
    private final AirportServiceGateClient airportServiceGateClient;

    public FlightService(FlightRepository flightRepository, AirportServiceGateClient airportServiceGateClient) {
        this.flightRepository = flightRepository;
        this.airportServiceGateClient = airportServiceGateClient;
    }

    public List<FlightResponse> list(UUID pathOrganizationId, IamPrincipal principal) {
        verifyTenant(pathOrganizationId, principal);
        return flightRepository.findByOrganizationIdOrderByScheduledDeparture(pathOrganizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public FlightResponse getOne(UUID pathOrganizationId, UUID flightId, IamPrincipal principal) {
        verifyTenant(pathOrganizationId, principal);
        return toResponse(findOwnedFlight(pathOrganizationId, flightId));
    }

    public FlightResponse updateStatus(
            UUID pathOrganizationId,
            UUID flightId,
            IamPrincipal principal,
            UpdateFlightStatusRequest request) {
        verifyTenant(pathOrganizationId, principal);
        FlightEntity entity = findOwnedFlight(pathOrganizationId, flightId);

        FlightStatus currentStatus = FlightStatus.valueOf(entity.getStatus());
        FlightStatus targetStatus = FlightStatus.valueOf(request.status());
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        entity.updateStatus(targetStatus.name());
        return toResponse(flightRepository.save(entity));
    }

    private FlightEntity findOwnedFlight(UUID pathOrganizationId, UUID flightId) {
        FlightEntity entity = flightRepository.findById(flightId).orElseThrow(FlightNotFoundException::new);
        if (!entity.getOrganizationId().equals(pathOrganizationId)) {
            throw new FlightNotFoundException();
        }
        return entity;
    }

    public FlightResponse create(
            UUID pathOrganizationId,
            IamPrincipal principal,
            CreateFlightRequest request,
            String rawAuthorizationHeader) {
        verifyTenant(pathOrganizationId, principal);

        GateSnapshot gate = airportServiceGateClient.fetchGate(
                pathOrganizationId, request.assignedGateId(), rawAuthorizationHeader);
        if (!ACTIVE_GATE_STATUS.equals(gate.status())) {
            throw new GateNotActiveException();
        }

        boolean hasOverlap = !flightRepository.findOverlappingOnGate(
                        request.assignedGateId(), request.scheduledDeparture(), request.scheduledArrival())
                .isEmpty();
        if (hasOverlap) {
            throw new GateConflictException();
        }

        FlightEntity entity = new FlightEntity(
                pathOrganizationId,
                request.flightNumber(),
                request.origin(),
                request.destination(),
                request.scheduledDeparture(),
                request.scheduledArrival(),
                "SCHEDULED",
                request.assignedGateId());

        try {
            return toResponse(flightRepository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new FlightNumberConflictException();
        }
    }

    private void verifyTenant(UUID pathOrganizationId, IamPrincipal principal) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }
    }

    private FlightResponse toResponse(FlightEntity entity) {
        return new FlightResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getFlightNumber(),
                entity.getOrigin(),
                entity.getDestination(),
                entity.getScheduledDeparture(),
                entity.getScheduledArrival(),
                entity.getStatus(),
                entity.getAssignedGateId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
