package com.aliozcan.airportops.flight_service.flight;

import com.aliozcan.airportops.flight_service.event.FlightCreatedEvent;
import com.aliozcan.airportops.flight_service.event.FlightStatusChangedEvent;
import com.aliozcan.airportops.flight_service.flight.dto.CreateFlightRequest;
import com.aliozcan.airportops.flight_service.flight.dto.FlightResponse;
import com.aliozcan.airportops.flight_service.flight.dto.UpdateFlightStatusRequest;
import com.aliozcan.airportops.flight_service.security.IamPrincipal;
import com.aliozcan.airportops.flight_service.task.TaskType;
import com.aliozcan.airportops.flight_service.task.TurnaroundTaskEntity;
import com.aliozcan.airportops.flight_service.task.TurnaroundTaskRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FlightService {

    private static final String ACTIVE_GATE_STATUS = "ACTIVE";
    private static final String OPEN_TASK_STATUS = "OPEN";

    private final FlightRepository flightRepository;
    private final AirportServiceGateClient airportServiceGateClient;
    private final TurnaroundTaskRepository turnaroundTaskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FlightService(
            FlightRepository flightRepository,
            AirportServiceGateClient airportServiceGateClient,
            TurnaroundTaskRepository turnaroundTaskRepository,
            ApplicationEventPublisher eventPublisher) {
        this.flightRepository = flightRepository;
        this.airportServiceGateClient = airportServiceGateClient;
        this.turnaroundTaskRepository = turnaroundTaskRepository;
        this.eventPublisher = eventPublisher;
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

    @Transactional
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
        FlightEntity saved = flightRepository.save(entity);

        eventPublisher.publishEvent(new FlightStatusChangedEvent(
                saved.getId(),
                saved.getOrganizationId(),
                saved.getFlightNumber(),
                currentStatus.name(),
                targetStatus.name()));

        return toResponse(saved);
    }

    private FlightEntity findOwnedFlight(UUID pathOrganizationId, UUID flightId) {
        FlightEntity entity = flightRepository.findById(flightId).orElseThrow(FlightNotFoundException::new);
        if (!entity.getOrganizationId().equals(pathOrganizationId)) {
            throw new FlightNotFoundException();
        }
        return entity;
    }

    @Transactional
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

        FlightEntity saved;
        try {
            saved = flightRepository.save(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new FlightNumberConflictException();
        }

        List<TurnaroundTaskEntity> tasks = Arrays.stream(TaskType.values())
                .map(taskType -> new TurnaroundTaskEntity(saved.getId(), taskType.name(), OPEN_TASK_STATUS))
                .toList();
        turnaroundTaskRepository.saveAll(tasks);

        eventPublisher.publishEvent(new FlightCreatedEvent(
                saved.getId(),
                saved.getOrganizationId(),
                saved.getFlightNumber(),
                saved.getOrigin(),
                saved.getDestination(),
                saved.getScheduledDeparture(),
                saved.getScheduledArrival(),
                saved.getAssignedGateId(),
                saved.getStatus()));

        return toResponse(saved);
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
