package es.workfactory.occupancy;

import es.workfactory.occupancy.domain.Booking;
import es.workfactory.occupancy.domain.Guest;
import es.workfactory.occupancy.domain.PoliceReportLine;
import es.workfactory.occupancy.domain.Totals;
import java.time.LocalDate;
import java.util.List;

/**
 * The console walkthrough. It takes the first booking, runs the two calculations and
 * declares the police report.
 *
 * It is here so you can explore the API without a browser. This file is not evaluated.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        String today = LocalDate.now().toString();
        ApiClient api = new ApiClient();

        List<Booking> bookings = api.listBookings();
        if (bookings.isEmpty()) {
            System.out.println("The API returns no bookings: check API_BASE and API_TOKEN.");
            System.exit(1);
        }

        Booking booking = bookings.get(0);
        List<Guest> guests = api.guestsOf(booking.id(), 1);

        System.out.println("Booking " + booking.id() + " at " + booking.property());
        System.out.println("  capacity:  " + booking.capacity());
        System.out.println("  occupancy: " + Totals.occupancy(booking, guests, today));

        List<PoliceReportLine> lines = Totals.policeReportLines(guests, today);
        System.out.println("  report:    " + lines.size() + " lines");

        String batchId = api.declare(booking.id(), lines);
        System.out.println("Declared as " + batchId);
        System.out.println(api.getBatch(batchId).toPrettyString());
    }
}
