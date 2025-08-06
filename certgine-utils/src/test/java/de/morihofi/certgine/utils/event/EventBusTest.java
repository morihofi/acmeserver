/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.event;

import de.morihofi.certgine.types.events.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

    @Test
    void testRegisterAndPublish() {
        EventBus bus = new EventBus();
        DummySubscriber sub = new DummySubscriber();
        bus.register(sub);

        ServerStartedEvent start = new ServerStartedEvent(null);
        bus.publish(start);
        assertEquals(1, sub.count);
        assertEquals(start, sub.last);

        ServerShutdownEvent shut = new ServerShutdownEvent(null);
        bus.publish(shut);
        assertEquals(2, sub.count);
        assertEquals(shut, sub.last);

        // Event not handled should be ignored
        bus.publish(new UnregisteredEvent());
        assertEquals(2, sub.count);

        bus.unregister(sub);
        bus.publish(new ServerStartedEvent(null));
        assertEquals(2, sub.count);
    }

    static class DummySubscriber implements EventSubscriber {
        int count;
        AbstractEvent last;

        @Override
        public List<Class<? extends AbstractEvent>> canHandle() {
            return List.of(ServerStartedEvent.class, ServerShutdownEvent.class);
        }

        @Override
        public void onEvent(AbstractEvent event) {
            count++;
            last = event;
        }
    }

    static class UnregisteredEvent extends AbstractEvent {
    }
}
