package org.example.reading.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reading.config.ReadingShelfConfig;
import org.example.reading.persistence.JsonReadingShelfPersistence;
import org.example.reading.persistence.ReadingShelfIo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ReadingShelfIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private ReadingShelfHttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @DisplayName("FR-06, FR-07, NFR-01, NFR-04: ephemeral loopback server serves static and inventory")
    void startsEphemeralLoopbackServerWithTemporaryData(@TempDir Path temp) throws Exception {
        int port = reserveLoopbackPort();
        Path dataPath = temp.resolve("reading-shelf.json");
        Path legacyPath = Path.of("data/reading-shelf.json");
        byte[] legacyBefore = Files.exists(legacyPath)
                ? Files.readAllBytes(legacyPath)
                : null;
        server = new ReadingShelfHttpServer(new ReadingShelfConfig(port, dataPath),
                new JsonReadingShelfPersistence(dataPath, successfulNioIo()));
        server.start();

        assertEquals("127.0.0.1", server.getAddress().getAddress().getHostAddress());
        assertEquals(port, server.getPort());

        HttpResponse<String> root = get(server.getPort(), "/");
        assertEquals(200, root.statusCode());
        assertTrue(root.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertTrue(root.body().contains("id=\"inventory\""));

        HttpResponse<String> inventory = get(server.getPort(), "/api/books");
        assertEquals(200, inventory.statusCode());
        assertEquals(0, mapper.readTree(inventory.body()).required("books").size());
        if (legacyBefore == null) {
            assertFalse(Files.exists(legacyPath));
        } else {
            assertArrayEquals(legacyBefore, Files.readAllBytes(legacyPath));
        }
    }

    @Test
    @DisplayName("FR-01, FR-02, FR-03, FR-04, FR-05, FR-06: API verifies create, list, status, duplicate, and validation")
    void apiCreateListStatusDuplicateAndValidation(@TempDir Path temp) throws Exception {
        int port = reserveLoopbackPort();
        Path dataPath = temp.resolve("reading-shelf.json");
        server = new ReadingShelfHttpServer(new ReadingShelfConfig(port, dataPath),
                new JsonReadingShelfPersistence(dataPath, successfulNioIo()));
        server.start();

        HttpResponse<String> validation = post(port, "/api/books",
                "{\"title\":\" \",\"author\":\"\",\"status\":\"Unknown\"}");
        assertEquals(400, validation.statusCode());
        JsonNode validationError = mapper.readTree(validation.body()).required("error");
        assertEquals("VALIDATION_ERROR", validationError.required("code").textValue());
        assertTrue(validationError.required("fieldErrors").has("title"));
        assertTrue(validationError.required("fieldErrors").has("author"));
        assertTrue(validationError.required("fieldErrors").has("status"));

        HttpResponse<String> malformed = post(port, "/api/books", "{not-json");
        assertEquals(400, malformed.statusCode());
        JsonNode malformedError = mapper.readTree(malformed.body()).required("error");
        assertEquals("MALFORMED_JSON", malformedError.required("code").textValue());
        assertEquals(0, malformedError.required("fieldErrors").size());

        HttpResponse<String> created = post(port, "/api/books",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"To Read\"}");
        assertEquals(201, created.statusCode());
        JsonNode createdBody = mapper.readTree(created.body());
        assertEquals("Dune", createdBody.required("book").required("title").textValue());
        assertEquals(1, createdBody.required("books").size());
        assertTrue(Files.exists(dataPath));

        HttpResponse<String> inventory = get(port, "/api/books");
        assertEquals(200, inventory.statusCode());
        assertEquals("Dune",
                mapper.readTree(inventory.body()).required("books").get(0).required("title").textValue());

        HttpResponse<String> changed = patch(port, "/api/books/status",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"Reading\"}");
        assertEquals(200, changed.statusCode());
        assertEquals("Reading",
                mapper.readTree(changed.body()).required("book").required("status").textValue());

        HttpResponse<String> duplicate = post(port, "/api/books",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"Finished\"}");
        assertEquals(409, duplicate.statusCode());
        JsonNode duplicateError = mapper.readTree(duplicate.body()).required("error");
        assertEquals("DUPLICATE_BOOK", duplicateError.required("code").textValue());
        assertTrue(duplicateError.required("fieldErrors").has("title"));
        assertTrue(duplicateError.required("fieldErrors").has("author"));

        HttpResponse<String> missing = patch(port, "/api/books/status",
                "{\"title\":\"Missing\",\"author\":\"Nobody\",\"status\":\"Finished\"}");
        assertEquals(404, missing.statusCode());
        JsonNode missingError = mapper.readTree(missing.body()).required("error");
        assertEquals("BOOK_NOT_FOUND", missingError.required("code").textValue());
        assertTrue(missingError.required("fieldErrors").isObject());

        HttpResponse<String> unknownRoute = get(port, "/api/missing");
        assertEquals(404, unknownRoute.statusCode());
        assertEquals("INTERNAL_ERROR",
                mapper.readTree(unknownRoute.body()).required("error").required("code").textValue());
    }

    @Test
    @DisplayName("FR-06: persisted inventory remains available after server restart")
    void inventoryPersistsAcrossRestart(@TempDir Path temp) throws Exception {
        int port = reserveLoopbackPort();
        Path dataPath = temp.resolve("reading-shelf.json");
        server = new ReadingShelfHttpServer(new ReadingShelfConfig(port, dataPath),
                new JsonReadingShelfPersistence(dataPath, successfulNioIo()));
        server.start();

        HttpResponse<String> created = post(port, "/api/books",
                "{\"title\":\"The Left Hand of Darkness\",\"author\":\"Ursula K. Le Guin\",\"status\":\"To Read\"}");
        assertEquals(201, created.statusCode());

        server.stop();
        server = new ReadingShelfHttpServer(new ReadingShelfConfig(port, dataPath),
                new JsonReadingShelfPersistence(dataPath, successfulNioIo()));
        server.start();

        HttpResponse<String> inventory = get(port, "/api/books");
        assertEquals(200, inventory.statusCode());
        JsonNode books = mapper.readTree(inventory.body()).required("books");
        assertEquals(1, books.size());
        assertEquals("The Left Hand of Darkness", books.get(0).required("title").textValue());
    }

    @Test
    @DisplayName("NFR-02: deterministic persistence save failure is returned through the HTTP error contract")
    void persistenceSaveFailureUsesStandardEnvelope(@TempDir Path temp) throws Exception {
        int port = reserveLoopbackPort();
        Path dataPath = temp.resolve("reading-shelf.json");
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(dataPath, new ReadingShelfIo() {
                    @Override
                    public boolean exists(Path path) {
                        return false;
                    }

                    @Override
                    public byte[] readAllBytes(Path path) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void createDirectories(Path directory) throws IOException {
                        Files.createDirectories(directory);
                    }

                    @Override
                    public Path createTempFile(Path directory, String prefix, String suffix) throws IOException {
                        return Files.createTempFile(directory, prefix, suffix);
                    }

                    @Override
                    public void writeAndForce(Path file, byte[] contents) throws IOException {
                        throw new IOException("injected flush failure");
                    }

                    @Override
                    public void atomicReplace(Path source, Path target) throws IOException {
                        throw new IOException("injected replacement failure");
                    }

                    @Override
                    public void syncDirectory(Path directory) throws IOException {
                        throw new IOException("injected directory-sync failure");
                    }

                    @Override
                    public void deleteIfExists(Path path) throws IOException {
                        Files.deleteIfExists(path);
                    }
                });
        server = new ReadingShelfHttpServer(new ReadingShelfConfig(port, dataPath), persistence);
        server.start();

        HttpResponse<String> response = post(port, "/api/books",
                "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"status\":\"To Read\"}");
        assertEquals(500, response.statusCode());
        JsonNode error = mapper.readTree(response.body()).required("error");
        assertEquals("PERSISTENCE_SAVE_ERROR", error.required("code").textValue());
        assertEquals("Unable to save the reading shelf",
                error.required("message").textValue());
        assertTrue(error.required("fieldErrors").isObject());
        assertFalse(Files.exists(dataPath));
    }

    @Test
    @DisplayName("NFR-02: malformed and unreadable runtime JSON prevent startup")
    void malformedAndUnreadableJsonPreventStartup(@TempDir Path temp) throws Exception {
        int malformedPort = reserveLoopbackPort();
        Path malformedPath = temp.resolve("malformed.json");
        Files.writeString(malformedPath, "{ definitely not json");

        IOException malformed = assertThrows(IOException.class,
                () -> new ReadingShelfHttpServer(
                        new ReadingShelfConfig(malformedPort, malformedPath)));
        assertTrue(malformed.getMessage().contains("Unable to load reading shelf"));
        assertTrue(Files.readString(malformedPath).contains("definitely"));

        int unreadablePort = reserveLoopbackPort();
        Path unreadablePath = temp.resolve("unreadable.json");
        ReadingShelfIo unreadableIo = new ReadingShelfIo() {
            @Override
            public boolean exists(Path path) {
                return true;
            }

            @Override
            public byte[] readAllBytes(Path path) throws IOException {
                throw new IOException("injected unreadable file");
            }

            @Override
            public void createDirectories(Path directory) {
            }

            @Override
            public Path createTempFile(Path directory, String prefix, String suffix) {
                return directory.resolve(prefix + suffix);
            }

            @Override
            public void writeAndForce(Path file, byte[] contents) {
            }

            @Override
            public void atomicReplace(Path source, Path target) {
            }

            @Override
            public void syncDirectory(Path directory) {
            }

            @Override
            public void deleteIfExists(Path path) {
            }
        };
        JsonReadingShelfPersistence unreadablePersistence =
                new JsonReadingShelfPersistence(unreadablePath, unreadableIo);

        IOException unreadable = assertThrows(IOException.class,
                () -> new ReadingShelfHttpServer(
                        new ReadingShelfConfig(unreadablePort, unreadablePath),
                        unreadablePersistence));
        assertTrue(unreadable.getMessage().contains("Unable to load reading shelf"));
        assertTrue(unreadable.getMessage().contains("unreadable.json"));
    }

    @Test
    @DisplayName("FR-07, NFR-01: occupied loopback port fails clearly without fallback")
    void occupiedLoopbackPortFailsWithoutFallback(@TempDir Path temp) throws Exception {
        try (ServerSocket occupied = new ServerSocket()) {
            occupied.bind(new InetSocketAddress("127.0.0.1", 0));
            int port = occupied.getLocalPort();
            ReadingShelfConfig config =
                    new ReadingShelfConfig(port, temp.resolve("reading-shelf.json"));

            IOException failure = assertThrows(IOException.class,
                    () -> new ReadingShelfHttpServer(config));

            assertNotNull(failure.getMessage());
            assertTrue(failure.getMessage().contains("Unable to bind loopback server"));
            assertTrue(failure.getMessage().contains("127.0.0.1:" + port));
            assertFalse(failure.getMessage().contains("fallback"));
        }
    }

    private int reserveLoopbackPort() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("127.0.0.1", 0));
            return socket.getLocalPort();
        }
    }

    private static ReadingShelfIo successfulNioIo() {
        ReadingShelfIo nio = ReadingShelfIo.nio();
        return new ReadingShelfIo() {
            @Override
            public boolean exists(Path path) throws IOException {
                return nio.exists(path);
            }

            @Override
            public byte[] readAllBytes(Path path) throws IOException {
                return nio.readAllBytes(path);
            }

            @Override
            public void createDirectories(Path directory) throws IOException {
                nio.createDirectories(directory);
            }

            @Override
            public Path createTempFile(Path directory, String prefix, String suffix)
                    throws IOException {
                return nio.createTempFile(directory, prefix, suffix);
            }

            @Override
            public void writeAndForce(Path file, byte[] contents) throws IOException {
                nio.writeAndForce(file, contents);
            }

            @Override
            public void atomicReplace(Path source, Path target) throws IOException {
                nio.atomicReplace(source, target);
            }

            @Override
            public void syncDirectory(Path directory) {
                // HTTP integration tests isolate transport behavior from platform
                // directory-sync support; T-10 verifies deterministic sync failures.
            }

            @Override
            public void deleteIfExists(Path path) throws IOException {
                nio.deleteIfExists(path);
            }
        };
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(int port, String path, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(int port, String path, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
