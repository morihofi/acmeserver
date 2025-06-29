/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Represents an HTTP request that contains information about the client's request to the server.
 * This interface provides methods to access various parts of the request, such as the path, method, headers, body,
 * IP address, and query parameters.
 */
public interface Request {


    /**
     * Returns the underlying {@link HttpServletRequest} object associated with this request.
     * <p>
     * This allows access to low-level servlet API methods and attributes not exposed through this interface.
     *
     * @return the original {@code HttpServletRequest} object representing the client's request
     */
    HttpServletRequest getHttpServletRequest();

    /**
     * Retrieves the path of the HTTP request.
     *
     * @return The path of the request, typically representing the requested resource (e.g., "/api/data").
     */
    String getPath();

    /**
     * Retrieves the HTTP method of the request.
     *
     * @return The HTTP method of the request (e.g., "GET", "POST", "PUT", "DELETE").
     */
    String getMethod();

    /**
     * Retrieves the value of a specific header from the request.
     *
     * @param name The name of the header to retrieve.
     * @return The value of the specified header, or {@code null} if the header is not present.
     */
    String getHeader(String name);

    /**
     * Retrieves the body content of the HTTP request.
     *
     * @return The body of the request as a {@code String}.
     * @throws IOException If an input or output exception occurs while reading the body content.
     */
    String getBody() throws IOException;

    /**
     * Retrieves the IP address of the client that sent the request.
     *
     * @return The IP address of the client as a {@code String}.
     */
    String getIP();

    /**
     * Retrieves the value of a specific query parameter from the request.
     *
     * @param name The name of the query parameter to retrieve.
     * @return The value of the specified query parameter, or {@code null} if the parameter is not present.
     */
    String getQueryParam(String name);

    /**
     * Retrieves the body content of the HTTP request as Bytes
     *
     * @return The body of the request as a {@code String}.
     * @throws IOException If an input or output exception occurs while reading the body content.
     */
    byte[] getBodyBytes() throws IOException;
}
