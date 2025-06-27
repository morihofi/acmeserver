package de.morihofi.certgine.server.common.intf.wrapper;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseWrapperTest {
    @Test
    void delegatesToServletResponse() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        class TestOutputStream extends ServletOutputStream {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener writeListener) {}
            @Override public void write(int b) throws IOException { out.write(b); }
        }
        ServletOutputStream sos = new TestOutputStream();
        HttpServletResponse servlet = Mockito.mock(HttpServletResponse.class);
        Mockito.when(servlet.getOutputStream()).thenReturn(sos);
        HttpResponseWrapper wrapper = new HttpResponseWrapper(servlet);
        wrapper.setHeader("X","v");
        assertEquals("v", wrapper.getHeader("X"));
        OutputStream os = wrapper.getOutputStream();
        os.write('a');
        wrapper.setBodyBytes("b".getBytes());
        assertArrayEquals("ab".getBytes(), out.toByteArray());
    }
}
