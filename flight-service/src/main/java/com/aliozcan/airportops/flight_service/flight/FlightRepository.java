package com.aliozcan.airportops.flight_service.flight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FlightRepository extends JpaRepository<FlightEntity, UUID> {

    List<FlightEntity> findByOrganizationIdOrderByScheduledDeparture(UUID organizationId);

    @Query("SELECT f FROM FlightEntity f WHERE f.assignedGateId = :gateId "
            + "AND f.scheduledDeparture < :newEnd AND f.scheduledArrival > :newStart")
    List<FlightEntity> findOverlappingOnGate(
            @Param("gateId") UUID gateId,
            @Param("newStart") Instant newStart,
            @Param("newEnd") Instant newEnd);
}
