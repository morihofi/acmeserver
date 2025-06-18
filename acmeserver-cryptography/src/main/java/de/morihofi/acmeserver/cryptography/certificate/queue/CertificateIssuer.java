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

package de.morihofi.acmeserver.cryptography.certificate.queue;

import de.morihofi.acmeserver.cryptography.csr.CsrDataUtil;
import de.morihofi.acmeserver.types.api.acme.dns.Identifier;

import de.morihofi.acmeserver.types.database.enums.AcmeOrderState;
import de.morihofi.acmeserver.types.database.entities.AcmeOrder;
import de.morihofi.acmeserver.types.database.entities.AcmeProvisioner;
import de.morihofi.acmeserver.types.intf.ICryptoStoreManager;
import de.morihofi.acmeserver.types.intf.IServerInstance;
import de.morihofi.acmeserver.utils.base64.Base64Tools;
import de.morihofi.acmeserver.cryptography.pem.PemUtil;
import de.morihofi.acmeserver.cryptography.certificate.X509Generator;
import de.morihofi.acmeserver.utils.network.dns.CAAValidator;
import de.morihofi.acmeserver.types.exception.exceptions.ACMECaaException;
import de.morihofi.acmeserver.types.events.BeforeAcmeCertificateCreatedEvent;
import de.morihofi.acmeserver.types.events.AcmeCertificateCreatedEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;

@Slf4j
public class CertificateIssuer {


    public static void generateCertificateForOrder(@NonNull AcmeOrder order, @NonNull ICryptoStoreManager cryptoStoreManager, @NonNull Session session, @NonNull IServerInstance serverInstance) throws
            IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            OperatorCreationException {
        String csr = order.getCertificateCSR();
        // Decode the CSR from the Request
        byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest csrObj = new PKCS10CertificationRequest(csrBytes);
        PemObject pkPemObject = new PemObject("PUBLIC KEY", csrObj.getSubjectPublicKeyInfo().getEncoded());

        Set<Identifier> csrIdentifiers = CsrDataUtil.getCsrIdentifiersAndVerifyWithIdentifiers(csr, order.getOrderIdentifiers());
        AcmeProvisioner provisioner = order.getAccount().getAcmeProvisioner();

        // Perform CAA checks for each DNS identifier
        String caDomain = serverInstance.getAppConfig().getServer().getDnsName();
        for (Identifier id : csrIdentifiers) {
            if (id.getTypeAsEnumConstant() == Identifier.IDENTIFIER_TYPE.DNS) {
                boolean allowed = CAAValidator.isIssuanceAllowed(
                        id.getValue(),
                        caDomain,
                        serverInstance.getAppConfig().getNetwork().getDnsConfig(),
                        serverInstance.getNetworkClient());
                if (!allowed) {
                    throw new ACMECaaException("CAA forbids issuance for domain " + id.getValue());
                }
            }
        }

        serverInstance.getEventBus().publish(new BeforeAcmeCertificateCreatedEvent(order));

        /*
            We just use the DNS Domain Names (Subject Alternative Name) and the public key of the CSR. We're not using
            the Basic Constrain etc., because this is defined by the CA that we are
        */

        log.info("Creating Certificate for order \"{}\" with DNS Names {}", order.getOrderId(),
                String.join(", ", csrIdentifiers.stream()
                        .map(identifier -> identifier.getTypeAsEnumConstant().toString() + ":" + identifier.getValue())
                        .toList()
                )
        );

        X509Certificate acmeGeneratedCertificate = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.SERVER)
                        .issuerKeyPair(provisioner.getIntermediateCaKeyPair(cryptoStoreManager))
                        .issuerCertificate(provisioner.getIntermediateCaCertificate(cryptoStoreManager))
                        .serverPublicKeyBytes(pkPemObject.getContent())
                        .identifiers(csrIdentifiers)
                        .startDate(order.getNotBefore())
                        .endDate(order.getNotAfter())
                        .provisioner(provisioner)
                        .serverInstance(serverInstance)
                        .build()
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
        serverInstance.getEventBus().publish(new AcmeCertificateCreatedEvent(order, acmeGeneratedCertificate));

        log.info("Stored certificate successful");
    }

    // utility class only
}
