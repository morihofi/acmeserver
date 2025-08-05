/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf.handler.common;

import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.Router;
import de.morihofi.certgine.server.common.intf.testing.MockRequest;
import de.morihofi.certgine.server.common.intf.testing.MockResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnyCORSBeforeHandlerTest {

    @Test
    void addsCorsHeaders() {
        MockResponse resp = new MockResponse();
        MockRequest req = new MockRequest();
        HandlerContext ctx = new HandlerContext(req, resp, new Router());
        new AnyCORSBeforeHandler().handle(ctx);
        assertEquals("*", resp.getHeader("Access-Control-Allow-Origin"));
        assertNotNull(resp.getHeader("Access-Control-Allow-Headers"));
    }
}
