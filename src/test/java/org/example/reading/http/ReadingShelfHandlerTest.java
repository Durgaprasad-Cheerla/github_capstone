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
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
