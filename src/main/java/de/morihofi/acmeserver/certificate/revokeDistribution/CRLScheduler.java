package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CRLScheduler {

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(MethodHandles.lookup().getClass());
    private static final int UPDATE_MINUTES = 5;


    private static final Map<String, CRLGenerator> crlMap = Collections.synchronizedMap(new HashMap<>());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static CRLGenerator getCrlGeneratorForProvisioner(String provisionerName) {
        if(!crlMap.containsKey(provisionerName)){
            throw new IllegalArgumentException(provisionerName + " has not an registered CRL generator");
        }
        return crlMap.get(provisionerName);
    }


    public static void startScheduler(){
        log.info("Initialized CRL Generation Scheduler");
        // Start the scheduled task to update the CRL every 5 minutes
        scheduler.scheduleAtFixedRate(CRLScheduler::schedule, 0, UPDATE_MINUTES, TimeUnit.MINUTES);
    }

    private static void schedule() {
        log.info("CRL Generation Scheduler is running");

        for (CRLGenerator crlGenerator : crlMap.values()){
            log.info("Generating CRL for {} provisioner", crlGenerator.getProvisioner().getProvisionerName());

            crlGenerator.updateCachedCRL(UPDATE_MINUTES);
        }

        log.info("CRL Generation Scheduler finished execution");
    }

    public static void addProvisionerToScheduler(Provisioner provisioner){
        log.info("{} provisioner has been added for CRL generation scheduling", provisioner.getProvisionerName());
        crlMap.put(provisioner.getProvisionerName(), new CRLGenerator(provisioner));
    }

    /**
     * Shuts down the executor service.
     * Should be called when the CRL instance is no longer needed.
     */
    public static void shutdown() {
        log.info("CRL Scheduler is shutting down");
        scheduler.shutdown();
        crlMap.clear();
    }

}
