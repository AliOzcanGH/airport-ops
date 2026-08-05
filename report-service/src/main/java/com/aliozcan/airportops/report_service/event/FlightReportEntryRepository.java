package com.aliozcan.airportops.report_service.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlightReportEntryRepository extends JpaRepository<FlightReportEntryEntity, UUID> {

    List<FlightReportEntryEntity> findByFlightId(UUID flightId);
}
