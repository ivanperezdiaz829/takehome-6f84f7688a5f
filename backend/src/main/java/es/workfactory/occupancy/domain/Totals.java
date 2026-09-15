package es.workfactory.occupancy.domain;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The two calculations the exercise asks for.
 *
 * They are kept apart from the HTTP client on purpose: this knows nothing about where the
 * data comes from, which is why it can be tested without starting anything up.
 */
public final class Totals {

    private Totals() {}

    /**
     * YOUR JOB (1 of 2): how many slots the booking takes up.
     *
     * It is compared against booking.capacity(). Differing from the number of declared
     * people is not an error: they are two counts and both are correct.
     */
    public static int occupancy(Booking booking, List<Guest> guests, String today) {
        int counter = 0;
        for (Guest guest : guests) {
            if (Ages.on(guest.birthDate(), today) >= 2) counter++;
        }
        return counter;
    }

    /**
     * YOUR JOB (2 of 2): the lines that are declared to the authority.
     *
     * Mind the values: what the provider returns and what the authority accepts are not
     * the same catalogue, even when the letters sometimes match.
     */
    public static List<PoliceReportLine> policeReportLines(List<Guest> guests, String today) {
        return guests.stream().map(guest -> {
            String docNum = guest.documentNumber();
            String docType = "PAS";
            if (docNum != null) {
                String cleaned = docNum.trim().toUpperCase();
                if (cleaned.matches("^[0-9]{8}[A-Z]$")) docType = "NIF";
                else if (cleaned.matches("^[XYZ][0-9]{7}[A-Z]$")) docType = "NIE";
                else if (cleaned.length() == 9 && Character.isLetter(cleaned.charAt(8))) docType = "NIF";
                else docType = "PAS";
            }

            String rawGender = guest.gender() != null ? guest.gender().trim().toUpperCase() : "";
            String gender = "O";
            if (rawGender.contains("MALE") || rawGender.equals("H") || rawGender.equals("M")) {
                if (rawGender.equals("M") && !rawGender.contains("FEMALE")) gender = "H";
                else if (rawGender.contains("FEMALE") || rawGender.equals("F")) gender = "M";
                else if (rawGender.contains("MALE") && !rawGender.contains("FEMALE")) gender = "H";
                else gender = "H";
            } else if (rawGender.contains("FEMALE") || rawGender.equals("F")) gender = "M";

            String rawKinship = guest.kinshipRelationship() != null ? guest.kinshipRelationship().trim().toUpperCase() : "";
            String kinship = "OT";
            if (rawKinship.contains("SPOUSE") || rawKinship.contains("WIFE") || rawKinship.contains("HUSBAND") || rawKinship.contains("CY")) kinship = "CY";
            else if (rawKinship.contains("CHILD") || rawKinship.contains("SON") || rawKinship.contains("DAUGHTER") || rawKinship.contains("HJ")) kinship = "HJ";
            else if (rawKinship.contains("PRIMARY") || rawKinship.contains("SELF") || rawKinship.contains("TITULAR") || rawKinship.contains("TI")) kinship = "TI";
            else if (rawKinship.isEmpty() || rawKinship.equals("NONE")) kinship = "TI";

            return new PoliceReportLine(
                    guest.firstName(),
                    guest.lastName(),
                    guest.birthDate(),
                    guest.nationality(),
                    gender,
                    kinship,
                    docType,
                    docNum
            );
        }).collect(Collectors.toList());
    }
}
