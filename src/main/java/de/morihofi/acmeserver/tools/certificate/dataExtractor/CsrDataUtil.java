package de.morihofi.acmeserver.tools.certificate.dataExtractor;

import de.morihofi.acmeserver.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.database.AcmeStatus;
import de.morihofi.acmeserver.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.tools.base64.Base64Tools;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class CsrDataUtil {

    /**
     * Extracts domain names from a Certificate Signing Request (CSR) in PEM format.
     *
     * @param csr The Certificate Signing Request in PEM format.
     * @return A list of domain names (Subject Alternative Names) extracted from the CSR. Including DNS and IP Adresses
     * @throws IOException If an error occurs while processing the CSR.
     */
    public static List<Identifier> getDomainsAndIPsFromCSR(String csr) throws IOException {
        byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest certRequest = new PKCS10CertificationRequest(csrBytes);

        List<Identifier> domainAndIpList = new ArrayList<>();

        // Extract the SAN extension
        Extension sanExtension = certRequest.getRequestedExtensions().getExtension(Extension.subjectAlternativeName);
        if (sanExtension != null) {
            GeneralNames san = GeneralNames.getInstance(sanExtension.getParsedValue());
            GeneralName[] names = san.getNames();

            // Loop through all names in the SAN
            for (GeneralName name : names) {
                if (name.getTagNo() == GeneralName.dNSName) {
                    String dnsName = name.getName().toString();
                    domainAndIpList.add(new Identifier(Identifier.IDENTIFIER_TYPE.DNS.name(), dnsName));
                } else if (name.getTagNo() == GeneralName.iPAddress) {
                    // Convert the octet sequence into a human-readable IP address
                    byte[] ip = DEROctetString.getInstance(name.getName()).getOctets();
                    String ipAddress = convertToIP(ip);
                    domainAndIpList.add(new Identifier(Identifier.IDENTIFIER_TYPE.IP.name(), ipAddress));
                }

            }
        }


        return domainAndIpList;
    }


    /**
     * Converts a byte array into a human-readable IP address.
     *
     * @param ip The IP address as a byte array.
     * @return The IP address as a string.
     */
    private static String convertToIP(byte[] ip) throws UnknownHostException {
        InetAddress ipAddress = InetAddress.getByAddress(ip);
        return ipAddress.getHostAddress();
    }

    public static List<Identifier> getCsrIdentifiersAndVerifyWithIdentifiers(
            String csr, List<ACMEOrderIdentifier> identifiers) throws IOException {
        // Extract CSR Domain Names
        List<Identifier> csrDomainNames = getDomainsAndIPsFromCSR(csr);
        if (csrDomainNames.isEmpty()) {
            throw new ACMEBadCsrException("CSR does not contain any domain names");
        }

        // Verify all ACME Identifiers are validated
        boolean allIdentifiersValid = identifiers.stream()
                .allMatch(acmeOrderIdentifier -> acmeOrderIdentifier.getChallengeStatus() == AcmeStatus.VALID);
        if (!allIdentifiersValid) {
            throw new ACMEServerInternalException("Not all ACME Identifiers were validated");
        }

        // Verify CSR domains match ACME identifiers
        List<String> identifierValues = identifiers.stream()
                .map(ACMEOrderIdentifier::getDataValue)
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
