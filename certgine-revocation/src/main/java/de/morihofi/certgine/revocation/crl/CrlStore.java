/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.revocation.crl;

import de.morihofi.certgine.types.cryptography.revoke.RevokedCertificate;
import de.morihofi.certgine.acme.types.entities.AcmeOrder;
import de.morihofi.certgine.acme.types.entities.AcmeProvisioner;
import de.morihofi.certgine.cryptography.crl.CrlGenerator;
import de.morihofi.certgine.types.intf.ICryptoStoreManager;
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
import java.util.*;

@Slf4j
public class CrlStore {

    public static final Map<String, CrlEntry> entryMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Updates the cache of the Certificate Revocation List (CRL). This method retrieves the list of revoked certificates from the database,
     * generates a new CRL based on the retrieved data, and updates the current CRL cache. It also logs the time of the last update and
     * handles any exceptions that occur during the process.
     */

    public static void updateCachedCRL(int updateMinutes, @NonNull AcmeProvisioner provisioner, @NonNull IServerInstance serverInstance) {
        try {

            ICryptoStoreManager csm = serverInstance.getCryptoStoreManager();

            // Get the list of revoked certificates from the database
            List<RevokedCertificate> revokedCertificates = AcmeOrder.getRevokedCertificates(provisioner.getName(), serverInstance);
            // Generate a new CRL
            X509CRL crl = CrlGenerator.generate(
                    CrlGenerator.Request.builder()
                            .revokedCertificates(revokedCertificates)
                            .caCert(csm.getIntermediateCertificate(provisioner.getInternalUuid()))
                            .caPrivateKey(csm.getIntermediateCertificateAuthorityKeyPair(provisioner.getInternalUuid()).getPrivate())
                            .updateMinutes(updateMinutes)
                            .build());

            // Update cache
            entryMap.put(provisioner.getName(), new CrlEntry(LocalTime.now(), crl));
        } catch (Exception e) {
            // Handle exceptions
            log.error("Unable to update CRL revocation list", e);
        }
    }

    @NonNull
    public static CrlEntry getCrlForProvisioner(@NonNull String provisionerName) {
        if (!entryMap.containsKey(provisionerName)) {
            throw new IllegalArgumentException(provisionerName + " has not an CRL available");
        }
        return entryMap.get(provisionerName);
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
     * @param serialNumber    The serial number of the certificate to check the status for.
     * @param provisionerName The provisioner instance used to obtain the current CRL.
     * @return A {@link CertificateStatus} indicating whether the certificate is valid or revoked. If revoked, additional details such as
     * the revocation date and reason are provided.
     * @throws CRLException If there is an issue obtaining the current CRL from the {@code crlGenerator}.
     */
    @NonNull
    static CertificateStatus getCertificateStatus(BigInteger serialNumber, @NonNull String provisionerName) throws CRLException {
        X509CRL crl = getCrlForProvisioner(provisionerName).currentCrl(); // Current CRL

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
