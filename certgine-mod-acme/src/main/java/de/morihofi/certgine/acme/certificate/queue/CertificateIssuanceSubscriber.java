/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.certificate.queue;

import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.enums.AcmeOrderState;
import de.morihofi.certgine.acme.types.events.AcmeCertificateIssuanceRequestedEvent;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.types.events.ServerShutdownEvent;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Subscriber that issues certificates asynchronously when requested via the
 * {@link AcmeCertificateIssuanceRequestedEvent}.
 */
@Slf4j
@RequiredArgsConstructor
public class CertificateIssuanceSubscriber implements EventSubscriber {
    private final IServerInstance serverInstance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Publishes issuance requests for orders which still require a certificate on startup.
     */
    public void initialize() {
        List<AcmeOrder> waiting = AcmeOrder.getAllAcmeOrdersWithState(
                AcmeOrderState.NEED_A_CERTIFICATE, serverInstance);
        for (AcmeOrder o : waiting) {
            serverInstance.getEventBus().publish(new AcmeCertificateIssuanceRequestedEvent(o));
        }
    }

    /** Shutdown the executor. */
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return Arrays.asList(AcmeCertificateIssuanceRequestedEvent.class, ServerShutdownEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) {
        if (event instanceof AcmeCertificateIssuanceRequestedEvent req) {
            executor.submit(() -> issue(req.getOrder()));
        } else if (event instanceof ServerShutdownEvent) {
            shutdown();
        }
    }

    private void issue(AcmeOrder order) {
        try (Session session = serverInstance.getDatabaseSession()) {
            CertificateIssuer.generateCertificateForOrder(order, session, serverInstance);
        } catch (Exception ex) {
            log.error("Error generating certificate for order {}", order.getOrderId(), ex);
        }
    }
}
