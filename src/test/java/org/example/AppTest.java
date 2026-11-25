package org.example;

import org.example.reading.config.ReadingShelfConfig;
import org.example.reading.http.ReadingShelfHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    @TempDir
    Path tempDir;

    private String previousPort;
    private String previousDataPath;

    @BeforeEach
    void rememberSystemProperties() {
        rememberProperties();
    }

    @AfterEach
    void restoreSystemProperties() {
        restoreProperty(ReadingShelfConfig.PORT_PROPERTY, previousPort);
        restoreProperty(ReadingShelfConfig.DATA_PATH_PROPERTY, previousDataPath);
    }

    @Test
    @DisplayName("FR-07, NFR-02: run returns failure and one safe line for runtime startup errors")
    void runReturnsFailureForRuntimeStartupError() {
        System.setProperty(ReadingShelfConfig.PORT_PROPERTY, "0");

        CapturedRun result = captureRun();

        assertEquals(1, result.exitCode());
        assertEquals("Failed to start reading shelf: Invalid reading.shelf.port property",
                result.stderr().trim());
        assertEquals(1, result.stderr().lines().count());
    }

    @Test
    @DisplayName("FR-07, NFR-02: run returns failure and one safe line for persistence startup errors")
    void runReturnsFailureForPersistenceStartupError() throws IOException {
        Path malformedData = tempDir.resolve("malformed.json");
        Files.writeString(malformedData, "{invalid json", StandardCharsets.UTF_8);
        System.setProperty(ReadingShelfConfig.DATA_PATH_PROPERTY, malformedData.toString());

        CapturedRun result = captureRun();

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().startsWith("Failed to start reading shelf: "));
        assertEquals(1, result.stderr().lines().count());
        assertFalse(result.stderr().contains("\tat "));
    }

    @Test
    @DisplayName("FR-07, NFR-02: main exits with status one on startup failure")
    void mainExitsWithFailureStatus() throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                .toString();
        Process process = new ProcessBuilder(javaExecutable,
                "-D" + ReadingShelfConfig.PORT_PROPERTY + "=0",
                "-cp", System.getProperty("java.class.path"),
                App.class.getName())
                .start();

        boolean exited = process.waitFor(15, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
        }
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(exited, "Application process did not exit after startup failure");
        assertEquals(1, process.exitValue());
        assertEquals(1, stderr.lines().count());
        assertTrue(stderr.startsWith("Failed to start reading shelf: "));
    }

    @Test
    @DisplayName("FR-07: server startup remains nonblocking and closeable")
    void serverStartupRemainsNonblocking() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        ReadingShelfConfig config =
                new ReadingShelfConfig(port, tempDir.resolve("runtime.json"));

        try (ReadingShelfHttpServer server = App.start(config)) {
            server.start();
            assertTrue(server.getPort() > 0);
        }
    }

    private CapturedRun captureRun() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        int exitCode;
        try (PrintStream capturedErr = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
            System.setErr(capturedErr);
            exitCode = App.run(new String[0]);
        } finally {
            System.setErr(originalErr);
        }
        return new CapturedRun(exitCode, stderr.toString(StandardCharsets.UTF_8));
    }

    private void rememberProperties() {
        previousPort = System.getProperty(ReadingShelfConfig.PORT_PROPERTY);
        previousDataPath = System.getProperty(ReadingShelfConfig.DATA_PATH_PROPERTY);
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private record CapturedRun(int exitCode, String stderr) {
    }
}
