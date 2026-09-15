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
            guest.firstName(),
            guest.lastName(),
            guest.birthDate(),
            guest.nationality(),
            guest.gender(),
            guest.kinshipRelationship(),
            null,
            guest.documentNumber());
        }).collect(Collectors.toList());
    }
}
