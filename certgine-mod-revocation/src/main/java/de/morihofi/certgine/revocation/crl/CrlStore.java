/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.crl;

import de.morihofi.certgine.revocation.RevocationStore;
import de.morihofi.certgine.cryptography.crl.CrlGenerator;
import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.RevokedStatus;

import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Slf4j
/**
 * Manages a cached Certificate Revocation List (CRL) for the CA. The store
 * allows the CRL to be generated, cached and retrieved for serving to clients
 * and for certificate status checks.
 */
public class CrlStore {

    /**
     * Cached CRL entry.
     */
    public static CrlEntry entry;

    /**
     * Updates the cache of the Certificate Revocation List (CRL) for the given
     * provisioner. The revoked certificates are retrieved from the database and a
     * new CRL is generated and stored.
     *
     * @param updateMinutes the validity period of the generated CRL in minutes
     * @param serverInstance the current server instance providing access to the
     *                      cryptographic material
     */

    public static void updateCachedCRL(int updateMinutes, @NonNull IServerInstance serverInstance) {
        try {

            // Get the list of revoked certificates from the store
            List<RevokedCertificate> revokedCertificates = RevocationStore.getRevokedCertificates(serverInstance);
            // Generate a new CRL
            X509CRL crl = CrlGenerator.generate(
                    CrlGenerator.Request.builder()
                            .revokedCertificates(revokedCertificates)
                            .caCert(serverInstance.getCryptoStoreManager()
                                    .getCertificateAuthorityX509Certificate(serverInstance.getRootCa()))
                            .caPrivateKey(serverInstance.getCryptoStoreManager()
                                    .getCertificateAuthorityKeyPair(serverInstance.getRootCa()).getPrivate())
                            .updateMinutes(updateMinutes)
                            .build());

            // Update cache
            entry = new CrlEntry(LocalTime.now(), crl);
        } catch (Exception e) {
            // Handle exceptions
            log.error("Unable to update CRL revocation list", e);
        }
    }

    /**
     * Retrieves the cached CRL entry.
     *
     * @return the cached CRL entry
     * @throws IllegalArgumentException if no CRL is available
     */
    @NonNull
    public static CrlEntry getCrl() {
        if (entry == null) {
            throw new IllegalArgumentException("No CRL available");
        }
        return entry;
    }

    /**
     * Determines the status of a certificate by its serial number, using a provided Certificate Revocation List (CRL). This method checks
     * if the specified certificate has been revoked according to the current CRL provided by the {@code crlGenerator}. If the certificate
     * is found in the CRL, it is considered revoked, and the method returns a {@link RevokedStatus} instance containing the revocation date
     * and reason. If the certificate is not found in the CRL, it is considered valid, and the method returns
     * {@link CertificateStatus#GOOD}.
     * <p>
     * The method uses the {@code serialNumber} to look up the certificate in the CRL. The revocation reason is determined by checking if
     * the revoked certificate entry has extensions; if so, it uses the ordinal of the {@link CRLReason} enum value. If there are no
     * extensions, the reason defaults to {@code CRLReason.unspecified}.
     *
     * @param serialNumber The serial number of the certificate to check the status for.
     * @return A {@link CertificateStatus} indicating whether the certificate is valid or revoked. If revoked, additional details such as
     * the revocation date and reason are provided.
     * @throws CRLException If there is an issue obtaining the current CRL from the {@code crlGenerator}.
     */
    @NonNull
    static CertificateStatus getCertificateStatus(BigInteger serialNumber) throws CRLException {
        X509CRL crl = getCrl().currentCrl(); // Current CRL

        CertificateStatus certStatus;
        X509CRLEntry revokedCertificate = crl.getRevokedCertificate(serialNumber);
        // Checking the certificate status using the CRL
        if (revokedCertificate != null) {
            // Certificate has been revoked
            Date revocationDate = revokedCertificate.getRevocationDate();
            int revocationReason =
                    revokedCertificate.hasExtensions() ? revokedCertificate.getRevocationReason().ordinal() : CRLReason.unspecified;
            certStatus = new RevokedStatus(revocationDate, revocationReason);
        } else {
            // Certificate is valid
            certStatus = CertificateStatus.GOOD;
        }
        return certStatus;
    }

    /**
     * Represents a cached CRL along with the time it was last generated.
     *
     * @param lastUpdate the time the CRL was generated
     * @param currentCrl the current CRL for the provisioner
     */
    public record CrlEntry(LocalTime lastUpdate, X509CRL currentCrl) {
        /**
         * Converts a given X509CRL object to its byte array representation. This method is useful for encoding the CRL for storage or
         * transmission.
         *
         * @return a byte array representing the encoded form of the provided CRL
         * @throws CRLException if there is an error in encoding the CRL
         */
        public byte @NonNull [] getCrlAsBytes() throws CRLException {
            // Return the CRL as a byte array
            return currentCrl.getEncoded();
        }
    }
}
