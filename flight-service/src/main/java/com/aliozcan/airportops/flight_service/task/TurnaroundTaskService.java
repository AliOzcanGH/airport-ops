package com.aliozcan.airportops.flight_service.task;

import com.aliozcan.airportops.flight_service.flight.FlightEntity;
import com.aliozcan.airportops.flight_service.flight.FlightNotFoundException;
import com.aliozcan.airportops.flight_service.flight.FlightRepository;
import com.aliozcan.airportops.flight_service.flight.TenantMismatchException;
import com.aliozcan.airportops.flight_service.security.IamPrincipal;
import com.aliozcan.airportops.flight_service.task.dto.TaskResponse;
import com.aliozcan.airportops.flight_service.task.dto.UpdateTaskStatusRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TurnaroundTaskService {

    private final TurnaroundTaskRepository taskRepository;
    private final FlightRepository flightRepository;

    public TurnaroundTaskService(
            TurnaroundTaskRepository taskRepository, FlightRepository flightRepository) {
        this.taskRepository = taskRepository;
        this.flightRepository = flightRepository;
    }

    public List<TaskResponse> list(UUID pathOrganizationId, UUID flightId, IamPrincipal principal) {
        verifyTenant(pathOrganizationId, principal);
        verifyFlightOwnership(pathOrganizationId, flightId);
        return taskRepository.findByFlightIdOrderByTaskType(flightId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse updateStatus(
            UUID pathOrganizationId,
            UUID flightId,
            UUID taskId,
            IamPrincipal principal,
            UpdateTaskStatusRequest request) {
        verifyTenant(pathOrganizationId, principal);
        verifyFlightOwnership(pathOrganizationId, flightId);

        TurnaroundTaskEntity task = taskRepository.findById(taskId).orElseThrow(TaskNotFoundException::new);
        if (!task.getFlightId().equals(flightId)) {
            throw new TaskNotFoundException();
        }

        TaskStatus currentStatus = TaskStatus.valueOf(task.getStatus());
        TaskStatus targetStatus = TaskStatus.valueOf(request.status());
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidTaskStatusTransitionException(currentStatus, targetStatus);
        }

        task.updateStatus(targetStatus.name(), request.assignedTo());
        return toResponse(taskRepository.save(task));
    }

    private void verifyTenant(UUID pathOrganizationId, IamPrincipal principal) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }
    }

    private void verifyFlightOwnership(UUID pathOrganizationId, UUID flightId) {
        FlightEntity flight = flightRepository.findById(flightId).orElseThrow(FlightNotFoundException::new);
        if (!flight.getOrganizationId().equals(pathOrganizationId)) {
            throw new FlightNotFoundException();
        }
    }

    private TaskResponse toResponse(TurnaroundTaskEntity entity) {
        return new TaskResponse(
                entity.getId(),
                entity.getFlightId(),
                entity.getTaskType(),
                entity.getStatus(),
                entity.getAssignedTo(),
                entity.getDueAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
