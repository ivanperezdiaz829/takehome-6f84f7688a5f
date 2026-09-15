package es.workfactory.occupancy.domain;

/** A police report line, in the format the authority expects. */
public record PoliceReportLine(
        String firstName,
        String lastName,
        String birthDate,
        String nationality,
        String sex,
        String kinship,
        String documentType,
        String documentNumber) {}
