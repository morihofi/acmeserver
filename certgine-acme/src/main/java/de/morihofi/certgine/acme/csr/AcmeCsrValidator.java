package de.morihofi.certgine.acme.csr;

import de.morihofi.certgine.cryptography.csr.CsrDataUtil;
import de.morihofi.certgine.types.api.acme.dns.Identifier;
import de.morihofi.certgine.acme.types.entities.AcmeOrderIdentifier;
import de.morihofi.certgine.acme.types.entities.enums.AcmeStatus;
import de.morihofi.certgine.types.exception.exceptions.ACMEBadCsrException;
import de.morihofi.certgine.types.exception.exceptions.ACMEServerInternalException;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Utility for validating CSR identifiers against ACME order identifiers.
 */
public final class AcmeCsrValidator {

    private AcmeCsrValidator() {
    }

    /**
     * Extracts identifiers from the CSR and verifies them against the ACME order identifiers.
     *
     * @param csr          CSR encoded as Base64URL string
     * @param identifiers  identifiers associated with the ACME order
     * @return set of identifiers contained in the CSR
     * @throws IOException if the CSR cannot be parsed
     */
    @NonNull
    public static Set<@NonNull Identifier> getCsrIdentifiersAndVerifyWithIdentifiers(
            String csr, List<AcmeOrderIdentifier> identifiers) throws IOException {
        Set<Identifier> csrDomainNames = CsrDataUtil.getDomainsAndIPsFromCSR(csr);
        if (csrDomainNames.isEmpty()) {
            throw new ACMEBadCsrException("CSR does not contain any identifiers");
        }

        boolean allIdentifiersValid = identifiers.stream()
                .allMatch(id -> id.getChallengeStatus() == AcmeStatus.VALID);
        if (!allIdentifiersValid) {
            throw new ACMEServerInternalException("Not all ACME identifiers were validated");
        }

        List<String> identifierValues = identifiers.stream()
                .map(AcmeOrderIdentifier::getDataValue)
                .toList();

        boolean allDomainsMatch = csrDomainNames.stream()
                .map(Identifier::getValue)
                .allMatch(identifierValues::contains);

        if (!allDomainsMatch) {
            throw new ACMEBadCsrException("One or more CSR domains do not match the ACME identifiers");
        }

        return csrDomainNames;
    }
}
