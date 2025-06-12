package de.morihofi.acmeserver.cryptography.revoke;

import de.morihofi.acmeserver.cryptography.keys.KeyHelper;
import de.morihofi.acmeserver.types.cryptography.revoke.RevokedCertificate;
import lombok.*;
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
import java.util.Date;
import java.util.List;

/**
 * Generates X509 certificate revocation lists using a request built via Lombok's builder.
 */
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
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(
                new JcaX509CertificateHolder(req.getCaCert()).getSubject(),
                new Date());
        crlBuilder.setNextUpdate(new Date(System.currentTimeMillis() + req.getUpdateMinutes() * 60L * 1000));

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
