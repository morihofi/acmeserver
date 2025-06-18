package de.morihofi.acmeserver.server.common.intf;

import com.google.gson.Gson;
import de.morihofi.acmeserver.types.httpserver.HandlerType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The HandlerContext class provides a context for handling HTTP requests and responses.
 * It encapsulates a Request, a Response, and a Router, and provides utility methods to simplify
 * working with HTTP headers, parameters, and response bodies.
 */
public record HandlerContext(Request request, Response response, Router router) {

    private static Gson gson = new Gson();;

    /**
     * Sets a response header.
     *
     * @param name  The name of the header.
     * @param value The value of the header.
     */
    public void header(String name, String value) {
        response.setHeader(name, value);
    }

    /**
     * Retrieves a request header.
     *
     * @param name The name of the header to retrieve.
     * @return The value of the specified header, or {@code null} if the header is not present.
     */
    public String header(String name) {
        return request.getHeader(name);
    }

    /**
     * Sets the Content-Type header of the response.
     *
     * @param contentType The value of the Content-Type header.
     */
    public void contentType(String contentType) {
        header("Content-Type", contentType);
    }

    /**
     * Retrieves the Content-Type header of the request.
     *
     * @return The value of the Content-Type header, or {@code null} if not present.
     */
    public String contentType() {
        return header("Content-Type");
    }

    /**
     * Retrieves the value of a query parameter from the request.
     *
     * @param paramName The name of the query parameter to retrieve.
     * @return The value of the specified query parameter, or {@code null} if the parameter is not present.
     */
    public String queryParam(String paramName) {
        return request.getQueryParam(paramName);
    }

    /**
     * Retrieves the value of a path parameter using the router.
     *
     * @param paramName The name of the path parameter to retrieve.
     * @return The value of the specified path parameter, or {@code null} if the parameter is not present.
     */
    public String pathParam(String paramName) {
        return router.getPathParam(path(), paramName);
    }

    /**
     * Retrieves the body of the request.
     *
     * @return The body of the request as a {@code String}.
     * @throws IOException If an input or output exception occurs while reading the body content.
     */
    public String body() throws IOException {
        return request.getBody();
    }

    /**
     * Sets the HTTP status code for the response.
     *
     * @param code The HTTP status code to set.
     */
    public void status(int code) {
        response.setStatus(code);
    }

    /**
     * Sets the HTTP status code for the response using an {@code HttpStatusCode} enum.
     *
     * @param code The {@code HttpStatusCode} to set.
     */
    public void status(HttpStatusCode code) {
        response.setStatus(code.getCode());
    }

    /**
     * Sets the response body to the given data as a {@code String}.
     *
     * @param data The response body data as a {@code String}.
     */
    public void result(String data) {
        result(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sets the response body to the given data as a byte array.
     *
     * @param data The response body data as a byte array.
     */
    public void result(byte[] data) {
        response.setBodyBytes(data);
    }

    /**
     * Sets an empty response body.
     */
    public void result() {
        result(""); // Empty body
    }

    /**
     * Retrieves the path of the HTTP request.
     *
     * @return The path of the request.
     */
    public String path() {
        return request.getPath();
    }

    /**
     * Retrieves all the response headers as a {@code Map}.
     *
     * @return A map containing all response headers, where each key is a header name and the corresponding value is the header value.
     */
    public Map<String, String> getResponseHeaders() {
        return response.getHeaders();
    }

    public void json(Object objectToBeSerialized) {
        String json = gson.toJson(objectToBeSerialized);
        contentType("application/json"); // Set Content-Type as JSON
        result(json); // Set the serialized JSON as the response body
    }

    public byte[] bodyAsBytes() throws IOException {
        return request.getBodyBytes();
    }

    public ByteBuffer bodyAsByteBuffer() throws IOException {
        return ByteBuffer.wrap(bodyAsBytes());
    }

    public <T> T bodyAsClass(Class<T> targetClazz) throws IOException {
        Gson gson = new Gson();
        String requestBody = body(); // Get the request body as a string
        return gson.fromJson(requestBody, targetClazz); // Deserialize into the specified class
    }

    public HandlerType method(){
       return HandlerType.valueOf(request.getMethod());
    }
}
