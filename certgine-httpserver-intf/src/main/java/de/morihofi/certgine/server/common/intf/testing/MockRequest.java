package de.morihofi.certgine.server.common.intf.testing;

import de.morihofi.certgine.server.common.intf.Request;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple mutable implementation of {@link Request} for use in unit tests.
 * <p>
 * Values may be configured fluently and default to a basic GET request
 * originating from {@code 127.0.0.1} with an empty body.
 */
public class MockRequest implements Request {

    private String path = "/";
    private String method = "GET";
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private byte[] body = new byte[0];
    private String ip = "127.0.0.1";
    private HttpServletRequest httpServletRequest;

    /**
     * Sets the path for the request.
     *
     * @param path request path
     * @return this instance for chaining
     */
    public MockRequest path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Sets the HTTP method for the request.
     *
     * @param method HTTP method such as GET or POST
     * @return this instance for chaining
     */
    public MockRequest method(String method) {
        this.method = method;
        return this;
    }

    /**
     * Adds a header to the request.
     *
     * @param name  header name
     * @param value header value
     * @return this instance for chaining
     */
    public MockRequest header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Adds a query parameter to the request.
     *
     * @param name  parameter name
     * @param value parameter value
     * @return this instance for chaining
     */
    public MockRequest queryParam(String name, String value) {
        queryParams.put(name, value);
        return this;
    }

    /**
     * Sets the body for the request using a {@link String} encoded as UTF-8.
     *
     * @param body body content
     * @return this instance for chaining
     */
    public MockRequest body(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /**
     * Sets the body for the request.
     *
     * @param bodyBytes body content as byte array
     * @return this instance for chaining
     */
    public MockRequest bodyBytes(byte[] bodyBytes) {
        this.body = bodyBytes;
        return this;
    }

    /**
     * Sets the originating IP address.
     *
     * @param ip ip address
     * @return this instance for chaining
     */
    public MockRequest ip(String ip) {
        this.ip = ip;
        return this;
    }

    /**
     * Sets the underlying {@link HttpServletRequest}.
     *
     * @param request servlet request
     * @return this instance for chaining
     */
    public MockRequest servletRequest(HttpServletRequest request) {
        this.httpServletRequest = request;
        return this;
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public String getIP() {
        return ip;
    }

    @Override
    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    @Override
    public byte[] getBodyBytes() {
        return body;
    }
}

