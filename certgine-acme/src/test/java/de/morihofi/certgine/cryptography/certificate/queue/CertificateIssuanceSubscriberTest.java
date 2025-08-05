/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.certificate.queue;

import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.enums.AcmeOrderState;
import de.morihofi.certgine.acme.types.events.AcmeCertificateIssuanceRequestedEvent;
import de.morihofi.certgine.types.events.EventBus;
import de.morihofi.certgine.types.intf.IServerInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CertificateIssuanceSubscriberTest {

    private static class RecordingExecutor extends AbstractExecutorService {
        private Runnable lastCommand;
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            this.lastCommand = command;
        }
    }

    @Test
    @DisplayName("initialize publishes events for waiting orders")
    void testInitializePublishesEvents() {
        EventBus bus = new EventBus();
        IServerInstance server = Mockito.mock(IServerInstance.class);
        Mockito.when(server.getEventBus()).thenReturn(bus);

        CertificateIssuanceSubscriber sub = new CertificateIssuanceSubscriber(server);

        List<AcmeCertificateIssuanceRequestedEvent> events = new ArrayList<>();
        bus.subscribe(AcmeCertificateIssuanceRequestedEvent.class, events::add);

        AcmeOrder o1 = new AcmeOrder();
        AcmeOrder o2 = new AcmeOrder();
        try (MockedStatic<AcmeOrder> mock = Mockito.mockStatic(AcmeOrder.class)) {
            mock.when(() -> AcmeOrder.getAllAcmeOrdersWithState(AcmeOrderState.NEED_A_CERTIFICATE, server))
                    .thenReturn(List.of(o1, o2));
            sub.initialize();
        }

        assertEquals(2, events.size());
        assertSame(o1, events.get(0).getOrder());
        assertSame(o2, events.get(1).getOrder());
    }

}

