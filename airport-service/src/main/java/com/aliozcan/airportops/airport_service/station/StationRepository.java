package com.aliozcan.airportops.airport_service.station;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StationRepository extends JpaRepository<StationEntity, UUID> {

    List<StationEntity> findByOrganizationIdOrderByStationName(UUID organizationId);
}
