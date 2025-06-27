package de.morihofi.certgine.server.common.intf.wrapper;

import de.morihofi.certgine.server.common.intf.Response;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class HttpResponseWrapper extends Response {
    private final HttpServletResponse response;
    private final Map<String, String> headers = new HashMap<>();

    public HttpResponseWrapper(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
        headers.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public void setBodyBytes(byte[] data) {
        try {
            response.getOutputStream().write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}