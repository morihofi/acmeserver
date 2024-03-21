package de.morihofi.acmeserver.certificate.queue;

import de.morihofi.acmeserver.certificate.acme.api.Provisioner;
import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.database.AcmeOrderState;
import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.HibernateUtil;
import de.morihofi.acmeserver.database.objects.ACMEOrder;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import de.morihofi.acmeserver.tools.certificate.PemUtil;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.tools.certificate.dataExtractor.CsrDataUtil;
import de.morihofi.acmeserver.tools.certificate.generator.ServerCertificateGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

public class CertificateIssuer {

    /**
     * Logger
     */
    private static final Logger log = LogManager.getLogger(CertificateIssuer.class);
    private static Thread certificateQueueIssueThread = null;

    public static synchronized void startThread(CryptoStoreManager cryptoStoreManager) {
        log.info("Starting certificate issuing thread...");
        if (certificateQueueIssueThread == null) {
            certificateQueueIssueThread = new Thread(new CertificateIssuingTask(cryptoStoreManager), "Certificate Issuing Thread");
            certificateQueueIssueThread.setDaemon(false); // Continue running until explicitly stopped, so only exit when all certificates are issued
            certificateQueueIssueThread.start();
        } else {
            log.info("Certificate issuing thread is already running.");
        }
    }

    public static void shutdown() throws InterruptedException {
        if (!certificateQueueIssueThread.isInterrupted()) {
            log.info("Stopping {}", certificateQueueIssueThread.getName());
            certificateQueueIssueThread.interrupt();
            while (certificateQueueIssueThread.isAlive()) {
                log.info("Waiting for {} to exiting", certificateQueueIssueThread.getName());
                Thread.sleep(1000);
            }
            log.info("{} has been stopped", certificateQueueIssueThread.getName());
        }
    }

    public static void generateCertificateForOrder(ACMEOrder order, CryptoStoreManager cryptoStoreManager, Session session) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        String csr = order.getCertificateCSR();
        // Decode the CSR from the Request
        byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
        PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());

        List<Identifier> csrIdentifiers = CsrDataUtil.getCsrIdentifiersAndVerifyWithIdentifiers(csr, order.getOrderIdentifiers());
        Provisioner provisioner = cryptoStoreManager.getProvisionerForName(order.getAccount().getProvisioner());

                        /*
                            We just use the DNS Domain Names (Subject Alternative Name) and the public key of the CSR. We're not using the Basic Constrain etc.
                         */

        log.info("Creating Certificate for order \"{}\" with DNS Names {}", order.getOrderId(),
                String.join(", ", csrIdentifiers.stream()
                        .map(identifier -> identifier.getTypeAsEnumConstant().toString() + ":" + identifier.getValue())
                        .toList()
                )
        );

        X509Certificate acmeGeneratedCertificate = ServerCertificateGenerator.createServerCertificate(
                provisioner.getIntermediateCaKeyPair(),
                provisioner.getIntermediateCaCertificate(),
                pkPemObject.getContent(),
                csrIdentifiers.toArray(new Identifier[0]),
                order.getNotBefore(),
                order.getNotAfter(),
                provisioner
        );

        BigInteger serialNumber = acmeGeneratedCertificate.getSerialNumber();
        String pemCertificate = PemUtil.certificateToPEM(acmeGeneratedCertificate.getEncoded());

        Timestamp expiresAt = new Timestamp(acmeGeneratedCertificate.getNotAfter().getTime());
        Timestamp issuedAt = new Timestamp(acmeGeneratedCertificate.getNotBefore().getTime());


        Transaction transaction = session.beginTransaction();


        //Set certificate details
        order.setCertificateSerialNumber(serialNumber);
        order.setCertificatePem(pemCertificate);
        order.setExpires(expiresAt);
        order.setCertificateIssued(issuedAt);

        order.setOrderState(AcmeOrderState.IDLE); //Set it back to idle
        session.merge(order);


        transaction.commit();

        log.info("Stored certificate successful");


    }


    private static class CertificateIssuingTask implements Runnable {

        private CryptoStoreManager cryptoStoreManager;

        public CertificateIssuingTask(CryptoStoreManager cryptoStoreManager) {
            this.cryptoStoreManager = cryptoStoreManager;
        }

        @SuppressFBWarnings("REC_CATCH_EXCEPTION")
        @Override
        public void run() {
            log.info("Certificate issuing thread started!");

            while (!Thread.currentThread().isInterrupted()) {
                log.trace("Looking for certificates to be issued in the database");


                List<ACMEOrder> waitingOrders = Database.getAllACMEOrdersWithState(AcmeOrderState.NEED_A_CERTIFICATE);

                if (!waitingOrders.isEmpty()) {

                    try (Session session = Objects.requireNonNull(HibernateUtil.getSessionFactory()).openSession()) {

                        //CryptoStoreManager csm = CryptoStoreManager;

                        ACMEOrder order = waitingOrders.get(0);
                        generateCertificateForOrder(order, cryptoStoreManager, session);

                    } catch (Exception ex) {
                        log.error("Error generating and/or store certificate", ex);
                    }

                } else {

                    //Waiting for new CSRs and try in a few seconds again
                    try {

                        Thread.sleep(20 * 1000); //Sleep 20 seconds

                    } catch (InterruptedException e) {
                        log.warn("Thread sleep is interrupted");
                        Thread.currentThread().interrupt();
                    }

                }


            }
            log.info("Certificate issuing thread is stopping gracefully.");
        }


    }

}
