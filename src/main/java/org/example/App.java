package org.example;

import org.example.reading.config.ReadingShelfConfig;
import org.example.reading.http.ReadingShelfHttpServer;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args) {
        try {
            start().start();
            return 0;
        } catch (IOException | RuntimeException exception) {
            System.err.println("Failed to start reading shelf: " + safeMessage(exception));
            return 1;
        }
    }

    public static ReadingShelfHttpServer start() throws IOException {
        return start(new ReadingShelfConfig());
    }

    public static ReadingShelfHttpServer start(ReadingShelfConfig config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
        return new ReadingShelfHttpServer(config);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String singleLineMessage = message.replaceAll("[\\p{Cntrl}]+", " ").trim();
        return singleLineMessage.isEmpty()
                ? exception.getClass().getSimpleName()
                : singleLineMessage;
    }
}
