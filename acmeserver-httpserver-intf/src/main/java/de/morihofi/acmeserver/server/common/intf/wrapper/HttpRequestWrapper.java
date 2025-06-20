package de.morihofi.acmeserver.server.common.intf.wrapper;

import de.morihofi.acmeserver.server.common.intf.Request;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class HttpRequestWrapper implements Request {
    private final HttpServletRequest request;

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
    public String getBody() throws IOException {
        return new String(request.getInputStream().readAllBytes());
    }

    @Override
    public String getIP() {
        return request.getRemoteAddr();
    }


    @Override
    public String getQueryParam(String name) {
        return request.getParameter(name);
    }

    @Override
    public byte[] getBodyBytes() throws IOException {
        return request.getInputStream().readAllBytes();
    }
}
