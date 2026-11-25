package org.example.reading.http;

import org.example.reading.config.ReadingShelfConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadingShelfHttpServerIntegrationTest {
    private ReadingShelfHttpServer server;

    @TempDir
    Path tempDirectory;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @DisplayName("FR-06, FR-07, NFR-01, NFR-04: server starts on an ephemeral loopback port")
    void startsAndServesInventoryFromTemporaryPath() throws Exception {
        int port = reservePort();
        Path dataPath = tempDirectory.resolve("reading-shelf.json");
        ReadingShelfConfig config = new ReadingShelfConfig(port, dataPath);

        server = new ReadingShelfHttpServer(config);
        server.start();

        InetSocketAddress address = server.getAddress();
        assertEquals("127.0.0.1", address.getAddress().getHostAddress());
        assertEquals(port, address.getPort());

        HttpResponse<String> response = get("http://127.0.0.1:" + port + "/api/books");
        assertEquals(200, response.statusCode());
        JsonNode body = new ObjectMapper().readTree(response.body());
        assertEquals(0, body.required("books").size());
    }

    @Test
    @DisplayName("NFR-02: malformed runtime JSON prevents startup and is not overwritten")
    void malformedJsonPreventsStartup(@TempDir Path temp) throws Exception {
        int port = reservePort();
        Path dataPath = temp.resolve("malformed.json");
        Files.writeString(dataPath, "{ definitely not json");

        ReadingShelfConfig config = new ReadingShelfConfig(port, dataPath);
        IOException failure = assertThrows(IOException.class,
                () -> new ReadingShelfHttpServer(config));

        assertTrue(failure.getMessage().contains("Unable to load reading shelf"));
        assertTrue(Files.readString(dataPath).contains("definitely"));
    }

    @Test
    @DisplayName("FR-07, NFR-01: occupied loopback port fails clearly without fallback")
    void occupiedPortFailsWithoutFallback(@TempDir Path temp) throws Exception {
        int port;
        try (ServerSocket occupied = new ServerSocket()) {
            occupied.bind(new InetSocketAddress("127.0.0.1", 0));
            port = occupied.getLocalPort();
            Path dataPath = temp.resolve("reading-shelf.json");
            ReadingShelfConfig config = new ReadingShelfConfig(port, dataPath);

            IOException failure = assertThrows(IOException.class,
                    () -> new ReadingShelfHttpServer(config));

            assertTrue(failure.getMessage().contains("Unable to bind loopback server"));
            assertTrue(failure.getMessage().contains("127.0.0.1:" + port));
            assertFalse(failure.getMessage().contains("fallback"));
        }
    }

    private static int reservePort() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("127.0.0.1", 0));
            return socket.getLocalPort();
        }
    }

    private static HttpResponse<String> get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString());
    }
}
