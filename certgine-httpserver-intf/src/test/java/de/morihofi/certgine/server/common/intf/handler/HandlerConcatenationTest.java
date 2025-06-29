/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf.handler;

import de.morihofi.certgine.server.common.intf.Handler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HandlerConcatenationTest {
    @Test
    void handlersInvokedInOrder() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Handler h1 = ctx -> counter.incrementAndGet();
        Handler h2 = ctx -> counter.addAndGet(2);
        HandlerConcatenation concat = new HandlerConcatenation(List.of(h1,h2));
        concat.handle(null);
        assertEquals(3, counter.get());
    }
}
