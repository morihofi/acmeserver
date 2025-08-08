package de.morihofi.certgine.cryptography.certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;

/**
 * Generates an {@link X509Certificate} based on the given request.
 */
public interface ICertificateGenerator {
    /**
     * Creates a certificate using the provided request parameters.
     *
     * @param req generation request
     * @return generated certificate
     * @throws CertificateException      if certificate creation fails
     * @throws OperatorCreationException if the content signer cannot be created
     * @throws CertIOException           if certificate extensions cannot be added
     */
    X509Certificate generate(X509Generator.Request req)
            throws CertificateException, OperatorCreationException, CertIOException;
}

