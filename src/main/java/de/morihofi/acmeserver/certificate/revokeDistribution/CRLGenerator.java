package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.provisioners.Provisioner;
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

/**
 * Manages the creation and updating of a Certificate Revocation List (CRL).
 * This class handles the generation of CRLs based on revoked certificates and maintains
 * a cache of the current CRL. It also schedules regular updates to the CRL.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP"})
public class CRLGenerator {

    private volatile byte[] currentCrlBytes = null;
    private volatile X509CRL currentCrl = null;
    private volatile LocalTime lastUpdate = null;


    /**
     * Instance for accessing the current provisioner
     */
    private final Provisioner provisioner;

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger(getClass());


    /**
     * Constructor for the CRL (Certificate Revocation List) class.
     * Initializes a new CRL instance with a given Provisioner.
     * It logs the initialization of the CRL generation task and starts
     * a scheduled task to update the CRL cache at a fixed rate.
     *
     * @param provisioner the Provisioner instance to be used for CRL operations
     */
    protected CRLGenerator(Provisioner provisioner) {
        this.provisioner = provisioner;
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
    public void updateCachedCRL(int updateMinutes) {
        try {
            // Get the list of revoked certificates from the database
            List<RevokedCertificate> revokedCertificates = Database.getRevokedCertificates(provisioner.getProvisionerName());
            // Generate a new CRL
            X509CRL crl = CertificateRevokationListGenerator.generateCRL(revokedCertificates, provisioner.getIntermediateCaCertificate(), provisioner.getIntermediateCaKeyPair().getPrivate(), updateMinutes);
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


    public Provisioner getProvisioner() {
        return provisioner;
    }
}
