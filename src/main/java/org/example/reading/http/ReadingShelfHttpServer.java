package org.example.reading.http;

import com.sun.net.httpserver.HttpServer;
import org.example.reading.config.ReadingShelfConfig;
import org.example.reading.persistence.JsonReadingShelfPersistence;
import org.example.reading.service.ShelfService;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Loopback-only lifecycle owner for the reading shelf HTTP application.
 */
public final class ReadingShelfHttpServer implements AutoCloseable {
    private final ReadingShelfConfig config;
    private final JsonReadingShelfPersistence persistence;
    private final ShelfService service;
    private final HttpServer httpServer;

    public ReadingShelfHttpServer(ReadingShelfConfig config) throws IOException {
        this(Objects.requireNonNull(config, "config must not be null"),
                new JsonReadingShelfPersistence(config));
    }

    public ReadingShelfHttpServer(ReadingShelfConfig config,
                                  JsonReadingShelfPersistence persistence) throws IOException {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        try {
            this.service = new ShelfService(persistence);
        } catch (IOException exception) {
            throw new IOException("Unable to load reading shelf at "
                    + persistence.getDataPath() + ": " + exception.getMessage(), exception);
        }
        this.httpServer = createServer(config);
        this.httpServer.createContext("/", new ReadingShelfHandler(service));
    }

    private static HttpServer createServer(ReadingShelfConfig config) throws IOException {
        if (!ReadingShelfConfig.DEFAULT_HOST.equals(config.getHost())) {
            throw new IOException("Refusing to bind to non-loopback host: "
                    + config.getHost());
        }
        try {
            InetAddress loopback = InetAddress.getByName(ReadingShelfConfig.DEFAULT_HOST);
            InetSocketAddress address =
                    new InetSocketAddress(loopback, config.getPort());
            try {
                return HttpServer.create(address, 0);
            } catch (BindException exception) {
                throw new IOException("Unable to bind loopback server to "
                        + ReadingShelfConfig.DEFAULT_HOST + ":" + config.getPort()
                        + ": port is occupied or unavailable", exception);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid loopback server configuration", exception);
            }
        } catch (UnknownHostException exception) {
            throw new IOException("Unable to resolve loopback host "
                    + ReadingShelfConfig.DEFAULT_HOST, exception);
        }
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        stop(0);
    }

    public void stop(int delaySeconds) {
        if (delaySeconds < 0) {
            throw new IllegalArgumentException("Stop delay must not be negative");
        }
        httpServer.stop(delaySeconds);
    }

    @Override
    public void close() {
        stop(0);
    }

    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    public String getHost() {
        return config.getHost();
    }

    public InetSocketAddress getAddress() {
        return httpServer.getAddress();
    }

    public JsonReadingShelfPersistence getPersistence() {
        return persistence;
    }

    public ShelfService getService() {
        return service;
    }
}
