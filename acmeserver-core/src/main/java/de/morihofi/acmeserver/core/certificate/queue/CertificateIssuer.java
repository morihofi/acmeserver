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

package de.morihofi.acmeserver.core.certificate.queue;

import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.core.certificate.provisioners.Provisioner;
import de.morihofi.acmeserver.core.certificate.provisioners.ProvisionerManager;
import de.morihofi.acmeserver.core.database.AcmeOrderState;
import de.morihofi.acmeserver.core.database.objects.ACMEOrder;
import de.morihofi.acmeserver.core.tools.ServerInstance;
import de.morihofi.acmeserver.core.tools.base64.Base64Tools;
import de.morihofi.acmeserver.core.tools.certificate.PemUtil;
import de.morihofi.acmeserver.core.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.core.tools.certificate.dataExtractor.CsrDataUtil;
import de.morihofi.acmeserver.core.tools.certificate.generator.ServerCertificateGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class CertificateIssuer {

    private static Thread certificateQueueIssueThread = null;

    public static synchronized void startThread(ServerInstance serverInstance) {
        log.info("Starting certificate issuing thread...");
        if (certificateQueueIssueThread == null) {
            certificateQueueIssueThread = new Thread(new CertificateIssuingTask(serverInstance), "Certificate Issuing Thread");
            certificateQueueIssueThread.setDaemon(
                    false); // Continue running until explicitly stopped, so only exit when all certificates are issued
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

    public static void generateCertificateForOrder(ACMEOrder order, CryptoStoreManager cryptoStoreManager, Session session) throws
            IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            OperatorCreationException {
        String csr = order.getCertificateCSR();
        // Decode the CSR from the Request
        byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
        PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());

        Set<Identifier> csrIdentifiers = CsrDataUtil.getCsrIdentifiersAndVerifyWithIdentifiers(csr, order.getOrderIdentifiers());
        Provisioner provisioner = ProvisionerManager.getProvisionerForName(order.getAccount().getProvisioner());

                        /*
                            We just use the DNS Domain Names (Subject Alternative Name) and the public key of the CSR. We're not using
                            the Basic Constrain etc.
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

        // Set certificate details
        order.setCertificateSerialNumber(serialNumber);
        order.setCertificatePem(pemCertificate);
        order.setExpires(expiresAt);
        order.setCertificateIssued(issuedAt);

        order.setOrderState(AcmeOrderState.IDLE); // Set it back to idle
        session.merge(order);

        transaction.commit();

        log.info("Stored certificate successful");
    }

    private record CertificateIssuingTask(ServerInstance serverInstance) implements Runnable {

        @SuppressFBWarnings("REC_CATCH_EXCEPTION")
        @Override
        public void run() {
            log.info("Certificate issuing thread started!");

            while (!Thread.currentThread().isInterrupted()) {
                log.trace("Looking for certificates to be issued in the database");

                List<ACMEOrder> waitingOrders = ACMEOrder.getAllACMEOrdersWithState(AcmeOrderState.NEED_A_CERTIFICATE, serverInstance);

                if (!waitingOrders.isEmpty()) {

                    try (Session session = Objects.requireNonNull(serverInstance.getHibernateUtil().getSessionFactory()).openSession()) {

                        // CryptoStoreManager csm = CryptoStoreManager;

                        ACMEOrder order = waitingOrders.get(0);
                        generateCertificateForOrder(order, serverInstance.getCryptoStoreManager(), session);
                    } catch (Exception ex) {
                        log.error("Error generating and/or store certificate", ex);
                    }
                } else {

                    // Waiting for new CSRs and try in a few seconds again
                    try {

                        Thread.sleep(20 * 1000); // Sleep 20 seconds
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
