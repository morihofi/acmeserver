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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {
    private Router router;
    private TestHandler handler;

    @BeforeEach
    void setup() {
        router = new Router();
        handler = new TestHandler();
    }

    private HandlerContext dummyContext(String path) {
        MockRequest req = new MockRequest().path(path);
        MockResponse resp = new MockResponse();
        return new HandlerContext(req, resp, router);
    }

    @Test
    @DisplayName("static handler retrieval")
    void testStaticHandler() throws Exception {
        router.addHandler(new Endpoint(HandlerType.GET, "/foo", handler));
        Handler got = router.getHandler("/foo", "GET", dummyContext("/foo"));
        assertNotNull(got);
        got.handle(dummyContext("/foo"));
        assertTrue(handler.called);
    }

    @Test
    @DisplayName("variable handler retrieval and path param")
    void testVariableHandler() throws Exception {
        router.addHandler(new Endpoint(HandlerType.GET, "/user/{id}", handler));
        Handler got = router.getHandler("/user/42", "GET", dummyContext("/user/42"));
        assertNotNull(got);
        assertEquals("42", router.getPathParam("/user/42", "id"));
    }

    @Test
    @DisplayName("before and after handlers")
    void testBeforeAfterHandlers() throws Exception {
        List<String> calls = new ArrayList<>();
        router.getPipeline().before("/api/*", c -> calls.add("before"));
        router.getPipeline().after("/api/*", c -> calls.add("after"));
        router.addHandler(new Endpoint(HandlerType.GET, "/api/data", handler));
        HandlerContext ctx = dummyContext("/api/data");
        Handler got = router.getHandler("/api/data", "GET", ctx);
        assertNotNull(got);
        got.handle(ctx);
        assertEquals(List.of("before", "after"), calls);
    }

    @Test
    @DisplayName("isAnyHandlerRegisteredForPath works")
    void testIsAnyHandlerRegisteredForPath() {
        router.addHandler(new Endpoint(HandlerType.GET, "/static", handler));
        router.addHandler(new Endpoint(HandlerType.GET, "/var/{id}", handler));
        assertTrue(router.isAnyHandlerRegisteredForPath("/static"));
        assertTrue(router.isAnyHandlerRegisteredForPath("/var/1"));
        assertFalse(router.isAnyHandlerRegisteredForPath("/none"));
    }

    static class TestHandler implements Handler {
        boolean called = false;

        @Override
        public void handle(HandlerContext context) {
            called = true;
        }
    }
}
