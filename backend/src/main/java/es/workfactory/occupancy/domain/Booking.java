package es.workfactory.occupancy.domain;

public record Booking(
        String id,
        String property,
        int capacity,
        String checkInDate,
        String checkOutDate) {}
