package de.morihofi.certgine.clientinstallagent.builder;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class WinPeResourceLoaderTest {

    @Test
    void readConfigFromPeBufferReadsGzipConfig() throws IOException {
        String json = "{\"foo\":\"bar\"}";

        ByteArrayOutputStream gzipOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(gzipOut)) {
            gos.write(json.getBytes(StandardCharsets.UTF_8));
        }
        byte[] gzipBytes = gzipOut.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(gzipBytes);

        CRC32 crc = new CRC32();
        crc.update(gzipBytes);
        writeIntLE(out, (int) crc.getValue());
        writeIntLE(out, gzipBytes.length);
        out.write("CGJOCFGv1".getBytes(StandardCharsets.US_ASCII));

        ByteBuffer buffer = ByteBuffer.wrap(out.toByteArray());

        Optional<String> result = WinPeResourceLoader.readConfigFromPeBuffer(buffer);
        assertTrue(result.isPresent());
        assertEquals(json, result.get());
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }
}

