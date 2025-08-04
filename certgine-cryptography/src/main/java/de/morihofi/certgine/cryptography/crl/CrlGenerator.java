/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.cryptography.crl;

import de.morihofi.certgine.cryptography.keys.KeyHelper;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Generates X509 certificate revocation lists using a request built via Lombok's builder.
 */
@Slf4j
public class CrlGenerator {

    /**
     * Request parameters for CRL generation.
     */
    @Getter
    @Builder
    public static class Request {
        /** List of certificates to revoke. */
        @Singular
        @NonNull private final List<RevokedCertificate> revokedCertificates;
        /** Issuer certificate used to sign the CRL. */
        @NonNull private final X509Certificate caCert;
        /** Private key of the issuer. */
        @NonNull private final PrivateKey caPrivateKey;
        /** Minutes after which the CRL should be updated. */
        private final int updateMinutes;
    }

    /**
     * Generate the CRL described by the provided {@link Request}.
     */
    public static X509CRL generate(@NonNull Request req)
            throws CertificateEncodingException, CRLException, OperatorCreationException {
        Instant now = Instant.now();
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(
                new JcaX509CertificateHolder(req.getCaCert()).getSubject(),
                Date.from(now));
        Instant nextUpdate = now.plus(Duration.ofMinutes(req.getUpdateMinutes()));
        crlBuilder.setNextUpdate(Date.from(nextUpdate));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));
        log.debug("CRL thisUpdate {} nextUpdate {}", formatter.format(now), formatter.format(nextUpdate));

        for (RevokedCertificate rc : req.getRevokedCertificates()) {
            crlBuilder.addCRLEntry(rc.serialNumber(), rc.revocationDate(), rc.revocationReason());
        }

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(
                KeyHelper.getSignatureAlgorithmBasedOnKeyType(req.getCaPrivateKey()));
        signerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        X509CRLHolder crlHolder = crlBuilder.build(signerBuilder.build(req.getCaPrivateKey()));
        JcaX509CRLConverter converter = new JcaX509CRLConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
        return converter.getCRL(crlHolder);
    }

    private CrlGenerator() {
    }
}
