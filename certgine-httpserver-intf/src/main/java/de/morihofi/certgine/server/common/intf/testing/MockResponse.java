package de.morihofi.certgine.server.common.intf.testing;

import de.morihofi.certgine.server.common.intf.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of {@link Response} for tests.
 * <p>
 * Captures written headers and body so that assertions can be made without
 * requiring a real HTTP server.
 */
public class MockResponse extends Response {

    private final Map<String, String> headers = new HashMap<>();
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public void setBodyBytes(byte[] data) {
        try {
            body.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public OutputStream getOutputStream() {
        return body;
    }

    /**
     * Returns the response body as a UTF-8 string.
     *
     * @return body content
     */
    public String getBodyAsString() {
        return body.toString(StandardCharsets.UTF_8);
    }

    /**
     * Returns the response body as raw bytes.
     *
     * @return body bytes
     */
    public byte[] getBodyAsBytes() {
        return body.toByteArray();
    }
}

