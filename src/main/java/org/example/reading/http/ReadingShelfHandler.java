package org.example.reading.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.reading.service.ShelfService;
import org.example.reading.shelf.Book;
import org.example.reading.shelf.ReadingStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP boundary for the reading shelf API and browser static assets.
 */
public final class ReadingShelfHandler implements HttpHandler {
    private static final int MAX_REQUEST_BYTES = 1024 * 1024;
    private static final int READ_CHUNK_BYTES = 8192;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_AUTHOR_LENGTH = 50;
    private static final String TITLE_LENGTH_MESSAGE =
            "title must be between 1 and 100 characters";
    private static final String AUTHOR_LENGTH_MESSAGE =
            "author must be between 1 and 50 characters";
    private static final String PAYLOAD_TOO_LARGE_MESSAGE =
            "Request body exceeds the 1 MB limit.";

    private final ShelfService service;
    private final ObjectMapper objectMapper;

    public ReadingShelfHandler(ShelfService service) {
        this(service, new ObjectMapper());
    }

    public ReadingShelfHandler(ShelfService service, ObjectMapper objectMapper) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("GET".equals(method) && "/".equals(path)) {
                serveStatic(exchange, "/web/index.html");
                return;
            }
            if ("GET".equals(method) && path.startsWith("/web/")) {
                serveStatic(exchange, path);
                return;
            }
            if ("GET".equals(method) && "/api/books".equals(path)) {
                handleGetBooks(exchange);
                return;
            }
            if ("POST".equals(method) && "/api/books".equals(path)) {
                handleCreate(exchange);
                return;
            }
            if ("PATCH".equals(method) && "/api/books/status".equals(path)) {
                handleStatusChange(exchange);
                return;
            }

