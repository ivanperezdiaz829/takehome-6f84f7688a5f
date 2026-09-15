package es.workfactory.occupancy.domain;

import java.util.List;

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
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * YOUR JOB (2 of 2): the lines that are declared to the authority.
     *
     * Mind the values: what the provider returns and what the authority accepts are not
     * the same catalogue, even when the letters sometimes match.
     */
    public static List<PoliceReportLine> policeReportLines(List<Guest> guests, String today) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
