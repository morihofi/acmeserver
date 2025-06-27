package de.morihofi.certgine.server.common.intf.wrapper;

import de.morihofi.certgine.server.common.intf.Request;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Liest den Request-Body einmal ein und hält ihn im Speicher,
 * damit er beliebig oft abgefragt werden kann.
 */
@RequiredArgsConstructor
public class HttpRequestWrapper implements Request {

    private final HttpServletRequest request;
    /** enthält den komplett eingelesenen Body */
    @Getter
    private final byte[] body;

    public HttpRequestWrapper(HttpServletRequest request) throws IOException {
        this.request = request;
        this.body    = request.getInputStream().readAllBytes();   // nur EINMAL lesen
    }

    // ------------------------------------------------------------------------
    // Request-Interface
    // ------------------------------------------------------------------------

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    @Override
    public String getPath() {
        return request.getRequestURI();
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public String getBody() {
        Charset cs = request.getCharacterEncoding() != null
                ? Charset.forName(request.getCharacterEncoding())
                : StandardCharsets.UTF_8;
        return new String(body, cs);
    }

    @Override
    public byte[] getBodyBytes() {
        return body.clone();   // copy without modifying original and without original reference
    }

    @Override
    public String getIP() {
        return request.getRemoteAddr();
    }

    @Override
    public String getQueryParam(String name) {
        return request.getParameter(name);
    }
}
