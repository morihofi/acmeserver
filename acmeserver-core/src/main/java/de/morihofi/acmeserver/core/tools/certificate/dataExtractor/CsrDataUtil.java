/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.core.tools.certificate.dataExtractor;

import de.morihofi.acmeserver.core.certificate.acme.api.endpoints.objects.Identifier;
import de.morihofi.acmeserver.core.database.AcmeStatus;
import de.morihofi.acmeserver.core.database.objects.ACMEOrderIdentifier;
import de.morihofi.acmeserver.core.exception.exceptions.ACMEBadCsrException;
import de.morihofi.acmeserver.core.exception.exceptions.ACMEServerInternalException;
import de.morihofi.acmeserver.core.tools.base64.Base64Tools;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsrDataUtil {

    /**
     * Extracts domain names from a Certificate Signing Request (CSR) in PEM format.
     *
     * @param csr The Certificate Signing Request in PEM format.
     * @return A list of domain names (Subject Alternative Names) extracted from the CSR. Including DNS and IP Adresses
     * @throws IOException If an error occurs while processing the CSR.
     */
    public static Set<Identifier> getDomainsAndIPsFromCSR(String csr) throws IOException {
        byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest certRequest = new PKCS10CertificationRequest(csrBytes);

        Set<Identifier> domainAndIpList = new HashSet<>();

        // Extract the subject DN to get the Common Name (CN)
        X500Name subject = certRequest.getSubject();
        RDN[] cnRDNs = subject.getRDNs(BCStyle.CN);  // Assumes there's only one CN RDN
        if(cnRDNs.length != 0){
            RDN cnRDN = cnRDNs[0];
            String commonName = cnRDN.getFirst().getValue().toString();
            domainAndIpList.add(new Identifier(Identifier.IDENTIFIER_TYPE.DNS, commonName));
        }


        // Extract the SAN extension
        Extension sanExtension = certRequest.getRequestedExtensions().getExtension(Extension.subjectAlternativeName);
        if (sanExtension != null) {
            GeneralNames san = GeneralNames.getInstance(sanExtension.getParsedValue());
            GeneralName[] names = san.getNames();

            // Loop through all names in the SAN
            for (GeneralName name : names) {
                if (name.getTagNo() == GeneralName.dNSName) {
                    String dnsName = name.getName().toString();
                    domainAndIpList.add(new Identifier(Identifier.IDENTIFIER_TYPE.DNS, dnsName));
                } else if (name.getTagNo() == GeneralName.iPAddress) {
                    // Convert the octet sequence into a human-readable IP address
                    byte[] ip = ASN1OctetString.getInstance(name.getName()).getOctets();
                    String ipAddress = convertToIP(ip);
                    domainAndIpList.add(new Identifier(Identifier.IDENTIFIER_TYPE.IP, ipAddress));
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

    public static Set<Identifier> getCsrIdentifiersAndVerifyWithIdentifiers(
            String csr, List<ACMEOrderIdentifier> identifiers) throws IOException {
        // Extract CSR Domain Names
        Set<Identifier> csrDomainNames = getDomainsAndIPsFromCSR(csr);
        if (csrDomainNames.isEmpty()) {
            throw new ACMEBadCsrException("CSR does not contain any identifiers");
        }

        // Verify all ACME Identifiers are validated
        boolean allIdentifiersValid = identifiers.stream()
                .allMatch(acmeOrderIdentifier -> acmeOrderIdentifier.getChallengeStatus() == AcmeStatus.VALID);
        if (!allIdentifiersValid) {
            throw new ACMEServerInternalException("Not all ACME identifiers were validated");
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
