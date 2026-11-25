package org.example.reading.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.example.reading.persistence.JsonReadingShelfPersistence;
import org.example.reading.service.ShelfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserContractIntegrationTest {
    @TempDir
    Path tempDir;

    private HttpServer server;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(tempDir.resolve("reading-shelf.json"));
        ShelfService service = new ShelfService(persistence);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new ReadingShelfHandler(service, new ObjectMapper()));
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
    @DisplayName("FR-02, FR-07: root serves the browser inventory shell")
    void rootServesBrowserShell() throws Exception {
        HttpResponse<String> response = get("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith("text/html"));
        assertTrue(response.body().contains("id=\"create-book-form\""));
        assertTrue(response.body().contains("id=\"inventory\""));
        assertTrue(response.body().contains("name=\"title\""));
        assertTrue(response.body().contains("name=\"author\""));
        assertTrue(response.body().contains("name=\"status\""));
        assertTrue(response.body().contains("/web/styles.css"));
        assertTrue(response.body().contains("/web/app.js"));
    }

    @Test
    @DisplayName("FR-01, FR-03, FR-05: JavaScript contract provides create and status actions")
    void javaScriptContractProvidesActionsAndFeedback() throws Exception {
        HttpResponse<String> createScript = get("/web/app.js");

        assertEquals(200, createScript.statusCode());
        assertTrue(createScript.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/javascript"));
        assertTrue(createScript.body().contains("fetch(\"/api/books\""));
        assertTrue(createScript.body().contains("fetch(\"/api/books/status\""));
        assertTrue(createScript.body().contains("fieldErrors"));
        assertTrue(createScript.body().contains("textContent"));
        assertTrue(createScript.body().contains("PATCH"));
        assertFalse(createScript.body().contains("innerHTML"));
    }

    @Test
    @DisplayName("FR-02, NFR-03: CSS supplies responsive inventory and control layouts")
    void cssContractIsResponsive() throws Exception {
        HttpResponse<String> response = get("/web/styles.css");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith("text/css"));
        assertTrue(response.body().contains(".book-card"));
        assertTrue(response.body().contains("@media (max-width: 760px)"));
        assertTrue(response.body().contains("grid-template-columns"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
