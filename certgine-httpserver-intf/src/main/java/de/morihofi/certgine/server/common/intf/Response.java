/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Represents an HTTP response that is sent back to the client.
 * This abstract class provides methods to manage response headers, status, and body content.
 */
public abstract class Response {
    private int status = 200; // OK

    /**
     * Retrieves the current HTTP status code of the response.
     *
     * @return The HTTP status code.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the HTTP status code for the response.
     *
     * @param status The HTTP status code to set.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Sets a header in the response with the given name and value.
     *
     * @param name  The name of the header.
     * @param value The value of the header.
     */
    public abstract void setHeader(String name, String value);

    /**
     * Retrieves the value of a specific header from the response.
     *
     * @param name The name of the header to retrieve.
     * @return The value of the specified header, or {@code null} if the header is not present.
     */
    public abstract String getHeader(String name);

    /**
     * Sets the body of the response using a {@code String} value.
     * The string is converted to a byte array using UTF-8 encoding.
     *
     * @param body The body of the response as a {@code String}.
     */
    public void setBody(String body) {
        setBodyBytes(body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sets the body of the response using a byte array.
     *
     * @param data The body of the response as a byte array.
     */
    public abstract void setBodyBytes(byte[] data);

    /**
     * Retrieves all the headers set for the response.
     *
     * @return A map containing all response headers, where each key is a header name and the corresponding value is the header value.
     */
    public abstract Map<String, String> getHeaders();

    public abstract OutputStream getOutputStream() throws IOException;
}
