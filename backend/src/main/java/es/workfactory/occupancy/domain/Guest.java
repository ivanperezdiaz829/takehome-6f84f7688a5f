package es.workfactory.occupancy.domain;

/** A guest, exactly as the provider API returns it. */
public record Guest(
        String id,
        String firstName,
        String lastName,
        String birthDate,
        String nationality,
        String gender,
        String kinshipRelationship,
        String documentNumber) {}
