package org.example.reading.persistence;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Narrow file operations used by JSON persistence.
 *
 * <p>The interface makes read, flush, replacement, and directory-sync failures
 * deterministic in tests while production uses Java NIO.</p>
 */
public interface ReadingShelfIo {
    boolean exists(Path path) throws IOException;

    byte[] readAllBytes(Path path) throws IOException;

    void createDirectories(Path directory) throws IOException;

    Path createTempFile(Path directory, String prefix, String suffix) throws IOException;

    void writeAndForce(Path file, byte[] contents) throws IOException;

    void atomicReplace(Path source, Path target) throws IOException;

    void syncDirectory(Path directory) throws IOException;

    void deleteIfExists(Path path) throws IOException;

    static ReadingShelfIo nio() {
        return new NioReadingShelfIo();
    }

    final class NioReadingShelfIo implements ReadingShelfIo {
        @Override
        public boolean exists(Path path) throws IOException {
            return Files.exists(path);
        }

        @Override
        public byte[] readAllBytes(Path path) throws IOException {
            return Files.readAllBytes(path);
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
            try (FileChannel channel = FileChannel.open(file,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(contents);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
        }

        @Override
        public void atomicReplace(Path source, Path target) throws IOException {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Atomic file replacement is unsupported", exception);
            }
        }

        @Override
        public void syncDirectory(Path directory) throws IOException {
            try (FileChannel channel = FileChannel.open(directory,
                    StandardOpenOption.READ)) {
                channel.force(true);
            } catch (AccessDeniedException exception) {
                throw new UnsupportedOperationException(
                        "Parent-directory synchronization is unsupported on this platform",
                        exception);
            }
        }

        @Override
        public void deleteIfExists(Path path) throws IOException {
            Files.deleteIfExists(path);
        }
    }
}
