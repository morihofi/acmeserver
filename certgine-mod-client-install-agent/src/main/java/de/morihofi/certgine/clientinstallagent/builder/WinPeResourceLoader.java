package de.morihofi.certgine.clientinstallagent.builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.CRC32;

public class WinPeResourceLoader {

    private static final byte[] MAGIC = "CGJOCFGv1".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    /**
     * Load PE file from resources and append configuration + trailer
     */
    public static ByteBuffer loadAndPatchPe(String resourceName, byte[] configBytes) throws IOException {
        // Load PE stub from resources
        ByteBuffer peBuffer = loadPeFromResources(resourceName);

        // Create in-memory output stream to build modified PE
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Write original PE content
        outputStream.write(peBuffer.array());

        // Append configuration and trailer
        appendConfigAndTrailer(outputStream, configBytes);

        return ByteBuffer.wrap(outputStream.toByteArray());
    }

    /**
     * Load PE file from resources as ByteBuffer
     */
    private static ByteBuffer loadPeFromResources(String resourceName) throws IOException {
        ClassLoader classLoader = WinPeResourceLoader.class.getClassLoader();
        try (InputStream is = /* classLoader.getResourceAsStream(resourceName)*/ Files.newInputStream(Paths.get("/home/fuxle/IdeaProjects/certgine/certgine-mod-client-install-agent/native/target/x86_64-pc-windows-gnu/release/agentstub.exe"))) {
            if (is == null) {
                throw new IOException("PE resource not found: " + resourceName);
            }

            // Use efficient channel-based reading for better performance
            ReadableByteChannel channel = Channels.newChannel(is);
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // 1MB initial capacity

            while (channel.read(buffer) != -1) {
                if (buffer.remaining() == 0) {
                    // Double buffer size when full
                    ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }

            buffer.flip();
            return buffer;
        }
    }

    /**
     * Append configuration + CRC + LEN + MAGIC to the output stream
     */
    private static void appendConfigAndTrailer(ByteArrayOutputStream outputStream, byte[] configBytes) {
        // Calculate CRC32 of configuration
        CRC32 crc = new CRC32();
        crc.update(configBytes);
        int crc32 = (int) crc.getValue();

        // Write configuration bytes
        outputStream.write(configBytes, 0, configBytes.length);

        // Write CRC32 (little-endian)
        writeIntLE(outputStream, crc32);

        // Write length (little-endian)
        writeIntLE(outputStream, configBytes.length);

        // Write magic bytes
        outputStream.write(MAGIC, 0, MAGIC.length);
    }

    /**
     * Write integer in little-endian format to output stream
     */
    private static void writeIntLE(ByteArrayOutputStream outputStream, int value) {
        outputStream.write(value & 0xFF);
        outputStream.write((value >>> 8) & 0xFF);
        outputStream.write((value >>> 16) & 0xFF);
        outputStream.write((value >>> 24) & 0xFF);
    }

    /**
     * (Optional) Method to read configuration from a ByteBuffer containing a modified PE file
     */
    public static Optional<String> readConfigFromPeBuffer(ByteBuffer buffer) throws IOException {
        int length = buffer.capacity();
        long trailerMin = MAGIC.length + 4 + 4; // magic + len + crc

        if (length < trailerMin) {
            return Optional.empty();
        }

        // Check MAGIC at the end
        byte[] magic = new byte[MAGIC.length];
        buffer.position(length - MAGIC.length);
        buffer.get(magic);
        if (!java.util.Arrays.equals(magic, MAGIC)) {
            return Optional.empty();
        }

        // Read length before MAGIC
        buffer.position(length - MAGIC.length - 4);
        int len = readIntLE(buffer);

        // Read CRC before length
        buffer.position(length - MAGIC.length - 8);
        int crc32 = readIntLE(buffer);

        // Calculate JSON start position
        int jsonStart = length - MAGIC.length - 8 - len;
        if (jsonStart < 0) {
            throw new IOException("Content corrupted (negative start).");
        }

        // Read JSON bytes
        buffer.position(jsonStart);
        byte[] jsonBytes = new byte[len];
        buffer.get(jsonBytes);

        // Verify CRC
        CRC32 crc = new CRC32();
        crc.update(jsonBytes);
        int calculatedCrc = (int) crc.getValue();
        if (calculatedCrc != crc32) {
            throw new IOException("CRC incorrect");
        }

        return Optional.of(new String(jsonBytes, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Read integer in little-endian format from ByteBuffer
     */
    private static int readIntLE(ByteBuffer buffer) {
        int b0 = buffer.get() & 0xFF;
        int b1 = buffer.get() & 0xFF;
        int b2 = buffer.get() & 0xFF;
        int b3 = buffer.get() & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }
}
