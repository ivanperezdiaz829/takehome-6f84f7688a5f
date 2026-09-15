package es.workfactory.occupancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import es.workfactory.occupancy.domain.Ages;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * What has to be green before you hand in.
 *
 * It starts your server, recomputes the numbers on its own against the API and compares them
 * with what yours answers. It does not read your code: only what it returns.
 *
 * Twelve tests per booking, plus three that are not about any booking in particular. When
 * something does not add up it tells you what it expected and what it got. Why it does not
 * add up is your job.
 *
 * It is an IT, so `mvn test` runs your tests and only yours. Run it with: mvn verify
 */
class CheckIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE = fromEnvironment("API_BASE");
    private static final String TOKEN = fromEnvironment("API_TOKEN");
    private static final int PORT = Integer.parseInt(Env.optional("CHECK_PORT", "3999"));
    private static final String MINE = "http://localhost:" + PORT;
    private static final String TODAY = LocalDate.now().toString();
    private static final List<String> FIELDS_THE_SCREEN_PAINTS =
            List.of("bookingId", "property", "capacity", "occupancy", "travellersDeclared");

    /**
     * The marker the starting point paints while the two render functions are unwritten. It is the
     * only thing about the front that can be asserted without a DOM, and it holds both ways: write
     * them and it is gone, throw the page away for a framework and it is gone too.
     */
    private static final String UNWRITTEN_SCREEN = "PANTALLA_SIN_ESCRIBIR";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private record Answer(int status, JsonNode body, String text) {}

    private record Seen(
            JsonNode booking, List<JsonNode> guests, long occupancy,
            Answer summary, Answer declared, Answer declaration) {

        String id() {
            return booking.path("id").asText();
        }

        @Override
        public String toString() {
            return id() + " - " + booking.path("property").asText();
        }
    }

    /**
     * Env.required stops the whole program, and inside a forked test JVM that reads as a crash
     * instead of as the reason. Here it has to be a plain failure.
     */
    private static String fromEnvironment(String name) {
        String value = Env.optional(name, "");
        if (value.isBlank()) {
            throw new IllegalStateException(name + " is missing. Copy .env.example to .env and fill it in.");
        }
        return value;
    }

    private static HttpServer server;
    private static final List<Seen> EVERY_BOOKING = new ArrayList<>();
    private static Answer screen;
    private static Answer list;

    @BeforeAll
    static void capture() throws Exception {
        server = Server.start(PORT);

        JsonNode listed = upstream("/bookings").path("items");
        if (!listed.elements().hasNext()) {
            throw new IllegalStateException("The API returns no bookings: check API_BASE and API_TOKEN.");
        }

        for (JsonNode booking : listed) {
            String id = booking.path("id").asText();
            List<JsonNode> guests = everyGuest(id);
            long occupancy = guests.stream()
                    .filter(guest -> Ages.on(guest.path("birthDate").asText(), TODAY) >= 2)
                    .count();
            EVERY_BOOKING.add(new Seen(
                    booking, guests, occupancy,
                    askMine("/api/bookings/" + id + "/summary", "GET"),
                    askMine("/api/bookings/" + id + "/declaration", "POST"),
                    askMine("/api/bookings/" + id + "/declaration", "GET")));
        }

        screen = askMine("/", "GET");
        list = askMine("/api/bookings", "GET");
    }

    @AfterAll
    static void stop() {
        if (server != null) server.stop(0);
    }

    /** The source of every per-booking test. Resolved after @BeforeAll has captured. */
    static Stream<Seen> everyBooking() {
        return EVERY_BOOKING.stream();
    }

    private static JsonNode upstream(String path) throws Exception {
        if (path.startsWith("/api")) {
            path = path.substring(4);
        }

        for (int attempt = 1; ; attempt++) {
            HttpResponse<String> response;
            try {
                response = HTTP.send(
                        HttpRequest.newBuilder(URI.create(path.startsWith("http") ? path : BASE + path))
                                .header("Authorization", "Bearer " + TOKEN)
                                .header("Content-Type", "application/json")
                                .timeout(Duration.ofSeconds(30))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
            } catch (Exception unreachable) {
                throw new IllegalStateException("Cannot reach the API at " + BASE + ". Check API_BASE in your .env.");
            }
            if (response.statusCode() < 300) return MAPPER.readTree(response.body());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IllegalStateException("The API rejects your token. Check API_TOKEN in your .env.");
            }
            if (attempt == 3) {
                throw new IllegalStateException("The API answered " + response.statusCode() + " at " + path);
            }
            Thread.sleep(400L * attempt);
        }
    }

    private static Answer askMine(String path, String method) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(MINE + path)).timeout(Duration.ofSeconds(30));
        if (method.equals("POST")) {
            request.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            request.GET();
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        try {
            return new Answer(response.statusCode(), MAPPER.readTree(response.body()), response.body());
        } catch (Exception notJson) {
            return new Answer(response.statusCode(), MAPPER.createObjectNode(), response.body());
        }
    }

    private static List<JsonNode> everyGuest(String bookingId) throws Exception {
        List<JsonNode> all = new ArrayList<>();
        Set<String> walked = new LinkedHashSet<>();
        String path = "/bookings/" + bookingId + "/guests";
        while (path != null && walked.add(path)) {
            JsonNode page = upstream(path);
            page.path("items").forEach(all::add);
            JsonNode next = page.path("_links").path("next").path("href");
            path = next.isMissingNode() || next.isNull() ? null : next.asText();
        }
        return all;
    }

    private static JsonNode rowFor(String bookingId) {
        for (JsonNode row : list.body().path("items")) {
            if (row.path("bookingId").asText().equals(bookingId)) return row;
        }
        return null;
    }

    @Test
    @DisplayName("GET / serves the screen")
    void theScreenIsServed() {
        assertEquals(200, screen.status());
        assertTrue(screen.text().toLowerCase().contains("<html"), "GET / does not return a page");
    }

    @Test
    @DisplayName("the screen is yours now, not the one we handed you")
    void theScreenIsYoursNow() {
        assertFalse(
                screen.text().contains(UNWRITTEN_SCREEN),
                "the page still carries the placeholder: renderList and renderDetail are unwritten");
    }

    @Test
    @DisplayName("GET /api/bookings lists every booking")
    void theListCarriesEveryBooking() {
        assertEquals(200, list.status(), list.text());
        List<String> listed = new ArrayList<>();
        list.body().path("items").forEach(row -> listed.add(row.path("bookingId").asText()));
        List<String> wanted = EVERY_BOOKING.stream().map(Seen::id).sorted().toList();
        assertEquals(wanted, listed.stream().sorted().toList());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("GET /summary answers 200")
    void summaryAnswers200(Seen seen) {
        assertEquals(200, seen.summary().status(), seen.summary().text());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the summary carries the capacity of the booking")
    void theSummaryCarriesTheCapacityOfTheBooking(Seen seen) {
        assertEquals(seen.booking().path("capacity").asInt(), seen.summary().body().path("capacity").asInt(-1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("occupancy leaves out the guests under 2")
    void occupancyLeavesOutTheGuestsUnder2(Seen seen) {
        assertEquals(seen.occupancy(), seen.summary().body().path("occupancy").asLong(-1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the police report counts every guest, whatever their age")
    void thePoliceReportCountsEveryGuestWhateverTheirAge(Seen seen) {
        assertEquals(seen.guests().size(), seen.summary().body().path("travellersDeclared").asInt(-1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("POST /declaration is accepted")
    void postingTheDeclarationIsAccepted(Seen seen) {
        assertTrue(seen.declared().status() < 400,
                () -> "answered " + seen.declared().status() + ": " + seen.declared().text());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("GET /declaration answers 200")
    void gettingTheDeclarationAnswers200(Seen seen) {
        assertEquals(200, seen.declaration().status(), seen.declaration().text());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the batch does not stay pending")
    void theBatchDoesNotStayPending(Seen seen) {
        JsonNode status = seen.declaration().body().get("status");
        assertTrue(status != null && !status.isNull(), "the batch has no status");
        assertNotEquals("pending", status.asText());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the batch says how many lines went through and how many did not")
    void theBatchSaysHowManyLinesWentThroughAndHowManyDidNot(Seen seen) {
        assertTrue(seen.declaration().body().path("accepted").isNumber(), "it does not say how many were accepted");
        assertTrue(seen.declaration().body().path("rejected").isNumber(), "it does not say how many were rejected");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the authority rejects no line")
    void theAuthorityRejectsNoLine(Seen seen) {
        assertEquals(0, seen.declaration().body().path("rejected").asInt(-1),
                () -> seen.declaration().body().path("rejections").toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the authority accepts every guest")
    void theAuthorityAcceptsEveryGuest(Seen seen) {
        assertEquals(seen.guests().size(), seen.declaration().body().path("accepted").asInt(-1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the summary carries the five fields the screen paints")
    void theSummaryCarriesTheFiveFieldsTheScreenPaints(Seen seen) {
        List<String> missing = FIELDS_THE_SCREEN_PAINTS.stream()
                .filter(field -> !seen.summary().body().hasNonNull(field))
                .toList();
        assertEquals(List.of(), missing);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyBooking")
    @DisplayName("the row in the list says the same as the summary")
    void theRowInTheListSaysTheSameAsTheSummary(Seen seen) {
        JsonNode row = rowFor(seen.id());
        assertNotNull(row, "this booking is not in GET /api/bookings");
        assertEquals(seen.booking().path("capacity").asInt(), row.path("capacity").asInt(-1));
        assertEquals(seen.occupancy(), row.path("occupancy").asLong(-1));
        assertEquals(seen.guests().size(), row.path("travellersDeclared").asInt(-1));
    }
}
