package es.workfactory.occupancy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import es.workfactory.occupancy.http.Json;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;

/**
 * Your API, and the screen it feeds. The routing, the static page and the error envelope
 * are already here so you can spend your time on what the exercise is about.
 *
 * Five endpoints. Four of them answer 501 until you write them.
 */
public final class Server {

    private static final Pattern BOOKING = Pattern.compile("^/api/bookings/([^/]+)/(summary|declaration)$");

    private Server() {}

    /**
     * YOUR JOB (1 of 4): every booking, each one with the same shape summary returns.
     *
     *   { items: [ <a summary>, <a summary>, ... ] }
     *
     * It is what the index of the screen lists. One shape for the list and the detail, so
     * both are painted by the same code.
     */
    private static void listBookings(HttpExchange exchange) throws IOException {
        try {
            List<es.workfactory.occupancy.domain.Booking> bookings = API.listBookings();
            List<Map<String, Object>> items = new ArrayList<>();
            for (es.workfactory.occupancy.domain.Booking b : bookings) {
                items.add(buildSummary(b.id(), b));
            }
            Json.send(exchange, 200, Map.of("items", items));
        } catch (Exception e) {
            Json.fail(exchange, 500, "API_ERROR", e.getMessage());
        }
    }

    /**
     * YOUR JOB (2 of 4): the two totals for a booking, plus how its declaration went.
     *
     * The shape the screen expects:
     *
     *   { bookingId, property, capacity,
     *     occupancy,           // slots taken up
     *     travellersDeclared,  // lines that go to the police report
     *     declaration: { batchId, status, accepted, rejected, rejections: [] } }
     */
    private static void summary(HttpExchange exchange, String bookingId) throws IOException {
        try {
            es.workfactory.occupancy.domain.Booking booking = API.getBooking(bookingId);
            Map<String, Object> sum = buildSummary(bookingId, booking);
            Json.send(exchange, 200, sum);
        } catch (Exception e) {
            Json.fail(exchange, 500, "API_ERROR", e.getMessage());
        }
    }

    /** YOUR JOB (3 of 4): declare the report for this booking and answer with the batch. */
    private static void declareBooking(HttpExchange exchange, String bookingId) throws IOException {
        try {
            es.workfactory.occupancy.domain.Booking booking = API.getBooking(bookingId);
            List<es.workfactory.occupancy.domain.Guest> guests = fetchAllGuests(bookingId);
            List<es.workfactory.occupancy.domain.PoliceReportLine> lines = es.workfactory.occupancy.domain.Totals.policeReportLines(guests, booking.checkInDate());

            String batchId = API.declare(bookingId, lines);
            BATCHES.put(bookingId, batchId);

            Json.send(exchange, 200, Map.of("batchId", batchId));
        } catch (Exception e) {
            Json.fail(exchange, 500, "API_ERROR", e.getMessage());
        }
    }

    /** YOUR JOB (4 of 4): how the declaration of this booking ended up. */
    private static void declarationOf(HttpExchange exchange, String bookingId) throws IOException {
        try {
            String batchId = BATCHES.get(bookingId);
            if (batchId == null) {
                Json.fail(exchange, 404, "NOT_DECLARED", "No declaration for this booking yet");
                return;
            }
            JsonNode batchNode = API.getBatch(batchId);
            Json.send(exchange, 200, MAPPER.convertValue(batchNode, Map.class));
        } catch (Exception e) {
            Json.fail(exchange, 500, "API_ERROR", e.getMessage());
        }
    }

    /** FUNCIONES A USAR EN CADA SECCIÓN DE "YOUR JOB" **/
    private static final ApiClient API = new ApiClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, String> BATCHES = new java.util.concurrent.ConcurrentHashMap<>();

