package de.morihofi.acmeserver.ui.frontend.legacy.util;

import de.morihofi.acmeserver.utils.conversion.HexConverter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Utility functions for certificate related tasks used by the legacy UI.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CertificateUtil {

    /**
     * Calculates the fingerprint of the provided certificate using the
     * specified hashing algorithm.
     *
     * @param certificate certificate to hash
     * @param algorithm   hashing algorithm (e.g. "SHA-1" or "SHA-256")
     * @return fingerprint as hexadecimal string
     * @throws CertificateEncodingException if the certificate cannot be encoded
     */
    @NonNull
    public static String getFingerprint(@NonNull X509Certificate certificate,
                                         @NonNull String algorithm)
            throws CertificateEncodingException {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(certificate.getEncoded());
            return HexConverter.bytesAsHexString(digest);
        } catch (Exception ex) {
            throw new CertificateEncodingException("Could not calculate fingerprint", ex);
        }
    }
}
