package de.morihofi.certgine.server.common.intf;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AbstractStaticServletTest {

    private HttpServletResponse mockResponse(ByteArrayServletOutputStream out) throws IOException {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getOutputStream()).thenReturn(out);
        return resp;
    }

    @Test
    @DisplayName("throws when base path missing")
    void testBasePathValidation() {
        InvalidServlet servlet = new InvalidServlet();
        assertThrows(ServletException.class, servlet::init);
    }

    @Test
    @DisplayName("serves static files")
    void testFileServing() throws Exception {
        TestServlet servlet = new TestServlet();
        servlet.init();

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getPathInfo()).thenReturn(null);

        ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();
        HttpServletResponse resp = mockResponse(out);

        servlet.service(req, resp);

        assertTrue(out.getContent().contains("Hello from static"));
        verify(resp).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("returns 404 when file missing")
    void testNotFound() throws Exception {
        TestServlet servlet = new TestServlet();
        servlet.init();

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getPathInfo()).thenReturn("/missing.html");

        ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();
        HttpServletResponse resp = mockResponse(out);

        servlet.service(req, resp);

        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

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

    static class TestServlet extends AbstractStaticServlet {
        @Override
        protected String getBasePath() {
            return "/test-static";
        }
    }

    static class InvalidServlet extends AbstractStaticServlet {
        @Override
        protected String getBasePath() {
            return "/missing";
        }
    }
}
