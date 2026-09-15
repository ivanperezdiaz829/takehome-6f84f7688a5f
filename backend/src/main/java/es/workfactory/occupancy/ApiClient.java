package es.workfactory.occupancy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.workfactory.occupancy.domain.Booking;
import es.workfactory.occupancy.domain.Guest;
import es.workfactory.occupancy.domain.PoliceReportLine;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The client for the two APIs. It is written, and it is written halfway.
 *
 * Read it before touching it: what is missing is not marked with a TODO.
 */
public final class ApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String base;
    private final String token;
    private final HttpClient http;

    public ApiClient() {
        this.base = Env.required("API_BASE");
        this.token = Env.required("API_TOKEN");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                // Explicit HTTP/1.1: by default the client tries to upgrade to HTTP/2 and there
                // are servers that do not answer that negotiation instead of rejecting it.
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private JsonNode request(String path, Object body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");

        if (body == null) {
            request.GET();
        } else {
            request.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
        }

        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " at " + path);
        }
        return MAPPER.readTree(response.body());
    }

    public List<Booking> listBookings() throws Exception {
        List<Booking> bookings = new ArrayList<>();
        for (JsonNode node : request("/bookings", null).path("items")) {
            bookings.add(booking(node));
        }
        return bookings;
    }

    public Booking getBooking(String id) throws Exception {
        return booking(request("/bookings/" + id, null));
    }

    /** Returns one page of guests. */
    public List<Guest> guestsOf(String bookingId, int page) throws Exception {
        List<Guest> guests = new ArrayList<>();
        JsonNode body = request("/bookings/" + bookingId + "/guests?page=" + page, null);
        for (JsonNode node : body.path("items")) {
            JsonNode document = node.path("documentNumber");
            guests.add(new Guest(
                    node.path("id").asText(),
                    node.path("firstName").asText(),
                    node.path("lastName").asText(),
                    node.path("birthDate").asText(),
                    node.path("nationality").asText(),
                    node.path("gender").asText(),
                    node.path("kinshipRelationship").asText(),
                    document.isNull() ? null : document.asText()));
        }
        return guests;
    }

    public String declare(String bookingId, List<PoliceReportLine> lines) throws Exception {
        return request("/ses/declarations", Map.of("bookingId", bookingId, "lines", lines))
                .path("batchId")
                .asText();
    }

    public JsonNode getBatch(String batchId) throws Exception {
        return request("/ses/declarations/" + batchId, null);
    }

    private static Booking booking(JsonNode node) {
        return new Booking(
                node.path("id").asText(),
                node.path("property").asText(),
                node.path("capacity").asInt(),
                node.path("checkInDate").asText(),
                node.path("checkOutDate").asText());
    }
}