    private static Map<String, Object> buildSummary(String bookingId, es.workfactory.occupancy.domain.Booking booking) throws Exception {
        List<es.workfactory.occupancy.domain.Guest> guests = fetchAllGuests(bookingId);
        String today = booking.checkInDate();
        int occupancy = es.workfactory.occupancy.domain.Totals.occupancy(booking, guests, today);
        List<es.workfactory.occupancy.domain.PoliceReportLine> lines = es.workfactory.occupancy.domain.Totals.policeReportLines(guests, today);

        Map<String, Object> summary = new HashMap<>();
        summary.put("bookingId", booking.id());
        summary.put("property", booking.property());
        summary.put("capacity", booking.capacity());
        summary.put("occupancy", occupancy);
        summary.put("travellersDeclared", lines.size());

        String batchId = BATCHES.get(bookingId);
        if (batchId != null) {
            JsonNode batchNode = API.getBatch(batchId);
            summary.put("declaration", MAPPER.convertValue(batchNode, Map.class));
        } else {
            summary.put("declaration", null);
        }
        return summary;
    }

    private static List<es.workfactory.occupancy.domain.Guest> fetchAllGuests(String bookingId) throws Exception {
        List<es.workfactory.occupancy.domain.Guest> all = new ArrayList<>();
        int page = 1;
        while (true) {
            List<es.workfactory.occupancy.domain.Guest> pageGuests = API.guestsOf(bookingId, page);
            if (pageGuests.isEmpty()) break;
            all.addAll(pageGuests);
            page++;
        }
        return all;
    }

    public static HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", Server::route);
        server.start();
        return server;
    }

    private static void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (method.equals("GET") && path.equals("/health")) {
            Json.send(exchange, 200, java.util.Map.of("ok", true));
            return;
        }

        if (method.equals("GET") && (path.equals("/") || path.equals("/index.html"))) {
            try {
                Json.html(exchange, screen());
            } catch (IOException missing) {
                Json.fail(exchange, 500, "SCREEN_MISSING", missing.getMessage());
            }
            return;
        }

        if (path.equals("/api/bookings")) {
            if (!method.equals("GET")) {
                Json.fail(exchange, 405, "METHOD_NOT_ALLOWED", method + " is not allowed here.");
                return;
            }
            listBookings(exchange);
            return;
        }

        Pattern guestsPattern = Pattern.compile("^/api/bookings/([^/]+)/guests$");
        Matcher guestsMatcher = guestsPattern.matcher(path);
        if (guestsMatcher.matches() && method.equals("GET")) {
            String bookingId = guestsMatcher.group(1);
            try {
                List<es.workfactory.occupancy.domain.Guest> guests = API.guestsOf(bookingId, 1);
                Map<String, Object> response = new HashMap<>();
                response.put("items", guests);
                Json.send(exchange, 200, response);
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("items", new ArrayList<>());
                Json.send(exchange, 200, response);
            }
            return;
        }

        Matcher booking = BOOKING.matcher(path);
        if (booking.matches()) {
            String bookingId = booking.group(1);
            String resource = booking.group(2);
            try {
                if (resource.equals("summary") && method.equals("GET")) {
                    summary(exchange, bookingId);
                } else if (resource.equals("declaration") && method.equals("POST")) {
                    declareBooking(exchange, bookingId);
                } else if (resource.equals("declaration") && method.equals("GET")) {
                    declarationOf(exchange, bookingId);
                } else {
                    Json.fail(exchange, 405, "METHOD_NOT_ALLOWED", method + " is not allowed here.");
                }
            } catch (RuntimeException breakage) {
                Json.fail(exchange, 500, "UNEXPECTED", "Something broke inside.");
            }
            return;
        }

        Json.fail(exchange, 404, "NOT_FOUND", "That route does not exist.");
    }

    /**
     * The page the front module serves, read from disk and not from the classpath: the front is a
     * module of its own, next to this one, so what you edit there is served without rebuilding.
     *
     * It walks up the way the environment is read, so it does not matter where you launch this from.
     */
    private static String screen() throws IOException {
        Path directory = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && directory != null; depth++, directory = directory.getParent()) {
            Path page = directory.resolve("frontend").resolve("index.html");
            if (Files.isRegularFile(page)) return Files.readString(page, StandardCharsets.UTF_8);
        }
        throw new IOException("frontend/index.html is not where the server looks for it.");
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(Env.optional("PORT", "3000"));
        start(port);
        System.out.println("Listening on http://localhost:" + port);
    }
}
