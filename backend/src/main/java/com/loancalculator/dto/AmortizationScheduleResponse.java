package com.loancalculator.dto;

import java.util.List;

public record AmortizationScheduleResponse(List<ScheduleEntry> schedule, ScheduleSummary summary) {
    public AmortizationScheduleResponse {
        schedule = List.copyOf(schedule);
    }
}
