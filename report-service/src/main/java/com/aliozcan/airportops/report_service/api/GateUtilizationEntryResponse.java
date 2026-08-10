package com.aliozcan.airportops.report_service.api;

import com.aliozcan.airportops.report_service.readmodel.GateUtilizationEntity;

import java.util.UUID;

public record GateUtilizationEntryResponse(UUID gateId, int flightCount) {

    public static GateUtilizationEntryResponse from(GateUtilizationEntity entity) {
        return new GateUtilizationEntryResponse(entity.getGateId(), entity.getFlightCount());
    }
}
