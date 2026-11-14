package com.loancalculator.domain;

import java.util.List;

public class PaymentSchedule {
    private List<ScheduleEvent> events;

    public PaymentSchedule() {
    }

    public PaymentSchedule(List<ScheduleEvent> events) {
        this.events = events;
    }

    public List<ScheduleEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ScheduleEvent> events) {
        this.events = events;
    }
}
