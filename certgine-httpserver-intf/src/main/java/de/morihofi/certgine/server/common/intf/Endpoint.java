package de.morihofi.certgine.server.common.intf;


import de.morihofi.certgine.types.httpserver.HandlerType;

/**
 * Represents an HTTP endpoint, which defines the handler for a specific type of request at a particular path.
 * An endpoint typically includes the handler type, the path to be handled, and the handler logic itself.
 */
public class Endpoint {
    private final HandlerType type;
    private final String path;
    private final Handler handler;

    /**
     * Constructs a new Endpoint with the given type, path, and handler.
     *
     * @param type    The {@code HandlerType} of the endpoint, representing the type of HTTP method (e.g., GET, POST).
     * @param path    The path that this endpoint is associated with, typically representing a specific resource.
     * @param handler The {@code Handler} that contains the logic to handle requests to this endpoint.
     */
    public Endpoint(HandlerType type, String path, Handler handler) {
        this.type = type;
        this.path = path;
        this.handler = handler;
    }

    /**
     * Retrieves the type of the handler for this endpoint.
     *
     * @return The {@code HandlerType} representing the HTTP method type.
     */
    public HandlerType getType() {
        return type;
    }

    /**
     * Retrieves the path associated with this endpoint.
     *
     * @return The path as a {@code String}, representing the resource for this endpoint.
     */
    public String getPath() {
        return path;
    }

    /**
     * Retrieves the handler associated with this endpoint.
     *
     * @return The {@code Handler} that contains the logic to handle the request.
     */
    public Handler getHandler() {
        return handler;
    }
}
