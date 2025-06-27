package de.morihofi.certgine.types.httpserver;

public enum HandlerType {
    /**
     * The HTTP GET method requests a representation of the specified resource.
     */
    GET,

    /**
     * The HTTP POST method sends data to the server. The type of the body of the request is indicated by the Content-Type header.
     */
    POST,

    /**
     * The HTTP PUT request method creates a new resource or replaces a representation
     * of the target resource with the request payload.
     * <p>
     * The difference between PUT and POST is that PUT is idempotent: calling it once
     * or several times successively has the same effect (that is no side effect), whereas
     * successive identical POST requests may have additional effects, akin to placing
     * an order several times.
     */
    PUT,

    /**
     * The HTTP PATCH request method applies partial modifications to a resource.
     * <p>
     * PATCH is somewhat analogous to the "update" concept found in CRUD (in general,
     * HTTP is different than CRUD, and the two should not be confused).
     */
    PATCH,

    /**
     * The HTTP DELETE request method deletes the specified resource.
     */
    DELETE,

    /**
     * The HTTP HEAD method requests the headers that would be returned if  the HEAD request's URL was instead requested with the HTTP GET method.
     */
    HEAD,

    /**
     * The HTTP TRACE method performs a message loop-back test along the path to the target resource, providing a useful debugging mechanism.
     */
    TRACE,

    /**
     * The HTTP CONNECT method starts two-way communications with the requested resource. It can be used to open a tunnel.
     */
    CONNECT,

    /**
     * The HTTP OPTIONS method requests permitted communication options for a given URL
     * or server. A client can specify a URL with this method, or an asterisk (*) to
     * refer to the entire server.
     */
    OPTIONS,

}
