package com.aliozcan.airportops.airport_service.gate;

import com.aliozcan.airportops.airport_service.gate.dto.CreateGateRequest;
import com.aliozcan.airportops.airport_service.gate.dto.GateResponse;
import com.aliozcan.airportops.airport_service.gate.dto.UpdateGateStatusRequest;
import com.aliozcan.airportops.airport_service.security.IamPrincipal;
import com.aliozcan.airportops.airport_service.station.StationEntity;
import com.aliozcan.airportops.airport_service.station.StationRepository;
import com.aliozcan.airportops.airport_service.station.TenantMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GateService {

    private final GateRepository gateRepository;
    private final StationRepository stationRepository;

    public GateService(GateRepository gateRepository, StationRepository stationRepository) {
        this.gateRepository = gateRepository;
        this.stationRepository = stationRepository;
    }

    public List<GateResponse> list(UUID pathOrganizationId, UUID stationId, IamPrincipal principal) {
        UUID station = verifyStationOwnership(pathOrganizationId, stationId, principal);
        return gateRepository.findByStationIdOrderByCode(station).stream()
                .map(this::toResponse)
                .toList();
    }

    public GateResponse getOneByOrganization(UUID pathOrganizationId, UUID gateId, IamPrincipal principal) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }

        GateEntity gate = gateRepository.findById(gateId).orElseThrow(GateNotFoundException::new);
        StationEntity station = stationRepository.findById(gate.getStationId())
                .orElseThrow(GateNotFoundException::new);
        if (!station.getOrganizationId().equals(pathOrganizationId)) {
            throw new GateNotFoundException();
        }
        return toResponse(gate);
    }

    public GateResponse getOne(UUID pathOrganizationId, UUID stationId, UUID gateId, IamPrincipal principal) {
        UUID station = verifyStationOwnership(pathOrganizationId, stationId, principal);

        GateEntity gate = gateRepository.findById(gateId)
                .filter(candidate -> candidate.getStationId().equals(station))
                .orElseThrow(GateNotFoundException::new);
        return toResponse(gate);
    }

    public GateResponse create(
            UUID pathOrganizationId, UUID stationId, IamPrincipal principal, CreateGateRequest request) {
        UUID station = verifyStationOwnership(pathOrganizationId, stationId, principal);

        GateEntity entity = new GateEntity(station, request.code(), request.terminal(), "ACTIVE");
        try {
            return toResponse(gateRepository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new GateCodeConflictException();
        }
    }

    public GateResponse updateStatus(
            UUID pathOrganizationId,
            UUID stationId,
            UUID gateId,
            IamPrincipal principal,
            UpdateGateStatusRequest request) {
        UUID station = verifyStationOwnership(pathOrganizationId, stationId, principal);

        GateEntity gate = gateRepository.findById(gateId)
                .filter(candidate -> candidate.getStationId().equals(station))
                .orElseThrow(GateNotFoundException::new);

        gate.setStatus(request.status());
        return toResponse(gateRepository.save(gate));
    }

    private UUID verifyStationOwnership(UUID pathOrganizationId, UUID stationId, IamPrincipal principal) {
        if (principal.organizationId() == null
                || !principal.organizationId().equals(pathOrganizationId)) {
            throw new TenantMismatchException();
        }

        StationEntity station = stationRepository.findById(stationId)
                .orElseThrow(StationNotFoundException::new);
        if (!station.getOrganizationId().equals(pathOrganizationId)) {
            throw new StationNotFoundException();
        }
        return station.getId();
    }

    private GateResponse toResponse(GateEntity entity) {
        return new GateResponse(
                entity.getId(),
                entity.getStationId(),
                entity.getCode(),
                entity.getTerminal(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
