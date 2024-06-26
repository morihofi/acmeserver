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

package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.tools.ServerInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CRLScheduler {

    /**
     * Logger
     */
    private static final Logger LOG = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private static final int UPDATE_MINUTES = 5;

    private static final Map<String, CRLGenerator> crlMap = Collections.synchronizedMap(new HashMap<>());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static CRLGenerator getCrlGeneratorForProvisioner(String provisionerName) {
        if (!crlMap.containsKey(provisionerName)) {
            throw new IllegalArgumentException(provisionerName + " has not an registered CRL generator");
        }
        return crlMap.get(provisionerName);
    }

    public static void startScheduler() {
        LOG.info("Initialized CRL Generation Scheduler");
        // Start the scheduled task to update the CRL every 5 minutes
        scheduler.scheduleAtFixedRate(CRLScheduler::schedule, 0, UPDATE_MINUTES, TimeUnit.MINUTES);
    }

    private static void schedule() {
        LOG.info("CRL Generation Scheduler is running");

        for (CRLGenerator crlGenerator : crlMap.values()) {
            LOG.info("Generating CRL for {} provisioner", crlGenerator.getProvisioner().getProvisionerName());

            crlGenerator.updateCachedCRL(UPDATE_MINUTES);
        }

        LOG.info("CRL Generation Scheduler finished execution");
    }

    public static void addProvisionerToScheduler(Provisioner provisioner, ServerInstance serverInstance) {
        LOG.info("{} provisioner has been added for CRL generation scheduling", provisioner.getProvisionerName());
        crlMap.put(provisioner.getProvisionerName(), new CRLGenerator(provisioner, serverInstance));
    }

    /**
     * Shuts down the executor service. Should be called when the CRL instance is no longer needed.
     */
    public static void shutdown() {
        LOG.info("CRL Scheduler is shutting down");
        scheduler.shutdown();
        crlMap.clear();
    }
}
