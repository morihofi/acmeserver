/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf.handler.common;

import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.HttpStatusCode;
import de.morihofi.certgine.server.common.intf.Router;
import de.morihofi.certgine.server.common.intf.testing.MockRequest;
import de.morihofi.certgine.server.common.intf.testing.MockResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OptionsHandlerTest {

    @Test
    void optionsStatusDependsOnHandler() {
        Router router = new Router();
        MockResponse resp = new MockResponse();
        MockRequest req = new MockRequest().path("/path").method("OPTIONS");
        HandlerContext ctx = new HandlerContext(req, resp, router);
        OptionsHandler handler = new OptionsHandler();
        handler.handle(ctx);
        assertEquals(HttpStatusCode.NOT_FOUND.getCode(), resp.getStatus());
        router.addHandler(new de.morihofi.certgine.server.common.intf.Endpoint(de.morihofi.certgine.types.httpserver.HandlerType.GET, "/path", c -> {
        }));
        resp.setStatus(0);
        handler.handle(ctx);
        assertEquals(HttpStatusCode.NO_CONTENT.getCode(), resp.getStatus());
    }
}
