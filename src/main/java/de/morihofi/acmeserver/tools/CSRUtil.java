package de.morihofi.acmeserver.tools;

import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSRUtil {
    public static List<String> getDomainsFromCSR(String csr) throws IOException {
        byte[] csrBytes = CertTools.decodeBase64URLAsBytes(csr);
        PKCS10CertificationRequest certRequest = new PKCS10CertificationRequest(csrBytes);

        List<String> domainList = new ArrayList<>();

        // Extract the SAN extension
        Extension sanExtension = certRequest.getRequestedExtensions().getExtension(Extension.subjectAlternativeName);
        if (sanExtension != null) {
            GeneralNames san = GeneralNames.getInstance(sanExtension.getParsedValue());
            GeneralName[] names = san.getNames();

            // Loop through all names in the SAN
            for (GeneralName name : names) {
                if (name.getTagNo() == GeneralName.dNSName) {
                    String dnsName = name.getName().toString();
                    domainList.add(dnsName);
                }
            }
        }


        return domainList;
    }
}
