package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.tools.certificate.generator.CertificateRevokationListGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the creation and updating of a Certificate Revocation List (CRL).
 * This class handles the generation of CRLs based on revoked certificates and maintains
 * a cache of the current CRL. It also schedules regular updates to the CRL.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class CRL {

    private volatile byte[] currentCrlBytes = null;
    private volatile X509CRL currentCrl = null;
    private volatile LocalTime lastUpdate = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Instance for accessing the current provisioner
     */
    private final Provisioner provisioner;

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger(getClass());

    private static final int UPDATE_MINUTES = 5;

    /**
     * Constructor for the CRL (Certificate Revocation List) class.
     * Initializes a new CRL instance with a given Provisioner.
     * It logs the initialization of the CRL generation task and starts
     * a scheduled task to update the CRL cache at a fixed rate.
     *
     * @param provisioner the Provisioner instance to be used for CRL operations
     */
    public CRL(Provisioner provisioner) {

        this.provisioner = provisioner;

        log.info("Initialized CRL Generation Task");
        // Start the scheduled task to update the CRL every 5 minutes (must be same as in generateCRL() function)
        scheduler.scheduleAtFixedRate(this::updateCRLCache, 0, UPDATE_MINUTES, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(){

            @Override
            public void run() {
                this.setName("CRL Shutdown Thread");
                super.run();


                final String provisionerName = provisioner.getProvisionerName(); // Retrieve the name of the provisioner in advance to keep the code clean

                log.info("Initiating shutdown of CRL Generator for Provisioner '{}'.", provisionerName);

                try {
                    shutdown();
                    log.info("Shutdown of CRL Generator for Provisioner '{}' completed successfully.", provisionerName);
                } catch (Exception e) {
                    log.error("An error occurred during the shutdown of CRL Generator for Provisioner '{}': {}", provisionerName, e.getMessage(), e);
                }
            }
        });

    }


    /**
     * Converts a given X509CRL object to its byte array representation.
     * This method is useful for encoding the CRL for storage or transmission.
     *
     * @param crl the X509CRL object to be converted into a byte array
     * @return a byte array representing the encoded form of the provided CRL
     * @throws CRLException if there is an error in encoding the CRL
     */
    private static byte[] getCrlAsBytes(X509CRL crl) throws CRLException {
        // Return the CRL as a byte array
        return crl.getEncoded();
    }


    /**
     * Updates the cache of the Certificate Revocation List (CRL).
     * This method retrieves the list of revoked certificates from the database,
     * generates a new CRL based on the retrieved data, and updates the current CRL cache.
     * It also logs the time of the last update and handles any exceptions that occur during the process.
     */
    private void updateCRLCache() {
        try {
            // Get the list of revoked certificates from the database
            List<RevokedCertificate> revokedCertificates = Database.getRevokedCertificates(provisioner.getProvisionerName());
            // Generate a new CRL
            X509CRL crl = CertificateRevokationListGenerator.generateCRL(revokedCertificates, provisioner.getIntermediateCaCertificate(), provisioner.getIntermediateCaKeyPair().getPrivate(), UPDATE_MINUTES);
            // Update the current CRL cache
            currentCrlBytes = getCrlAsBytes(crl);
            currentCrl = crl;
            // Update the last update time
            lastUpdate = LocalTime.now();
        } catch (Exception e) {
            // Handle exceptions
            log.error("Unable to update CRL revokation list", e);
        }
    }

    /**
     * Retrieves the current CRL in byte array format.
     *
     * @return The current CRL as a byte array.
     */
    public byte[] getCurrentCrlBytes() {
        return currentCrlBytes;
    }

    /**
     * Retrieves the current X509CRL object.
     *
     * @return The current X509CRL.
     * @throws CRLException If retrieving the CRL fails.
     */
    public X509CRL getCurrentCrl() throws CRLException {
        // Return the CRL as a byte array
        return currentCrl;
    }

    /**
     * Retrieves the time of the last CRL update.
     *
     * @return The time of the last update as a {@link LocalTime}.
     */
    public LocalTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Shuts down the executor service.
     * Should be called when the CRL instance is no longer needed.
     */
    public void shutdown() {
        scheduler.shutdown();
    }



}
