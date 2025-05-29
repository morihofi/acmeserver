/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.certificate.revokeDistribution;


import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CRLScheduler {

    //FIXME: Add trigger for update crl on provisioner removal/add

    private static final int UPDATE_MINUTES = 720; // 12 hours

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public static void startScheduler(@NonNull IServerInstance serverInstance) {
        log.info("Initialized CRL Generation Scheduler");
        // Start the scheduled task to update the CRL every 5 minutes
        scheduler.scheduleAtFixedRate(() -> schedule(serverInstance), 0, UPDATE_MINUTES, TimeUnit.MINUTES);
    }

    private static void schedule(@NonNull IServerInstance serverInstance) {
        log.info("CRL Generation Scheduler is running");

        for (AcmeProvisioner provisioner : AcmeProvisioner.getAllProvisioners(serverInstance)) {
            log.info("Generating CRL for {} provisioner", provisioner.getName());

            CrlStore.updateCachedCRL(UPDATE_MINUTES, provisioner, serverInstance);
        }

        log.info("CRL Scheduler finished execution");
    }

}
