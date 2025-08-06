/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.acme.certificate.queue;

import de.morihofi.certgine.acme.csr.AcmeCsrValidator;
import de.morihofi.certgine.types.api.acme.dns.Identifier;

import de.morihofi.certgine.acme.types.entities.enums.AcmeOrderState;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.types.cryptography.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.utils.base64.Base64Tools;
import de.morihofi.certgine.cryptography.pem.PemUtil;
import de.morihofi.certgine.cryptography.certificate.X509Generator;
import de.morihofi.certgine.utils.network.dns.CAAValidator;
import de.morihofi.certgine.types.exception.exceptions.ACMECaaException;
import de.morihofi.certgine.acme.types.events.BeforeAcmeCertificateCreatedEvent;
import de.morihofi.certgine.acme.types.events.AcmeCertificateCreatedEvent;
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
import java.time.Instant;
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

        Set<Identifier> csrIdentifiers = AcmeCsrValidator.getCsrIdentifiersAndVerifyWithIdentifiers(csr, order.getOrderIdentifiers());
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

        Instant notBefore = order.getNotBefore();
        Instant notAfter = order.getNotAfter();

        X509Certificate acmeGeneratedCertificate = X509Generator.generate(
                X509Generator.Request.builder()
                        .type(X509Generator.Type.SERVER)
                        .issuerKeyPair(cryptoStoreManager.getIntermediateCertificateAuthorityKeyPair(provisioner.getInternalUuid()))
                        .issuerCertificate(cryptoStoreManager.getIntermediateCertificate(provisioner.getInternalUuid()))
                        .serverPublicKeyBytes(pkPemObject.getContent())
                        .identifiers(csrIdentifiers)
                        .startDate(java.util.Date.from(notBefore))
                        .endDate(java.util.Date.from(notAfter))
                        .ocspServiceEndpoint(provisioner.getFullOcspUrl(serverInstance))
                        .crlDistributionUrl(provisioner.getFullCrlUrl(serverInstance))
                        .serverInstance(serverInstance)
                        .build()
        );

        BigInteger serialNumber = acmeGeneratedCertificate.getSerialNumber();
        String pemCertificate = PemUtil.certificateToPEM(acmeGeneratedCertificate.getEncoded());

        Instant expiresAt = acmeGeneratedCertificate.getNotAfter().toInstant();
        Instant issuedAt = acmeGeneratedCertificate.getNotBefore().toInstant();

        Transaction transaction = session.beginTransaction();

        // Set certificate details
        order.setCertificateSerialNumber(serialNumber);
        order.setCertificatePem(pemCertificate);
        order.setExpires(expiresAt);
        order.setCertificateIssued(issuedAt);
        order.setCertificateExpires(expiresAt);

        order.setOrderState(AcmeOrderState.IDLE); // Set it back to idle
        session.merge(order);

        transaction.commit();
        serverInstance.getEventBus().publish(new AcmeCertificateCreatedEvent(order, acmeGeneratedCertificate));

        log.info("Stored certificate successful");
    }

}
