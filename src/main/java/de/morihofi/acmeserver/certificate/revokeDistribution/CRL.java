package de.morihofi.acmeserver.certificate.revokeDistribution;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.revokeDistribution.objects.RevokedCertificate;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.tools.CertMisc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CRL {

    private volatile byte[] currentCrlBytes = null;
    private volatile X509CRL currentCrl = null;
    private volatile LocalTime lastUpdate = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final Provisioner provisioner;

    private final Logger log = LogManager.getLogger(getClass());

    private static final int UPDATE_MINUTES = 5;

    public CRL(Provisioner provisioner) {

        this.provisioner = provisioner;

        log.info("Initialized CRL Generation Task");
        // Start the scheduled task to update the CRL every 5 minutes (must be same as in generateCRL() function
        scheduler.scheduleAtFixedRate(this::updateCRLCache, 0, UPDATE_MINUTES, TimeUnit.MINUTES);
    }

    private static X509CRL generateCRL(List<RevokedCertificate> revokedCertificates,
                               X509Certificate caCert,
                               PrivateKey caPrivateKey) throws CertificateEncodingException, CRLException, OperatorCreationException {


        // Create the CRL Builder
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(
                new JcaX509CertificateHolder(caCert).getSubject(),
                new Date()
        );

        // Add an expiration date
        crlBuilder.setNextUpdate(new Date(System.currentTimeMillis() + UPDATE_MINUTES * 60 * 1000L)); //Update in 5 mins


        // Add the revoked serial numbers
        for (RevokedCertificate revokedCertificate : revokedCertificates) {
            crlBuilder.addCRLEntry(revokedCertificate.getSerialNumber(), revokedCertificate.getRevokationDate(), revokedCertificate.getRevokationReason());
        }

        // Sign the CRL with the CA's private key
        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(CertMisc.getSignatureAlgorithmBasedOnKeyType(caPrivateKey));
        X509CRLHolder crlHolder = crlBuilder.build(signerBuilder.build(caPrivateKey));

        // Convert the CRL to a Java CRL object
        JcaX509CRLConverter converter = new JcaX509CRLConverter();
        converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        return converter.getCRL(crlHolder);
    }

    private static byte[] getCrlAsBytes(X509CRL crl) throws CRLException {
        // Return the CRL as a byte array
        return crl.getEncoded();
    }



    private void updateCRLCache() {
        try {
            // Get the list of revoked certificates from the database
            List<RevokedCertificate> revokedCertificates = Database.getRevokedCertificates();
            // Generate a new CRL
            X509CRL crl = generateCRL(revokedCertificates, provisioner.getIntermediateCertificate(), provisioner.getIntermediateKeyPair().getPrivate());
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

    public byte[] getCurrentCrlBytes() {
        return currentCrlBytes;
    }

    public X509CRL getCurrentCrl() throws CRLException {
        // Return the CRL as a byte array
        return currentCrl;
    }

    public LocalTime getLastUpdate() {
        return lastUpdate;
    }

    // Make sure to shut down the executor service when it is no longer needed
    public void shutdown() {
        scheduler.shutdown();
    }



}
