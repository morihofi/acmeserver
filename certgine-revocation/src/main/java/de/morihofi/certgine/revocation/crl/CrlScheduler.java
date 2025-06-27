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

package de.morihofi.certgine.revocation.crl;


import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CrlScheduler {

    //FIXME: Add trigger for update crl on provisioner removal/add

    /** Update interval in minutes used for scheduled CRL generation. */
    public static final int UPDATE_MINUTES = 720; // 12 hours
    public static final String CRON_EXPRESSION = "0 */12 * * *";

    private final TimedScheduler scheduler;
    private final IServerInstance serverInstance;

    public CrlScheduler(@NonNull IServerInstance serverInstance, TimedScheduler scheduler) {
        this.serverInstance = serverInstance;
        this.scheduler = scheduler;
    }

    public void startScheduler() {
        log.info("Initialized CRL Generation Scheduler");
        scheduler.schedule(CRON_EXPRESSION, this::schedule);
    }

    private void schedule() {
        log.info("CRL Generation Scheduler is running");

        for (AcmeProvisioner provisioner : AcmeProvisioner.getAllProvisioners(serverInstance)) {
            log.info("Generating CRL for {} provisioner", provisioner.getName());

            CrlStore.updateCachedCRL(UPDATE_MINUTES, provisioner, serverInstance);
        }

        log.info("CRL Scheduler finished execution");
    }

}
