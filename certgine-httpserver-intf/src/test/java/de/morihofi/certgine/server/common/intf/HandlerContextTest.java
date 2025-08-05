/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf;

import de.morihofi.certgine.server.common.intf.testing.MockRequest;
import de.morihofi.certgine.server.common.intf.testing.MockResponse;
import de.morihofi.certgine.types.httpserver.HandlerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandlerContextTest {
    private Router router;
    private MockRequest request;
    private MockResponse response;

    static class Data {int x;}

    @BeforeEach
    void setup() {
        router = new Router();
        request = new MockRequest();
        response = new MockResponse();
    }

    private HandlerContext ctx() {
        return new HandlerContext(request, response, router);
    }

    @Test
    @DisplayName("header and content type")
    void testHeader() {
        HandlerContext ctx = ctx();
        ctx.header("X-Test", "v");
        assertEquals("v", response.getHeader("X-Test"));
        ctx.contentType("text/plain");
        assertEquals("text/plain", response.getHeader("Content-Type"));
    }

    @Test
    @DisplayName("query and path params")
    void testParams() {
        router.addHandler(new Endpoint(HandlerType.GET, "/items/{id}", c -> {
        }));
        request.path("/items/5").queryParam("q", "1");
        HandlerContext ctx = ctx();
        assertEquals("1", ctx.queryParam("q"));
        assertEquals("5", ctx.pathParam("id"));
    }

    @Test
    @DisplayName("json serialization")
    void testJson() {
        HandlerContext ctx = ctx();
        ctx.json(Map.of("a", 1));
        assertEquals("application/json", response.getHeader("Content-Type"));
        assertTrue(response.getOutputStream().toString().contains("\"a\""));
    }

    @Test
    @DisplayName("body as class and bytes")
    void testBodyReading() throws IOException {
        request.body("{\"x\":5}");
        HandlerContext ctx = ctx();
        assertArrayEquals(request.getBodyBytes(), ctx.bodyAsBytes());
        Data d = ctx.bodyAsClass(Data.class);
        assertEquals(5, d.x);
    }

    @Test
    @DisplayName("method enum")
    void testMethod() {
        request.method("POST");
        HandlerContext ctx = ctx();
        assertEquals(HandlerType.POST, ctx.method());
    }
}

