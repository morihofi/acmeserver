package de.morihofi.acmeserver.utils.event;

import de.morihofi.acmeserver.types.events.AbstractEvent;
import de.morihofi.acmeserver.types.events.ServerShutdownEvent;
import de.morihofi.acmeserver.types.events.ServerStartedEvent;
import de.morihofi.acmeserver.types.events.AcmeAccountCreatedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GlobalEventBusTest {

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

    @Test
    void testRegisterAndPublish() {
        DummySubscriber sub = new DummySubscriber();
        GlobalEventBus.register(sub);

        ServerStartedEvent start = new ServerStartedEvent(null);
        GlobalEventBus.publish(start);
        assertEquals(1, sub.count);
        assertEquals(start, sub.last);

        ServerShutdownEvent shut = new ServerShutdownEvent(null);
        GlobalEventBus.publish(shut);
        assertEquals(2, sub.count);
        assertEquals(shut, sub.last);

        // Event not handled should be ignored
        GlobalEventBus.publish(new AcmeAccountCreatedEvent(null));
        assertEquals(2, sub.count);

        GlobalEventBus.unregister(sub);
        GlobalEventBus.publish(new ServerStartedEvent(null));
        assertEquals(2, sub.count);
    }
}
