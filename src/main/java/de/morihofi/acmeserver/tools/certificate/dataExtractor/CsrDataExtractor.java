package de.morihofi.acmeserver.tools.certificate.dataExtractor;

import de.morihofi.acmeserver.tools.base64.Base64Tools;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsrDataExtractor {

    /**
     * Extracts domain names from a Certificate Signing Request (CSR) in PEM format.
     *
     * @param csr The Certificate Signing Request in PEM format.
     * @return A list of domain names (Subject Alternative Names) extracted from the CSR. Including DNS and IP Adresses
     * @throws IOException If an error occurs while processing the CSR.
     */
    public static List<String> getDomainsAndIPsFromCSR(String csr) throws IOException {
        byte[] csrBytes = Base64Tools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest certRequest = new PKCS10CertificationRequest(csrBytes);

        List<String> domainAndIpList = new ArrayList<>();

        // Extract the SAN extension
        Extension sanExtension = certRequest.getRequestedExtensions().getExtension(Extension.subjectAlternativeName);
        if (sanExtension != null) {
            GeneralNames san = GeneralNames.getInstance(sanExtension.getParsedValue());
            GeneralName[] names = san.getNames();

            // Loop through all names in the SAN
            for (GeneralName name : names) {
                if (name.getTagNo() == GeneralName.dNSName || name.getTagNo() == GeneralName.iPAddress) {
                    String dnsName = name.getName().toString();
                    domainAndIpList.add(dnsName);
                }
            }
        }


        return domainAndIpList;
    }
}
