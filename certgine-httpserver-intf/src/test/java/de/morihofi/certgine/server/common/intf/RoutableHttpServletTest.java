package de.morihofi.certgine.server.common.intf;

import de.morihofi.certgine.types.httpserver.HandlerType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RoutableHttpServletTest {

    static class ByteArrayServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        @Override
        public void write(int b) {
            bos.write(b);
        }
        @Override
        public boolean isReady() {
            return true;
        }
        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
        String getContent() {
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    static class EmptyServletInputStream extends ServletInputStream {
        @Override
        public int read() {
            return -1;
        }
        @Override
        public boolean isFinished() {
            return true;
        }
        @Override
        public boolean isReady() {
            return true;
        }
        @Override
        public void setReadListener(ReadListener readListener) {
        }
    }

    private HttpServletRequest mockRequest(String method, String uri) throws IOException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getInputStream()).thenReturn(new EmptyServletInputStream());
        return req;
    }

    private HttpServletResponse mockResponse(ByteArrayServletOutputStream out) throws IOException {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getOutputStream()).thenReturn(out);
        return resp;
    }

    @Test
    @DisplayName("resolves registered handler")
    void testHandlerResolution() throws Exception {
        RoutableHttpServlet servlet = new RoutableHttpServlet() {};
        servlet.getRouter().addHandler(new Endpoint(HandlerType.GET, "/test", ctx -> ctx.result("ok")));

        HttpServletRequest req = mockRequest("GET", "/test");
        ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();
        HttpServletResponse resp = mockResponse(out);

        servlet.service(req, resp);

        assertEquals("ok", out.getContent());
        verify(resp, never()).sendError(anyInt());
    }

    @Test
    @DisplayName("handles OPTIONS requests")
    void testOptionsRequest() throws Exception {
        RoutableHttpServlet servlet = new RoutableHttpServlet() {};
        servlet.getRouter().addHandler(new Endpoint(HandlerType.GET, "/opt", ctx -> ctx.result("ignored")));

        HttpServletRequest req = mockRequest("OPTIONS", "/opt");
        ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();
        HttpServletResponse resp = mockResponse(out);

        servlet.service(req, resp);

        assertEquals("", out.getContent());
        verify(resp, never()).sendError(anyInt());
    }

    @Test
    @DisplayName("delegates to exception handler")
    void testExceptionHandling() throws Exception {
        RoutableHttpServlet servlet = new RoutableHttpServlet() {};
        servlet.getRouter().addHandler(new Endpoint(HandlerType.GET, "/boom", ctx -> { throw new RuntimeException("boom"); }));

        HttpServletRequest req = mockRequest("GET", "/boom");
        ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();
        HttpServletResponse resp = mockResponse(out);

        servlet.service(req, resp);

        assertEquals("Internal Server Error, see logs for details", out.getContent());
        verify(resp, never()).sendError(anyInt());
    }
}
