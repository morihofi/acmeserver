/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpRequestWrapperTest {
    @Test
    void delegatesToServletRequest() throws IOException {
        byte[] body = "data".getBytes();
        HttpServletRequest servlet = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servlet.getRequestURI()).thenReturn("/p");
        Mockito.when(servlet.getMethod()).thenReturn("POST");
        Mockito.when(servlet.getHeader("X")).thenReturn("v");
        Mockito.when(servlet.getParameter("q")).thenReturn("1");
        Mockito.when(servlet.getRemoteAddr()).thenReturn("ip");
        class TestInputStream extends ServletInputStream {
            private final ByteArrayInputStream in = new ByteArrayInputStream(body);

            @Override
            public int read() {
                return in.read();
            }

            @Override
            public boolean isFinished() {
                return in.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        }
        Mockito.when(servlet.getInputStream()).thenAnswer(i -> new TestInputStream());
        HttpRequestWrapper wrapper = new HttpRequestWrapper(servlet);
        assertEquals("/p", wrapper.getPath());
        assertEquals("POST", wrapper.getMethod());
        assertEquals("v", wrapper.getHeader("X"));
        assertEquals("1", wrapper.getQueryParam("q"));
        assertEquals("ip", wrapper.getIP());
        assertEquals("data", wrapper.getBody());
        assertArrayEquals(body, wrapper.getBodyBytes());
    }
}
