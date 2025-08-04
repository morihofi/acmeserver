/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.ocsp;


import de.morihofi.certgine.cryptography.keys.KeyHelper;
import de.morihofi.certgine.types.database.entities.acme.AcmeProvisioner;
import de.morihofi.certgine.types.database.entities.acme.AcmeOrder;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
import de.morihofi.certgine.types.intf.IServerInstance;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * OCSP Utility class
 */
@Slf4j
public class OcspProcessor {

    /**
     * Processes an OCSP (Online Certificate Status Protocol) request for a given
     * certificate serial number. The certificate status is resolved directly from
     * the database and the appropriate OCSP response is generated.
     *
     * @param serialNumber The serial number of the certificate for which the OCSP response is requested.
     * @param provisioner  Provisioner Instance
     * @return An OCSPResp object representing the OCSP response for the given certificate.
     * @throws OCSPException                if there is an issue with OCSP processing.
     * @throws CertificateEncodingException if there is an issue with encoding certificates.
     * @throws OperatorCreationException    if there is an issue with operator creation.
     * @throws NoSuchAlgorithmException     if there is an issue with signing algorithm.
     * @throws UnrecoverableKeyException    if there is an issue recovering the key.
     * @throws KeyStoreException            if there is an issue with the keystore.
     */
    public static OCSPResp processOCSPRequest(BigInteger serialNumber, @NonNull AcmeProvisioner provisioner, @NonNull IServerInstance serverInstance) throws
            OCSPException, CertificateEncodingException, OperatorCreationException, KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException {

        RevokedCertificate rc = AcmeOrder.getRevokedCertificate(serialNumber, provisioner.getName(), serverInstance);
        CertificateStatus certStatus;
        if (rc != null) {
            certStatus = new RevokedStatus(rc.revocationDate(), rc.revocationReason());
        } else {
            certStatus = CertificateStatus.GOOD;
        }

        log.info("Status for serial number {} is: {}", serialNumber, (certStatus != CertificateStatus.GOOD ? "revoked" : "valid"));

        ICryptoStoreManager csm = serverInstance.getCryptoStoreManager();

        X509Certificate caCert  = serverInstance.getCryptoStoreManager()
                .getIntermediateCertificate(provisioner.getInternalUuid());
        KeyPair caKeyPair       = serverInstance.getCryptoStoreManager()
                .getIntermediateCertificateAuthorityKeyPair(provisioner.getInternalUuid());

        // Creating the OCSP response
        SubjectPublicKeyInfo caPublicKeyInfo = SubjectPublicKeyInfo.getInstance(caCert.getPublicKey().getEncoded());
        DigestCalculator digCalc = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        BasicOCSPRespBuilder respBuilder = new BasicOCSPRespBuilder(caPublicKeyInfo, digCalc);

        respBuilder.addResponse(new CertificateID(digCalc,
                new JcaX509CertificateHolder(caCert), serialNumber), certStatus);

        // Creating and signing the OCSP response
        Instant producedAt = Instant.now();
        BasicOCSPResp basicResp = respBuilder.build(
                new JcaContentSignerBuilder(KeyHelper.getSignatureAlgorithmBasedOnKeyType(caKeyPair.getPrivate())).build(
                        caKeyPair.getPrivate()),
                new X509CertificateHolder[]{new JcaX509CertificateHolder(caCert)},
                Date.from(producedAt));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
        log.debug("OCSP response produced at {}", formatter.format(producedAt));

        return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicResp);
    }
}
