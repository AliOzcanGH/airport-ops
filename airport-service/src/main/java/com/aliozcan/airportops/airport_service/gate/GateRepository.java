package com.aliozcan.airportops.airport_service.gate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GateRepository extends JpaRepository<GateEntity, UUID> {

    List<GateEntity> findByStationIdOrderByCode(UUID stationId);
}
