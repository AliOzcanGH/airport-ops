package com.aliozcan.airportops.flight_service.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TurnaroundTaskRepository extends JpaRepository<TurnaroundTaskEntity, UUID> {

    List<TurnaroundTaskEntity> findByFlightIdOrderByTaskType(UUID flightId);
}