            if (path.startsWith("/api/")) {
                sendError(exchange, 404, "INTERNAL_ERROR",
                        "The requested API route was not found", Map.of());
            } else {
                sendPlain(exchange, 404, "Not Found");
            }
        } catch (RuntimeException exception) {
            if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                sendError(exchange, 500, "INTERNAL_ERROR",
                        "An unexpected server error occurred", Map.of());
            }
        }
    }

    private void handleGetBooks(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("books", service.listBooks());
            sendJson(exchange, 200, response);
        } catch (IOException exception) {
            if (isFailClosed(exception)) {
                sendError(exchange, 500, "PERSISTENCE_ERROR",
                        "Persistence is in a fail-closed state", Map.of());
            } else {
                sendError(exchange, 500, "PERSISTENCE_LOAD_ERROR",
                        "Unable to load the reading shelf", Map.of());
            }
        } catch (RuntimeException exception) {
            sendError(exchange, 500, "INTERNAL_ERROR",
                    "An unexpected server error occurred", Map.of());
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        JsonNode body;
        try {
            body = readJsonBody(exchange);
        } catch (PayloadTooLargeException exception) {
            sendPayloadTooLarge(exchange);
            return;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            sendMalformedJson(exchange);
            return;
        }

        Map<String, String> fieldErrors = createFieldErrors(body);
        if (!fieldErrors.isEmpty()) {
            sendError(exchange, 400, "VALIDATION_ERROR",
                    "The submitted book contains invalid fields", fieldErrors);
            return;
        }

        try {
            String title = body.required("title").textValue();
            String author = body.required("author").textValue();
            ReadingStatus status = ReadingStatus.fromValue(
                    body.required("status").textValue());
            Book created = service.createBook(title, author, status);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("book", created);
            response.put("books", service.listBooks());
            if (service.getLastDurabilityWarning() != null) {
                response.put("durabilityWarning", "PLATFORM_LIMITED_DURABILITY");
            }
            sendJson(exchange, 201, response);
        } catch (ShelfService.ValidationException exception) {
            sendError(exchange, 400, "VALIDATION_ERROR",
                    "The submitted book contains invalid fields",
                    exception.getFieldErrors());
        } catch (ShelfService.DuplicateBookException exception) {
            Map<String, String> duplicateFieldErrors = new LinkedHashMap<>();
            duplicateFieldErrors.put("title", "A book with this title and author already exists");
            duplicateFieldErrors.put("author", "A book with this title and author already exists");
            sendError(exchange, 409, "DUPLICATE_BOOK",
                    exception.getMessage(), duplicateFieldErrors);
        } catch (IOException exception) {
            sendPersistenceSaveError(exchange, exception);
        } catch (IllegalArgumentException exception) {
            Map<String, String> statusFieldErrors = new LinkedHashMap<>();
            statusFieldErrors.put("status", "Unsupported reading status");
            sendError(exchange, 400, "VALIDATION_ERROR",
                    "The submitted book contains invalid fields", statusFieldErrors);
        } catch (RuntimeException exception) {
            sendError(exchange, 500, "INTERNAL_ERROR",
                    "An unexpected server error occurred", Map.of());
        }
    }

    private void handleStatusChange(HttpExchange exchange) throws IOException {
        JsonNode body;
        try {
            body = readJsonBody(exchange);
        } catch (PayloadTooLargeException exception) {
            sendPayloadTooLarge(exchange);
            return;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            sendMalformedJson(exchange);
            return;
        }

        Map<String, String> fieldErrors = statusFieldErrors(body);
        if (!fieldErrors.isEmpty()) {
            sendError(exchange, 400, "VALIDATION_ERROR",
                    "The submitted status change contains invalid fields", fieldErrors);
            return;
        }

        try {
            String title = body.required("title").textValue();
            String author = body.required("author").textValue();
            ReadingStatus status = ReadingStatus.fromValue(
                    body.required("status").textValue());
            Book changed = service.changeStatus(title, author, status);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("book", changed);
            if (service.getLastDurabilityWarning() != null) {
                response.put("durabilityWarning", "PLATFORM_LIMITED_DURABILITY");
            }
            sendJson(exchange, 200, response);
        } catch (ShelfService.ValidationException exception) {
            sendError(exchange, 400, "VALIDATION_ERROR",
                    "The submitted status change contains invalid fields",
                    exception.getFieldErrors());
        } catch (ShelfService.BookNotFoundException exception) {
            sendError(exchange, 404, "BOOK_NOT_FOUND",
                    exception.getMessage(), Map.of());
        } catch (IOException exception) {
            sendPersistenceSaveError(exchange, exception);
        } catch (IllegalArgumentException exception) {
            Map<String, String> statusFieldErrors = new LinkedHashMap<>();
            statusFieldErrors.put("status", "Unsupported reading status");
            sendError(exchange, 400, "VALIDATION_ERROR",
                    "The submitted status change contains invalid fields", statusFieldErrors);
        } catch (RuntimeException exception) {
            sendError(exchange, 500, "INTERNAL_ERROR",
                    "An unexpected server error occurred", Map.of());
        }
    }

    private Map<String, String> createFieldErrors(JsonNode body) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (body == null || !body.isObject()) {
            return errors;
        }

        if (!isTrimmedLengthValid(body.get("title"), MAX_TITLE_LENGTH)) {
            errors.put("title", TITLE_LENGTH_MESSAGE);
        }
        if (!isTrimmedLengthValid(body.get("author"), MAX_AUTHOR_LENGTH)) {
            errors.put("author", AUTHOR_LENGTH_MESSAGE);
        }
        JsonNode status = body.get("status");
        if (status == null || !status.isTextual()) {
            errors.put("status", "Reading status is required");
        } else {
            try {
                ReadingStatus.fromValue(status.textValue());
            } catch (IllegalArgumentException exception) {
                errors.put("status", "Unsupported reading status");
            }
        }
        return errors;
    }

    private static boolean isTrimmedLengthValid(JsonNode value, int maxLength) {
        if (value == null || !value.isTextual()) {
            return false;
        }
        int length = value.textValue().trim().length();
        return length >= 1 && length <= maxLength;
    }

    private Map<String, String> statusFieldErrors(JsonNode body) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (body == null || !body.isObject()) {
            return errors;
        }

        JsonNode title = body.get("title");
        if (title == null || !title.isTextual()
                || title.textValue().trim().isEmpty()) {
            errors.put("title", "Title must not be blank");
        }
        JsonNode author = body.get("author");
        if (author == null || !author.isTextual()
                || author.textValue().trim().isEmpty()) {
            errors.put("author", "Author must not be blank");
        }
        JsonNode status = body.get("status");
        if (status == null || !status.isTextual()) {
            errors.put("status", "Reading status is required");
        } else {
            try {
                ReadingStatus.fromValue(status.textValue());
            } catch (IllegalArgumentException exception) {
                errors.put("status", "Unsupported reading status");
            }
        }
        return errors;
    }

    private JsonNode readJsonBody(HttpExchange exchange)
            throws IOException, JsonProcessingException {
        byte[] bytes = readBoundedBody(exchange.getRequestBody());
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Request body is empty");
        }
        JsonNode body = objectMapper.readTree(bytes);
        if (body == null) {
            throw new IllegalArgumentException("Request body must be JSON");
        }
        return body;
    }

    /**
     * Reads at most MAX_REQUEST_BYTES. Does not use available() or Content-Length;
     * stops reading as soon as the cap is exceeded.
     */
    private byte[] readBoundedBody(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[READ_CHUNK_BYTES];
        int total = 0;
        int read;
        while ((read = input.read(chunk)) != -1) {
            total += read;
            if (total > MAX_REQUEST_BYTES) {
                throw new PayloadTooLargeException();
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private void sendPayloadTooLarge(HttpExchange exchange) throws IOException {
        sendError(exchange, 413, "PAYLOAD_TOO_LARGE",
                PAYLOAD_TOO_LARGE_MESSAGE, Map.of());
    }

    private void sendMalformedJson(HttpExchange exchange) throws IOException {
        sendError(exchange, 400, "MALFORMED_JSON",
                "The request body is not valid JSON", Map.of());
    }

    private void sendPersistenceSaveError(HttpExchange exchange, IOException exception)
            throws IOException {
        if (isFailClosed(exception)) {
            sendError(exchange, 500, "PERSISTENCE_ERROR",
                    "Persistence is in a fail-closed state", Map.of());
        } else {
            sendError(exchange, 500, "PERSISTENCE_SAVE_ERROR",
                    "Unable to save the reading shelf", Map.of());
        }
    }

    private boolean isFailClosed(IOException exception) {
        return exception.getMessage() != null
                && exception.getMessage().contains("PERSISTENCE_ERROR");
    }

    private void serveStatic(HttpExchange exchange, String resourcePath)
            throws IOException {
        if (!"/web/index.html".equals(resourcePath)
                && (resourcePath.contains("..") || !resourcePath.startsWith("/web/"))) {
            sendPlain(exchange, 404, "Not Found");
            return;
        }

        try (InputStream input = ReadingShelfHandler.class
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                sendPlain(exchange, 404, "Not Found");
                return;
            }
            byte[] bytes = input.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type",
                    contentType(resourcePath));
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

    private String contentType(String path) {
        if (path.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (path.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        return "text/html; charset=UTF-8";
    }

    private void sendJson(HttpExchange exchange, int status, Object body)
            throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type",
                "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendError(HttpExchange exchange, int status, String code,
                           String message, Map<String, String> fieldErrors)
            throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("fieldErrors", fieldErrors);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("error", error);
        sendJson(exchange, status, envelope);
    }

    private void sendPlain(HttpExchange exchange, int status, String message)
            throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type",
                "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * Signals an oversize request body. Deliberately NOT an IllegalArgumentException so
     * the malformed-JSON catch blocks do not swallow it.
     */
    private static final class PayloadTooLargeException extends RuntimeException {
        PayloadTooLargeException() {
            super("Request body exceeds the limit", null, false, false);
        }
    }
}
