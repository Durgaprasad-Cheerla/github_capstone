package org.example.reading.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.example.reading.persistence.JsonReadingShelfPersistence;
import org.example.reading.service.ShelfService;
import org.example.reading.shelf.Book;
import org.example.reading.shelf.ReadingStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReadingShelfHandlerTest {
    @TempDir
    Path tempDir;

    private HttpServer server;
    private HttpClient client;
    private int port;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        JsonReadingShelfPersistence persistence =
                mock(JsonReadingShelfPersistence.class);
        when(persistence.load()).thenReturn(new ArrayList<>());
        ShelfService service = new ShelfService(persistence);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new ReadingShelfHandler(service, mapper));
        server.start();
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("FR-02: GET /api/books returns the inventory")
    void getInventory() throws Exception {
        HttpResponse<String> response = get("/api/books");

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertTrue(body.has("books"));
        assertEquals(0, body.get("books").size());
    }

    @Test
    @DisplayName("FR-01, FR-05: POST /api/books validates fields with a stable envelope")
    void createValidation() throws Exception {
        HttpResponse<String> response = post("/api/books",
                "{\"title\":\" \",\"author\":\"\",\"status\":\"Unknown\"}");

        assertEquals(400, response.statusCode());
        JsonNode error = mapper.readTree(response.body()).get("error");
        assertEquals("VALIDATION_ERROR", error.get("code").textValue());
        assertTrue(error.get("fieldErrors").has("title"));
        assertTrue(error.get("fieldErrors").has("author"));
        assertTrue(error.get("fieldErrors").has("status"));
    }

    @Test
    @DisplayName("FR-01, FR-06: POST /api/books creates and persists a book")
    void createBook() throws Exception {
        JsonReadingShelfPersistence persistence =
                mock(JsonReadingShelfPersistence.class);
        when(persistence.load()).thenReturn(new ArrayList<>());
        ShelfService service = new ShelfService(persistence);
        server.stop(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new ReadingShelfHandler(service, mapper));
        server.start();
        port = server.getAddress().getPort();

        HttpResponse<String> response = post("/api/books",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"To Read\"}");

        assertEquals(201, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals("Dune", body.get("book").get("title").textValue());
        assertEquals(1, body.get("books").size());
    }

    @Test
    @DisplayName("FR-03: PATCH /api/books/status changes status")
    void changeStatus() throws Exception {
        JsonReadingShelfPersistence persistence =
                mock(JsonReadingShelfPersistence.class);
        when(persistence.load()).thenReturn(new ArrayList<>());
        ShelfService service = new ShelfService(persistence);
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);
        server.stop(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new ReadingShelfHandler(service, mapper));
        server.start();
        port = server.getAddress().getPort();

        HttpResponse<String> response = patch("/api/books/status",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"Reading\"}");

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals("Reading", body.get("book").get("status").textValue());
    }

    @Test
    @DisplayName("FR-03, FR-05: PATCH unknown identity returns BOOK_NOT_FOUND")
    void notFound() throws Exception {
        HttpResponse<String> response = patch("/api/books/status",
                "{\"title\":\"Missing\",\"author\":\"Nobody\",\"status\":\"Finished\"}");

        assertEquals(404, response.statusCode());
        JsonNode error = mapper.readTree(response.body()).get("error");
        assertEquals("BOOK_NOT_FOUND", error.get("code").textValue());
        assertTrue(error.get("fieldErrors").isObject());
    }

    @Test
    @DisplayName("FR-04: duplicate create returns 409 DUPLICATE_BOOK")
    void duplicate() throws Exception {
        JsonReadingShelfPersistence persistence =
                mock(JsonReadingShelfPersistence.class);
        when(persistence.load()).thenReturn(new ArrayList<>());
        ShelfService service = new ShelfService(persistence);
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);
        server.stop(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new ReadingShelfHandler(service, mapper));
        server.start();
        port = server.getAddress().getPort();

        HttpResponse<String> response = post("/api/books",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"Finished\"}");

        assertEquals(409, response.statusCode());
        JsonNode error = mapper.readTree(response.body()).get("error");
        assertEquals("DUPLICATE_BOOK", error.get("code").textValue());
        assertTrue(error.get("fieldErrors").has("title"));
        assertTrue(error.get("fieldErrors").has("author"));
    }

    @Test
    @DisplayName("FR-05, F-05: malformed JSON returns MALFORMED_JSON")
    void malformedJson() throws Exception {
        HttpResponse<String> response = post("/api/books", "{not-json");

        assertEquals(400, response.statusCode());
        JsonNode error = mapper.readTree(response.body()).get("error");
        assertEquals("MALFORMED_JSON", error.get("code").textValue());
        assertTrue(error.get("fieldErrors").isObject());
        assertEquals(0, error.get("fieldErrors").size());
    }

    @Test
    @DisplayName("NFR-02: persistence failure returns PERSISTENCE_SAVE_ERROR")
    void persistenceSaveError() throws Exception {
        JsonReadingShelfPersistence persistence =
                mock(JsonReadingShelfPersistence.class);
        when(persistence.load()).thenReturn(new ArrayList<>());
        Mockito.doThrow(new java.io.IOException("disk full"))
                .when(persistence).saveWithResult(anyList());
        ShelfService service = new ShelfService(persistence);
        server.stop(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new ReadingShelfHandler(service, mapper));
        server.start();
        port = server.getAddress().getPort();

        HttpResponse<String> response = post("/api/books",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"To Read\"}");

        assertEquals(500, response.statusCode());
        JsonNode error = mapper.readTree(response.body()).get("error");
        assertEquals("PERSISTENCE_SAVE_ERROR", error.get("code").textValue());
        assertTrue(error.get("fieldErrors").isObject());
    }

    @Test
    @DisplayName("FR-07: unknown static asset returns 404")
    void unknownAsset() throws Exception {
        HttpResponse<String> response = get("/web/missing.js");

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("FR-01, FR-05: title length 100 is accepted and 101 is rejected after trimming")
    void createTitleLengthBoundaries() throws Exception {
        HttpResponse<String> accepted = post("/api/books",
                bookJson("a".repeat(100), "Author", "To Read"));
        assertEquals(201, accepted.statusCode());

        HttpResponse<String> padded = post("/api/books",
                bookJson("  " + "b".repeat(100) + "  ", "Author", "To Read"));
        assertEquals(201, padded.statusCode());

        HttpResponse<String> rejected = post("/api/books",
                bookJson("c".repeat(101), "Author", "To Read"));
        assertEquals(400, rejected.statusCode());
        JsonNode error = mapper.readTree(rejected.body()).get("error");
        assertEquals("VALIDATION_ERROR", error.get("code").textValue());
        assertEquals("title must be between 1 and 100 characters",
                error.get("fieldErrors").get("title").textValue());
        assertEquals(1, error.get("fieldErrors").size());
    }

    @Test
    @DisplayName("FR-01, FR-05: author length 50 is accepted and 51 is rejected after trimming")
    void createAuthorLengthBoundaries() throws Exception {
        HttpResponse<String> accepted = post("/api/books",
                bookJson("Title A", "a".repeat(50), "To Read"));
        assertEquals(201, accepted.statusCode());

        HttpResponse<String> padded = post("/api/books",
                bookJson("Title B", "  " + "b".repeat(50) + "  ", "To Read"));
        assertEquals(201, padded.statusCode());

        HttpResponse<String> rejected = post("/api/books",
                bookJson("Title C", "c".repeat(51), "To Read"));
        assertEquals(400, rejected.statusCode());
        JsonNode error = mapper.readTree(rejected.body()).get("error");
        assertEquals("VALIDATION_ERROR", error.get("code").textValue());
        assertEquals("author must be between 1 and 50 characters",
                error.get("fieldErrors").get("author").textValue());
        assertEquals(1, error.get("fieldErrors").size());
    }

    @Test
    @DisplayName("FR-01, FR-05: empty, whitespace-only and missing title or author use the length messages")
    void createBlankAndMissingFieldsUseLengthMessages() throws Exception {
        HttpResponse<String> blank = post("/api/books",
                "{\"title\":\"   \",\"author\":\"\\t \",\"status\":\"To Read\"}");
        assertEquals(400, blank.statusCode());
        JsonNode blankErrors = mapper.readTree(blank.body()).get("error").get("fieldErrors");
        assertEquals("title must be between 1 and 100 characters",
                blankErrors.get("title").textValue());
        assertEquals("author must be between 1 and 50 characters",
                blankErrors.get("author").textValue());

        HttpResponse<String> missing = post("/api/books", "{\"status\":\"To Read\"}");
        assertEquals(400, missing.statusCode());
        JsonNode missingErrors = mapper.readTree(missing.body()).get("error").get("fieldErrors");
        assertEquals("title must be between 1 and 100 characters",
                missingErrors.get("title").textValue());
        assertEquals("author must be between 1 and 50 characters",
                missingErrors.get("author").textValue());
    }

    @Test
    @DisplayName("FR-01, FR-04: POST /api/books stores trimmed values and detects case-insensitive duplicates")
    void createStoresTrimmedValuesAndDetectsDuplicates() throws Exception {
        HttpResponse<String> created = post("/api/books",
                bookJson("  Dune  ", "  Frank Herbert ", "To Read"));
        assertEquals(201, created.statusCode());
        JsonNode book = mapper.readTree(created.body()).get("book");
        assertEquals("Dune", book.get("title").textValue());
        assertEquals("Frank Herbert", book.get("author").textValue());

        HttpResponse<String> duplicate = post("/api/books",
                bookJson(" DUNE", "frank herbert  ", "Finished"));
        assertEquals(409, duplicate.statusCode());
        JsonNode error = mapper.readTree(duplicate.body()).get("error");
        assertEquals("DUPLICATE_BOOK", error.get("code").textValue());
        assertTrue(error.get("fieldErrors").has("title"));
        assertTrue(error.get("fieldErrors").has("author"));
    }

    @Test
    @DisplayName("FR-03: PATCH finds a book ignoring case and whitespace and has no length cap")
    void changeStatusIgnoresCaseAndHasNoLengthCap() throws Exception {
        String longTitle = "L".repeat(101);
        String longAuthor = "A".repeat(51);
        JsonReadingShelfPersistence persistence =
                mock(JsonReadingShelfPersistence.class);
        when(persistence.load()).thenReturn(new ArrayList<>(List.of(
                new Book("  DUNE ", " frank herbert", ReadingStatus.TO_READ),
                new Book(longTitle, longAuthor, ReadingStatus.TO_READ))));
        restartServer(new ShelfService(persistence));

        HttpResponse<String> lookup = patch("/api/books/status",
                bookJson("dune", "FRANK HERBERT ", "Reading"));
        assertEquals(200, lookup.statusCode());
        JsonNode changed = mapper.readTree(lookup.body()).get("book");
        assertEquals("  DUNE ", changed.get("title").textValue());
        assertEquals("Reading", changed.get("status").textValue());

        HttpResponse<String> longBook = patch("/api/books/status",
                bookJson(longTitle, longAuthor, "Finished"));
        assertEquals(200, longBook.statusCode());
        assertEquals("Finished",
                mapper.readTree(longBook.body()).get("book").get("status").textValue());
    }

    @Test
    @DisplayName("FR-05: POST body over 1,048,576 bytes with a Content-Length returns 413 PAYLOAD_TOO_LARGE")
    void createOversizeBodyWithContentLengthReturns413() throws Exception {
        HttpResponse<String> response = post("/api/books",
                paddedBookJson(1_048_576 + 1));

        assertPayloadTooLarge(response);
    }

    @Test
    @DisplayName("FR-05: POST body over 1,048,576 bytes without a Content-Length returns 413 PAYLOAD_TOO_LARGE")
    void createOversizeBodyWithoutContentLengthReturns413() throws Exception {
        byte[] body = paddedBookJson(1_048_576 + 1).getBytes(StandardCharsets.UTF_8);

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/api/books"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.fromPublisher(
                                HttpRequest.BodyPublishers.ofByteArray(body)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertPayloadTooLarge(response);
    }

    @Test
    @DisplayName("FR-05: PATCH body over 1,048,576 bytes returns 413 PAYLOAD_TOO_LARGE")
    void changeStatusOversizeBodyReturns413() throws Exception {
        HttpResponse<String> response = patch("/api/books/status",
                paddedBookJson(1_048_576 + 1));

        assertPayloadTooLarge(response);
    }

    @Test
    @DisplayName("FR-05: POST body of exactly 1,048,576 bytes is accepted")
    void createBodyAtTheLimitIsAccepted() throws Exception {
        String body = paddedBookJson(1_048_576);
        assertEquals(1_048_576, body.getBytes(StandardCharsets.UTF_8).length);

        HttpResponse<String> response = post("/api/books", body);

        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("FR-05: empty request bodies stay 400 MALFORMED_JSON")
    void emptyBodyStaysMalformedJson() throws Exception {
        HttpResponse<String> create = post("/api/books", "");
        HttpResponse<String> status = patch("/api/books/status", "");

        for (HttpResponse<String> response : List.of(create, status)) {
            assertEquals(400, response.statusCode());
            assertEquals("MALFORMED_JSON", mapper.readTree(response.body())
                    .get("error").get("code").textValue());
        }
    }

    private void assertPayloadTooLarge(HttpResponse<String> response) throws Exception {
        assertEquals(413, response.statusCode());
        JsonNode error = mapper.readTree(response.body()).get("error");
        assertEquals("PAYLOAD_TOO_LARGE", error.get("code").textValue());
        assertEquals("Request body exceeds the 1 MB limit.", error.get("message").textValue());
        assertTrue(error.get("fieldErrors").isObject());
        assertFalse(error.get("fieldErrors").fields().hasNext());
    }

    private void restartServer(ShelfService service) throws Exception {
        server.stop(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new ReadingShelfHandler(service, mapper));
        server.start();
        port = server.getAddress().getPort();
    }

    private static String bookJson(String title, String author, String status) {
        return "{\"title\":\"" + title + "\",\"author\":\"" + author
                + "\",\"status\":\"" + status + "\"}";
    }

    /** A valid JSON book body padded with trailing spaces to exactly the requested byte size. */
    private static String paddedBookJson(int totalBytes) {
        String json = bookJson("Dune", "Frank Herbert", "To Read");
        return json + " ".repeat(totalBytes - json.length());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
