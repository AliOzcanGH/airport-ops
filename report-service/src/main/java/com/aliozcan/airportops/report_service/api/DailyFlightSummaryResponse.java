package com.aliozcan.airportops.report_service.api;

import com.aliozcan.airportops.report_service.readmodel.DailyFlightSummaryEntity;

import java.time.LocalDate;

public record DailyFlightSummaryResponse(
        LocalDate date,
        int totalFlights,
        int delayedFlights,
        int cancelledFlights) {

    public static DailyFlightSummaryResponse from(LocalDate date, DailyFlightSummaryEntity entity) {
        if (entity == null) {
            return new DailyFlightSummaryResponse(date, 0, 0, 0);
        }
        return new DailyFlightSummaryResponse(
                date, entity.getTotalFlights(), entity.getDelayedFlights(), entity.getCancelledFlights());
    }
}
